package org.openmbee.mms.federatedpersistence.dao;

import org.openmbee.mms.core.exceptions.ForbiddenException;
import org.openmbee.mms.data.dao.OrgDAO;
import org.openmbee.mms.core.config.Constants;
import org.openmbee.mms.core.config.ContextHolder;
import org.openmbee.mms.core.dao.OrgPersistence;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.data.domains.global.Organization;
import org.openmbee.mms.data.domains.global.Project;
import org.openmbee.mms.federatedpersistence.utils.FederatedJsonUtils;
import org.openmbee.mms.json.OrgJson;
import org.openmbee.mms.json.ProjectJson;
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
        if (orgJson.getIsArchived() != null) {
            organization.setDeleted(Boolean.parseBoolean(orgJson.getIsArchived()));
        }
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
    public void deleteById(String orgId) {
        Optional<Organization> organization = orgDAO.findByOrganizationId(orgId);
        if(organization.isEmpty()) {
            throw new NotFoundException(getOrgNotFoundMessage(orgId));
        }
        orgDAO.delete(organization.get());
    }

    @Override
    public void archiveById(String orgId) {
        //TODO not called locally, otherwise delete
        ContextHolder.setContext(orgId);
        Optional<Organization> org = orgDAO.findByOrganizationId(orgId);

        if (org.isEmpty()) {
            throw new NotFoundException("Org state is invalid, cannot delete.");
        }
        
        Organization p = org.get();
        p.setDeleted(true);
        orgDAO.save(p);
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
        orgJson.setIsArchived(String.valueOf(organization.isDeleted()));
        orgJson.remove(OrgJson.DELETED);
        return orgJson;
    }

    private String getOrgNotFoundMessage(String id) {
        return String.format("org %s not found", id);
    }
}
