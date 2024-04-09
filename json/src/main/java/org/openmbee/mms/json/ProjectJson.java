package org.openmbee.mms.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties({BaseJson.REFID, BaseJson.COMMITID, BaseJson.TYPE, "empty"})
@Schema(name = "Project", requiredProperties = {ProjectJson.ORGID, BaseJson.NAME})
public class ProjectJson extends BaseJson<ProjectJson> {

    public static final String ORGID = "orgId";
    public static final String PROJECTTYPE = "schema";
    public static final String DELETED = "deleted";

    @Override
    public String getProjectId() {
        return (String) this.get(ID);
    }

    public String getOrgId() {
        return (String) this.get(ORGID);
    }

    @Schema(description = "Acceptable values depends on the specific build and implementation (ex. default, cameo, jupyter), "
        + "if a value isn't supported, will fall back to default. This can influence the project's underlying schema used and "
        + "additional validation or processing when getting or updating elements.", defaultValue = "default")
    @JsonProperty("schema")
    public String getProjectType() {
        return (String) this.get(PROJECTTYPE);
    }

    @JsonProperty("schema")
    public ProjectJson setProjectType(String projectType) {
        this.put(PROJECTTYPE, projectType);
        return this;
    }

    public ProjectJson setOrgId(String orgId) {
        this.put(ORGID, orgId);
        return this;
    }
}
