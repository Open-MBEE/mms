package org.openmbee.mms.core.security;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.openmbee.mms.core.dao.BranchPersistence;
import org.openmbee.mms.core.dao.ProjectPersistence;
import org.openmbee.mms.core.exceptions.NotFoundException;
import org.openmbee.mms.core.config.ContextHolder;
import org.openmbee.mms.core.dao.OrgPersistence;
import org.openmbee.mms.core.services.PermissionService;
import org.openmbee.mms.core.utils.AuthenticationUtils;
import org.openmbee.mms.json.OrgJson;
import org.openmbee.mms.json.ProjectJson;
import org.openmbee.mms.json.RefJson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service("mss")
public class MethodSecurityService {

    private PermissionService permissionService;
    private ProjectPersistence projectPersistence;
    private BranchPersistence branchPersistence;
    private OrgPersistence orgPersistence;

    @Autowired
    public void setPermissionService(PermissionService permissionService) {
        this.permissionService = permissionService;
    }

    @Autowired
    public void setOrgPersistence(OrgPersistence orgPersistence) {
        this.orgPersistence = orgPersistence;
    }

    @Autowired
    public void setProjectPersistence(ProjectPersistence projectPersistence) {
        this.projectPersistence = projectPersistence;
    }

    @Autowired
    public void setBranchPersistence(BranchPersistence branchPersistence) {
        this.branchPersistence = branchPersistence;
    }

    public boolean hasOrgPrivilege(Authentication authentication, String orgId, String privilege, boolean allowAnonIfPublic) {
        CompletableFuture<Boolean> permissionsFuture = CompletableFuture.supplyAsync(() ->
        {
            if (allowAnonIfPublic && permissionService.isOrgPublic(orgId)) {
                return true;
            }
            if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
                return false;
            }
            if (permissionService.hasOrgPrivilege(privilege, authentication.getName(), AuthenticationUtils.getGroups(authentication), orgId)) {
                return true;
            }
            return false;
        });

        CompletableFuture<Boolean> existsFuture = CompletableFuture.supplyAsync(() -> orgExists(orgId));
        return completeFutures(permissionsFuture, existsFuture, "Org");
    }

    public boolean hasProjectPrivilege(Authentication authentication, String projectId, String privilege, boolean allowAnonIfPublic) {
        CompletableFuture<Boolean> permissionsFuture = CompletableFuture.supplyAsync(() ->
        {
            if (allowAnonIfPublic && permissionService.isProjectPublic(projectId)) {
                return true;
            }
            if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
                return false;
            }
            if (permissionService.hasProjectPrivilege(privilege, authentication.getName(), AuthenticationUtils.getGroups(authentication), projectId)) {
                return true;
            }
            return false;
        });

        CompletableFuture<Boolean> existsFuture = CompletableFuture.supplyAsync(() -> projectExists(projectId));
        return completeFutures(permissionsFuture, existsFuture, "Project");
    }

    public boolean hasBranchPrivilege(Authentication authentication, String projectId, String branchId, String privilege, boolean allowAnonIfPublic) {
        CompletableFuture<Boolean> permissionsFuture = CompletableFuture.supplyAsync(() ->
        {
            if (allowAnonIfPublic && permissionService.isProjectPublic(projectId)) {
                return true;
            }
            if (authentication == null || authentication instanceof AnonymousAuthenticationToken) {
                return false;
            }
            if (permissionService.hasBranchPrivilege(privilege, authentication.getName(), AuthenticationUtils.getGroups(authentication), projectId, branchId)) {
                return true;
            }
            return false;
        });

        CompletableFuture<Boolean> existsFuture = CompletableFuture.supplyAsync(() -> branchExists(projectId, branchId));
        return completeFutures(permissionsFuture, existsFuture, "Branch");
    }

    private boolean orgExists(String orgId) {
        Optional<OrgJson> o = orgPersistence.findById(orgId);
        return o.isPresent();
    }

    private boolean projectExists(String projectId) {
        ContextHolder.getContext().setProjectId(projectId);
        Optional<ProjectJson> p = projectPersistence.findById(projectId);
        return p.isPresent();
    }

    private boolean branchExists(String projectId, String branchId){
        if(! projectExists(projectId)) {
            return false;
        }
        Optional<RefJson> branchesOption = branchPersistence.findById(projectId, branchId);
        return branchesOption.isPresent();
    }

    private boolean completeFutures(CompletableFuture<Boolean> permissionsFuture, CompletableFuture<Boolean> existsFuture, String context) {
        try {
            if (!isBooleanFutureTrue(permissionsFuture)){
                return false;
            }
            if (!isBooleanFutureTrue(existsFuture)){
                throw new NotFoundException(context + " not found");
            }
            return true;
        } catch(CompletionException ex) {
            if(ex.getCause() instanceof RuntimeException){
                throw (RuntimeException) ex.getCause();
            }
            return false;
        }
    }

    private boolean isBooleanFutureTrue(CompletableFuture<Boolean> future) {
        if(future != null) {
            Boolean joinResult = future.join();
            return joinResult != null && joinResult;
        }
        return false;
    }
}
