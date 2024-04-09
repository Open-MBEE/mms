package org.openmbee.mms.oauth.security;

import java.io.File;
import java.io.IOException;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.*;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;


import org.openmbee.mms.core.exceptions.UnauthorizedException;
import org.openmbee.mms.oauth.constants.OAuthErrorConstants;
import org.openmbee.mms.oauth.util.OAuthTokenUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.apache.http.HttpException;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.ssl.SSLContexts;
import org.openmbee.mms.oauth.constants.OAuthConstants;
import org.springframework.security.core.userdetails.UserDetails;

public class OAuthProcessor {
    
    private static Logger logger = LoggerFactory.getLogger(OAuthProcessor.class);

    @Value("${oauth.rs_id}")
    private String rs_id;

    @Value("${oauth.environment}")
    private String environment;

    @Value("${oauth.keystoreLocation}")
    private String keystoreLocation;

    @Value("${oauth.keystorePassword}")
    private char[] keystorePassword;

    @Value("${oauth.certificatePassword}")
    private String certificatePassword;

    @Value("${oauth.loa}")
    private String loa;

    @Value("${oauth.clients}")
    private String allowedClients;

    @Value("${oauth.baseUrl}")
    private String oauthBaseUrl;

    @Value("${oauth.validationEndpoint}")
    private String oauthValidationEndpoint;

    @Value("#{'${oauth.clientIdWhitelist}'.split(',')}")
    private Set<String> clientIdWhitelist;

    private File jks;
    Map<String, String> grants = new HashMap<String, String>();


    private OAuthUserDetailsService userDetailsService;

    public OAuthUserDetailsService getUserDetailsService() {
        return userDetailsService;
    }

    @Autowired
    public void setUserDetailsService(OAuthUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }


