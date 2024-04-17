package org.openmbee.mms.permissions;

import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.ArrayList;
import java.util.List;
import org.openmbee.mms.core.exceptions.MMSException;
import org.openmbee.mms.core.objects.Rejection;
import org.openmbee.mms.core.security.MethodSecurityService;
import org.openmbee.mms.permissions.objects.PermissionLookup;
import org.openmbee.mms.permissions.objects.PermissionLookupRequest;
import org.openmbee.mms.permissions.objects.PermissionLookupResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Auth")
public class PermissionsLookupController {

    MethodSecurityService mss;

    @Autowired
    public PermissionsLookupController(MethodSecurityService mss) {
        this.mss = mss;
    }

    @PutMapping(value = "/permissions", consumes = MediaType.APPLICATION_JSON_VALUE)
    public PermissionLookupResponse lookupPermissions(
            @RequestBody PermissionLookupRequest req,
            Authentication auth) {
        PermissionLookupResponse res = new PermissionLookupResponse();
        List<PermissionLookup> lookups = new ArrayList<>();
        res.setLookups(lookups);
        res.setAllPassed(true);
        for (PermissionLookup lookup: req.getLookups()) {
            try {
                boolean result;
                String pri = lookup.getPrivilege().name();
                boolean anon = lookup.isAllowAnonIfPublic();
                switch(lookup.getType()) {
                    case ORG:
                        result = mss.hasOrgPrivilege(auth, lookup.getOrgId(), pri, anon);
                        break;
                    case PROJECT:
                        result = mss.hasProjectPrivilege(auth, lookup.getProjectId(), pri, anon);
                        break;
                    case BRANCH:
                        result = mss.hasBranchPrivilege(auth, lookup.getProjectId(), lookup.getRefId(), pri, anon);
                        break;
                    case GROUP:
                        result = mss.hasGroupPrivilege(auth, lookup.getGroupName(), pri, anon);
                        break;
                    default:
                        result = false;
                }
                lookup.setHasPrivilege(result);
                lookups.add(lookup);
                if (!result) {
                    res.setAllPassed(false);
                }
            } catch (MMSException e) {
                res.addRejection(new Rejection(lookup, e.getCode().value(), e.getMessage()));
                res.setAllPassed(false);
            } catch (Exception e) {
                res.addRejection(new Rejection(lookup, HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage()));
                res.setAllPassed(false);
            }
        }
        return res;
    }
}
