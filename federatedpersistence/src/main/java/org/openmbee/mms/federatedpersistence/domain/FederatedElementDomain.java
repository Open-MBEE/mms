package org.openmbee.mms.federatedpersistence.domain;

import java.util.Optional;

import org.openmbee.mms.data.dao.ProjectDAO;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.core.utils.ElementUtils;
import org.openmbee.mms.crud.domain.ElementDomain;
import org.openmbee.mms.crud.services.ElementUtilsFactory;
import org.openmbee.mms.data.domains.global.Project;
import org.openmbee.mms.json.ElementJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FederatedElementDomain implements ElementDomain {
    private static final Logger logger = LoggerFactory.getLogger(FederatedNodeChangeDomain.class);
    
    private ProjectDAO projectRepository;
    private ElementUtilsFactory elementUtilsFactory;
    
    @Autowired
    public void setProjectRepository(ProjectDAO projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Autowired
    public void setElementUtilsFactory(ElementUtilsFactory elementUtilsFactory) {
        this.elementUtilsFactory = elementUtilsFactory;
    }

    @Override
    public ElementUtils getElementUtils(String projectId) {
        return elementUtilsFactory.getElementUtil(getProjectType(projectId));
    }

    @Override
    public Integer getNodeType(String projectId, ElementJson element){
        if (null == projectId) {
            return 0;
        } 

        ElementUtils elementUtils = getElementUtils(projectId);
        if (null == elementUtils) {
            return 0;
        } 
        return elementUtils.getNodeType(element).ordinal()+1;
    }

    private String getProjectType(String projectId) {
        return getProject(projectId).getProjectType();
    }

    private Project getProject(String projectId) {
        Optional<Project> p = projectRepository.findByProjectId(projectId);
        if (p.isPresent()) {
            return p.get();
        }
        throw new NotFoundException("project not found");
    }

}