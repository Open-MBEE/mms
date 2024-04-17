package org.openmbee.mms.groups.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.openmbee.mms.core.config.AuthorizationConstants;
import org.openmbee.mms.core.config.Privileges;
import org.openmbee.mms.core.dao.GroupPersistence;
import org.openmbee.mms.core.dao.UserGroupsPersistence;
import org.openmbee.mms.core.dao.UserPersistence;
import org.openmbee.mms.core.exceptions.*;
import org.openmbee.mms.core.objects.*;
import org.openmbee.mms.core.security.MethodSecurityService;
import org.openmbee.mms.core.services.PermissionService;
import org.openmbee.mms.groups.constants.GroupConstants;
import org.openmbee.mms.groups.objects.*;
import org.openmbee.mms.groups.services.GroupValidationService;
import org.openmbee.mms.json.GroupJson;
import org.openmbee.mms.json.OrgJson;
import org.openmbee.mms.json.UserJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/groups")
@Tag(name = "Groups")
@Transactional
public class LocalGroupsController {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected ObjectMapper om;

    private GroupPersistence groupPersistence;
    private GroupValidationService groupValidationService;
    private UserPersistence userPersistence;
    private UserGroupsPersistence userGroupsPersistence;

    protected PermissionService permissionService;

    protected MethodSecurityService mss;

    public Map<String, Object> convertToMap(Object obj) {
        return om.convertValue(obj, new TypeReference<Map<String, Object>>() {});
    }

    @Autowired
    public void setGroupPersistence(GroupPersistence groupPersistence) {
        this.groupPersistence = groupPersistence;
    }

    @Autowired
    public void setUserGroupsPersistence(UserGroupsPersistence userGroupsPersistence) {
        this.userGroupsPersistence = userGroupsPersistence;
    }

    @Autowired
    public void setGroupValidationService(GroupValidationService groupValidationService) {
        this.groupValidationService = groupValidationService;
    }

    @Autowired
    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Autowired
    public void setMss(MethodSecurityService mss) {
        this.mss = mss;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper om) {
        this.om = om;
    }

    @Autowired
    public void setUserPersistence(UserPersistence userPersistence) {
        this.userPersistence = userPersistence;
    }

    @PutMapping("/{group}")
    @PreAuthorize("isAuthenticated()")
    @ResponseBody
    public void createLocalGroup(@PathVariable String group) {
        GroupJson groupJson = groupPersistence.findByName(group).orElse(null);
        if (groupJson != null) {
            throw new ConflictException(GroupConstants.GROUP_ALREADY_EXISTS);
        }

        if (!groupValidationService.isValidGroupName(group)) {
            throw new BadRequestException(GroupConstants.INVALID_GROUP_NAME);
        }

        groupJson = new GroupJson();
        groupJson.setName(group);
        groupPersistence.save(groupJson);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public GroupsResponse getAllGroups(
        Authentication auth) {

        GroupsResponse response = new GroupsResponse();
        Collection<GroupJson> allGroups = groupPersistence.findAll();
        for (GroupJson group : allGroups) {
            if (mss.hasOrgPrivilege(auth, group.getName(), Privileges.GROUP_READ.name(), false)) {
                response.getGroups().add(group);
            }
        }
        return response;
    }

    @GetMapping(value = "/{group}")
    @PreAuthorize("@mss.hasGroupPrivilege(authentication, #group, 'GROUP_READ', true)")
    public GroupResponse getGroup(@PathVariable String group) {
        return new GroupResponse(groupPersistence.findByName(group).orElseThrow(() -> new NotFoundException(GroupConstants.GROUP_NOT_FOUND)),
            userGroupsPersistence.findUsersInGroup(group).stream().map(UserJson::getUsername).collect(Collectors.toList()));
    }

    @DeleteMapping("/{group}")
    @PreAuthorize("@mss.hasGroupPrivilege(authentication, #group, 'GROUP_DELETE', false)")
    @ResponseBody
    @Transactional
    public void deleteLocalGroup(@PathVariable String group) {
        GroupJson groupJson = groupPersistence.findByName(group).orElseThrow(() -> new NotFoundException(GroupConstants.GROUP_NOT_FOUND));
        if (groupValidationService.canDeleteGroup(groupJson)) {
            groupPersistence.delete(groupJson);
        } else {
            throw new BadRequestException(GroupConstants.GROUP_NOT_EMPTY);
        }
    }

    @PostMapping("/{group}/users")
    @PreAuthorize("@mss.hasGroupPrivilege(authentication, #group, 'GROUP_EDIT', true)")
    public GroupUpdateResponse updateGroupUsers(@PathVariable String group,
            @RequestBody GroupUpdateRequest groupUpdateRequest) {

        if (groupUpdateRequest.getAction() == null) {
            throw new BadRequestException(GroupConstants.INVALID_ACTION);
        }

        if (groupUpdateRequest.getUsers() == null ||
            groupUpdateRequest.getUsers().isEmpty()) {
            throw new BadRequestException(GroupConstants.NO_USERS_PROVIDED);
        }

        if (groupValidationService.isRestrictedGroup(group)) {
            throw new BadRequestException(GroupConstants.RESTRICTED_GROUP);
        }

        if(groupPersistence.findByName(group).isEmpty()) {
            throw new NotFoundException(GroupConstants.GROUP_NOT_FOUND);
        }
        GroupUpdateResponse response = new GroupUpdateResponse();
        response.setAdded(new ArrayList<>());
        response.setRemoved(new ArrayList<>());
        response.setRejected(new ArrayList<>());
        response.setGroup(group);

        groupUpdateRequest.getUsers().forEach(user -> {
            if (groupUpdateRequest.getAction() == Action.ADD) {
                if (!userGroupsPersistence.addUserToGroup(group, user)) {
                    response.getRejected().add(user);
                    return;
                }
                response.getAdded().add(user);
            } else { //REMOVE
                if (!userGroupsPersistence.removeUserFromGroup(group, user)) {
                    response.getRejected().add(user);
                    return;
                }
                response.getRemoved().add(user);
            }
        });
        return response;
    }

    protected void handleSingleResponse(BaseResponse<GroupsResponse> res) {
        if (res.getRejected() != null && !res.getRejected().isEmpty()) {
            List<Rejection> rejected = res.getRejected();
            int code = rejected.get(0).getCode();
            switch(code) {
                case 304:
                    throw new NotModifiedException(res);
                case 400:
                    throw new BadRequestException(res);
                case 401:
                    throw new UnauthorizedException(res);
                case 403:
                    throw new ForbiddenException(res);
                case 404:
                    throw new NotFoundException(res);
                case 409:
                    throw new ConflictException(res);
                case 410:
                    throw new DeletedException(res);
                case 500:
                    throw new InternalErrorException(res);
                default:
                    break;
            }
        }
    }
}