    OAuth2Authentication validateAuthToken(String accessToken) {
        boolean isAuthenticated = true;
        OAuth2Authentication oauth = null;
        try {
            if (accessToken == null) {
                throw new Exception(OAuthErrorConstants.NO_TOKEN_RECEIVED);
            }
            // check that the token is a Bearer Token
            if (!accessToken.contains(OAuthConstants.BEARER) && !accessToken.contains(OAuthConstants.BEARER.toLowerCase())) {
                throw new Exception(OAuthErrorConstants.UNKNOWN_TOKEN);
            }

            ClassLoader classLoader = OAuthProcessor.class.getClassLoader();
            KeyStore keystore = KeyStore.getInstance(OAuthConstants.JKS);
            SSLConnectionSocketFactory sslsf;
            try (InputStream keystoreStream = classLoader.getResourceAsStream(keystoreLocation)) { // prevent leak of keystore stream
                // Load the Keystore with Application Certificate
                keystore.load(keystoreStream, keystorePassword);

                try {
                    // check to see if keystore is empty or not
                    SSLContext sslcontext;
                    if (keystore.size() <= 0) {
                        sslcontext = SSLContexts.custom().loadKeyMaterial(jks, keystorePassword, certificatePassword.toCharArray()).build();
                    } else {
                        sslcontext = SSLContexts.custom().loadKeyMaterial(keystore, certificatePassword.toCharArray()).build();
                    }
                    // Allow TLSv1.2 protocol only
                    sslsf = new SSLConnectionSocketFactory(sslcontext, new String[] { OAuthConstants.TLS_VERSION }, null,
                        SSLConnectionSocketFactory.getDefaultHostnameVerifier());
                } catch (GeneralSecurityException gse) {
                    throw new GeneralSecurityException( OAuthErrorConstants.PROBLEM_LOADING_CERTIFICATE + gse.getMessage());
                }
            }

            // Create the client to use for the connection for the access token validation
            // HTTP Post Call
            CloseableHttpClient client = HttpClients.custom().setSSLSocketFactory(sslsf).build();
            HttpPost post = new HttpPost(oauthBaseUrl + oauthValidationEndpoint);
            // Create a list of the values to add to the Post call as an HTTP Form
            List<NameValuePair> values = new ArrayList<NameValuePair>(2);
            values.add(new BasicNameValuePair(OAuthConstants.GRANT_TYPE, OAuthConstants.GRANT_TYPE_VALIDATE_BEARER));
            String[] tokenValue = accessToken.split(" ");
            if (tokenValue.length == 2) {
                values.add(new BasicNameValuePair(OAuthConstants.TOKEN, tokenValue[1]));
            } else {
                throw new Exception(OAuthErrorConstants.UNKNOWN_TOKEN);
            }
            values.add(new BasicNameValuePair(OAuthConstants.CLIENT_ID, rs_id));
            post.setEntity(new UrlEncodedFormEntity(values));
            post.addHeader(OAuthConstants.MEDIATYPE, OAuthConstants.MEDIATYPE_URL_ENCODED);
            // Execute Post

            try (CloseableHttpResponse response = client.execute(post)){
                if (response == null)
                    throw new HttpException(OAuthErrorConstants.HTTP_FAILED);
                switch (response.getStatusLine().getStatusCode()) {
                    case 200:
                        // A 200 indicates that the token was valid
                        String token = extractToken(response);
                        
                        // If Post successful get the return JSON object and parse if the GSON Java Lib
                        grants = OAuthTokenUtil.parsesOAuthRSResponse(token);
                        // If grants are null or empty throw an exception
                        if (grants == null || grants.isEmpty()) {
                            isAuthenticated = false;
                            throw new UnauthorizedException(OAuthErrorConstants.NO_GRANTS_PROVIDED);
                        } else {
                            String userName = grants.containsKey(OAuthConstants.SAMA_ACCOUNT_NAME) ? grants.get(OAuthConstants.SAMA_ACCOUNT_NAME) : "";
                            if (!userName.isEmpty()) {
                                UserDetails userDetails = this.userDetailsService.loadUserByUsername(userName);
                                oauth = new OAuth2Authentication(accessToken,grants,userDetails);
                            } else {
                                throw new UnauthorizedException(OAuthErrorConstants.UNKNOWN_USER);
                            }
                            // if we did get those check to see if we are including authnlevel or not and
                            // make sure the LOA value is valid
                            if (grants.containsKey(OAuthConstants.GRANT_LOA)) {
                                double grantLoa = Double.parseDouble(grants.get(OAuthConstants.GRANT_LOA));
                                if (grantLoa < Double.parseDouble(loa)) {
                                    throw new IllegalAccessException(OAuthErrorConstants.INSUFFICIENT_LOA);
                                }
                            } else {
                                throw new IllegalAccessException(OAuthErrorConstants.UNKNOWN_LOA);
                            }

                            if (grants.containsKey(OAuthConstants.CLIENT_ID)) {
                                String clientId = grants.get(OAuthConstants.CLIENT_ID);
                                if(! clientIdWhitelist.contains(clientId)) {
                                    throw new IllegalAccessException(String.format(OAuthErrorConstants.CLIENT_ID_NOT_WHITELISTED, clientId));
                                }
                            } else {
                                throw new IllegalAccessException(OAuthErrorConstants.CLIENT_ID_REQUIRED);
                            }
                        }
                        break;
                    case 400:
                        // 400 indicates that the bearer token was not valid
                        throw new Exception(OAuthErrorConstants.INVALD_TOKEN);
                    default:
                        throw new HttpResponseException(response.getStatusLine().getStatusCode(),response.getStatusLine().getReasonPhrase());
                }
            } catch (Exception e) {
                throw new Exception(e);
            }
        } catch (Exception e) {
            isAuthenticated =  false;
            logger.error(e.getMessage(),e);
        }
        if (oauth != null) {
            oauth.setAuthenticated(isAuthenticated);
        }
        return oauth;
    }

    private String extractToken(CloseableHttpResponse response) {
        try(BufferedReader buffer = new BufferedReader(new InputStreamReader(response.getEntity().getContent()))){
            return buffer.lines().collect(Collectors.joining("\n"));
        } catch (IOException ex) {
            throw new UnauthorizedException(OAuthErrorConstants.NO_TOKEN_RECEIVED);
        }
    }

}