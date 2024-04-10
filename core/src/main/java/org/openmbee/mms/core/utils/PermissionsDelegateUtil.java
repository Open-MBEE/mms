package org.openmbee.mms.core.utils;

import org.openmbee.mms.core.delegation.PermissionsDelegate;
import org.openmbee.mms.core.delegation.PermissionsDelegateFactory;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.services.DefaultPermissionService;
import org.openmbee.mms.json.GroupJson;
import org.openmbee.mms.json.OrgJson;
import org.openmbee.mms.json.ProjectJson;
import org.openmbee.mms.json.RefJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Component
public class PermissionsDelegateUtil {
    private static final Logger logger = LoggerFactory.getLogger(PermissionsDelegateUtil.class);

    private List<PermissionsDelegateFactory> permissionsDelegateFactories;

    @Autowired
    public void setPermissionsDelegateFactories(List<PermissionsDelegateFactory> permissionsDelegateFactories) {
        this.permissionsDelegateFactories = permissionsDelegateFactories;
    }

    public PermissionsDelegate getPermissionsDelegate(final OrgJson organization) {
        Optional<PermissionsDelegate> permissionsDelegate = permissionsDelegateFactories.stream()
            .map(v -> v.getPermissionsDelegate(organization)).filter(Objects::nonNull).findFirst();

        if (permissionsDelegate.isPresent()) {
            return permissionsDelegate.get();
        }

        throw new InternalErrorException(
            "No valid permissions scheme found for organization " + organization.getId()
                + " (" + organization.getName() + ")");
    }

    public PermissionsDelegate getPermissionsDelegate(final ProjectJson project) {
        Optional<PermissionsDelegate> permissionsDelegate = permissionsDelegateFactories.stream()
            .map(v -> v.getPermissionsDelegate(project)).filter(Objects::nonNull).findFirst();

        if(permissionsDelegate.isPresent()) {
            return permissionsDelegate.get();
        }

        throw new InternalErrorException(
            "No valid permissions scheme found for project " + project.getProjectId()
                + " (" + project.getName() + ")");
    }

    public PermissionsDelegate getPermissionsDelegate(final RefJson branch) {
        Optional<PermissionsDelegate> permissionsDelegate = permissionsDelegateFactories.stream()
            .map(v -> v.getPermissionsDelegate(branch)).filter(Objects::nonNull).findFirst();

        if(permissionsDelegate.isPresent()) {
            return permissionsDelegate.get();
        }

        throw new InternalErrorException(
            "No valid permissions scheme found for branch " +
                (branch.getRefId() == null ? "?" : branch.getRefId()) +
                " of project " +
                (branch.getProjectId() == null ? "?" : branch.getProjectId()));
    }

    public PermissionsDelegate getPermissionsDelegate(final GroupJson group) {
        Optional<PermissionsDelegate> permissionsDelegate = permissionsDelegateFactories.stream()
            .map(v -> v.getPermissionsDelegate(group)).filter(Objects::nonNull).findFirst();

        if(permissionsDelegate.isPresent()) {
            return permissionsDelegate.get();
        }

        throw new InternalErrorException(
            "No valid permissions scheme found for project " + group.getName()
                + " (" + group.getName() + ")");
    }
}
