package org.openmbee.mms.federatedpersistence.permissions;

import org.openmbee.mms.core.builders.PermissionUpdatesResponseBuilder;
import org.openmbee.mms.core.config.AuthorizationConstants;
import org.openmbee.mms.core.objects.PermissionResponse;
import org.openmbee.mms.core.objects.PermissionUpdateRequest;
import org.openmbee.mms.core.objects.PermissionUpdateResponse;
import org.openmbee.mms.core.objects.PermissionUpdatesResponse;
import org.openmbee.mms.data.domains.global.Group;
import org.openmbee.mms.data.domains.global.OrgGroupPerm;
import org.openmbee.mms.data.domains.global.OrgUserPerm;
import org.openmbee.mms.data.domains.global.Organization;
import org.openmbee.mms.data.domains.global.Privilege;
import org.openmbee.mms.data.domains.global.Role;
import org.openmbee.mms.data.domains.global.User;
import org.openmbee.mms.federatedpersistence.permissions.exceptions.PermissionException;
import org.openmbee.mms.rdb.repositories.OrgGroupPermRepository;
import org.openmbee.mms.rdb.repositories.OrgUserPermRepository;
import org.openmbee.mms.rdb.repositories.OrganizationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.*;

@Component
@Scope(value = "prototype")
public class DefaultOrgPermissionsDelegate extends AbstractDefaultPermissionsDelegate {

    private OrganizationRepository orgRepo;
    private OrgUserPermRepository orgUserPermRepo;
    private OrgGroupPermRepository orgGroupPermRepo;

    private Organization organization;

    public DefaultOrgPermissionsDelegate(Organization organization) {
        this.organization = organization;
    }

    @Autowired
    public void setOrgRepo(OrganizationRepository orgRepo) {
        this.orgRepo = orgRepo;
    }

    @Autowired
    public void setOrgUserPermRepo(OrgUserPermRepository orgUserPermRepo) {
        this.orgUserPermRepo = orgUserPermRepo;
    }

    @Autowired
    public void setOrgGroupPermRepo(OrgGroupPermRepository orgGroupPermRepo) {
        this.orgGroupPermRepo = orgGroupPermRepo;
    }

    @Override
    @Transactional
    public boolean hasPermission(String user, Set<String> groups, String privilege) {

        Optional<Privilege> priv = getPrivRepo().findByName(privilege);
        if (!priv.isPresent()) {
            throw new PermissionException(HttpStatus.BAD_REQUEST, "No such privilege");
        }

        Set<Role> roles = priv.get().getRoles();
        if (orgUserPermRepo.existsByOrganizationAndUser_UsernameAndRoleIn(organization, user, roles)) {
            return true;
        }
        if (!groups.isEmpty() && orgGroupPermRepo.existsByOrganizationAndGroup_NameInAndRoleIn(organization, groups, roles)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean hasGroupPermissions(String group, String privilege) {
        for (OrgGroupPerm perm: orgGroupPermRepo.findAllByOrganization_OrganizationId(organization.getOrganizationId())) {
            if (perm.getGroup().getName().equals(group) && perm.getRole().getPrivileges().stream()
                    .anyMatch(v -> v.getName().equals(privilege))) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void initializePermissions(String creator) {
        initializePermissions(creator, false);
    }

    @Override
    public void initializePermissions(String creator, boolean inherit) {
        if(inherit) {
            throw new IllegalArgumentException("Cannot inherit permissions for an Org");
        }

        Optional<User> user = getUserRepo().findByUsernameIgnoreCase(creator);
        Optional<Role> role = getRoleRepo().findByName(AuthorizationConstants.ADMIN);

        if (!user.isPresent()) {
            throw new PermissionException(HttpStatus.NOT_FOUND, "User not found");
        } else if (!role.isPresent()) {
            throw new PermissionException(HttpStatus.NOT_FOUND, "Role not found");
        }

        OrgUserPerm perm = new OrgUserPerm(organization, user.get(), role.get());
        orgUserPermRepo.save(perm);
    }

    @Override
    public boolean setInherit(boolean isInherit) {
        if(isInherit) {
            throw new IllegalArgumentException("Cannot inherit permissions for an Org");
        }
        return false;
    }

    @Override
    public PermissionResponse getInherit() {
        //Orgs will not inherit
        return PermissionResponse.getDefaultResponse();
    }

    @Override
    public void setPublic(boolean isPublic) {
        organization.setPublic(isPublic);
        orgRepo.save(organization);
    }

    @Override
    @Transactional
    public PermissionUpdateResponse updateUserPermissions(PermissionUpdateRequest req) {
        FederatedPermissionsUpdateResponseBuilder responseBuilder = new FederatedPermissionsUpdateResponseBuilder();

        switch(req.getAction()) {
            case MODIFY:
                for (PermissionUpdateRequest.Permission p: req.getPermissions()) {
                    Optional<User> user = getUserRepo().findByUsernameIgnoreCase(p.getName());
                    Optional<Role> role = getRoleRepo().findByName(p.getRole());
                    if (!user.isPresent() || !role.isPresent()) {
                        //throw exception or skip
                        continue;
                    }
                    Optional<OrgUserPerm> exist = orgUserPermRepo.findByOrganizationAndUser(organization, user.get());
                    OrgUserPerm p1;
                    if (exist.isPresent()) {
                        p1 = exist.get();
                        if (!role.get().equals(p1.getRole())) {
                            responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.REMOVE, p1);
                            p1.setRole(role.get());
                            orgUserPermRepo.save(p1);
                        } else {
                            continue;
                        }
                    } else {
                        p1 = new OrgUserPerm(organization, user.get(), role.get());
                        orgUserPermRepo.save(p1);
                    }
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.ADD, p1);
                }
                break;
            case REPLACE:
                responseBuilder.insertPermissionUpdates_OrgUserPerm(PermissionUpdateResponse.Action.REMOVE,
                    orgUserPermRepo.findAllByOrganization(organization));
                orgUserPermRepo.deleteByOrganization(organization);

                for (PermissionUpdateRequest.Permission p: req.getPermissions()) {
                    Optional<User> user = getUserRepo().findByUsernameIgnoreCase(p.getName());
                    Optional<Role> role = getRoleRepo().findByName(p.getRole());
                    if (!user.isPresent() || !role.isPresent()) {
                        //throw exception or skip
                        continue;
                    }
                    OrgUserPerm p1 = new OrgUserPerm(organization, user.get(), role.get());
                    orgUserPermRepo.save(p1);
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.ADD, p1);
                }
                break;
            case REMOVE:
                Set<String> users = new HashSet<>();
                req.getPermissions().forEach(p -> {
                    Optional<User> user = getUserRepo().findByUsernameIgnoreCase(p.getName());
                    if(! user.isPresent()) {
                        //throw or skip;
                        return;
                    }
                    users.add(p.getName());
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.REMOVE,
                        orgUserPermRepo.findByOrganizationAndUser(organization, user.get()).orElse(null));
                });
                orgUserPermRepo.deleteByOrganizationAndUser_UsernameIn(organization, users);
                break;
        }
        return responseBuilder.getPermissionUpdateResponse();
    }

