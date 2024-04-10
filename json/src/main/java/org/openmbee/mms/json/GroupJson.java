package org.openmbee.mms.json;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;

@JsonIgnoreProperties({BaseJson.COMMITID, BaseJson.REFID, BaseJson.PROJECTID, BaseJson.ID, "empty"})
@Schema(name = "Group", requiredProperties = {BaseJson.NAME, BaseJson.TYPE})
public class GroupJson extends BaseJson<GroupJson> {

}
