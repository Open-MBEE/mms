package org.openmbee.mms.crud.domain;

import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.json.ElementJson;

public interface NodeUpdateFilter {
    boolean filterUpdate(NodeChangeInfo nodeChangeInfo, ElementJson updated, ElementJson existing);
}
