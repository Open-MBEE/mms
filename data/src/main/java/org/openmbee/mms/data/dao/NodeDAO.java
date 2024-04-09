package org.openmbee.mms.data.dao;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.openmbee.mms.core.dao.BaseDAO;
import org.openmbee.mms.data.domains.scoped.Node;

public interface NodeDAO extends BaseDAO {

    Node save(Node node);

    List<Node> saveAll(List<Node> nodes);

    List<Node> insertAll(List<Node> nodes);

    List<Node> updateAll(List<Node> nodes);

    void deleteAll(List<Node> nodes);

    Optional<Node> findByNodeId(String nodeId);

    List<Node> findAllByNodeIds(Collection<String> ids);

    List<Node> findAll();

    List<Node> findAllByDeleted(boolean deleted);

    List<Node> findAllByDeletedAndNodeType(boolean deleted, int nodeType);

    List<Node> findAllByNodeType(int nodeType);
}
