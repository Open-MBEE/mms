package org.openmbee.mms.twc.utilities;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openmbee.mms.twc.TeamworkCloud;
import org.openmbee.mms.twc.TeamworkCloudEndpoints;
import org.openmbee.mms.twc.constants.TwcConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class TwcPermissionUtils {

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

	public TwcPermissionUtils() {

	}

	/**
	 * This function will get the MMS TWC roles map Gets the Available roles for a
	 * given Resource ID Gets the list of users for a given Workspace ID ,Resource
	 * ID and role ID Checks whether that current user is part of that list or not
	 * This method works only for Custom scope ,will not work for Global scope -
	 * Need to come up with a method for Global scope
	 * 
	 * @param workspaceId
	 * @param resourceId
	 * @param twc
	 * @param user
	 * @param privilege
	 * @param twcRoles
	 * @return
	 */

	public boolean hasPermissionToAccessProject(String workspaceId, String resourceId, TeamworkCloud twc, String user,
			String privilege, List<String> twcRoles) {

		// TODO:: add distributed caching for performance

		Map<String, String> projectRoleIdMap = getTwcRolesForGivenResourceId(resourceId, twc);
		List<String> users = null;
		String roleId = null;

		if (projectRoleIdMap == null || twcRoles == null || twcRoles.isEmpty())
			return false;

		for (int inx = 0; inx < twcRoles.size(); inx++) {
			roleId = projectRoleIdMap.get(twcRoles.get(inx));
			users = getUsersList(twc, resourceId, roleId);
			if (users != null && users.contains(user))
				return true;
		}
		return false;
	}

	/**
	 * Function takes in Workspace ID ,resource ID ,role ID and then gets the list
	 * of users who have access to this project
	 * 
	 * @param workspaceId
	 * @param twc
	 * @param resourceId
	 * @param roleId
	 * @return
	 */
	public List<String> getUsersList(TeamworkCloud twc, String resourceId, String roleId) {

		// TODO:: add distributed caching for performance

		List<String> users = null;
		ResponseEntity<String> respEntity = getRestResponse(
				TeamworkCloudEndpoints.GETPROJECTUSERS.buildUrl(twc, resourceId, roleId), twc);

		if (respEntity == null || respEntity.getBody() == null)
			return null;

		JSONArray usersJsonArray = jsonUtils.parseStringToJsonArray(respEntity.getBody());
		users = jsonUtils.convertJsonArrayToStringArray(usersJsonArray);

		return users;
	}

	/**
	 * It takes in the resource ID and then returns the available role ID's for that
	 * projects The reason to get Role ID is to query the users based on role ID
	 * 
	 * @param resourceId
	 * @param twc
	 * @return
	 */
	public Map<String, String> getTwcRolesForGivenResourceId(String resourceId, TeamworkCloud twc) {
		
		//TODO:: add distributed caching for performance

		Map<String, String> roleNameIDMap = new HashMap<String, String>();
		ResponseEntity<String> respEntity = getRestResponse(TeamworkCloudEndpoints.GETROLESID.buildUrl(twc, resourceId),
				twc);

		if (respEntity == null || respEntity.getBody() == null)
			return null;

		JSONArray rolesJsonArray = jsonUtils.parseStringToJsonArray(respEntity.getBody());
		if (rolesJsonArray != null) {
			for (int idx = 0; idx < rolesJsonArray.length(); idx++) {
				JSONObject roleJsonObj = rolesJsonArray.getJSONObject(idx);
				roleNameIDMap.put(roleJsonObj.getString(TwcConstants.NAME_JSONOBJECT),
						roleJsonObj.getString(TwcConstants.ID_JSONOBJECT));
			}
		}

		return roleNameIDMap;
	}

	/**
	 * This method is used to establish connection twc Rest API's by calling
	 * Teamwork cloud endpoints Time being added Admin account .Later need to
	 * implement secure methods like CyberArk
	 * 
	 * @param twcRestUrl
	 * @return
	 */
	private ResponseEntity<String> getRestResponse(String twcRestUrl, TeamworkCloud twc) {
		RestTemplate restTemplate = restUtils.getRestTemplate();
		HttpHeaders headers = restUtils.basicAuthHeader(twc.getAdminUsername(), twc.getAdminPwd());
		ResponseEntity<String> respEntity = null;

		try {
			HttpEntity<String> entityReq = new HttpEntity<>(null, headers);
			respEntity = restTemplate.exchange(twcRestUrl, HttpMethod.GET, entityReq, String.class);

		} catch (Exception Ex) {
			return null;
		}
		return respEntity;
	}

}