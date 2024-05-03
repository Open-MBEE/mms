package org.openmbee.mms.crud.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.openmbee.mms.core.config.Constants;
import org.openmbee.mms.core.dao.OrgPersistence;
import org.openmbee.mms.core.dao.ProjectPersistence;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.core.objects.OrganizationsResponse;
import org.openmbee.mms.json.OrgJson;
import org.openmbee.mms.json.ProjectJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class OrgDeleteService {
    private OrgPersistence orgPersistence;
    private ProjectPersistence projectPersistence;
    private ProjectDeleteService projectDeleteService;
    protected ObjectMapper om;

    @Autowired
    public void setOrgPersistence(OrgPersistence orgPersistence) {
        this.orgPersistence = orgPersistence;
    }

    @Autowired
    public void setProjectPersistence(ProjectPersistence projectPersistence) {
        this.projectPersistence = projectPersistence;
    }

    @Autowired
    public void setProjectDeleteService(ProjectDeleteService projectDeleteService) {
        this.projectDeleteService = projectDeleteService;
    }

    @Autowired
    public void setOm(ObjectMapper om) {
        this.om = om;
    }

    public OrganizationsResponse deleteOrg(String orgId, boolean hard) {
        OrganizationsResponse response = new OrganizationsResponse();
        OrgJson orgJson;
        Optional<OrgJson> orgJsonOption = orgPersistence.findById(orgId);

        List<OrgJson> res = new ArrayList<>();

        //Do not try to do a soft delete when an error condition is present.
        if(orgJsonOption.isEmpty() && !hard) {
            throw new NotFoundException("Project state is invalid");
        }

        orgJson = orgJsonOption.orElseGet(() -> {
            OrgJson newOrg = new OrgJson();
            newOrg.setId(orgId);
            return newOrg;
        });

        if(hard){
            orgPersistence.deleteById(orgId);
            orgJson.setDeleted(true);
        } else {
            List<ProjectJson> orgProjs = projectPersistence.findAllByOrgId(orgId).stream().collect(Collectors.toList());
            //Archive all projects contained by org
            for (ProjectJson proj: orgProjs) {
                projectDeleteService.deleteProject(proj.getId(), false);
            }
            orgPersistence.archiveById(orgId);
            orgJson.setIsArchived(Constants.TRUE);
        }

        
        res.add(orgJson);
        return response.setOrgs(res);
    }
}