    @Override
    @Transactional
    public PermissionUpdateResponse updateGroupPermissions(PermissionUpdateRequest req) {
        FederatedPermissionsUpdateResponseBuilder responseBuilder = new FederatedPermissionsUpdateResponseBuilder();
        switch(req.getAction()) {
            case MODIFY:
                for (PermissionUpdateRequest.Permission p: req.getPermissions()) {
                    Pair<Group, Role> pair = getGroupAndRole(p);
                    if (pair == null) {
                        continue;
                    }
                    Optional<OrgGroupPerm> exist = orgGroupPermRepo.findByOrganizationAndGroup(organization, pair.getFirst());
                    OrgGroupPerm p1;
                    if (exist.isPresent()) {
                        p1 = exist.get();
                        if (!pair.getSecond().equals(p1.getRole())) {
                            responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.REMOVE, p1);
                            p1.setRole(pair.getSecond());
                            orgGroupPermRepo.save(p1);
                        } else {
                            continue;
                        }
                    } else {
                        p1 = new OrgGroupPerm(organization, pair.getFirst(), pair.getSecond());
                        orgGroupPermRepo.save(p1);
                    }
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.ADD, p1);
                }
                break;
            case REPLACE:
                responseBuilder.insertPermissionUpdates_OrgGroupPerm(PermissionUpdateResponse.Action.REMOVE,
                    orgGroupPermRepo.findAllByOrganization(organization));
                orgGroupPermRepo.deleteByOrganization(organization);
                for (PermissionUpdateRequest.Permission p: req.getPermissions()) {
                    Pair<Group, Role> pair = getGroupAndRole(p);
                    if (pair == null) {
                        continue;
                    }
                    OrgGroupPerm p1 = new OrgGroupPerm(organization, pair.getFirst(), pair.getSecond());
                    orgGroupPermRepo.save(p1);
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.ADD, p1);
                }
                break;
            case REMOVE:
                Set<String> groups = new HashSet<>();
                req.getPermissions().forEach(p -> {
                    Optional<Group> group = getGroupRepo().findByName(p.getName());
                    if(! group.isPresent()) {
                        //throw or skip
                        return;
                    }

                    groups.add(p.getName());
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.REMOVE,
                        orgGroupPermRepo.findByOrganizationAndGroup(organization, group.get()).orElse(null));
                });
                orgGroupPermRepo.deleteByOrganizationAndGroup_NameIn(organization, groups);
                break;
        }
        return responseBuilder.getPermissionUpdateResponse();
    }

    @Override
    public PermissionResponse getUserRoles() {
        PermissionResponse res = PermissionResponse.getDefaultResponse();
        for (OrgUserPerm perm: orgUserPermRepo.findAllByOrganization_OrganizationId(organization.getOrganizationId())) {
            res.getPermissions().add(new PermissionResponse.Permission(
                perm.getUser().getUsername(),
                perm.getRole().getName(),
                false
            ));
        }
        return res;
    }

    @Override
    public PermissionResponse getGroupRoles() {
        PermissionResponse res = PermissionResponse.getDefaultResponse();
        for (OrgGroupPerm perm: orgGroupPermRepo.findAllByOrganization_OrganizationId(organization.getOrganizationId())) {
            res.getPermissions().add(new PermissionResponse.Permission(
                perm.getGroup().getName(),
                perm.getRole().getName(),
                false
            ));
        }
        return res;
    }

    @Override
    public PermissionUpdatesResponse recalculateInheritedPerms() {
        //Do nothing, can't inherit permissions
        return new PermissionUpdatesResponseBuilder().getPermissionUpdatesResponse();
    }

}
