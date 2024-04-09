package org.openmbee.mms.core.dao;

import org.openmbee.mms.json.OrgJson;

import java.util.Collection;
import java.util.Optional;

public interface OrgPersistence {

    OrgJson save(OrgJson orgJson);

    Optional<OrgJson> findById(String orgId);

    Collection<OrgJson> findAll();

    OrgJson deleteById(String orgId);

    boolean hasPublicPermissions(String orgId);
}
