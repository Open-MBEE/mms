package org.openmbee.mms.groups.services;

import org.openmbee.mms.core.dao.UserGroupsPersistence;
import org.openmbee.mms.json.GroupJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.regex.Pattern;

import static org.openmbee.mms.core.config.AuthorizationConstants.EVERYONE;
import static org.openmbee.mms.core.config.AuthorizationConstants.MMSADMIN;

@Service
public class GroupValidationService {

    private static final Set<String> RESTRICTED_NAMES = Set.of(MMSADMIN, EVERYONE);
    private final Pattern VALID_GROUP_NAME_PATTERN = Pattern.compile("^[ -~]+");
    private UserGroupsPersistence userGroupsPersistence;

    @Autowired
    public void setUserGroupsPersistence(UserGroupsPersistence userGroupsPersistence) {
        this.userGroupsPersistence = userGroupsPersistence;
    }

    public boolean isRestrictedGroup(String groupName) {
        return RESTRICTED_NAMES.contains(groupName);
    }

    public boolean isValidGroupName(String groupName) {
        return groupName != null &&
            !isRestrictedGroup(groupName) &&
            VALID_GROUP_NAME_PATTERN.matcher(groupName).matches();
    }

    public boolean canDeleteGroup(GroupJson group) {
        if(isRestrictedGroup(group.getName())) {
            return false;
        }
        return userGroupsPersistence.findUsersInGroup(group.getName()).isEmpty();
    }
}
