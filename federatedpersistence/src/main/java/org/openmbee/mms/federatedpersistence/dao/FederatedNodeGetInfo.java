package org.openmbee.mms.federatedpersistence.dao;

import org.openmbee.mms.core.services.NodeGetInfo;
import org.openmbee.mms.data.domains.scoped.Node;

import java.util.Map;
import java.util.Set;

public interface FederatedNodeGetInfo extends NodeGetInfo {

    Set<String> getReqIndexIds();

    NodeGetInfo setReqIndexIds(Set<String> reqIndexIds);

    Map<String, Node> getExistingNodeMap();

    NodeGetInfo setExistingNodeMap(Map<String, Node> existingNodeMap);
}
