package org.openmbee.mms.oauth.constants;

public class OAuthErrorConstants {
    public static final String HTTP_FAILED = "Http call failed";
    public static final String INSUFFICIENT_LOA = "User does not have proper LOA.";
    public static final String INVALD_TOKEN = "Invalid bearer token !!";
    public static final String NO_GRANTS_PROVIDED = "No Grants provided for the user.";
    public static final String NO_TOKEN_RECEIVED= "No access token received."; 
    public static final String PROBLEM_LOADING_CERTIFICATE = "Problem occurred when loading the application certificates : "; 
    public static final String UNKNOWN_LOA = "LOA value not provided therefore LOA can not be checked";
    public static final String UNKNOWN_TOKEN= "Token is either of an unknown type or not a Bearer token"; 
    public static final String UNKNOWN_USER = "Unknown User.";
    public static final String CLIENT_ID_REQUIRED = "Client id is required.";
    public static final String CLIENT_ID_NOT_WHITELISTED = "Client id %s is not in the client id whitelist.";
}