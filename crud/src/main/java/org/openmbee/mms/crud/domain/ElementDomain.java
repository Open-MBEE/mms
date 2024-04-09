package org.openmbee.mms.crud.domain;

import org.openmbee.mms.core.utils.ElementUtils;
import org.openmbee.mms.json.ElementJson;

public interface ElementDomain {

    public abstract ElementUtils getElementUtils(String projectId);
    
    public abstract Integer getNodeType(String projectId, ElementJson element);

}