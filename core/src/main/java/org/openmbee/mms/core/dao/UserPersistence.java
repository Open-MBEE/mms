package org.openmbee.mms.core.dao;

import org.openmbee.mms.json.UserJson;

import java.util.Collection;
import java.util.Optional;

public interface UserPersistence {

    UserJson save(UserJson user);
    Optional<UserJson> findByUsername(String username);
    Collection<UserJson> findAll();

}
