package org.openmbee.mms.view.controllers;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.Map;

import org.openmbee.mms.view.services.ViewService;
import org.openmbee.mms.core.objects.ElementsRequest;
import org.openmbee.mms.core.objects.ElementsResponse;
import org.openmbee.mms.core.services.GenericServiceFactory;
import org.openmbee.mms.crud.controllers.BaseController;
import org.openmbee.mms.json.MountJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/projects/{projectId}/refs/{refId}")
@Tag(name = "Views")
public class VeController extends BaseController {

    private GenericServiceFactory genericServiceFactory;

    @Autowired
    public void setGenericServiceFactory( GenericServiceFactory serviceFactory){
        this.genericServiceFactory = serviceFactory;
    }

    @GetMapping("/mounts")
    @PreAuthorize("@mss.hasBranchPrivilege(authentication, #projectId, #refId, 'BRANCH_READ', true)")
    public MountsResponse getMounts(
        @PathVariable String projectId,
        @PathVariable String refId,
        @RequestParam(required = false) String commitId,
        @RequestParam(required = false) Map<String, String> params) {
        MountJson json = genericServiceFactory.getServiceForSchema( ViewService.class ,getProjectType(projectId)).getProjectUsages(projectId, refId, params.get("commitId"), new ArrayList<>(), true);
        MountsResponse res = new MountsResponse();
        res.getProjects().add(json);
        return res;
    }

    @GetMapping("/documents")
    @PreAuthorize("@mss.hasBranchPrivilege(authentication, #projectId, #refId, 'BRANCH_READ', true)")
    public DocumentsResponse getDocuments(
        @PathVariable String projectId,
        @PathVariable String refId,
        @RequestParam(required = false) String commitId,
        @RequestParam(required = false) Map<String, String> params) {

        ElementsResponse docs = genericServiceFactory.getServiceForSchema( ViewService.class , getProjectType(projectId)).getDocuments(projectId, refId, params);
        return (new DocumentsResponse()).setDocuments(docs.getElements());
    }

    @GetMapping("/views/{viewId}")
    @PreAuthorize("@mss.hasBranchPrivilege(authentication, #projectId, #refId, 'BRANCH_READ', true)")
    public ElementsResponse getView(
        @PathVariable String projectId,
        @PathVariable String refId,
        @PathVariable String viewId,
        @RequestParam(required = false) String commitId,
        @RequestParam(required = false) Map<String, String> params) {

        ElementsResponse res = genericServiceFactory.getServiceForSchema( ViewService.class , getProjectType(projectId)).getView(projectId, refId, viewId, params);
        handleSingleResponse(res);
        return res;
    }

    @PutMapping("/views")
    @PreAuthorize("@mss.hasBranchPrivilege(authentication, #projectId, #refId, 'BRANCH_READ', true)")
    public ElementsResponse getViews(
        @PathVariable String projectId,
        @PathVariable String refId,
        @RequestBody ElementsRequest req,
        @RequestParam(required = false) String commitId,
        @RequestParam(required = false) Map<String, String> params) {

        return genericServiceFactory.getServiceForSchema( ViewService.class , getProjectType(projectId)).getViews(projectId, refId, req, params);
    }

    @PostMapping("/views")
    @PreAuthorize("@mss.hasBranchPrivilege(authentication, #projectId, #refId, 'BRANCH_EDIT_CONTENT', false)")
    public ElementsResponse createOrUpdateViews(
        @PathVariable String projectId,
        @PathVariable String refId,
        @RequestBody ElementsRequest req,
        @RequestParam(required = false) String overwrite,
        @RequestParam(required = false) Map<String, String> params,
        @Parameter(hidden = true) Authentication auth) {

        ViewService viewService = genericServiceFactory.getServiceForSchema( ViewService.class , getProjectType(projectId));
        ElementsResponse res = viewService.createOrUpdate(projectId, refId, req, params, auth.getName());
        viewService.addChildViews(res, params);
        return res;
    }

    @GetMapping("/groups")
    @PreAuthorize("@mss.hasBranchPrivilege(authentication, #projectId, #refId, 'BRANCH_READ', true)")
    public GroupsResponse getGroups(
        @PathVariable String projectId,
        @PathVariable String refId,
        @RequestParam(required = false) Map<String, String> params) {

        ElementsResponse groups = genericServiceFactory.getServiceForSchema( ViewService.class , getProjectType(projectId)).getGroups(projectId, refId, params);
        return (new GroupsResponse()).setGroups(groups.getElements());
    }
}
