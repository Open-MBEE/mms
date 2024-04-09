package org.openmbee.mms.federatedpersistence.dao;

import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.data.domains.scoped.Node;

import java.util.Map;
import java.util.Set;

public interface FederatedNodeChangeInfo extends NodeChangeInfo, FederatedNodeGetInfo {
    Map<String, Node> getToSaveNodeMap();

    NodeChangeInfo setToSaveNodeMap(Map<String, Node> toSaveNodeMap);

    Set<String> getOldDocIds();

    NodeChangeInfo setOldDocIds(Set<String> oldDocIds);
}
