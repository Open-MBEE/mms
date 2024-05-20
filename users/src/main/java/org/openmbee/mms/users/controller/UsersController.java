package org.openmbee.mms.users.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.openmbee.mms.core.config.AuthorizationConstants;
import org.openmbee.mms.core.dao.UserGroupsPersistence;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.core.exceptions.UnauthorizedException;
import org.openmbee.mms.core.utils.AuthenticationUtils;
import org.openmbee.mms.json.GroupJson;
import org.openmbee.mms.json.UserJson;
import org.openmbee.mms.users.security.DefaultUsersDetailsService;
import org.openmbee.mms.users.security.UsersDetails;
import org.openmbee.mms.users.objects.UserCreateRequest;
import org.openmbee.mms.users.objects.UserGroupsResponse;
import org.openmbee.mms.users.objects.UsersResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

@RestController
@Tag(name = "Auth")
public class UsersController {

    private DefaultUsersDetailsService usersDetailsService;

    private UserGroupsPersistence userGroupsPersistence;

    @Autowired
    public void setUserGroupsPersistence(UserGroupsPersistence userGroupsPersistence) {
        this.userGroupsPersistence = userGroupsPersistence;
    }

    @Autowired
    public void setUsersDetailsService(DefaultUsersDetailsService usersDetailsService) {
        this.usersDetailsService = usersDetailsService;
    }

    @PostMapping(value = "/users", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AuthorizationConstants.IS_MMSADMIN)
    public UsersResponse createOrUpdateUser(@RequestBody UserCreateRequest req) {
        UsersResponse res = new UsersResponse();
        Collection<UserJson> users = new ArrayList<>();
        
        UsersDetails userDetails;
        try {
            userDetails = usersDetailsService.loadUserByUsername(req.getUsername());
        } catch (UsernameNotFoundException e) {
            users.add(usersDetailsService.register(req));
            res.setUsers(users);
            return res;
        }
        users.add(usersDetailsService.update(req, userDetails.getUser()));
        res.setUsers(users);
        return res;
    }

    @GetMapping(value = "/users")
    @PreAuthorize("isAuthenticated()")
    public UsersResponse getUsers() {
        UsersResponse res = new UsersResponse();
        Collection<UserJson> users = new ArrayList<>();
        users.addAll(usersDetailsService.getUsers());
        res.setUsers(users);
        return res;
    }

    @GetMapping(value = "/users/:username")
    @PreAuthorize("isAuthenticated()")
    public UsersResponse getUser(@PathVariable String username) {
        UsersResponse res = new UsersResponse();
        UserJson user = usersDetailsService.loadUserByUsername(username).getUser();
        Collection<UserJson> users = new ArrayList<>();
        users.add(user);
        res.setUsers(users);
        return res;
    }

    @GetMapping(value = "/users/:username/groups")
    @PreAuthorize("isAuthenticated()")
    public UserGroupsResponse getUserGroups(@PathVariable String username) {
        return new UserGroupsResponse(usersDetailsService.loadUserByUsername(username).getUser(), userGroupsPersistence.findGroupsAssignedToUser(username).stream().map(GroupJson::getName).collect(Collectors.toList()));
    }

    @GetMapping(value = "/whoami")
    @PreAuthorize("isAuthenticated()")
    public UsersResponse getCurrentUser() {
        final Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String user = authentication.getName();
        UsersResponse res = new UsersResponse();
        Collection<UserJson> users = new ArrayList<>();
        users.add(usersDetailsService.loadUserByUsername(user).getUser());
        res.setUsers(users);
        return res;
    }

    @PostMapping(value = "/password", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public Object updatePassword(@RequestBody UserCreateRequest req,
        Authentication auth) {
        final String requester = auth.getName();
        final boolean requesterAdmin = AuthenticationUtils
            .hasGroup(auth, AuthorizationConstants.MMSADMIN);

        try {
            if (requesterAdmin || requester.equals(req.getUsername())) {
                usersDetailsService.changeUserPassword(req.getUsername(), req.getPassword(), requesterAdmin);
            } else {
                throw new UnauthorizedException("Not authorized");
            }

        } catch (UsernameNotFoundException e) {
            if (requesterAdmin) {
                throw new NotFoundException("User not found");
            } else {
                throw new UnauthorizedException("Not authorized");
            }
        }
        return "";
    }

}