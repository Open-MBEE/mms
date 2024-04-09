package org.openmbee.mms.permissions;

import io.swagger.v3.oas.annotations.tags.Tag;

import org.openmbee.mms.core.builders.PermissionUpdatesResponseBuilder;
import org.openmbee.mms.core.objects.PermissionUpdatesResponse;
import org.openmbee.mms.core.services.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Auth")
public class PermissionsController {

    PermissionService permissionService;

    @Autowired
    public PermissionsController(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @PostMapping(value = "/orgs/{orgId}/permissions", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@mss.hasOrgPrivilege(authentication, #orgId, 'ORG_UPDATE_PERMISSIONS', false)")
    public PermissionUpdatesResponse updateOrgPermissions(
        @PathVariable String orgId,
        @RequestBody PermissionsRequest req) {

        PermissionUpdatesResponseBuilder responseBuilder = new PermissionUpdatesResponseBuilder();

        if (req.getGroups() != null) {
            responseBuilder.insert(permissionService.updateOrgGroupPerms(req.getGroups(), orgId));
        }
        if (req.getUsers() != null) {
            responseBuilder.insert(permissionService.updateOrgUserPerms(req.getUsers(), orgId));
        }
        if (req.getPublic() != null) {
            responseBuilder.setPublic(permissionService.setOrgPublic(req.getPublic(), orgId));
        }
        return responseBuilder.getPermissionUpdatesReponse();
    }

    @PostMapping(value = "/projects/{projectId}/permissions", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@mss.hasProjectPrivilege(authentication, #projectId, 'PROJECT_UPDATE_PERMISSIONS', false)")
    public PermissionUpdatesResponse updateProjectPermissions(
        @PathVariable String projectId,
        @RequestBody PermissionsRequest req) {

        PermissionUpdatesResponseBuilder responseBuilder = new PermissionUpdatesResponseBuilder();

        if (req.getGroups() != null) {
            responseBuilder.insert(permissionService.updateProjectGroupPerms(req.getGroups(), projectId));
        }
        if (req.getUsers() != null) {
            responseBuilder.insert(permissionService.updateProjectUserPerms(req.getUsers(), projectId));
        }
        if (req.getPublic() != null) {
            responseBuilder.setPublic(permissionService.setProjectPublic(req.getPublic(), projectId));
        }
        if (req.getInherit() != null) {
            responseBuilder.insert(permissionService.setProjectInherit(req.getInherit(), projectId));
        }
        return responseBuilder.getPermissionUpdatesReponse();
    }

    @PostMapping(value = "/projects/{projectId}/refs/{refId}/permissions", consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@mss.hasBranchPrivilege(authentication, #projectId, #refId, 'BRANCH_UPDATE_PERMISSIONS', false)")
    public PermissionUpdatesResponse updateBranchPermissions(
        @PathVariable String projectId,
        @PathVariable String refId,
        @RequestBody PermissionsRequest req) {

        PermissionUpdatesResponseBuilder responseBuilder = new PermissionUpdatesResponseBuilder();

        if (req.getGroups() != null) {
            responseBuilder.insertGroups(permissionService.updateBranchGroupPerms(req.getGroups(), projectId, refId));
        }
        if (req.getUsers() != null) {
            responseBuilder.insertUsers(permissionService.updateBranchUserPerms(req.getUsers(), projectId, refId));
        }
        if (req.getInherit() != null) {
            responseBuilder.insert(permissionService.setBranchInherit(req.getInherit(), projectId, refId));
        }
        return responseBuilder.getPermissionUpdatesReponse();
    }

    @GetMapping(value = "/orgs/{orgId}/permissions")
    @PreAuthorize("@mss.hasOrgPrivilege(authentication, #orgId, 'ORG_READ_PERMISSIONS', true)")
    public PermissionsResponse getOrgPermissions(
        @PathVariable String orgId) {

        PermissionsResponse res = new PermissionsResponse();
        res.setGroups(permissionService.getOrgGroupRoles(orgId));
        res.setUsers(permissionService.getOrgUserRoles(orgId));
        res.setPublic(permissionService.isOrgPublic(orgId));
        return res;
    }

    @GetMapping(value = "/projects/{projectId}/permissions")
    @PreAuthorize("@mss.hasProjectPrivilege(authentication, #projectId, 'PROJECT_READ_PERMISSIONS', true)")
    public PermissionsResponse getProjectPermissions(
        @PathVariable String projectId) {

        PermissionsResponse res = new PermissionsResponse();
        res.setGroups(permissionService.getProjectGroupRoles(projectId));
        res.setUsers(permissionService.getProjectUserRoles(projectId));
        res.setPublic(permissionService.isProjectPublic(projectId));
        res.setInherit(permissionService.isProjectInherit(projectId));
        return res;
    }

    @GetMapping(value = "/projects/{projectId}/refs/{refId}/permissions")
    @PreAuthorize("@mss.hasBranchPrivilege(authentication, #projectId, #refId, 'BRANCH_READ_PERMISSIONS', true)")
    public PermissionsResponse getBranchPermissions(
        @PathVariable String projectId,
        @PathVariable String refId) {

        PermissionsResponse res = new PermissionsResponse();
        res.setGroups(permissionService.getBranchGroupRoles(projectId, refId));
        res.setUsers(permissionService.getBranchUserRoles(projectId, refId));
        res.setPublic(permissionService.isProjectPublic(projectId));
        res.setInherit(permissionService.isBranchInherit(projectId, refId));
        return res;
    }
}
