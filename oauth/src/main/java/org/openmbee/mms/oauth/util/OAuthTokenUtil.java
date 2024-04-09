package org.openmbee.mms.oauth.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

public class OAuthTokenUtil {

    /**
     * Parse a json object into a map with string keys and string values
     * 
     * @param token the json object
     * @return a Map of the json object with string keys and string values
     * @throws Exception if there is an error parsing the json
     */
    public static Map<String, String> parseToken(String token) throws Exception {
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        return gson.fromJson(token, type);
    }

    /**
     * Parse a JWT Token that is base64 url encoded into a map with string keys and
     * string values
     * 
     * @param token JWT base64 url encode string
     * @return a Map of the json object with string keys and string values
     * @throws Exception if there is an error parsing the json
     */
    public static Map<String, String> parseJWT(String token) throws Exception {
        byte[] decodedBytes = Base64.getUrlDecoder().decode(token);
        String jwt = new String(decodedBytes, "UTF-8");
        Gson gson = new Gson();
        Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        return gson.fromJson(jwt, type);
    }

    /**
     * Converts token json object string into string Map that only contains the
     * access token and not other info.
     * 
     * @param token json
     * @return
     * @throws Exception
     */
    ///
    /// </summary>
    /// <param name="token"></param>
    /// <returns>String dictionary with token contents.</returns>
    public static Map<String, String> parsesOAuthRSResponse(String token) throws Exception {
        Map<String, String> jsonToken = new HashMap<String, String>();
        JsonElement jelement = new JsonParser().parse(token);
        JsonObject jobject = jelement.getAsJsonObject();
        JsonObject result = jobject;
        if (jobject.has("access_token")) {
            result = jobject.getAsJsonObject("access_token");
        }

        if (jobject.has("client_id")) {
            result.add("client_id", jobject.get("client_id"));
        }

        if (jobject.has("scope")) {
            result.add("scope", jobject.get("scope"));
        }

        Gson gson = new Gson();
        // This gives a warning because the JSON object could contain a value that is
        // not a string and has no toString method
        // For our case it should be fine because we will always have string values
        return (Map<String, String>) gson.fromJson(result, jsonToken.getClass());
    }
}
