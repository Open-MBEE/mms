package org.openmbee.mms.crud.controllers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.openmbee.mms.core.dao.BranchPersistence;
import org.openmbee.mms.core.dao.ProjectPersistence;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.exceptions.ConflictException;
import org.openmbee.mms.core.exceptions.DeletedException;
import org.openmbee.mms.core.exceptions.ForbiddenException;
import org.openmbee.mms.core.exceptions.InternalErrorException;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.core.exceptions.NotModifiedException;
import org.openmbee.mms.core.exceptions.UnauthorizedException;
import org.openmbee.mms.core.objects.BaseResponse;
import org.openmbee.mms.core.objects.Rejection;
import org.openmbee.mms.core.security.MethodSecurityService;
import org.openmbee.mms.core.services.NodeService;
import org.openmbee.mms.core.services.PermissionService;
import org.openmbee.mms.crud.services.ServiceFactory;
import org.openmbee.mms.json.ProjectJson;
import org.openmbee.mms.json.RefJson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public abstract class BaseController {

    protected final Logger logger = LoggerFactory.getLogger(getClass());

    protected ObjectMapper om;

    protected ServiceFactory serviceFactory;

    protected ProjectPersistence projectPersistence;

    protected BranchPersistence branchPersistence;

    protected PermissionService permissionService;

    protected MethodSecurityService mss;

    @Autowired
    public void setServiceFactory(ServiceFactory serviceFactory) {
        this.serviceFactory = serviceFactory;
    }

    @Autowired
    public void setProjectPersistence(ProjectPersistence projectPersistence) {
        this.projectPersistence = projectPersistence;
    }

    @Autowired
    public void setBranchPersistence(BranchPersistence branchPersistence) {
        this.branchPersistence = branchPersistence;
    }

    @Autowired
    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Autowired
    public void setMss(MethodSecurityService mss) {
        this.mss = mss;
    }

    @Autowired
    public void setObjectMapper(ObjectMapper om) {
        this.om = om;
    }

    public Map<String, Object> convertToMap(Object obj) {
        return om.convertValue(obj, new TypeReference<Map<String, Object>>() {});
    }

    protected void findBranch(String projectId, String id){
        getProjectType(projectId);
        Optional<RefJson> branchesOption = branchPersistence.findById(projectId, id);
        if (!branchesOption.isPresent()) {
            throw new NotFoundException("branch not found");
        }
    }

    protected String getProjectType(String projectId) {
        Optional<ProjectJson> p = projectPersistence.findById(projectId);
        if (p.isPresent()) {
            return p.get().getProjectType();
        }
        throw new NotFoundException("project not found");
    }

    protected NodeService getNodeService(String projectId) {
        return serviceFactory.getNodeService(this.getProjectType(projectId));
    }

    protected void handleSingleResponse(BaseResponse res) {
        if (res.getRejected() != null && !res.getRejected().isEmpty()) {
            List<Rejection> rejected = res.getRejected();
            int code = rejected.get(0).getCode();
            switch(code) {
                case 304:
                    throw new NotModifiedException(res);
                case 400:
                    throw new BadRequestException(res);
                case 401:
                    throw new UnauthorizedException(res);
                case 403:
                    throw new ForbiddenException(res);
                case 404:
                    throw new NotFoundException(res);
                case 409:
                    throw new ConflictException(res);
                case 410:
                    throw new DeletedException(res);
                case 500:
                    throw new InternalErrorException(res);
                default:
                    break;
            }
        }
    }
}
