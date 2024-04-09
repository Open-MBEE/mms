package org.openmbee.mms.federatedpersistence.dao;

import org.openmbee.mms.core.services.NodeGetInfo;
import org.openmbee.mms.core.services.NodeGetInfoImpl;
import org.openmbee.mms.data.domains.scoped.Node;

import java.util.Map;
import java.util.Set;

public class FederatedNodeGetInfoImpl extends NodeGetInfoImpl implements FederatedNodeGetInfo {

    private Set<String> reqIndexIds;
    private Map<String, Node> existingNodeMap;

    @Override
    public Set<String> getReqIndexIds() {
        return reqIndexIds;
    }

    @Override
    public NodeGetInfo setReqIndexIds(Set<String> reqIndexIds) {
        this.reqIndexIds = reqIndexIds;
        return this;
    }

    @Override
    public Map<String, Node> getExistingNodeMap() {
        return existingNodeMap;
    }

    @Override
    public NodeGetInfo setExistingNodeMap(Map<String, Node> existingNodeMap) {
        this.existingNodeMap = existingNodeMap;
        return this;
    }
}
