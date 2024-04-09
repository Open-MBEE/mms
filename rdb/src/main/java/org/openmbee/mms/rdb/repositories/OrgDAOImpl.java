package org.openmbee.mms.rdb.repositories;

import java.util.List;
import java.util.Optional;

import org.openmbee.mms.data.dao.OrgDAO;
import org.openmbee.mms.data.domains.global.Organization;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
public class OrgDAOImpl implements OrgDAO {

    private OrganizationRepository orgRepository;

    @Autowired
    public void setOrganizationRepository(OrganizationRepository orgRepository) {
        this.orgRepository = orgRepository;
    }

    @Override
    public Optional<Organization> findByOrganizationId(String id) {
        return orgRepository.findByOrganizationId(id);
    }

    @Override
    public Optional<Organization> findByOrganizationName(String name) {
        return orgRepository.findByOrganizationName(name);
    }

    @Override
    public Organization save(Organization org) {
        return orgRepository.save(org);
    }

    @Override
    public void delete(Organization org) {
        orgRepository.delete(org);
    }

    @Override
    public List<Organization> findAll() {
        return orgRepository.findAll();
    }
}
