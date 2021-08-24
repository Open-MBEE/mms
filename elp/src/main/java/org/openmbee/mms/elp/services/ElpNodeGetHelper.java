package org.openmbee.mms.elp.services;

import org.openmbee.mms.crud.services.NodeGetHelper;
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

@Service("elpNodeGetHelper")
@Scope(
    value = ConfigurableBeanFactory.SCOPE_PROTOTYPE,
    proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ElpNodeGetHelper extends NodeGetHelper {
    private static final Logger logger = LoggerFactory.getLogger(ElpNodeGetHelper.class);

    private Set<String> userGroups;
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
        Set<String> existingElementReadGroups;
        Set<String> existingElementWriteGroups;

        if(existingElement != null) {
            existingElementReadGroups = groupUtils.getReadOnlyGroups(existingElement);
            existingElementWriteGroups = groupUtils.getReadWriteGroups(existingElement);
        } else {
            existingElementReadGroups = Collections.emptySet();
            existingElementWriteGroups = Collections.emptySet();
        }

        if(operation == Operation.GET) {
            //Check for read or write access in new element
            if(!Collections.disjoint(userGroups, existingElementReadGroups) || !Collections.disjoint(userGroups, existingElementWriteGroups) ) {
                return true;
            }
        }
        return false;
    }


    private Set<String> getUserGroups() {
        if(userGroups == null) {
            userGroups = groupUtils.getCurrentUserGroups();
        }
        return userGroups;
    }
}
