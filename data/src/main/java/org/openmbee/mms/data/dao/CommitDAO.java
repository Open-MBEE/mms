package org.openmbee.mms.data.dao;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.openmbee.mms.data.domains.scoped.Branch;
import org.openmbee.mms.data.domains.scoped.Commit;

public interface CommitDAO {

    Commit save(Commit commit);

    Optional<Commit> findById(long id);

    Optional<Commit> findByCommitId(String commitId);

    List<Commit> findAll();

    Optional<Commit> findLatestByRef(Branch ref);

    Optional<Commit> findByRefAndTimestamp(Branch ref, Instant timestamp);

    List<Commit> findByRefAndTimestampAndLimit(Branch ref, Instant timestamp, int limit);
}
