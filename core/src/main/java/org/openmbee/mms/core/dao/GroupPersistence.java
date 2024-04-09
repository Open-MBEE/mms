package org.openmbee.mms.core.dao;

import org.openmbee.mms.json.GroupJson;

import java.util.Collection;
import java.util.Optional;

public interface GroupPersistence {

    GroupJson save(GroupJson groupJson);
    void delete(GroupJson groupJson);
    Optional<GroupJson> findByName(String name);
    Collection<GroupJson> findAll();
}
