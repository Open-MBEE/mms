package org.openmbee.mms.twc.utilities;

import org.openmbee.mms.twc.TeamworkCloud;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.codec.Base64;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.Charset;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RestUtils {

    public static final String AUTHORIZATION = HttpHeaders.AUTHORIZATION;
    public static final String SET_COOKIE = "Set-Cookie";
    public static final String US_ASCII = "US-ASCII";
    public static final String BASIC = "Basic ";

    public RestTemplate getRestTemplate() {
        return new RestTemplate();
    }

    public String getCookieValue(ResponseEntity<?> responseEntity, String cookieName) {


        if (responseEntity == null || responseEntity.getHeaders() == null)
            return null;

        Pattern pattern = Pattern.compile(cookieName + "=([^;]+)(;.*)?");

        for(String cookie : responseEntity.getHeaders().get(SET_COOKIE)) {
            Matcher matcher = pattern.matcher(cookie);
            if (matcher.matches()) {
                return matcher.group(1);
            }
        }

        return null;
    }
    
    
    public HttpHeaders basicAuthHeader(String username, String password){
    	   return new HttpHeaders() {{
    	         String auth = username + ":" + password;
    	         byte[] encodedAuth = Base64.encode( 
    	            auth.getBytes(Charset.forName(US_ASCII)) );
    	         String authHeader = BASIC + new String( encodedAuth );
    	         set( AUTHORIZATION, authHeader );
    	      }};
    	}

    /**
     * This method is used to establish connection twc Rest API's by calling
     * Teamwork cloud endpoints Time being added Admin account .Later need to
     * implement secure methods like CyberArk
     *
     * @param twcRestUrl
     * @return
     */
    public ResponseEntity<String> getRestResponse(String twcRestUrl, TeamworkCloud twc) {
        RestTemplate restTemplate = getRestTemplate();
        HttpHeaders headers = basicAuthHeader(twc.getAdminUsername(), twc.getAdminPwd());
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
