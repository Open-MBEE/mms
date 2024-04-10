package org.openmbee.mms.twc.utilities;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openmbee.mms.twc.TeamworkCloud;
import org.openmbee.mms.twc.TeamworkCloudEndpoints;
import org.springframework.beans.factory.annotation.Autowired;
import org.openmbee.mms.data.domains.global.Group;
import org.openmbee.mms.data.domains.global.User;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

public class AdminUtils {
    private RestUtils restUtils;

    private JsonUtils jsonUtils;

    @Autowired
    public void setRestUtils(RestUtils restUtils) {
        this.restUtils = restUtils;
    }

    @Autowired
    public void setJsonUtils(JsonUtils jsonUtils) {
        this.jsonUtils = jsonUtils;
    }

    public AdminUtils() {

    };

    public JSONObject getUserByUsername(String username, TeamworkCloud twc) {
        ResponseEntity<String> respEntity = restUtils.getRestResponse(
            TeamworkCloudEndpoints.GETUSER.buildUrl(twc, username), twc);

        if (respEntity == null || respEntity.getBody() == null)
            return null;

        return jsonUtils.parseStringtoJsonObject(respEntity.getBody());
    }
}
