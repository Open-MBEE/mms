package org.openmbee.mms.federatedpersistence.dao;

import org.openmbee.mms.core.services.NodeChangeInfo;
import org.openmbee.mms.core.services.NodeChangeInfoImpl;
import org.openmbee.mms.core.services.NodeGetInfo;
import org.openmbee.mms.data.domains.scoped.Node;

import java.util.Map;
import java.util.Set;

public class FederatedNodeChangeInfoImpl extends NodeChangeInfoImpl implements FederatedNodeChangeInfo {
    private Map<String, Node> toSaveNodeMap;
    private Map<String, Node> existingNodeMap;
    private Set<String> oldDocIds;
    private Set<String> reqIndexIds;

    @Override
    public Map<String, Node> getToSaveNodeMap() {
        return toSaveNodeMap;
    }

    @Override
    public NodeChangeInfo setToSaveNodeMap(Map<String, Node> toSaveNodeMap) {
        this.toSaveNodeMap = toSaveNodeMap;
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

    @Override
    public Set<String> getOldDocIds() {
        return oldDocIds;
    }

    @Override
    public NodeChangeInfo setOldDocIds(Set<String> oldDocIds) {
        this.oldDocIds = oldDocIds;
        return this;
    }

    @Override
    public Set<String> getReqIndexIds() {
        return reqIndexIds;
    }

    @Override
    public NodeChangeInfo setReqIndexIds(Set<String> reqIndexIds) {
        this.reqIndexIds = reqIndexIds;
        return this;
    }
}
