package org.openmbee.mms.crud.controllers.orgs;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.Optional;

import org.openmbee.mms.core.config.Formats;
import org.openmbee.mms.core.config.Privileges;
import org.openmbee.mms.core.dao.OrgPersistence;
import org.openmbee.mms.core.objects.OrganizationsRequest;
import org.openmbee.mms.core.objects.OrganizationsResponse;
import org.openmbee.mms.core.objects.Rejection;
import org.openmbee.mms.crud.CrudConstants;
import org.openmbee.mms.crud.controllers.BaseController;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.json.OrgJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/orgs")
@Tag(name = "Orgs")
public class OrgsController extends BaseController {

    OrgPersistence organizationRepository;

    @Autowired
    public OrgsController(OrgPersistence organizationRepository) {
        this.organizationRepository = organizationRepository;
    }

    @GetMapping
    public OrganizationsResponse getAllOrgs( Authentication auth) {

        OrganizationsResponse response = new OrganizationsResponse();
        Collection<OrgJson> allOrgs = organizationRepository.findAll();
        for (OrgJson org : allOrgs) {
            if (mss.hasOrgPrivilege(auth, org.getId(), Privileges.ORG_READ.name(), true)) {
                response.getOrgs().add(org);
            }
        }
        return response;
    }

    @GetMapping(value = "/{orgId}")
    @PreAuthorize("@mss.hasOrgPrivilege(authentication, #orgId, 'ORG_READ', true)")
    public OrganizationsResponse getOrg(
        @PathVariable String orgId) {

        OrganizationsResponse response = new OrganizationsResponse();
        Optional<OrgJson> orgOption = organizationRepository.findById(orgId);
        if (!orgOption.isPresent()) {
            throw new NotFoundException(response.addMessage("Organization not found."));
        }
        response.getOrgs().add(orgOption.get());
        return response;
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("isAuthenticated()")
    public OrganizationsResponse createOrUpdateOrgs(
        @RequestBody OrganizationsRequest orgPost,
        Authentication auth) {

        OrganizationsResponse response = new OrganizationsResponse();
        if (orgPost.getOrgs().isEmpty()) {
            throw new BadRequestException(response.addMessage("No orgs provided"));
        }

        for (OrgJson org : orgPost.getOrgs()) {
            if (org.getId() == null || org.getId().isEmpty()) {
                org.setId(UUID.randomUUID().toString());
            }

            OrgJson o = organizationRepository.findById(org.getId()).orElse(new OrgJson());
            boolean newOrg = true;
            if (o.getId() != null) {
                if (!mss.hasOrgPrivilege(auth, o.getId(), Privileges.ORG_EDIT.name(), false)) {
                    response.addRejection(new Rejection(org, 403, "No permission to update org"));
                    continue;
                }
                newOrg = false;
                org.merge(o);
            } else {
                if (org.getCreated() == null || org.getCreated().isEmpty()) {
                    org.setCreated(Formats.FORMATTER.format(Instant.now()));
                }
                if (org.getType() == null || org.getType().isEmpty()) {
                    org.setType(CrudConstants.ORG);
                }
                if (org.getCreator() == null || org.getCreator().isEmpty()) {
                    org.setCreator(auth.getName());
                }
            }
            logger.info("Saving organization: {}", org.getId());
            OrgJson saved = organizationRepository.save(org);
            if (newOrg) {
                permissionService.initOrgPerms(org.getId(), auth.getName());
            }
            response.getOrgs().add(saved);
        }
        if (orgPost.getOrgs().size() == 1) {
            handleSingleResponse(response);
        }
        return response;
    }

    @DeleteMapping(value = "/{orgId}")
    @PreAuthorize("@mss.hasOrgPrivilege(authentication, #orgId, 'ORG_DELETE', false)")
    public OrganizationsResponse deleteOrg(@PathVariable String orgId) {

        OrganizationsResponse response = new OrganizationsResponse();
        Optional<OrgJson> orgOption = organizationRepository.findById(orgId);
        if (!orgOption.isPresent()) {
            throw new NotFoundException(response.addMessage("Organization not found."));
        }
        if (!projectPersistence.findAllByOrgId(orgId).isEmpty()) {
            throw new BadRequestException(response.addMessage("Organization is not empty"));
        }
        OrgJson deleted = organizationRepository.deleteById(orgId);
        response.setOrgs(List.of(deleted));
        return response;
    }
}
