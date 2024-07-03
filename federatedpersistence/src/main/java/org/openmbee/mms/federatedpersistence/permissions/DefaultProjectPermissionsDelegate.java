package org.openmbee.mms.federatedpersistence.permissions;

import org.apache.commons.lang3.builder.EqualsBuilder;

import org.openmbee.mms.core.config.AuthorizationConstants;
import org.openmbee.mms.core.objects.PermissionResponse;
import org.openmbee.mms.core.objects.PermissionUpdateRequest;
import org.openmbee.mms.core.objects.PermissionUpdateResponse;
import org.openmbee.mms.core.objects.PermissionUpdatesResponse;
import org.openmbee.mms.data.domains.global.*;
import org.openmbee.mms.federatedpersistence.permissions.exceptions.PermissionException;
import org.openmbee.mms.rdb.repositories.OrgGroupPermRepository;
import org.openmbee.mms.rdb.repositories.OrgUserPermRepository;
import org.openmbee.mms.rdb.repositories.ProjectGroupPermRepository;
import org.openmbee.mms.rdb.repositories.ProjectRepository;
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
public class DefaultProjectPermissionsDelegate extends AbstractDefaultPermissionsDelegate {

    private ProjectUserPermRepository projectUserPermRepo;
    private ProjectGroupPermRepository projectGroupPermRepo;
    private ProjectRepository projectRepo;
    private OrgGroupPermRepository orgGroupPermRepo;
    private OrgUserPermRepository orgUserPermRepo;

    private Project project;

    public DefaultProjectPermissionsDelegate(Project project) {
        this.project = project;
    }

    @Autowired
    public void setProjectUserPermRepo(ProjectUserPermRepository projectUserPermRepo) {
        this.projectUserPermRepo = projectUserPermRepo;
    }

    @Autowired
    public void setProjectGroupPermRepo(ProjectGroupPermRepository projectGroupPermRepo) {
        this.projectGroupPermRepo = projectGroupPermRepo;
    }

    @Autowired
    public void setProjectRepo(ProjectRepository projectRepo) {
        this.projectRepo = projectRepo;
    }

    @Autowired
    public void setOrgGroupPermRepo(OrgGroupPermRepository orgGroupPermRepo) {
        this.orgGroupPermRepo = orgGroupPermRepo;
    }

    @Autowired
    public void setOrgUserPermRepo(OrgUserPermRepository orgUserPermRepo) {
        this.orgUserPermRepo = orgUserPermRepo;
    }

