package org.openmbee.mms.federatedpersistence.permissions;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.openmbee.mms.core.config.AuthorizationConstants;
import org.openmbee.mms.core.objects.PermissionResponse;
import org.openmbee.mms.core.objects.PermissionUpdateRequest;
import org.openmbee.mms.core.objects.PermissionUpdateResponse;
import org.openmbee.mms.core.objects.PermissionUpdatesResponse;
import org.openmbee.mms.data.domains.global.Branch;
import org.openmbee.mms.data.domains.global.BranchGroupPerm;
import org.openmbee.mms.data.domains.global.BranchUserPerm;
import org.openmbee.mms.data.domains.global.Group;
import org.openmbee.mms.data.domains.global.Privilege;
import org.openmbee.mms.data.domains.global.ProjectGroupPerm;
import org.openmbee.mms.data.domains.global.ProjectUserPerm;
import org.openmbee.mms.data.domains.global.Role;
import org.openmbee.mms.data.domains.global.User;
import org.openmbee.mms.federatedpersistence.permissions.exceptions.PermissionException;
import org.openmbee.mms.rdb.repositories.BranchGroupPermRepository;
import org.openmbee.mms.rdb.repositories.BranchRepository;
import org.openmbee.mms.rdb.repositories.BranchUserPermRepository;
import org.openmbee.mms.rdb.repositories.ProjectGroupPermRepository;
import org.openmbee.mms.rdb.repositories.ProjectUserPermRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.data.util.Pair;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import javax.transaction.Transactional;
import java.util.*;

@Component
@Scope(value = "prototype")
public class DefaultBranchPermissionsDelegate extends AbstractDefaultPermissionsDelegate {

    private BranchRepository branchRepo;
    private BranchUserPermRepository branchUserPermRepo;
    private BranchGroupPermRepository branchGroupPermRepo;
    private ProjectGroupPermRepository projectGroupPermRepo;
    private ProjectUserPermRepository projectUserPermRepo;
    private Branch branch;

    public DefaultBranchPermissionsDelegate(Branch branch) {
        this.branch = branch;
    }

    @Autowired
    public void setBranchRepo(BranchRepository branchRepo) {
        this.branchRepo = branchRepo;
    }

    @Autowired
    public void setBranchUserPermRepo(BranchUserPermRepository branchUserPermRepo) {
        this.branchUserPermRepo = branchUserPermRepo;
    }

    @Autowired
    public void setBranchGroupPermRepo(BranchGroupPermRepository branchGroupPermRepo) {
        this.branchGroupPermRepo = branchGroupPermRepo;
    }

    @Autowired
    public void setProjectGroupPermRepo(ProjectGroupPermRepository projectGroupPermRepo) {
        this.projectGroupPermRepo = projectGroupPermRepo;
    }

    @Autowired
    public void setProjectUserPermRepo(ProjectUserPermRepository projectUserPermRepo) {
        this.projectUserPermRepo = projectUserPermRepo;
    }

    @Override
    @Transactional
    public boolean hasPermission(String user, Set<String> groups, String privilege) {

        Optional<Privilege> priv = getPrivRepo().findByName(privilege);
        if (!priv.isPresent()) {
            throw new PermissionException(HttpStatus.BAD_REQUEST, "No such privilege");
        }

        Set<Role> roles = priv.get().getRoles();
        if (branchUserPermRepo.existsByBranchAndUser_UsernameAndRoleIn(branch, user, roles)) {
            return true;
        }
        if (!groups.isEmpty() && branchGroupPermRepo.existsByBranchAndGroup_NameInAndRoleIn(branch, groups, roles)) {
            return true;
        }
        return false;
    }

