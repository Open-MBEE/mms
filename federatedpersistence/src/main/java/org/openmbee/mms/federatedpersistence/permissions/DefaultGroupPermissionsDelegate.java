package org.openmbee.mms.federatedpersistence.permissions;

import org.openmbee.mms.core.builders.PermissionUpdateResponseBuilder;
import org.openmbee.mms.core.builders.PermissionUpdatesResponseBuilder;
import org.openmbee.mms.core.config.AuthorizationConstants;
import org.openmbee.mms.core.objects.PermissionResponse;
import org.openmbee.mms.core.objects.PermissionUpdateRequest;
import org.openmbee.mms.core.objects.PermissionUpdateResponse;
import org.openmbee.mms.core.objects.PermissionUpdatesResponse;
import org.openmbee.mms.data.domains.global.Group;
import org.openmbee.mms.data.domains.global.GroupGroupPerm;
import org.openmbee.mms.data.domains.global.GroupUserPerm;
import org.openmbee.mms.data.domains.global.Privilege;
import org.openmbee.mms.data.domains.global.Role;
import org.openmbee.mms.data.domains.global.User;
import org.openmbee.mms.federatedpersistence.permissions.exceptions.PermissionException;
import org.openmbee.mms.rdb.repositories.GroupGroupPermRepository;
import org.openmbee.mms.rdb.repositories.GroupUserPermRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.openmbee.mms.core.config.AuthorizationConstants.EVERYONE;

public class DefaultGroupPermissionsDelegate extends AbstractDefaultPermissionsDelegate {

    private GroupUserPermRepository groupUserPermRepo;
    private GroupGroupPermRepository groupGroupPermRepo;

    private final Group group;

    public DefaultGroupPermissionsDelegate(Group group) {
        this.group = group;
    }

    @Autowired
    public void setGroupUserPermRepo(GroupUserPermRepository groupUserPermRepo) {
        this.groupUserPermRepo = groupUserPermRepo;
    }

    @Autowired
    public void setGroupGroupPermRepo(GroupGroupPermRepository groupGroupPermRepo) {
        this.groupGroupPermRepo = groupGroupPermRepo;
    }

    @Override
    public boolean hasPermission(String user, Set<String> groupPerms, String privilege) {

        Optional<Privilege> priv = getPrivRepo().findByName(privilege);
        if (priv.isEmpty()) {
            throw new PermissionException(HttpStatus.BAD_REQUEST, "No such privilege");
        }

        //Return false if group is remotely managed
        if (group.getType().equals(Group.VALID_GROUP_TYPES.REMOTE) && privilege.equalsIgnoreCase("GROUP_EDIT")) {
            throw new PermissionException(HttpStatus.BAD_REQUEST, "Unable to edit remote groups.");
        }

        Set<Role> roles = priv.get().getRoles();
        if (groupUserPermRepo.existsByGroupAndUser_UsernameAndRoleIn(group, user, roles)) {
            return true;
        }
        return !groupPerms.isEmpty() && groupGroupPermRepo.existsByGroupAndGroupPerm_NameInAndRoleIn(group, groupPerms, roles);
    }

    @Override
    public boolean hasGroupPermissions(String group, String privilege) {
        for (GroupGroupPerm perm: groupGroupPermRepo.findAllByGroup_Name(group)) {
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
            throw new IllegalArgumentException("Cannot inherit permissions for a Group");
        }

        Optional<User> user = getUserRepo().findByUsername(creator);
        Optional<Role> role = getRoleRepo().findByName(AuthorizationConstants.ADMIN);

        if (user.isEmpty()) {
            throw new PermissionException(HttpStatus.NOT_FOUND, "User not found");
        } else if (role.isEmpty()) {
            throw new PermissionException(HttpStatus.NOT_FOUND, "Role not found");
        }

        GroupUserPerm perm = new GroupUserPerm(group, user.get(), role.get());
        groupUserPermRepo.save(perm);

        Optional<Role> eRole = getRoleRepo().findByName(AuthorizationConstants.READER);
        Optional<Group> ePerm = getGroupRepo().findByName(AuthorizationConstants.EVERYONE);

        if (ePerm.isEmpty()) {
            return;
        } else if (eRole.isEmpty()) {
            throw new PermissionException(HttpStatus.NOT_FOUND, "Role not found");
        }

        GroupGroupPerm evPerm = new GroupGroupPerm(group, ePerm.get(), eRole.get());
        groupGroupPermRepo.save(evPerm);
    }

    @Override
    public boolean setInherit(boolean isInherit) {
        if(isInherit) {
            throw new IllegalArgumentException("Cannot inherit permissions for a Group");
        }
        return false;
    }

    @Override
    public void setPublic(boolean isPublic) {
        group.setPublic(isPublic);
        getGroupRepo().save(group);
    }

