package org.openmbee.mms.localuser.controllers;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.openmbee.mms.core.config.AuthorizationConstants;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.core.exceptions.UnauthorizedException;
import org.openmbee.mms.core.utils.AuthenticationUtils;
import org.openmbee.mms.json.UserJson;
import org.openmbee.mms.localuser.security.UserCreateRequest;
import org.openmbee.mms.localuser.security.UserDetailsServiceImpl;
import org.openmbee.mms.localuser.security.UsersResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;

@RestController
@Tag(name = "Auth")
public class LocalUserController {

    private UserDetailsServiceImpl userDetailsService;

    @Autowired
    public LocalUserController(UserDetailsServiceImpl userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @PostMapping(value = "/user", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize(AuthorizationConstants.IS_MMSADMIN)
    public UserCreateRequest createUser(@RequestBody UserCreateRequest req) {
        try {
            userDetailsService.loadUserByUsername(req.getUsername());
        } catch (UsernameNotFoundException e) {
            userDetailsService.register(req);
            return req;
        }
        throw new BadRequestException("User already exists");
    }

    @GetMapping(value = "/users")
    @PreAuthorize("isAuthenticated()")
    public UsersResponse getUsers(@RequestParam(required = false) String user) {
        UsersResponse res = new UsersResponse();
        Collection<UserJson> users = new ArrayList<>();
        if (user != null) {
            users.add(userDetailsService.loadUserByUsername(user).getUser());
        } else {
            users = userDetailsService.getUsers();
        }
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
                userDetailsService.changeUserPassword(req.getUsername(), req.getPassword(), requesterAdmin);
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
