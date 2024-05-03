package org.openmbee.mms.crud.controllers.branches;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Optional;
import java.util.UUID;

import org.openmbee.mms.core.config.Constants;
import org.openmbee.mms.core.config.Privileges;
import org.openmbee.mms.core.exceptions.BadRequestException;
import org.openmbee.mms.core.exceptions.MMSException;
import org.openmbee.mms.core.objects.RefsRequest;
import org.openmbee.mms.core.objects.RefsResponse;
import org.openmbee.mms.core.objects.Rejection;
import org.openmbee.mms.core.services.BranchService;
import org.openmbee.mms.crud.controllers.BaseController;
import org.openmbee.mms.json.RefJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

import static org.openmbee.mms.core.config.Constants.BRANCH_ID_VALID_PATTERN;

@RestController
@RequestMapping("/projects/{projectId}/refs")
@Tag(name = "Refs")
public class BranchesController extends BaseController {

    private BranchService branchService;

    @Autowired
    public BranchesController(BranchService branchService) {
        this.branchService = branchService;
    }

    @GetMapping
    @PreAuthorize("@mss.hasProjectPrivilege(authentication, #projectId, 'PROJECT_READ', true)")
    public RefsResponse getAllRefs(
        @PathVariable String projectId,
        @RequestParam(required = false, defaultValue = Constants.FALSE) boolean includeDeleted,
        Authentication auth) {

        getProjectType(projectId);
        RefsResponse res = branchService.getBranches(projectId);
        List<RefJson> filtered = new ArrayList<>();
        if (!permissionService.isProjectPublic(projectId)) {
            for (RefJson ref: res.getRefs()) {
                try {
                    if (mss.hasBranchPrivilege(auth, projectId, ref.getId(),
                        Privileges.BRANCH_READ.name(), false)
                        && (!ref.isDeleted() || includeDeleted))
                        {
                        filtered.add(ref);
                    }
                } catch (MMSException e) {
                    logger.warn("error in getting branch permissions: projectId=" +
                        projectId + ", refId=" + ref.getId(), e);
                }
            }
        } else if (!includeDeleted) {
            for (RefJson ref: res.getRefs()) {
                if (!ref.isDeleted()) {
                    filtered.add(ref);
                }
            }
        } else {
            filtered = res.getRefs();
        }
        res.setRefs(filtered);
        return res;
    }

    @GetMapping(value = "/{refId}")
    @PreAuthorize("@mss.hasBranchPrivilege(authentication, #projectId, #refId, 'BRANCH_READ', true)")
    public RefsResponse getRef(
        @PathVariable String projectId,
        @PathVariable String refId) {

        return branchService.getBranch(projectId, refId);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("@mss.hasProjectPrivilege(authentication, #projectId, 'PROJECT_CREATE_BRANCH', false)")
    public RefsResponse createRefs( //this can also handle update
        @PathVariable String projectId,
        @RequestBody RefsRequest projectsPost,
        Authentication auth) {

        if (projectsPost.getRefs().isEmpty()) {
            throw new BadRequestException(new RefsResponse().addMessage("Empty request"));
        }
        RefsResponse response = new RefsResponse();
        for (RefJson branch : projectsPost.getRefs()) {
            try {
                if (branch.getId() == null || branch.getId().isEmpty()) {
                    branch.setId(UUID.randomUUID().toString());
                }
                if(!isBranchIdValid(branch.getId())) {
                    response.addRejection(new Rejection(branch, 400, "Branch id is invalid."));
                    continue;
                }
                Optional<RefJson> existingOptional = branchPersistence.findById(projectId, branch.getId());
                if (existingOptional.isPresent()) {
                    //Branch exists, should merge the json, but cannot change parent ref or commit
                    //if branch exists it will get resurrected if it was deleted
                    RefJson existing = existingOptional.get();
                    if (branch.getParentRefId() != null && !branch.getParentRefId().equals(existing.getParentRefId()) ||
                        branch.getParentCommitId() != null && !branch.getParentCommitId().equals(existing.getParentCommitId())) {
                        response.addRejection(new Rejection(branch, 400, "Cannot change existing branch's origin"));
                        continue;
                    }
                    branch.merge(existingOptional.get());
                    RefJson res = branchService.updateBranch(projectId, branch);
                    response.getRefs().add(res);
                    continue;
                }
                RefJson res;
                branch.setCreator(auth.getName());
                res = branchService.createBranch(projectId, branch);
                permissionService.initBranchPerms(projectId, branch.getId(), true, auth.getName());
                response.getRefs().add(res);
            } catch (MMSException e) {
                response.addRejection(new Rejection(branch, e.getCode().value(), e.getMessageObject().toString()));
            }
        }
        if (projectsPost.getRefs().size() == 1) {
            handleSingleResponse(response);
        }
        return response;
    }

    @DeleteMapping("/{refId}")
    @PreAuthorize("@mss.hasBranchPrivilege(authentication, #projectId, #refId, 'BRANCH_DELETE', false)")
    public RefsResponse deleteRef(
        @PathVariable String projectId,
        @PathVariable String refId) {

        return branchService.deleteBranch(projectId, refId);
    }

    static boolean isBranchIdValid(String branchId) {
        return branchId != null && BRANCH_ID_VALID_PATTERN.matcher(branchId).matches();
    }
}