    @Override
    @Transactional
    public boolean hasPermission(String user, Set<String> groups, String privilege) {

        Optional<Privilege> priv = getPrivRepo().findByName(privilege);
        if (!priv.isPresent()) {
            throw new PermissionException(HttpStatus.BAD_REQUEST, "No such privilege");
        }

        Set<Role> roles = priv.get().getRoles();

        if (projectUserPermRepo.existsByProjectAndUser_UsernameAndRoleIn(project, user, roles)) {
            return true;
        }

        if (!groups.isEmpty() && projectGroupPermRepo.existsByProjectAndGroup_NameInAndRoleIn(project, groups, roles)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean hasGroupPermissions(String group, String privilege) {
        for (ProjectGroupPerm perm: projectGroupPermRepo.findAllByProject_ProjectId(project.getProjectId())) {
            if (perm.getGroup().getName().equals(group) && perm.getRole().getPrivileges().stream().
                    anyMatch(v -> v.getName().equals(privilege))) {
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

        ProjectUserPerm perm = new ProjectUserPerm(project, user.get(), role.get(), false);
        projectUserPermRepo.save(perm);
        project.setInherit(inherit);
        projectRepo.save(project);
    }

    @Override
    public boolean setInherit(boolean isInherit) {
        if (project.isInherit() != isInherit) {
            project.setInherit(isInherit);
            projectRepo.save(project);
            return true;
        }
        return false;
    }

    @Override
    public PermissionResponse getInherit() {
        PermissionResponse response = PermissionResponse.getDefaultResponse();
        if (project.isInherit()) {
            PermissionResponse.Permission permission = new PermissionResponse.Permission();
            permission.setInherited(project.isInherit());
            response.getPermissions().add(permission);
        }
        return response;
    }


    @Override
    public void setPublic(boolean isPublic) {
        project.setPublic(isPublic);
        projectRepo.save(project);
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
                    Optional<ProjectUserPerm> exist = projectUserPermRepo.findByProjectAndUserAndInheritedIsFalse(project, user.get());
                    ProjectUserPerm p1;
                    if (exist.isPresent()) {
                        p1 = exist.get();
                        if (!role.get().equals(p1.getRole())) {
                            responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.REMOVE, p1);
                            p1.setRole(role.get());
                            projectUserPermRepo.save(p1);
                        } else {
                            continue;
                        }
                    } else {
                        p1 = new ProjectUserPerm(project, user.get(), role.get(), false);
                        projectUserPermRepo.save(p1);
                    }
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.ADD, p1);
                }
                break;
            case REPLACE:
                responseBuilder.insertPermissionUpdates_ProjectUserPerm(PermissionUpdateResponse.Action.REMOVE,
                    projectUserPermRepo.findAllByProjectAndInherited(project, false));
                projectUserPermRepo.deleteByProjectAndInherited(project, false);
                for (PermissionUpdateRequest.Permission p: req.getPermissions()) {
                    Optional<User> user = getUserRepo().findByUsernameIgnoreCase(p.getName());
                    Optional<Role> role = getRoleRepo().findByName(p.getRole());
                    if (!user.isPresent() || !role.isPresent()) {
                        //throw exception or skip
                        continue;
                    }
                    ProjectUserPerm p2 = new ProjectUserPerm(project, user.get(), role.get(), false);
                    projectUserPermRepo.save(p2);
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.ADD, p2);
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
                        projectUserPermRepo.findByProjectAndUserAndInheritedIsFalse(project, user.get()).orElse(null));
                });
                projectUserPermRepo.deleteByProjectAndUser_UsernameInAndInheritedIsFalse(project, users);
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
                    Optional<ProjectGroupPerm> exist = projectGroupPermRepo.findByProjectAndGroupAndInheritedIsFalse(project, pair.getFirst());
                    ProjectGroupPerm p1;
                    if (exist.isPresent()) {
                        p1 = exist.get();
                        if (!pair.getSecond().equals(p1.getRole())) {
                            responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.REMOVE, p1);
                            p1.setRole(pair.getSecond());
                            projectGroupPermRepo.save(p1);
                        } else {
                            continue;
                        }
                    } else {
                        p1 = new ProjectGroupPerm(project, pair.getFirst(), pair.getSecond(), false);
                        projectGroupPermRepo.save(p1);
                    }
                    responseBuilder.insertPermissionUpdate(PermissionUpdateResponse.Action.ADD, p1);
                }
                break;
            case REPLACE:
                responseBuilder.insertPermissionUpdates_ProjectGroupPerm(PermissionUpdateResponse.Action.REMOVE,
                    projectGroupPermRepo.findAllByProjectAndInherited(project, false));
                projectGroupPermRepo.deleteByProjectAndInherited(project, false);
                for (PermissionUpdateRequest.Permission p: req.getPermissions()) {
                    Pair<Group, Role> pair = getGroupAndRole(p);
                    if (pair == null) {
                        continue;
                    }
                    ProjectGroupPerm p1 = new ProjectGroupPerm(project, pair.getFirst(), pair.getSecond(), false);
                    projectGroupPermRepo.save(p1);
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
                        projectGroupPermRepo.findByProjectAndGroupAndInheritedIsFalse(project, group.get()).orElse(null));
                });
                projectGroupPermRepo.deleteByProjectAndGroup_NameInAndInheritedIsFalse(project, groups);
                break;
        }
        return responseBuilder.getPermissionUpdateResponse();
    }

    @Override
    public PermissionResponse getUserRoles() {
        PermissionResponse res = PermissionResponse.getDefaultResponse();
        for (ProjectUserPerm perm: projectUserPermRepo.findAllByProject_ProjectId(project.getProjectId())) {
            res.getPermissions().add(new PermissionResponse.Permission(
                perm.getUser().getUsername(),
                perm.getRole().getName(),
                perm.isInherited()
            ));
        }
        return res;
    }

    @Override
    public PermissionResponse getGroupRoles() {
        PermissionResponse res = PermissionResponse.getDefaultResponse();
        for (ProjectGroupPerm perm: projectGroupPermRepo.findAllByProject_ProjectId(project.getProjectId())) {
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

    private class ProjectUserPermWrapper implements Wrapper<ProjectUserPerm> {
        private ProjectUserPerm perm;

        public ProjectUserPermWrapper(ProjectUserPerm perm) {
            this.perm = perm;
        }

        @Override
        public ProjectUserPerm getPerm() {
            return perm;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProjectUserPermWrapper that = (ProjectUserPermWrapper) o;
            EqualsBuilder e = new EqualsBuilder();
            e.append(perm.getProject().getProjectId(), that.perm.getProject().getProjectId());
            e.append(perm.getUser().getUsername(), that.perm.getUser().getUsername());
            e.append(perm.getRole().getName(), that.perm.getRole().getName());
            e.append(perm.isInherited(), that.perm.isInherited());
            return e.isEquals();
        }

        @Override
        public int hashCode() {
            return Objects.hash(perm.getProject().getProjectId(), perm.getUser().getUsername(),
                perm.getRole().getName(), perm.isInherited());
        }
    }

    private class ProjectGroupPermWrapper implements Wrapper<ProjectGroupPerm> {
        private ProjectGroupPerm perm;

        public ProjectGroupPermWrapper(ProjectGroupPerm perm) {
            this.perm = perm;
        }

        @Override
        public ProjectGroupPerm getPerm() {
            return perm;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProjectGroupPermWrapper that = (ProjectGroupPermWrapper) o;
            EqualsBuilder e = new EqualsBuilder();
            e.append(perm.getProject().getProjectId(), that.perm.getProject().getProjectId());
            e.append(perm.getGroup().getName(), that.perm.getGroup().getName());
            e.append(perm.getRole().getName(), that.perm.getRole().getName());
            e.append(perm.isInherited(), that.perm.isInherited());
            return e.isEquals();
        }

        @Override
        public int hashCode() {
            return Objects.hash(perm.getProject().getProjectId(), perm.getGroup().getName(),
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

        List<ProjectUserPerm> projectUserPermList = projectUserPermRepo.findAllByProjectAndInherited(project, true);
        projectUserPermRepo.deleteAll(projectUserPermList);
        responseBuilder.getUsers().insertPermissionUpdates_ProjectUserPerm(PermissionUpdateResponse.Action.REMOVE, projectUserPermList);

        List<ProjectGroupPerm> projectGroupPermList = projectGroupPermRepo.findAllByProjectAndInherited(project, true);
        projectGroupPermRepo.deleteAll(projectGroupPermList);
        responseBuilder.getGroups().insertPermissionUpdates_ProjectGroupPerm(PermissionUpdateResponse.Action.REMOVE, projectGroupPermList);

        PermissionSet<ProjectUserPerm> userPermissions = new PermissionSet<>();
        PermissionSet<ProjectGroupPerm> groupPermissions = new PermissionSet<>();

        if (project.isInherit()) {
            for (OrgUserPerm p: orgUserPermRepo.findAllByOrganization(project.getOrganization())) {
                userPermissions.add(new ProjectUserPermWrapper(new ProjectUserPerm(project, p.getUser(), p.getRole(), true)));
            }
            for (OrgGroupPerm p: orgGroupPermRepo.findAllByOrganization(project.getOrganization())) {
                groupPermissions.add(new ProjectGroupPermWrapper(new ProjectGroupPerm(project, p.getGroup(), p.getRole(), true)));
            }
        } else {
            for (OrgUserPerm p: orgUserPermRepo.findAllByOrganizationAndRole_Name(project.getOrganization(), AuthorizationConstants.ADMIN)) {
                userPermissions.add(new ProjectUserPermWrapper(new ProjectUserPerm(project, p.getUser(), p.getRole(), true)));
            }
            for (OrgGroupPerm p: orgGroupPermRepo.findAllByOrganizationAndRole_Name(project.getOrganization(), AuthorizationConstants.ADMIN)) {
                groupPermissions.add(new ProjectGroupPermWrapper(new ProjectGroupPerm(project, p.getGroup(), p.getRole(), true)));
            }
        }

        projectUserPermRepo.saveAll(userPermissions.getAll());
        responseBuilder.getUsers().insertPermissionUpdates_ProjectUserPerm(PermissionUpdateResponse.Action.ADD, userPermissions.getAll());

        projectGroupPermRepo.saveAll(groupPermissions.getAll());
        responseBuilder.getGroups().insertPermissionUpdates_ProjectGroupPerm(PermissionUpdateResponse.Action.ADD, groupPermissions.getAll());

        return responseBuilder.getPermissionUpdatesResponse();
    }
}
