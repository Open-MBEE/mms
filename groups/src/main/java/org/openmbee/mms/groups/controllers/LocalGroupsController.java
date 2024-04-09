package org.openmbee.mms.groups.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

import org.openmbee.mms.core.config.AuthorizationConstants;
import org.openmbee.mms.core.dao.GroupPersistence;
import org.openmbee.mms.core.dao.UserGroupsPersistence;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.exceptions.ConflictException;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.groups.constants.GroupConstants;
import org.openmbee.mms.groups.objects.*;
import org.openmbee.mms.groups.services.GroupValidationService;
import org.openmbee.mms.json.GroupJson;
import org.openmbee.mms.json.UserJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/groups")
@Tag(name = "Groups")
public class LocalGroupsController {

    private GroupPersistence groupPersistence;
    private UserGroupsPersistence userGroupsPersistence;
    private GroupValidationService groupValidationService;

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

    @PutMapping("/{group}")
    @PreAuthorize(AuthorizationConstants.IS_MMSADMIN)
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
    @PreAuthorize(AuthorizationConstants.IS_MMSADMIN)
    public GroupsResponse getAllGroups() {
        Collection<GroupJson> groups = groupPersistence.findAll();
        GroupsResponse response = new GroupsResponse();
        response.setGroups(groups.stream().map(GroupJson::getName).collect(Collectors.toList()));
        return response;
    }

    @GetMapping("/{group}")
    @PreAuthorize(AuthorizationConstants.IS_MMSADMIN)
    public GroupResponse getGroup(@PathVariable String group) {
        if(groupValidationService.isRestrictedGroup(group)) {
            throw new BadRequestException(GroupConstants.RESTRICTED_GROUP);
        }
        return new GroupResponse(groupPersistence.findByName(group).orElseThrow(() -> new NotFoundException(GroupConstants.GROUP_NOT_FOUND)),
            userGroupsPersistence.findUsersInGroup(group).stream().map(UserJson::getUsername).collect(Collectors.toList()));
    }

    @DeleteMapping("/{group}")
    @PreAuthorize(AuthorizationConstants.IS_MMSADMIN)
    @ResponseBody
    public void deleteLocalGroup(@PathVariable String group) {
        GroupJson groupJson = groupPersistence.findByName(group).orElseThrow(() -> new NotFoundException(GroupConstants.GROUP_NOT_FOUND));
        if (groupValidationService.canDeleteGroup(groupJson)) {
            groupPersistence.delete(groupJson);
        } else {
            throw new BadRequestException(GroupConstants.GROUP_NOT_EMPTY);
        }
    }

    @PostMapping("/{group}/users")
    @PreAuthorize(AuthorizationConstants.IS_MMSADMIN)
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
}
