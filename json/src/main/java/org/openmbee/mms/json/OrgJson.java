package org.openmbee.mms.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties({BaseJson.COMMITID, BaseJson.REFID, BaseJson.PROJECTID, BaseJson.TYPE, "empty"})
@Schema(name = "Org", requiredProperties = {BaseJson.NAME})
public class OrgJson extends BaseJson<OrgJson> {
    public static final String DELETED = "deleted";

    public OrgJson setDeleted(boolean deleted) {
        this.put(DELETED, deleted);
        return this;
    }
}