    @Override
    @Transactional
    public boolean hasGroupPermissions(String group, String privilege) {
        for (BranchGroupPerm perm: branchGroupPermRepo.findAllByBranch(branch)) {
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

        Optional<User> user = getUserRepo().findByUsernameIgnoreCase(creator);
        Optional<Role> role = getRoleRepo().findByName("ADMIN");

        if (!user.isPresent()) {
            throw new PermissionException(HttpStatus.NOT_FOUND, "User not found");
        } else if (!role.isPresent()) {
            throw new PermissionException(HttpStatus.NOT_FOUND, "Role not found");
        }

        branch.setInherit(inherit);
        branch = branchRepo.save(branch);

        BranchUserPerm perm = new BranchUserPerm(branch, user.get(), role.get(), false);
        branchUserPermRepo.save(perm);
    }

    @Override
    public boolean setInherit(boolean isInherit) {
        if (branch.isInherit() != isInherit) {
            branch.setInherit(isInherit);
            branchRepo.save(branch);
            return true;
        }
        return false;
    }

    @Override
    public PermissionResponse getInherit() {
        PermissionResponse response = PermissionResponse.getDefaultResponse();
        if (branch.isInherit()) {
            PermissionResponse.Permission permission = new PermissionResponse.Permission();
            permission.setInherited(branch.isInherit());
            response.getPermissions().add(permission);
        }
        return response;
    }

    @Override
    public void setPublic(boolean isPublic) {
        //Not supported
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
                    Optional<BranchUserPerm> exist = branchUserPermRepo.findByBranchAndUserAndInheritedIsFalse(branch, user.get());
                    BranchUserPerm perm;
                    if (exist.isPresent()) {
                        perm = exist.get();
                        if (!role.get().equals(perm.getRole())) {
                            responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.REMOVE, perm);
                            perm.setRole(role.get());
                            branchUserPermRepo.save(perm);
                        } else {
                            continue;
                        }
                    } else {
                        perm = new BranchUserPerm(branch, user.get(), role.get(), false);
                        branchUserPermRepo.save(perm);
                    }
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.ADD, perm);
                }
                break;
            case REPLACE:
                responseBuilder.insertPermissionUpdates_BranchUserPerm(PermissionUpdateResponse.Action.REMOVE,
                    branchUserPermRepo.findAllByBranchAndInherited(branch, false));
                branchUserPermRepo.deleteByBranchAndInherited(branch, false);
                for (PermissionUpdateRequest.Permission p: req.getPermissions()) {
                    Optional<User> user = getUserRepo().findByUsernameIgnoreCase(p.getName());
                    Optional<Role> role = getRoleRepo().findByName(p.getRole());
                    if (!user.isPresent() || !role.isPresent()) {
                        //throw exception or skip
                        continue;
                    }
                    BranchUserPerm perm = new BranchUserPerm(branch, user.get(), role.get(), false);
                    branchUserPermRepo.save(perm);
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.ADD, perm);
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
                        branchUserPermRepo.findByBranchAndUserAndInheritedIsFalse(branch, user.get()).orElse(null));
                });
                branchUserPermRepo.deleteByBranchAndUser_UsernameInAndInheritedIsFalse(branch, users);
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
                    Optional<BranchGroupPerm> exist = branchGroupPermRepo.findByBranchAndGroupAndInheritedIsFalse(branch, pair.getFirst());
                    BranchGroupPerm perm;
                    if (exist.isPresent()) {
                        perm = exist.get();
                        if (!pair.getSecond().equals(perm.getRole())) {
                            responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.REMOVE, perm);
                            perm.setRole(pair.getSecond());
                            branchGroupPermRepo.save(perm);
                        } else {
                            continue;
                        }
                    } else {
                        perm = new BranchGroupPerm(branch, pair.getFirst(), pair.getSecond(), false);
                        branchGroupPermRepo.save(perm);
                    }
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.ADD, perm);
                }
                break;
            case REPLACE:
                responseBuilder.insertPermissionUpdates_BranchGroupPerm(PermissionUpdateResponse.Action.REMOVE,
                    branchGroupPermRepo.findAllByBranchAndInherited(branch, false));
                branchGroupPermRepo.deleteByBranchAndInherited(branch, false);
                for (PermissionUpdateRequest.Permission p: req.getPermissions()) {
                    Pair<Group, Role> pair = getGroupAndRole(p);
                    if (pair == null) {
                        continue;
                    }
                    BranchGroupPerm perm = new BranchGroupPerm(branch, pair.getFirst(), pair.getSecond(), false);
                    branchGroupPermRepo.save(perm);
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.ADD, perm);
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
                        branchGroupPermRepo.findByBranchAndGroupAndInheritedIsFalse(branch, group.get()).orElse(null));
                });
                branchGroupPermRepo.deleteByBranchAndGroup_NameInAndInheritedIsFalse(branch, groups);
                break;
        }
        return responseBuilder.getPermissionUpdateResponse();
    }

    @Override
    @Transactional
    public PermissionResponse getUserRoles() {
        PermissionResponse res = PermissionResponse.getDefaultResponse();
        for (BranchUserPerm perm: branchUserPermRepo.findAllByBranch(branch)) {
            res.getPermissions().add(new PermissionResponse.Permission(
                perm.getUser().getUsername(),
                perm.getRole().getName(),
                perm.isInherited()
            ));
        }
        return res;
    }

    @Override
    @Transactional
    public PermissionResponse getGroupRoles() {
        PermissionResponse res = PermissionResponse.getDefaultResponse();
        for (BranchGroupPerm perm: branchGroupPermRepo.findAllByBranch(branch)) {
            res.getPermissions().add(new PermissionResponse.Permission(
                perm.getGroup().getName(),
                perm.getRole().getName(),
                perm.isInherited()
            ));
        }
        return res;
    }

    private interface Wrapper<T>{
        T getPerm();
    }

    private class BranchUserPermWrapper implements Wrapper<BranchUserPerm> {
        private BranchUserPerm perm;

        public BranchUserPermWrapper(BranchUserPerm perm) {
            this.perm = perm;
        }

        @Override
        public BranchUserPerm getPerm() {
            return perm;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BranchUserPermWrapper that = (BranchUserPermWrapper) o;
            EqualsBuilder e = new EqualsBuilder();
            e.append(perm.getBranch().getProject().getProjectId(), that.perm.getBranch().getProject().getProjectId());
            e.append(perm.getBranch().getBranchId(), that.perm.getBranch().getBranchId());
            e.append(perm.getUser().getUsername(), that.perm.getUser().getUsername());
            e.append(perm.getRole().getName(), that.perm.getRole().getName());
            e.append(perm.isInherited(), that.perm.isInherited());
            return e.isEquals();
        }

        @Override
        public int hashCode() {
            return Objects.hash(perm.getBranch().getProject().getProjectId(),
                perm.getBranch().getBranchId(), perm.getUser().getUsername(),
                perm.getRole().getName(), perm.isInherited());
        }
    }

    private class BranchGroupPermWrapper implements Wrapper<BranchGroupPerm> {
        private BranchGroupPerm perm;

        public BranchGroupPermWrapper(BranchGroupPerm perm) {
            this.perm = perm;
        }

        @Override
        public BranchGroupPerm getPerm() {
            return perm;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            BranchGroupPermWrapper that = (BranchGroupPermWrapper) o;
            EqualsBuilder e = new EqualsBuilder();
            e.append(perm.getBranch().getProject().getProjectId(), that.perm.getBranch().getProject().getProjectId());
            e.append(perm.getBranch().getBranchId(), that.perm.getBranch().getBranchId());
            e.append(perm.getGroup().getName(), that.perm.getGroup().getName());
            e.append(perm.getRole().getName(), that.perm.getRole().getName());
            e.append(perm.isInherited(), that.perm.isInherited());
            return e.isEquals();
        }

        @Override
        public int hashCode() {
            return Objects.hash(perm.getBranch().getProject().getProjectId(),
                perm.getBranch().getBranchId(), perm.getGroup().getName(),
                perm.getRole().getName(), perm.isInherited());
        }
    }

    private class PermissionSet<T> {
        private Map<Wrapper<T>, T> map = new HashMap<>();

        public void add(Wrapper<T> obj) {
            map.put(obj, obj.getPerm());
        }

        public Collection<T> getAll() {
            return map.values();
        }
    }


    @Override
    public PermissionUpdatesResponse recalculateInheritedPerms() {
        FederatedPermissionUpdatesResponseBuilder responseBuilder = new FederatedPermissionUpdatesResponseBuilder();

        Collection<BranchUserPerm> branchUserPerms = branchUserPermRepo.findAllByBranchAndInherited(branch, true);
        branchUserPermRepo.deleteAll(branchUserPerms);
        responseBuilder.getUsers().insertPermissionUpdates_BranchUserPerm(PermissionUpdateResponse.Action.REMOVE, branchUserPerms);

        Collection<BranchGroupPerm> branchGroupPerms = branchGroupPermRepo.findAllByBranchAndInherited(branch, true);
        branchGroupPermRepo.deleteAll(branchGroupPerms);
        responseBuilder.getGroups().insertPermissionUpdates_BranchGroupPerm(PermissionUpdateResponse.Action.REMOVE, branchGroupPerms);


        PermissionSet<BranchUserPerm> userPermissions = new PermissionSet<>();
        PermissionSet<BranchGroupPerm> groupPermissions = new PermissionSet<>();

        if (branch.isInherit()) {
            for (ProjectUserPerm p: projectUserPermRepo.findAllByProject(branch.getProject())) {
                userPermissions.add(new BranchUserPermWrapper(new BranchUserPerm(branch, p.getUser(), p.getRole(), true)));
            }
            for (ProjectGroupPerm p: projectGroupPermRepo.findAllByProject(branch.getProject())) {
                groupPermissions.add(new BranchGroupPermWrapper(new BranchGroupPerm(branch, p.getGroup(), p.getRole(), true)));
            }
        } else {
            for (ProjectUserPerm p: projectUserPermRepo.findAllByProjectAndRole_Name(branch.getProject(), AuthorizationConstants.ADMIN)) {
                userPermissions.add(new BranchUserPermWrapper(new BranchUserPerm(branch, p.getUser(), p.getRole(), true)));
            }
            for (ProjectGroupPerm p: projectGroupPermRepo.findAllByProjectAndRole_Name(branch.getProject(), AuthorizationConstants.ADMIN)) {
                groupPermissions.add(new BranchGroupPermWrapper(new BranchGroupPerm(branch, p.getGroup(), p.getRole(), true)));
            }
        }

        branchUserPermRepo.saveAll(userPermissions.getAll());
        responseBuilder.getUsers().insertPermissionUpdates_BranchUserPerm(PermissionUpdateResponse.Action.ADD, userPermissions.getAll());

        branchGroupPermRepo.saveAll(groupPermissions.getAll());
        responseBuilder.getGroups().insertPermissionUpdates_BranchGroupPerm(PermissionUpdateResponse.Action.ADD, groupPermissions.getAll());

        return responseBuilder.getPermissionUpdatesReponse();
    }
}
