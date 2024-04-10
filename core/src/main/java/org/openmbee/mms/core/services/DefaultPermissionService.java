package org.openmbee.mms.core.services;

import java.util.*;

import org.openmbee.mms.core.builders.PermissionUpdatesResponseBuilder;
import org.openmbee.mms.core.config.AuthorizationConstants;
import org.openmbee.mms.core.config.Constants;
import org.openmbee.mms.core.dao.BranchPersistence;
import org.openmbee.mms.core.dao.OrgPersistence;
import org.openmbee.mms.core.dao.ProjectPersistence;
import org.openmbee.mms.core.dao.GroupPersistence;
import org.openmbee.mms.core.delegation.PermissionsDelegate;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.core.objects.PermissionResponse;
import org.openmbee.mms.core.objects.PermissionUpdateRequest;
import org.openmbee.mms.core.objects.PermissionUpdateResponse;
import org.openmbee.mms.core.objects.PermissionUpdatesResponse;
import org.openmbee.mms.core.utils.PermissionsDelegateUtil;
import org.openmbee.mms.json.GroupJson;
import org.openmbee.mms.json.OrgJson;
import org.openmbee.mms.json.ProjectJson;
import org.openmbee.mms.json.RefJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("defaultPermissionService")
public class DefaultPermissionService implements PermissionService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultPermissionService.class);

    private BranchPersistence branchPersistence;
    private ProjectPersistence projectPersistence;
    private OrgPersistence orgPersistence;
    private GroupPersistence groupPersistence;

    private PermissionsDelegateUtil permissionsDelegateUtil;

    @Autowired
    public void setPermissionsDelegateUtil(PermissionsDelegateUtil permissionsDelegateUtil) {
        this.permissionsDelegateUtil = permissionsDelegateUtil;
    }

    @Autowired
    public void setBranchPersistence(BranchPersistence branchPersistence) {
        this.branchPersistence = branchPersistence;
    }

    @Autowired
    public void setProjectPersistence(ProjectPersistence projectPersistence) {
        this.projectPersistence = projectPersistence;
    }

    @Autowired
    public void setOrgPersistence(OrgPersistence orgPersistence) {
        this.orgPersistence = orgPersistence;
    }

    @Autowired
    public void setGroupPersistence(GroupPersistence groupPersistence) {
        this.groupPersistence = groupPersistence;
    }

    @Override
    public void initOrgPerms(String orgId, String creator) {
        OrgJson organization = getOrganization(orgId);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(organization);
        permissionsDelegate.initializePermissions(creator);
    }

    @Override
    public void initProjectPerms(String projectId, boolean inherit, String creator) {
        ProjectJson project = getProject(projectId);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(project);
        permissionsDelegate.initializePermissions(creator, inherit);

        recalculateInheritedPerms(project);
        initBranchPerms(projectId, Constants.MASTER_BRANCH, true, creator);
    }

    @Override
    public void initBranchPerms(String projectId, String branchId, boolean inherit, String creator) {
        RefJson branch = getBranch(projectId, branchId, BRANCH_NOTFOUND_BEHAVIOR.THROW);
        if(branch == null) {
            logger.error("Error initiating branch permissions " + projectId + " / " + branchId);
            throw new InternalErrorException("Could not initiate branch permissions");
        }
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(branch);
        permissionsDelegate.initializePermissions(creator, inherit);

        recalculateInheritedPerms(branch);
    }

    @Override
    @Transactional
    public void initGroupPerms(String groupName, String creator) {
        GroupJson group = getGroup(groupName);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(group);
        permissionsDelegate.initializePermissions(creator);
    }

    @Override
    public PermissionUpdatesResponse updateOrgUserPerms(PermissionUpdateRequest req, String orgId) {
        OrgJson organization = getOrganization(orgId);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(organization);
        PermissionUpdatesResponseBuilder responseBuilder = new PermissionUpdatesResponseBuilder();
        responseBuilder.getUsers().insert(permissionsDelegate.updateUserPermissions(req));

        Collection<ProjectJson> projects = projectPersistence.findAllByOrgId(orgId);

        for (ProjectJson project : projects) {
            responseBuilder.insert(recalculateInheritedPerms(project));
        }

        return responseBuilder.getPermissionUpdatesResponse();
    }

    @Override
    public PermissionUpdatesResponse updateOrgGroupPerms(PermissionUpdateRequest req, String orgId) {
        OrgJson organization = getOrganization(orgId);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(organization);
        PermissionUpdatesResponseBuilder responseBuilder = new PermissionUpdatesResponseBuilder();
        responseBuilder.getGroups().insert(permissionsDelegate.updateGroupPermissions(req));

        Collection<ProjectJson> projects = projectPersistence.findAllByOrgId(orgId);
        for (ProjectJson project : projects) {
            responseBuilder.insert(recalculateInheritedPerms(project));
        }

        return responseBuilder.getPermissionUpdatesResponse();
    }

    @Override
    public PermissionUpdatesResponse updateProjectUserPerms(PermissionUpdateRequest req, String projectId) {
        ProjectJson project = getProject(projectId);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(project);
        PermissionUpdatesResponseBuilder responseBuilder = new PermissionUpdatesResponseBuilder();
        responseBuilder.getUsers().insert(permissionsDelegate.updateUserPermissions(req));

        Collection<RefJson> branches = branchPersistence.findAll(projectId);
        for (RefJson b : branches) {
            responseBuilder.insert(recalculateInheritedPerms(b));
        }

        return responseBuilder.getPermissionUpdatesResponse();
    }

    @Override
    public PermissionUpdatesResponse updateProjectGroupPerms(PermissionUpdateRequest req, String projectId) {
        ProjectJson project = getProject(projectId);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(project);
        PermissionUpdatesResponseBuilder responseBuilder = new PermissionUpdatesResponseBuilder();
        responseBuilder.getGroups().insert(permissionsDelegate.updateGroupPermissions(req));

        Collection<RefJson> branches = branchPersistence.findAll(projectId);
        for (RefJson b : branches) {
            responseBuilder.insert(recalculateInheritedPerms(b));
        }

        return responseBuilder.getPermissionUpdatesResponse();
    }

    @Override
    public PermissionUpdateResponse updateBranchUserPerms(PermissionUpdateRequest req, String projectId, String branchId) {
        RefJson branch = getBranch(projectId, branchId, BRANCH_NOTFOUND_BEHAVIOR.THROW);
        if(branch == null) {
            throw new NotFoundException("Branch not found");
        }
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(branch);
        return permissionsDelegate.updateUserPermissions(req);
    }

    @Override
    public PermissionUpdateResponse updateBranchGroupPerms(PermissionUpdateRequest req, String projectId, String branchId) {
        RefJson branch = getBranch(projectId, branchId, BRANCH_NOTFOUND_BEHAVIOR.THROW);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(branch);
        return permissionsDelegate.updateGroupPermissions(req);
    }

    @Override
    public PermissionUpdatesResponse updateGroupUserPerms(PermissionUpdateRequest req, String groupName) {
        GroupJson group = getGroup(groupName);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(group);
        PermissionUpdatesResponseBuilder responseBuilder = new PermissionUpdatesResponseBuilder();
        responseBuilder.getUsers().insert(permissionsDelegate.updateUserPermissions(req));

        return responseBuilder.getPermissionUpdatesResponse();
    }

    @Override
    public PermissionUpdatesResponse updateGroupGroupPerms(PermissionUpdateRequest req, String groupName) {
        GroupJson group = getGroup(groupName);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(group);
        PermissionUpdatesResponseBuilder responseBuilder = new PermissionUpdatesResponseBuilder();
        responseBuilder.getGroups().insert(permissionsDelegate.updateGroupPermissions(req));

        return responseBuilder.getPermissionUpdatesResponse();
    }

    @Override
    public PermissionUpdatesResponse setProjectInherit(boolean isInherit, String projectId) {
        PermissionUpdatesResponseBuilder responseBuilder = new PermissionUpdatesResponseBuilder();
        responseBuilder.setInherit(isInherit);
        ProjectJson project = getProject(projectId);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(project);
        if (permissionsDelegate.setInherit(isInherit)) {
            responseBuilder.insert(recalculateInheritedPerms(project));
        }
        return responseBuilder.getPermissionUpdatesResponse();
    }

    @Override
    public PermissionUpdatesResponse setBranchInherit(boolean isInherit, String projectId, String branchId) {
        PermissionUpdatesResponseBuilder responseBuilder = new PermissionUpdatesResponseBuilder();
        responseBuilder.setInherit(isInherit);
        RefJson branch = getBranch(projectId, branchId, BRANCH_NOTFOUND_BEHAVIOR.THROW);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(branch);
        if (permissionsDelegate.setInherit(isInherit)) {
            responseBuilder.insert(recalculateInheritedPerms(branch));
        }
        return responseBuilder.getPermissionUpdatesResponse();
    }

    @Override
    public boolean setOrgPublic(boolean isPublic, String orgId) {
        OrgJson organization = getOrganization(orgId);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(organization);
        permissionsDelegate.setPublic(isPublic);
        return true;
    }

    @Override
    public boolean setProjectPublic(boolean isPublic, String projectId) {
        ProjectJson project = getProject(projectId);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(project);
        permissionsDelegate.setPublic(isPublic);
        return true;
    }

    @Override
    @Transactional
    public boolean setGroupPublic(boolean isPublic, String groupName) {
        GroupJson group = getGroup(groupName);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(group);
        permissionsDelegate.setPublic(isPublic);
        return true;
    }

    @Override
    public boolean hasOrgPrivilege(String privilege, String user, Set<String> groups, String orgId) {
        if (groups.contains(AuthorizationConstants.MMSADMIN)) return true;

        OrgJson organization = getOrganization(orgId);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(organization);
        return permissionsDelegate.hasPermission(user, groups, privilege);
    }

    @Override
    public boolean hasProjectPrivilege(String privilege, String user, Set<String> groups, String projectId) {
        if (groups.contains(AuthorizationConstants.MMSADMIN)) return true;

        ProjectJson project = getProject(projectId);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(project);
        return permissionsDelegate.hasPermission(user, groups, privilege);
    }

    @Override
    public boolean hasBranchPrivilege(String privilege, String user, Set<String> groups, String projectId, String branchId) {
        if (groups.contains(AuthorizationConstants.MMSADMIN)) return true;

        RefJson branch = getBranch(projectId, branchId, BRANCH_NOTFOUND_BEHAVIOR.THROW);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(branch);
        return permissionsDelegate.hasPermission(user, groups, privilege);
    }

    @Override
    public boolean hasGroupPrivilege(String privilege, String user, Set<String> groups, String groupName) {
        //Return true, however admin are only allowed "delete" perms on Remote group types
        if (groups.contains(AuthorizationConstants.MMSADMIN)) return true;

        GroupJson group = getGroup(groupName);

        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(group);
        return permissionsDelegate.hasPermission(user, groups, privilege);
    }

    @Override
    public boolean isProjectInherit(String projectId) {
        return projectPersistence.inheritsPermissions(projectId);
    }

    @Override
    public boolean isBranchInherit(String projectId, String branchId) {
        return branchPersistence.inheritsPermissions(projectId, branchId);
    }

    @Override
    public boolean isOrgPublic(String orgId) {
        return orgPersistence.hasPublicPermissions(orgId);
    }

    @Override
    public boolean isProjectPublic(String projectId) {
        return projectPersistence.hasPublicPermissions(projectId);
    }

    @Override
    public boolean isGroupPublic(String groupName) {
        return groupPersistence.hasPublicPermissions(groupName);
    }

    @Override
    public PermissionResponse getOrgGroupRoles(String orgId) {
        OrgJson organization = getOrganization(orgId);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(organization);
        return permissionsDelegate.getGroupRoles();
    }

    @Override
    public PermissionResponse getOrgUserRoles(String orgId) {
        OrgJson organization = getOrganization(orgId);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(organization);
        return permissionsDelegate.getUserRoles();
    }

    @Override
    public PermissionResponse getProjectGroupRoles(String projectId) {
        ProjectJson project = getProject(projectId);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(project);
        return permissionsDelegate.getGroupRoles();
    }

    @Override
    public PermissionResponse getProjectUserRoles(String projectId) {
        ProjectJson project = getProject(projectId);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(project);
        return permissionsDelegate.getUserRoles();
    }

    @Override
    public PermissionResponse getBranchGroupRoles(String projectId, String branchId) {
        RefJson branch = getBranch(projectId, branchId, BRANCH_NOTFOUND_BEHAVIOR.IGNORE);

        if (branch == null) {
            return PermissionResponse.getDefaultResponse();
        }

        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(branch);
        return permissionsDelegate.getGroupRoles();
    }

    @Override
    public PermissionResponse getBranchUserRoles(String projectId, String branchId) {
        RefJson branch = getBranch(projectId, branchId, BRANCH_NOTFOUND_BEHAVIOR.IGNORE);

        if (branch == null) {
            return PermissionResponse.getDefaultResponse();
        }

        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(branch);
        return permissionsDelegate.getUserRoles();
    }

    @Override
    @Transactional
    public PermissionResponse getGroupGroupRoles(String groupName) {
        GroupJson group = getGroup(groupName);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(group);
        return permissionsDelegate.getGroupRoles();
    }

    @Override
    @Transactional
    public PermissionResponse getGroupUserRoles(String groupName) {
        GroupJson group = getGroup(groupName);
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(group);
        return permissionsDelegate.getUserRoles();
    }
    

    private PermissionUpdatesResponse recalculateInheritedPerms(ProjectJson project) {

        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(project);
        PermissionUpdatesResponseBuilder responseBuilder = new PermissionUpdatesResponseBuilder();
        responseBuilder.insert(permissionsDelegate.recalculateInheritedPerms());

        Collection<RefJson> branches = branchPersistence.findAll(project.getProjectId());
        for (RefJson branch : branches) {
            responseBuilder.insert(recalculateInheritedPerms(branch));
        }
        return responseBuilder.getPermissionUpdatesResponse();
    }

    private PermissionUpdatesResponse recalculateInheritedPerms(RefJson branch) {
        PermissionsDelegate permissionsDelegate = permissionsDelegateUtil.getPermissionsDelegate(branch);
        return permissionsDelegate.recalculateInheritedPerms();
    }

    private OrgJson getOrganization(String orgId) {
        Optional<OrgJson> org = orgPersistence.findById(orgId);

        if (!org.isPresent()) {
            throw new NotFoundException("Organization " + orgId + " not found");
        }

        return org.get();
    }

    private ProjectJson getProject(String projectId) {
        Optional<ProjectJson> proj = projectPersistence.findById(projectId);

        if (proj.isEmpty()) {
            throw new NotFoundException("Project " + projectId + " not found");
        }
        return proj.get();
    }

    private GroupJson getGroup(String groupName) {
        Optional<GroupJson> group = groupPersistence.findByName(groupName);

        if (!group.isPresent()) {
            throw new NotFoundException("Group " + groupName + " not found");
        }
        return group.get();
    }

    private enum BRANCH_NOTFOUND_BEHAVIOR {THROW, CREATE, IGNORE}

    private RefJson getBranch(String projectId, String branchId, BRANCH_NOTFOUND_BEHAVIOR mode) {
        Optional<RefJson> branch = branchPersistence.findById(projectId, branchId);
        if (branch.isEmpty()) {
            switch (mode) {
                case THROW:
                    throw new NotFoundException("Branch " + projectId + " " + branchId + " not found");
                /* branch should never be created here
                case CREATE:
                    RefJson b = new RefJson();
                    b.setProjectId(projectId);
                    b.setRefId(branchId);
                    return branchPersistence.save(b);
                */
                default:
                    //do nothing
                    break;
            }
        }
        return branch.orElse(null);
    }
}
