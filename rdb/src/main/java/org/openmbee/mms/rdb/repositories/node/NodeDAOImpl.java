package org.openmbee.mms.rdb.repositories.node;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import org.openmbee.mms.data.dao.NodeDAO;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.data.domains.scoped.Node;
import org.openmbee.mms.rdb.repositories.BaseDAOImpl;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.openmbee.mms.core.exceptions.NotFoundException;

@Component
public class NodeDAOImpl extends BaseDAOImpl implements NodeDAO {

    private final String INSERT_SQL = "INSERT INTO \"nodes%s\" (nodeid, docid, lastcommit, initialcommit, deleted, nodetype) VALUES (?, ?, ?, ?, ?, ?)";
    private final String UPDATE_SQL = "UPDATE \"nodes%s\" SET nodeid = ?, docid = ?, lastcommit = ?, initialcommit = ?, deleted = ?, nodetype = ? WHERE id = ?";

    public Node save(Node node) throws InvalidDataAccessApiUsageException, DataRetrievalFailureException {
        if (node.getId() == null) {
            KeyHolder keyHolder = new GeneratedKeyHolder();

            getConn().update(new PreparedStatementCreator() {
                public PreparedStatement createPreparedStatement(Connection connection)
                    throws SQLException {
                    PreparedStatement ps = connection
                        .prepareStatement(String.format(INSERT_SQL, getSuffix()),
                            new String[]{"id"});
                    return prepareStatement(ps, node);
                }
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                node.setId(keyHolder.getKey().longValue());
            } else {
                throw new NotFoundException("Key value was null");
            }
        } else {
            getConn().update(new PreparedStatementCreator() {
                public PreparedStatement createPreparedStatement(Connection connection)
                    throws SQLException {
                    PreparedStatement ps = connection
                        .prepareStatement(String.format(UPDATE_SQL, getSuffix()));
                    return prepareStatement(ps, node);
                }
            });
        }
        return node;
    }

    public List<Node> saveAll(List<Node> nodes) {
        List<Node> newNodes = new ArrayList<>();
        List<Node> updateNodes = new ArrayList<>();

        for (Node n : nodes) {
            if (n.getId() == null) {
                newNodes.add(n);
            } else {
                updateNodes.add(n);
            }
        }

        if (!newNodes.isEmpty()) {
            insertAll(newNodes);
        }

        if (!updateNodes.isEmpty()) {
            updateAll(updateNodes);
        }
        return nodes;
    }

    public List<Node> insertAll(List<Node> nodes) {
        try (Connection rawConn = Objects.requireNonNull(getConn().getDataSource()).getConnection(); PreparedStatement ps = rawConn.prepareStatement(String.format(INSERT_SQL, getSuffix()), new String[]{"id"});) {
            //jdbctemplate doesn't have read generated keys for batch, need to use raw jdbc, depends on driver
            for (Node n : nodes) {
                prepareStatement(ps, n);
                ps.addBatch();
            }
            ps.executeBatch();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                int i = 0;
                while (rs.next()) {
                    nodes.get(i).setId(rs.getLong(1));
                    i++;
                }
            }

            return nodes;
        } catch (SQLException ex) {
            logger.error("node insert all", ex);
            throw new InternalErrorException(ex);
        }
    }

    public List<Node> updateAll(List<Node> nodes) {
        String updateSql = String.format(UPDATE_SQL, getSuffix());
        getConn().batchUpdate(updateSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                Node n = nodes.get(i);
                prepareStatement(ps, n);
            }

            @Override
            public int getBatchSize() {
                return nodes.size();
            }
        });
        return nodes;
    }

    public void deleteAll(List<Node> nodes) {
        String deleteSql = String.format("DELETE FROM \"nodes%s\" WHERE id = ?", getSuffix());
        getConn().batchUpdate(deleteSql, new BatchPreparedStatementSetter() {
            @Override
            public void setValues(PreparedStatement ps, int i) throws SQLException {
                ps.setLong(1, nodes.get(i).getId());
            }

            @Override
            public int getBatchSize() {
                return nodes.size();
            }
        });
    }

    public Optional<Node> findById(long id) {
        String sql = String.format("SELECT * FROM \"nodes%s\" WHERE id = ?",
            getSuffix());

        List<Node> l = getConn()
            .query(sql, new Object[]{id}, new NodeRowMapper());
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));

    }

    public Optional<Node> findByNodeId(String nodeId) {
        String sql = String.format("SELECT * FROM \"nodes%s\" WHERE nodeid = ?",
            getSuffix());

        List<Node> l = getConn()
            .query(sql, new Object[]{nodeId}, new NodeRowMapper());
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));

    }

    public List<Node> findAllByNodeIds(Collection<String> ids) {
        if (ids == null || ids.isEmpty()) {
            return new ArrayList<>();
        }
        String sql = String.format("SELECT * FROM \"nodes%s\" WHERE nodeid IN (%s)",
            getSuffix(), "'" + String.join("','", ids) + "'");
        return getConn().query(sql, new NodeRowMapper());
    }

    public List<Node> findAll() {
        String sql = String.format("SELECT * FROM \"nodes%s\"", getSuffix());
        return getConn().query(sql, new NodeRowMapper());
    }

    public List<Node> findAllByDeleted(boolean deleted) {
        String sql = String.format("SELECT * FROM \"nodes%s\" WHERE deleted = ?",
            getSuffix());
        return getConn().query(sql, new Object[]{deleted}, new NodeRowMapper());
    }

    public List<Node> findAllByDeletedAndNodeType(boolean deleted, int nodeType) {
        String sql = String.format("SELECT * FROM \"nodes%s\" WHERE deleted = ? AND nodetype = ?",
            getSuffix());
        return getConn().query(sql, new Object[]{deleted, nodeType}, new NodeRowMapper());
    }

    public List<Node> findAllByNodeType(int nodeType) {
        String sql = String.format("SELECT * FROM \"nodes%s\" WHERE nodetype = ?",
            getSuffix());
        return getConn().query(sql, new Object[]{nodeType}, new NodeRowMapper());
    }

    private PreparedStatement prepareStatement(PreparedStatement ps, Node n) throws SQLException {
        ps.setString(1, n.getNodeId());
        ps.setString(2, n.getDocId());
        ps.setString(3, n.getLastCommit());
        ps.setString(4, n.getInitialCommit());
        ps.setBoolean(5, n.isDeleted());
        ps.setInt(6, n.getNodeType());
        if (n.getId() != null) {
            ps.setLong(7, n.getId());
        }
        return ps;
    }
}