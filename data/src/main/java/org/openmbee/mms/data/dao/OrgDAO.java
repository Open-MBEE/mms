package org.openmbee.mms.data.dao;

import java.util.List;
import java.util.Optional;
import org.openmbee.mms.data.domains.global.Organization;

public interface OrgDAO {

    Optional<Organization> findByOrganizationId(String id);

    Optional<Organization> findByOrganizationName(String name);

    Organization save(Organization org);

    void delete(Organization org);

    List<Organization> findAll();
}
