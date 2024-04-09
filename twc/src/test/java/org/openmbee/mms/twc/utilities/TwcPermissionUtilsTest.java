package org.openmbee.mms.twc.utilities;

import org.junit.Before;
import org.junit.Test;
import org.openmbee.mms.twc.TeamworkCloud;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.*;

import static junit.framework.TestCase.assertNull;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TwcPermissionUtilsTest {

	private final String adminUserName = "AdminUser";
	private final String adminPwd = "AdminPwd";

	private JsonUtils jsonUtils;
	private RestUtils restUtils;
	private TeamworkCloud twc;
	private RestTemplate mockRestTemplate;
	private HttpHeaders httpHeaders;
	private Map<String, String> projectRoleIdMap;
	private String resourceId;
	private String workspaceId;
	private String roleId;
	private List<String> actualUsers;
	private List<String> twcRoles;
	private String user;
	private String privilege;

	@Before
	public void setup() {
		twc = new TeamworkCloud();
		twc.setAliases(Collections.emptyList());
		twc.setUrl("twc.domain.com");
		twc.setPort("8111");
		twc.setProtocol("http");

		restUtils = mock(RestUtils.class);
		mockRestTemplate = mock(RestTemplate.class);
		jsonUtils = mock(JsonUtils.class);
		httpHeaders = mock(HttpHeaders.class);
		resourceId = "resourceID1";
		workspaceId = "workspaceID1";
		roleId = "roleID1";
		user = "user1";
		privilege = "read";

		when(restUtils.getRestTemplate()).thenReturn(mockRestTemplate);
	}

	@Test
	public void hasPermissionUserListNullTest() {

		boolean hasPermission;
		TwcPermissionUtils twcPermUtils = new TwcPermissionUtils();
		twcPermUtils.setJsonUtils(jsonUtils);
		twcPermUtils.setRestUtils(restUtils);
		String userName = "UserId1";

		List<String> expectedUsers = null;

		JSONArray jsonArrRoles = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("Permission", "TWC_permissions");
		jsonObj.put("name", "Reviewer");
		jsonObj.put("ID", "12345");
		jsonArrRoles.put(jsonObj);

		twcRoles = new ArrayList<String>();
		twcRoles.add("Reviewer");

		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonArrRoles.toString(), httpHeaders,
				HttpStatus.OK);

		when(jsonUtils.parseStringToJsonArray(responseEntity.getBody())).thenReturn(jsonArrRoles);

		when(jsonUtils.convertJsonArrayToStringArray(jsonArrRoles)).thenReturn(expectedUsers);

		when(restUtils.basicAuthHeader(adminUserName, adminPwd)).thenReturn(httpHeaders);

		when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
				.thenAnswer((invocation) -> {

					return responseEntity;
				});

		hasPermission = twcPermUtils.hasPermissionToAccessProject(workspaceId, resourceId, twc, userName, privilege,
				twcRoles);
		assertFalse(hasPermission);

	}

	@Test
	public void hasPermissionPassTest() {

		boolean hasPermission;
		TwcPermissionUtils twcPermUtils = new TwcPermissionUtils();
		twcPermUtils.setJsonUtils(jsonUtils);
		twcPermUtils.setRestUtils(restUtils);
		String userName = "UserId1";

		List<String> expectedUsers = new ArrayList<String>();
		expectedUsers.add("UserId1");
		expectedUsers.add("UserId2");
		expectedUsers.add("UserId3");

		JSONArray jsonArrRoles = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("Permission", "TWC_permissions");
		jsonObj.put("name", "Reviewer");
		jsonObj.put("ID", "12345");
		jsonArrRoles.put(jsonObj);

		twcRoles = new ArrayList<String>();
		twcRoles.add("Reviewer");

		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonArrRoles.toString(), httpHeaders,
				HttpStatus.OK);

		when(jsonUtils.parseStringToJsonArray(responseEntity.getBody())).thenReturn(jsonArrRoles);

		when(jsonUtils.convertJsonArrayToStringArray(jsonArrRoles)).thenReturn(expectedUsers);

		when(restUtils.basicAuthHeader(adminUserName, adminPwd)).thenReturn(httpHeaders);

		when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
				.thenAnswer((invocation) -> {

					return responseEntity;
				});

		hasPermission = twcPermUtils.hasPermissionToAccessProject(workspaceId, resourceId, twc, userName, privilege,
				twcRoles);
		assertTrue(hasPermission);

	}

	@Test
	public void hasPermissionFailTest() {

		boolean hasPermission;
		TwcPermissionUtils twcPermUtils = new TwcPermissionUtils();
		twcPermUtils.setJsonUtils(jsonUtils);
		twcPermUtils.setRestUtils(restUtils);
		String userName = "UserId1";

		List<String> expectedUsers = new ArrayList<String>();
		expectedUsers.add("User1");
		expectedUsers.add("User2");
		expectedUsers.add("User3");

		JSONArray jsonArrRoles = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("Permission", "TWC_permissions");
		jsonObj.put("name", "Reviewer");
		jsonObj.put("ID", "12345");
		jsonArrRoles.put(jsonObj);

		twcRoles = new ArrayList<String>();
		twcRoles.add("Reviewer");

		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonArrRoles.toString(), httpHeaders,
				HttpStatus.OK);

		when(jsonUtils.parseStringToJsonArray(responseEntity.getBody())).thenReturn(jsonArrRoles);

		when(jsonUtils.convertJsonArrayToStringArray(jsonArrRoles)).thenReturn(expectedUsers);

		when(restUtils.basicAuthHeader(adminUserName, adminPwd)).thenReturn(httpHeaders);

		when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
				.thenAnswer((invocation) -> {

					return responseEntity;
				});

		hasPermission = twcPermUtils.hasPermissionToAccessProject(workspaceId, resourceId, twc, userName, privilege,
				twcRoles);
		assertFalse(hasPermission);

	}

	@Test
	public void hasPermissionNullProjectRolesMap() {

		boolean hasPermission;
		TwcPermissionUtils twcPermUtils = new TwcPermissionUtils();
		twcPermUtils.setJsonUtils(jsonUtils);
		twcPermUtils.setRestUtils(restUtils);
		String userName = "UserId1";

		twcRoles = new ArrayList<String>();
		twcRoles.add("Reviewer");

		ResponseEntity<String> responseEntity = mock(ResponseEntity.class);

		when(restUtils.basicAuthHeader(adminUserName, adminPwd)).thenReturn(httpHeaders);

		when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
				.thenAnswer((invocation) -> {
					return responseEntity;
				});

		hasPermission = twcPermUtils.hasPermissionToAccessProject(workspaceId, resourceId, twc, userName, privilege,
				twcRoles);
		assertFalse(hasPermission);

	}

	@Test
	public void emptyTwcRoles() {

		boolean hasPermission;
		TwcPermissionUtils twcPermUtils = new TwcPermissionUtils();
		twcPermUtils.setJsonUtils(jsonUtils);
		twcPermUtils.setRestUtils(restUtils);

		twcRoles = new ArrayList<String>();

		hasPermission = twcPermUtils.hasPermissionToAccessProject(workspaceId, resourceId, twc, user, privilege,
				twcRoles);
		assertFalse(hasPermission);
	}

	@Test
	public void nullTwcRoles() {

		boolean hasPermission;
		TwcPermissionUtils twcPermUtils = new TwcPermissionUtils();
		twcPermUtils.setJsonUtils(jsonUtils);
		twcPermUtils.setRestUtils(restUtils);

		hasPermission = twcPermUtils.hasPermissionToAccessProject(workspaceId, resourceId, twc, user, privilege,
				twcRoles);
		assertFalse(hasPermission);
	}

	@Test
	public void getUsersListTestSuccesful() {

		TwcPermissionUtils twcPermUtils = new TwcPermissionUtils();
		twcPermUtils.setJsonUtils(jsonUtils);
		twcPermUtils.setRestUtils(restUtils);
		List<String> expectedUsers = new ArrayList<String>();
		expectedUsers.add("UserId1");
		expectedUsers.add("UserId2");
		expectedUsers.add("UserId3");

		JSONArray jsonArr = new JSONArray();
		jsonArr.put("UserId1");
		jsonArr.put("UserId2");
		jsonArr.put("UserId3");

		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonArr.toString(), httpHeaders, HttpStatus.OK);

		when(jsonUtils.parseStringToJsonArray(responseEntity.getBody())).thenReturn(jsonArr);

		when(jsonUtils.convertJsonArrayToStringArray(jsonArr)).thenReturn(expectedUsers);

		when(restUtils.basicAuthHeader(adminUserName, adminPwd)).thenReturn(httpHeaders);

		when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
				.thenAnswer((invocation) -> {
					Object[] args = invocation.getArguments();

					assertEquals("http://twc.domain.com:8111/osmc/"
							+ "resources/resourceID1/roles/roleID1/users", args[0]);

					return responseEntity;
				});

		actualUsers = twcPermUtils.getUsersList(twc, resourceId, roleId);
		assertTrue(expectedUsers.equals(actualUsers));

	}

	@Test
	public void getTwcRolesSuccesFulTest() {

		TwcPermissionUtils twcPermUtils = new TwcPermissionUtils();
		twcPermUtils.setJsonUtils(jsonUtils);
		twcPermUtils.setRestUtils(restUtils);
		Map<String, String> expectedMap = new HashMap<String, String>();
		expectedMap.put("Reviewer", "12345");

		JSONArray jsonArr = new JSONArray();
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("Permission", "TWC_permissions");
		jsonObj.put("name", "Reviewer");
		jsonObj.put("ID", "12345");
		jsonArr.put(jsonObj);

		ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonArr.toString(), httpHeaders, HttpStatus.OK);

		when(jsonUtils.parseStringToJsonArray(responseEntity.getBody())).thenReturn(jsonArr);

		when(restUtils.basicAuthHeader(adminUserName, adminPwd)).thenReturn(httpHeaders);

		when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
				.thenAnswer((invocation) -> {
					Object[] args = invocation.getArguments();

					assertEquals("http://twc.domain.com:8111/osmc/resources/resourceID1/roles", args[0]);

					return responseEntity;
				});

		projectRoleIdMap = twcPermUtils.getTwcRolesForGivenResourceId(resourceId, twc);
		assertTrue(expectedMap.equals(projectRoleIdMap));

	}

	@Test
	public void nullResponseEntityBodyForUsersList() {

		TwcPermissionUtils twcPermUtils = new TwcPermissionUtils();
		twcPermUtils.setJsonUtils(jsonUtils);
		twcPermUtils.setRestUtils(restUtils);

		ResponseEntity<String> responseEntity = mock(ResponseEntity.class);

		when(restUtils.basicAuthHeader(adminUserName, adminPwd)).thenReturn(httpHeaders);

		when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
				.thenAnswer((invocation) -> {
					Object[] args = invocation.getArguments();

					assertEquals("http://twc.domain.com:8111/osmc/"
							+ "resources/resourceID1/roles/roleID1/users", args[0]);

					return responseEntity;
				});

		actualUsers = twcPermUtils.getUsersList(twc, resourceId, roleId);
		assertNull(actualUsers);

	}

	@Test
	public void nullResponseEntityBodyForTwcRoles() {

		TwcPermissionUtils twcPermUtils = new TwcPermissionUtils();
		twcPermUtils.setJsonUtils(jsonUtils);
		twcPermUtils.setRestUtils(restUtils);

		ResponseEntity<String> responseEntity = mock(ResponseEntity.class);

		when(restUtils.basicAuthHeader(adminUserName, adminPwd)).thenReturn(httpHeaders);

		when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
				.thenAnswer((invocation) -> {
					Object[] args = invocation.getArguments();

					assertEquals("http://twc.domain.com:8111/osmc/resources/resourceID1/roles", args[0]);

					return responseEntity;
				});

		projectRoleIdMap = twcPermUtils.getTwcRolesForGivenResourceId(resourceId, twc);
		assertNull(projectRoleIdMap);

	}

	@Test
	public void nullResponseEntityForUserLists() {

		ResponseEntity<String> responseEntity = mock(ResponseEntity.class);
		TwcPermissionUtils twcPermUtils = new TwcPermissionUtils();
		twcPermUtils.setJsonUtils(jsonUtils);
		twcPermUtils.setRestUtils(restUtils);

		when(restUtils.basicAuthHeader(adminUserName, adminPwd)).thenReturn(httpHeaders);

		when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
				.thenAnswer((invocation) -> {
					throw new RuntimeException("Test Exception -- should be caught");
				});

		actualUsers = twcPermUtils.getUsersList(twc, resourceId, roleId);
		assertNull(actualUsers);

	}

	@Test
	public void nullResponseEntityForTwcRoles() {

		ResponseEntity<String> responseEntity = mock(ResponseEntity.class);
		TwcPermissionUtils twcPermUtils = new TwcPermissionUtils();
		twcPermUtils.setJsonUtils(jsonUtils);
		twcPermUtils.setRestUtils(restUtils);

		when(restUtils.basicAuthHeader(adminUserName, adminPwd)).thenReturn(httpHeaders);

		when(mockRestTemplate.exchange(anyString(), any(HttpMethod.class), any(HttpEntity.class), eq(String.class)))
				.thenAnswer((invocation) -> {
					throw new RuntimeException("Test Exception -- should be caught");
				});

		projectRoleIdMap = twcPermUtils.getTwcRolesForGivenResourceId(resourceId, twc);
		assertNull(projectRoleIdMap);

	}

}
