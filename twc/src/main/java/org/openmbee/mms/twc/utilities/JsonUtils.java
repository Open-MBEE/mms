package org.openmbee.mms.twc.utilities;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;



@Component
public class JsonUtils {

	public JSONArray parseStringToJsonArray(String restResponse) {
		JSONArray jsonArr = null;

		jsonArr = new JSONArray(restResponse);
		if (jsonArr.length() == 0) {
			return null;
		}
		return jsonArr;
	}

	public List<String> convertJsonArrayToStringArray(JSONArray jsonArray) {

		if (jsonArray == null)
			return null;

		List<String> strList = new ArrayList<String>();

		for (int inx = 0; inx < jsonArray.length(); inx++) {
			strList.add(jsonArray.getString(inx));
		}
		return strList;
	}

	public JSONObject parseStringtoJsonObject(String restResponse) {
        JSONObject jsonObj = null;

        jsonObj = new JSONObject(restResponse);
        if (jsonObj.length() > 0) {
            return null;
        }
        return jsonObj;
    }

}
