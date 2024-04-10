package org.openmbee.mms.permissions.delegation;

import org.openmbee.mms.core.delegation.PermissionsDelegate;
import org.openmbee.mms.core.delegation.PermissionsDelegateFactory;
import org.openmbee.mms.data.domains.global.Branch;
import org.openmbee.mms.data.domains.global.Group;
import org.openmbee.mms.data.domains.global.Organization;
import org.openmbee.mms.data.domains.global.Project;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

public class DefaultPermissionsDelegateFactory implements PermissionsDelegateFactory {

    @Autowired
    ApplicationContext applicationContext;

    @Override
    public PermissionsDelegate getPermissionsDelegate(Project project) {
        return autowire(new DefaultProjectPermissionsDelegate(project));
    }

    @Override
    public PermissionsDelegate getPermissionsDelegate(Organization organization) {
        return autowire(new DefaultOrgPermissionsDelegate(organization));
    }

    @Override
    public PermissionsDelegate getPermissionsDelegate(Branch branch) {
        return autowire(new DefaultBranchPermissionsDelegate(branch));
    }

    @Override
    public PermissionsDelegate getPermissionsDelegate(Group group) {
        return autowire(new DefaultGroupPermissionsDelegate(group));
    }

    private PermissionsDelegate autowire(PermissionsDelegate permissionsDelegate) {
        applicationContext.getAutowireCapableBeanFactory().autowireBean(permissionsDelegate);
        return permissionsDelegate;
    }

}
