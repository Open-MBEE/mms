package org.openmbee.mms.data.dao;

import org.openmbee.mms.data.domains.global.Group;

import java.util.List;
import java.util.Optional;

public interface GroupDAO {

    Optional<Group> findByGroupName(String id);

    Group save(Group group);

    void delete(Group group);

    List<Group> findAll();
}