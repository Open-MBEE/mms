package org.openmbee.mms.crud.domain;

import org.openmbee.mms.core.config.Formats;
import org.openmbee.mms.crud.CrudConstants;
import org.openmbee.mms.json.CommitJson;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;

@Component
public class CommitDomain {

    public void initCommitJson(CommitJson cmjs, Instant now) {
        cmjs.setCreated(Formats.FORMATTER.format(now));
        cmjs.setAdded(new ArrayList<>());
        cmjs.setDeleted(new ArrayList<>());
        cmjs.setUpdated(new ArrayList<>());
        cmjs.setType(CrudConstants.COMMIT);
    }

}
