package org.openmbee.mms.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties({BaseJson.COMMITID, BaseJson.REFID, BaseJson.PROJECTID, BaseJson.TYPE,
    BaseJson.ID, "empty"})
@Schema(name = "Group", requiredProperties = {BaseJson.NAME})
public class GroupJson extends BaseJson<GroupJson> {

}
