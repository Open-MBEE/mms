package org.openmbee.mms.twc.security;

import org.openmbee.mms.twc.TeamworkCloud;
import org.openmbee.mms.twc.TeamworkCloudEndpoints;
import org.openmbee.mms.twc.constants.TwcConstants;
import org.openmbee.mms.twc.utilities.RestUtils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.elasticsearch.rest.RestRequest;
import org.elasticsearch.rest.RestResponse;
import org.springframework.web.client.RestTemplate;

public class TwcAuthenticationProvider {

	RestUtils restUtils;

	private TeamworkCloud twc;

	public TwcAuthenticationProvider(RestUtils restTemplateFactory, TeamworkCloud twc) {
		this.restUtils = restTemplateFactory;
		this.twc = twc;
	}

	public String getAuthentication(String authToken)  {
		
		if (authToken == null) {
			return null;
		}

		ResponseEntity<RestResponse> respEntity = checkAuthentication(authToken);
		if (respEntity == null)
			return null;

		String loggedInUser = restUtils.getCookieValue(respEntity, TwcConstants.TWCCURRENTUSER);
		return loggedInUser;
	}
	
	private ResponseEntity<RestResponse> checkAuthentication(String authToken) {
		RestTemplate restTemplate = restUtils.getRestTemplate();
		HttpHeaders headers = new HttpHeaders();
		//2021x R2 - found iss of handling token format
		authToken = authToken.replace(":", "");
		headers.set(RestUtils.AUTHORIZATION, authToken);

		ResponseEntity<RestResponse> respEntity = null;

		try {
			HttpEntity<RestRequest> entityReq = new HttpEntity<>(null, headers);
			respEntity = restTemplate.exchange(TeamworkCloudEndpoints.LOGIN.buildUrl(twc), HttpMethod.GET, entityReq, RestResponse.class);
		} catch (Exception Ex) {
			return null;
		}
		return respEntity;
	}
}
