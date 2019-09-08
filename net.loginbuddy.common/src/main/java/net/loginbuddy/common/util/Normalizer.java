package net.loginbuddy.common.util;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.loginbuddy.common.api.HttpHelper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Map;
import java.util.logging.Logger;

public class Normalizer {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(Normalizer.class));

    private static final String MAPPING_OIDC = "{\"sub\":\"$.sub\", \"name\": \"$.name\",\"given_name\": \"$.given_name\",\"family_name\": \"$.family_name\",\"picture\": \"$.picture\",\"email\":\"$.email\", \"email_verified\":\"$.email_verified\", \"provider\":\"asis:provider\", \"preferred_username\": \"$.preferred_username\"}";

    /**
     * Mappings attributes so that receiving clients can expect the same details at the same location in the response message.
     * The output of this method is found at 'details_normalized' in the client response
     */
    public static JSONObject normalizeDetails(String provider, JSONObject mappings, JSONObject userinfoRespObject, String access_token) {
        JSONObject result = new JSONObject();
        try {
            mappings = (mappings == null || mappings.size() == 0) ? (JSONObject) new JSONParser().parse(MAPPING_OIDC.replace("asis:provider", "asis:" + provider)) : mappings;
        } catch (ParseException e) {
            // should not occur!
            LOGGER.severe("The default mapping for OpenID Connect claims is invalid! Continuing as if nothing has happened ... .");
        }
        if (userinfoRespObject != null && userinfoRespObject.size() > 0) {

            // parse the userinfo object to run JSONPath based mappings later
            DocumentContext ctx = JsonPath.parse(userinfoRespObject.toJSONString());

            for (Object nextEntry : mappings.entrySet()) {
                Map.Entry entry = (Map.Entry) nextEntry;
                String mappingKey = (String) entry.getKey();

                String mappingRule = "";
                String outputValue = "";

                /**
                 * If it is a JSONObject we need to do a side call to retrieve additional data
                 * If it is 'resource_type=protected' we'll include the current access_token
                 * Otherwise we'll assume it is an unprotected resource
                 */
                Object mr = entry.getValue();
                if (mr instanceof JSONObject) {
                    try {

                        JSONObject jo = (JSONObject) mr;
                        String resource = (String) jo.get("resource");
                        String resource_type = (String) jo.get("resource_type");
                        mappingRule = (String) jo.get("mapping_rule");

                        if(resource == null || mappingRule == null) {
                            throw new IllegalArgumentException(String.format("Missing resource or mapping_rule. Mapping for mapping key [%s] cannot be executed", mappingKey));
                        }

                        MsgResponse msg;
                        if ("protected".equalsIgnoreCase(resource_type)) {
                            msg = HttpHelper.getAPI(access_token, resource);
                        } else {
                            msg = HttpHelper.getAPI(resource);
                        }
                        if (msg.getStatus() == 200 && msg.getContentType().startsWith("application/json")) {
                            outputValue = readString(JsonPath.parse(msg.getMsg()), mappingRule, "");
                        } else {
                            LOGGER.warning(String.format("Retrieving resource: [%s] for mapping rule: [%s] failed", resource, mappingRule));
                        }

                    } catch(Exception e) {
                        LOGGER.warning(String.format("Mapping rule: [%s] could not be executed: %s", mappingRule, e.getMessage()));
                    }
                    mappingRule = ""; // this way it will not get executed again
                } else {
                    mappingRule = (String)mr;
                }

                // this must be a JSONPath based rule
                if (mappingRule.startsWith("$")) {
                    outputValue = readString(ctx, mappingRule, "");
                } else if (mappingRule.contains("[")) {
                    String userinfoClaim = (String) userinfoRespObject.get(mappingRule.substring(0, mappingRule.indexOf("[")));
                    int idx = Integer.parseInt(Character.toString(mappingRule.charAt(mappingRule.indexOf("[") + 1)));
                    try {
                        outputValue = userinfoClaim.split(" ")[idx];
                    } catch (Exception e) {
                        LOGGER.warning(String.format("invalid indexed mapping: 'mappings.%s' --> 'userinfo.%s': invalid index: %s", mappingKey, mappingRule, e.getMessage()));
                    }
                } else if (mappingRule.startsWith("asis:")) {
                    outputValue = mappingRule.substring(5);
                } else if (mappingRule.trim().length() > 0) {
                    Object value = userinfoRespObject.get(mappingRule);
                    outputValue = value == null ? "" : String.valueOf(value);
                }
                result.put(mappingKey, outputValue == null ? "" : outputValue);
            }
        }
        return result;
    }

    private static <T> T read(DocumentContext ctx, String jsonPath, Class T) {
        return (T) ctx.read(jsonPath, T);
    }

    private static boolean readBoolean(DocumentContext ctx, String jsonPath, boolean defaultValue) {
        try {
            return read(ctx, jsonPath, Boolean.class);
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private static String readString(DocumentContext ctx, String jsonPath, String defaultValue) {
        try {
            return read(ctx, jsonPath, String.class);
        } catch (Exception e) {
            return defaultValue;
        }
    }

}
