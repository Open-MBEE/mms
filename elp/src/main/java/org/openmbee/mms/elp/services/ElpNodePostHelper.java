package org.openmbee.mms.elp.services;

import org.openmbee.mms.crud.services.NodePostHelper;
import org.openmbee.mms.elp.utils.GroupUtils;
import org.openmbee.mms.json.ElementJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

@Service("elpNodePostHelper")
@Scope(
    value = ConfigurableBeanFactory.SCOPE_PROTOTYPE,
    proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ElpNodePostHelper extends NodePostHelper {
    private static final Logger logger = LoggerFactory.getLogger(ElpNodePostHelper.class);

    private Set<String> userGroups;
    private Set<String> existingGroups;
    private GroupUtils groupUtils;

    @Autowired
    public void setGroupUtils(GroupUtils groupUtils) {
        this.groupUtils = groupUtils;
    }

    @Override
    protected boolean validateOperation(Operation operation, Map<String, Object> newElement, Map<String, Object> existingElement, boolean overwrite) {
        if(groupUtils.isAdmin(getUserGroups())) {
            return true;
        }

        Set<String> userGroups = getUserGroups();

        Set<String> newElementWriteGroups;
        Set<String> newElementReadGroups;
        Set<String> existingElementWriteGroups;
        Set<String> existingElementReadGroups;

        if(newElement != null) {
            newElementWriteGroups = groupUtils.getReadWriteGroups(newElement);
            newElementReadGroups = groupUtils.getReadOnlyGroups(newElement);
        } else {
            newElementWriteGroups = Collections.emptySet();
            newElementReadGroups = Collections.emptySet();
        }
        if(existingElement != null) {
            existingElementWriteGroups = groupUtils.getReadWriteGroups(existingElement);
            existingElementReadGroups = groupUtils.getReadOnlyGroups(existingElement);
        } else {
            existingElementWriteGroups = Collections.emptySet();
            existingElementReadGroups = Collections.emptySet();
        }

        //Need to use existing groups if no groups are provided and not overwriting
        if(!overwrite && existingElement != null) {
            if(groupUtils.isOnlyEveryone(newElementReadGroups)) {
                newElementReadGroups = existingElementReadGroups;
            }
            if(groupUtils.isOnlyEveryone(newElementWriteGroups)) {
                newElementWriteGroups = existingElementWriteGroups;
            }
        }

        if(newElement != null) {
            //Ensure read groups are meaningful if they are included
            if(groupUtils.isOnlyEveryone(newElementWriteGroups) && !groupUtils.isOnlyEveryone(newElementReadGroups)) {
                return false;
            }
            //Ensure groups actually exist
            if(!getExistingGroups().containsAll(newElementReadGroups)
                || !getExistingGroups().containsAll(newElementWriteGroups)) {
                return false;
            }
        }

        if(operation == Operation.ADD) {
            //Check for write access in new element
            if(Collections.disjoint(userGroups, newElementWriteGroups)) {
                return false;
            }
        } else if(operation == Operation.UPDATE) {
            //Check for write access in old and new element
            if (Collections.disjoint(userGroups, newElementWriteGroups)
                || Collections.disjoint(userGroups, existingElementWriteGroups)) {
                return false;
            }
        } else {
            return false;
        }

        return true;
    }


    private Set<String> getUserGroups() {
        if(userGroups == null) {
            userGroups = groupUtils.getCurrentUserGroups();
        }
        return userGroups;
    }

    private Set<String> getExistingGroups() {
        if(existingGroups == null) {
            existingGroups = groupUtils.getExistingGroups();
        }
        return existingGroups;
    }
}
