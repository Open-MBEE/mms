package org.openmbee.mms.rdb.repositories.commit;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.openmbee.mms.data.dao.BranchDAO;
import org.openmbee.mms.data.dao.CommitDAO;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.config.Constants;
import org.openmbee.mms.data.domains.scoped.Branch;
import org.openmbee.mms.data.domains.scoped.Commit;
import org.openmbee.mms.rdb.repositories.BaseDAOImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Component;

@Component
public class CommitDAOImpl extends BaseDAOImpl implements CommitDAO {

    private BranchDAO branchRepository;

    @Autowired
    public void setBranchRepository(BranchDAO branchRepository) {
        this.branchRepository = branchRepository;
    }

    public Commit save(Commit commit) {
        String sql = "INSERT INTO commits (commitType, creator, commitid, branchId, timestamp, comment) VALUES (?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();

        getConn().update(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(Connection connection)
                throws SQLException {
                PreparedStatement ps = connection.prepareStatement(sql, new String[]{"id"});
                ps.setInt(1, commit.getCommitType().getId());
                ps.setString(2, commit.getCreator());
                ps.setString(3, commit.getCommitId());
                ps.setString(4, commit.getBranchId());
                ps.setTimestamp(5, Timestamp.from(commit.getTimestamp()));
                ps.setString(6, commit.getComment());

                return ps;
            }
        }, keyHolder);

        if (keyHolder.getKeyList().isEmpty() || keyHolder.getKey() == null) {
            logger.error("commit db save failed");
            throw new InternalErrorException("Commit db save failed");
        }
        commit.setId(keyHolder.getKey().longValue());
        return commit;
    }

    public Optional<Commit> findById(long id) {
        String sql = "SELECT * FROM commits WHERE id = ?";

        List<Commit> l = getConn()
            .query(sql, new CommitRowMapper(), new Object[]{id});
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));

    }

    public Optional<Commit> findByCommitId(String commitId) {
        String sql = "SELECT * FROM commits WHERE commitid = ?";

        List<Commit> l = getConn()
            .query(sql, new CommitRowMapper(), new Object[]{commitId});
        return l.isEmpty() ? Optional.empty() : Optional.of(l.get(0));

    }

    public List<Commit> findAll() {
        String sql = "SELECT * FROM commits ORDER BY timestamp DESC";
        return getConn().query(sql, new CommitRowMapper());
    }

    private List<Commit> findByRefAndLimit(String refId, Long cid, Instant timestamp, int limit) {
        int commitCol = 0;
        int timestampCol = 0;
        int limitCol = 0;
        int currentExtraCol = 2;
        StringBuilder query = new StringBuilder("SELECT * FROM commits WHERE branchid = ?");
        if (cid != null && cid != 0) {
            query.append(" AND timestamp <= (SELECT timestamp FROM commits WHERE id = ?)");
            commitCol = currentExtraCol;
            currentExtraCol++;
        }
        if (timestamp != null) {
            if (timestamp.truncatedTo(ChronoUnit.MILLIS).equals(timestamp)) {
                //if original timestamp doesn't have microseconds, add a millisecond to counter
                //timestamp in db that has microseconds
                timestamp = timestamp.plusMillis(1);
            }
            query.append(" AND timestamp <= ?");
            timestampCol = currentExtraCol;
            currentExtraCol++;
        }
        query.append(" ORDER BY timestamp DESC");
        if (limit != 0) {
            query.append(" LIMIT ?");
            limitCol = currentExtraCol;
        }
        final int commitColNum = commitCol;
        final int timestampColNum = timestampCol;
        final int limitColNum = limitCol;
        Instant finalTimestamp = timestamp;
        return getConn().query(new PreparedStatementCreator() {
            public PreparedStatement createPreparedStatement(Connection connection)
                throws SQLException {
                PreparedStatement ps = connection
                    .prepareStatement(query.toString());
                ps.setString(1, refId);
                if (commitColNum != 0) {
                    ps.setLong(commitColNum, cid);
                }
                if (timestampColNum != 0) {
                    ps.setTimestamp(timestampColNum, Timestamp.from(finalTimestamp));
                }
                if (limitColNum != 0) {
                    ps.setInt(limitColNum, limit);
                }
                return ps;
            }
        }, new CommitRowMapper());
    }

    public Optional<Commit> findLatestByRef(Branch ref) {
        return findByRefAndTimestamp(ref, null);
    }

    public Optional<Commit> findByRefAndTimestamp(Branch ref, Instant timestamp) {
        List<Commit> res = findByRefAndTimestampAndLimit(ref, timestamp, 1);
        if (!res.isEmpty()) {
            return Optional.of(res.get(0));
        }
        return Optional.empty();
    }

    public List<Commit> findByRefAndTimestampAndLimit(Branch ref, Instant timestamp, int limit) {
        List<Commit> commits = new ArrayList<>();
        String currentRef = ref.getBranchId();
        Long currentCid = 0L;
        while (currentRef != null && (commits.size() < limit || limit == 0)) {
            int currentLimit = limit == 0 ? 0 : limit - commits.size();
            List<Commit> next = findByRefAndLimit(currentRef, currentCid, timestamp, currentLimit);
            commits.addAll(next);

            currentRef = ref.getParentRefId();
            currentCid = ref.getParentCommit();

            if (currentRef == null || currentRef.equals(Constants.MASTER_BRANCH)) {
                break;
            }
            Optional<Branch> parent = branchRepository.findByBranchId(currentRef);
            if (!parent.isPresent()) {
                break; //this is actually inconsistent data and should be error?
            }
            ref = parent.get();
        }
        return commits;
    }
}
