package org.openmbee.mms.federatedpersistence.dao;

import org.openmbee.mms.core.exceptions.ForbiddenException;
import org.openmbee.mms.data.dao.OrgDAO;
import org.openmbee.mms.core.dao.OrgPersistence;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.data.domains.global.Organization;
import org.openmbee.mms.federatedpersistence.utils.FederatedJsonUtils;
import org.openmbee.mms.json.OrgJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

@Component("federatedOrgPersistence")
public class FederatedOrgPersistence implements OrgPersistence {

    private OrgDAO orgDAO;
    private FederatedJsonUtils jsonUtils;

    @Autowired
    public void setOrgDAO(OrgDAO orgDAO) {
        this.orgDAO = orgDAO;
    }

    @Autowired
    public void setJsonUtils(FederatedJsonUtils jsonUtils) {
        this.jsonUtils = jsonUtils;
    }

    @Override
    public OrgJson save(OrgJson orgJson) {
        Optional<Organization> organizationOptional = orgDAO.findByOrganizationId(orgJson.getId());
        Organization organization = organizationOptional.orElse(new Organization());
        organization.setOrganizationId(orgJson.getId());
        organization.setOrganizationName(orgJson.getName());
        return getOrgJson(orgDAO.save(organization));
    }

    @Override
    public Optional<OrgJson> findById(String orgId) {
        return orgDAO.findByOrganizationId(orgId).map(this::getOrgJson);
    }

    @Override
    public Collection<OrgJson> findAll() {
        return orgDAO.findAll().stream().map(this::getOrgJson).collect(Collectors.toList());
    }

    @Override
    public OrgJson deleteById(String orgId) {
        Optional<Organization> organization = orgDAO.findByOrganizationId(orgId);
        if(organization.isEmpty()) {
            throw new NotFoundException(getOrgNotFoundMessage(orgId));
        }
        orgDAO.delete(organization.get());
        return getOrgJson(organization.get());
    }

    @Override
    public boolean hasPublicPermissions(String orgId) {
        Optional<Organization> organization = orgDAO.findByOrganizationId(orgId);
        if (organization.isEmpty()) {
            throw new NotFoundException(getOrgNotFoundMessage(orgId));
        }
        return organization.get().isPublic();
    }

    protected OrgJson getOrgJson(Organization organization) {
        OrgJson orgJson = new OrgJson();
        orgJson.merge(jsonUtils.convertToMap(organization));
        return orgJson;
    }

    private String getOrgNotFoundMessage(String id) {
        return String.format("org %s not found", id);
    }
}
