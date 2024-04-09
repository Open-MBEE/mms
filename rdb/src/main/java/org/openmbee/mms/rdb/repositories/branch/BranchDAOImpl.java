package org.openmbee.mms.rdb.repositories.branch;

import org.openmbee.mms.core.config.Constants;
import org.openmbee.mms.core.config.ContextHolder;
import org.openmbee.mms.data.dao.BranchDAO;
import org.openmbee.mms.data.domains.scoped.Branch;
import org.openmbee.mms.rdb.config.DatabaseDefinitionService;
import org.openmbee.mms.rdb.repositories.BaseDAOImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;
import org.openmbee.mms.core.exceptions.NotFoundException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

@Component
public class BranchDAOImpl extends BaseDAOImpl implements BranchDAO {

    private DatabaseDefinitionService branchesOperations;

    @Autowired
    public void setBranchesOperations(DatabaseDefinitionService branchesOperations) {
        this.branchesOperations = branchesOperations;
    }

    private final String INSERT_SQL = "INSERT INTO branches (docId, description, branchId, branchName, parentRefId, parentCommit, timestamp, tag, deleted) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
    private final String UPDATE_SQL = "UPDATE branches SET docId = ?, description = ?, branchId = ?, branchName = ?, parentRefId = ?, parentCommit = ?, timestamp = ?, tag = ?, deleted = ? WHERE id = ?";

    public Branch save(Branch branch) {

        if (branch.getId() == null) {
            ContextHolder.setContext(ContextHolder.getContext().getProjectId(), branch.getBranchId());
            if (!Constants.MASTER_BRANCH.equals(branch.getBranchId())) {
                branchesOperations.createBranch();
                branchesOperations.copyTablesFromParent(branch.getBranchId(), branch.getParentRefId(), null);
            }

            KeyHolder keyHolder = new GeneratedKeyHolder();

            getConn().update(new PreparedStatementCreator() {
                public PreparedStatement createPreparedStatement(Connection connection)
                    throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(INSERT_SQL, new String[]{"id"});
                    return prepareStatement(ps, branch);
                }
            }, keyHolder);
            if (keyHolder.getKey() != null) {
                branch.setId(keyHolder.getKey().longValue());
            } else {
                throw new NotFoundException("Key value was null");
            }
        } else {
            getConn().update(new PreparedStatementCreator() {
                public PreparedStatement createPreparedStatement(Connection connection)
                    throws SQLException {
                    PreparedStatement ps = connection.prepareStatement(UPDATE_SQL);
                    return prepareStatement(ps, branch);
                }
            });
        }
        return branch;
    }

    public Optional<Branch> findById(long id) {
        String sql = "SELECT * FROM branches WHERE id = ?";

        List<Branch> l = getConn()
            .query(sql, new Object[]{id}, new BranchRowMapper());
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    public Optional<Branch> findByBranchId(String branchId) {
        String sql = "SELECT * FROM branches WHERE branchId = ?";

        List<Branch> l = getConn()
            .query(sql, new Object[]{branchId}, new BranchRowMapper());
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));
    }

    public List<Branch> findAll() {
        String sql = "SELECT * FROM branches WHERE deleted = false";

        return getConn().query(sql, new BranchRowMapper());
    }

    public void delete(Branch branch) {
        String sql = "UPDATE branches SET deleted = true WHERE branchId = ?";

        getConn().update(sql, branch.getBranchId());
    }

    private PreparedStatement prepareStatement(PreparedStatement ps, Branch branch) throws SQLException {
        ps.setString(1, branch.getDocId());
        ps.setString(2, branch.getDescription());
        ps.setString(3, branch.getBranchId());
        ps.setString(4, branch.getBranchName());
        ps.setString(5, branch.getParentRefId());
        ps.setLong(6, branch.getBranchId().equals(Constants.MASTER_BRANCH) ?  0L : branch.getParentCommit());
        ps.setTimestamp(7, Timestamp.from(branch.getTimestamp()));
        ps.setBoolean(8, branch.isTag());
        ps.setBoolean(9, branch.isDeleted());
        if (branch.getId() != null) {
            ps.setLong(10, branch.getId());
        }
        return ps;
    }
}