    @Override
    public PermissionUpdateResponse updateUserPermissions(PermissionUpdateRequest req) {
        PermissionUpdateResponseBuilder responseBuilder = new PermissionUpdateResponseBuilder();

        switch(req.getAction()) {
            case MODIFY:
                for (PermissionUpdateRequest.Permission p: req.getPermissions()) {
                    Optional<User> user = getUserRepo().findByUsername(p.getName());
                    Optional<Role> role = getRoleRepo().findByName(p.getRole());
                    if (user.isEmpty() || role.isEmpty()) {
                        //throw exception or skip
                        continue;
                    }
                    Optional<GroupUserPerm> exist = groupUserPermRepo.findByGroupAndUser(group, user.get());
                    GroupUserPerm p1;
                    if (exist.isPresent()) {
                        p1 = exist.get();
                        if (!role.get().equals(p1.getRole())) {
                            responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.REMOVE, p1);
                            p1.setRole(role.get());
                            groupUserPermRepo.save(p1);
                        } else {
                            continue;
                        }
                    } else {
                        p1 = new GroupUserPerm(group, user.get(), role.get());
                        groupUserPermRepo.save(p1);
                    }
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.ADD, p1);
                }
                break;
            case REPLACE:
                responseBuilder.insertPermissionUpdates_GroupUserPerm(PermissionUpdateResponse.Action.REMOVE,
                    groupUserPermRepo.findAllByGroup(group));
                groupUserPermRepo.deleteByGroup(group);

                for (PermissionUpdateRequest.Permission p: req.getPermissions()) {
                    Optional<User> user = getUserRepo().findByUsername(p.getName());
                    Optional<Role> role = getRoleRepo().findByName(p.getRole());
                    if (!user.isPresent() || !role.isPresent()) {
                        //throw exception or skip
                        continue;
                    }
                    GroupUserPerm p1 = new GroupUserPerm(group, user.get(), role.get());
                    groupUserPermRepo.save(p1);
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.ADD, p1);
                }
                break;
            case REMOVE:
                Set<String> users = new HashSet<>();
                req.getPermissions().forEach(p -> {
                    Optional<User> user = getUserRepo().findByUsername(p.getName());
                    if(! user.isPresent()) {
                        //throw or skip;
                        return;
                    }
                    users.add(p.getName());
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.REMOVE,
                        groupUserPermRepo.findByGroupAndUser(group, user.get()).orElse(null));
                });
                groupUserPermRepo.deleteByGroupAndUser_UsernameIn(group, users);
                break;
        }
        return responseBuilder.getPermissionUpdateResponse();
    }

    @Override
    public PermissionUpdateResponse updateGroupPermissions(PermissionUpdateRequest req) {
        PermissionUpdateResponseBuilder responseBuilder = new PermissionUpdateResponseBuilder();
        switch(req.getAction()) {
            case MODIFY:
                for (PermissionUpdateRequest.Permission p: req.getPermissions()) {
                    Pair<Group, Role> pair = getGroupAndRole(p);
                    if (pair.getFirst() == null || pair.getSecond() == null) {
                        continue;
                    }
                    Optional<GroupGroupPerm> exist = groupGroupPermRepo.findByGroupAndGroupPerm(group, pair.getFirst());
                    GroupGroupPerm p1;
                    if (exist.isPresent()) {
                        p1 = exist.get();
                        if (!pair.getSecond().equals(p1.getRole())) {
                            responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.REMOVE, p1);
                            p1.setRole(pair.getSecond());
                            groupGroupPermRepo.save(p1);
                        } else {
                            continue;
                        }
                    } else {
                        p1 = new GroupGroupPerm(group, pair.getFirst(), pair.getSecond());
                        groupGroupPermRepo.save(p1);
                    }
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.ADD, p1);
                }
                break;
            case REPLACE:
                responseBuilder.insertPermissionUpdates_GroupGroupPerm(PermissionUpdateResponse.Action.REMOVE,
                    groupGroupPermRepo.findAllByGroup(group));
                groupGroupPermRepo.deleteByGroup(group);
                for (PermissionUpdateRequest.Permission p: req.getPermissions()) {
                    Pair<Group, Role> pair = getGroupAndRole(p);
                    if (pair.getFirst() == null || pair.getSecond() == null) {
                        continue;
                    }
                    GroupGroupPerm p1 = new GroupGroupPerm(group, pair.getFirst(), pair.getSecond());
                    groupGroupPermRepo.save(p1);
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.ADD, p1);
                }
                break;
            case REMOVE:
                Set<String> groupPerms = new HashSet<>();
                req.getPermissions().forEach(p -> {
                    Optional<Group> groupPerm = getGroupRepo().findByName(p.getName());
                    if(! groupPerm.isPresent()) {
                        //throw or skip
                        return;
                    }

                    groupPerms.add(p.getName());
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.REMOVE,
                        groupGroupPermRepo.findByGroupAndGroupPerm(group, groupPerm.get()).orElse(null));
                });
                groupGroupPermRepo.deleteByGroupAndGroupPerm_NameIn(group, groupPerms);
                break;
        }
        return responseBuilder.getPermissionUpdateResponse();
    }

    @Override
    public PermissionResponse getUserRoles() {
        PermissionResponse res = PermissionResponse.getDefaultResponse();
        for (GroupUserPerm perm: groupUserPermRepo.findAllByGroup_Name(group.getName())) {
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
        for (GroupGroupPerm perm: groupGroupPermRepo.findAllByGroup_Name(group.getName())) {
            res.getPermissions().add(new PermissionResponse.Permission(
                perm.getGroupPerm().getName(),
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
