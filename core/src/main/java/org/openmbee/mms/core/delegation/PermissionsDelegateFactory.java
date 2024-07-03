package org.openmbee.mms.core.delegation;

import org.openmbee.mms.json.OrgJson;
import org.openmbee.mms.json.ProjectJson;
import org.openmbee.mms.json.RefJson;
import org.openmbee.mms.json.GroupJson;

public interface PermissionsDelegateFactory {

    PermissionsDelegate getPermissionsDelegate(ProjectJson project);

    PermissionsDelegate getPermissionsDelegate(OrgJson organization);

    PermissionsDelegate getPermissionsDelegate(RefJson branch);

    PermissionsDelegate getPermissionsDelegate(GroupJson group);
}
