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

    private static final Logger LOGGER = Logger.getLogger(Normalizer.class.getName());

    private static final String MAPPING_OIDC = "{" +
            "\"sub\":\"$.details_provider.userinfo.sub\", " +
            "\"name\": \"$.details_provider.userinfo.name\"," +
            "\"given_name\": \"$.details_provider.userinfo.given_name\"," +
            "\"family_name\": \"$.details_provider.userinfo.family_name\"," +
            "\"picture\": \"$.details_provider.userinfo.picture\"," +
            "\"email\":\"$.details_provider.userinfo.email\", " +
            "\"email_verified\":\"$.details_provider.userinfo.email_verified\", " +
            "\"provider\":\"$.details_provider.provider\", " +
            "\"preferred_username\": \"$.details_provider.userinfo.preferred_username\"}";

    public static String getDefaultMapping() {
        return MAPPING_OIDC;
    }
    /**
     * Mappings attributes so that receiving clients can expect the same details at the same location in the response message.
     * The output of this method is found at 'details_normalized' in the client response
     */
    public static JSONObject normalizeDetails(JSONObject mappings, JSONObject loginbuddyResponse, String access_token) {
        JSONObject result = new JSONObject();
        try {
            mappings = (mappings == null || mappings.size() == 0) ? (JSONObject) new JSONParser().parse(MAPPING_OIDC) : mappings;
        } catch (ParseException e) {
            // should not occur!
            LOGGER.severe("The default mapping for OpenID Connect claims is invalid! Continuing as if nothing has happened ... .");
        }
        if (loginbuddyResponse != null && loginbuddyResponse.size() > 0) {

            Object mr;
            MsgResponse msg;
            String mappedValue = "";
            DocumentContext ctx = JsonPath.parse(loginbuddyResponse.toJSONString());

            for (Object nextEntry : mappings.entrySet()) {
                Map.Entry entry = (Map.Entry) nextEntry;
                String mappingKey = (String) entry.getKey();

                /**
                 * If it is a JSONObject we need to do a side call to retrieve additional data
                 * If it is 'resource_type=protected' we'll include the current access_token
                 * Otherwise, we'll assume it is an unprotected resource
                 */
                mr = entry.getValue();
                if (mr instanceof JSONObject) {
                    try {
                        JSONObject jo = (JSONObject) mr;
                        String resource = (String) jo.get("resource");
                        String resource_type = (String) jo.get("resource_type");
                        if(resource == null || resource.trim().length() == 0) {
                            throw new IllegalArgumentException("Missing resource for mapping!");
                        }
                        if ("protected".equalsIgnoreCase(resource_type)) {
                            msg = HttpHelper.getAPI(access_token, resource);
                        } else {
                            msg = HttpHelper.getAPI(resource);
                        }
                        if (msg.getStatus() == 200 && msg.getContentType().startsWith("application/json")) {
                            mappedValue = getMappedValue(loginbuddyResponse, JsonPath.parse(msg.getMsg()), mappingKey, (String) jo.get("mapping_rule"));
                        } else {
                            LOGGER.warning(String.format("Retrieving resource: [%s] for mapping rule: [%s] failed", resource, (String) jo.get("mapping_rule")));
                        }
                    } catch(Exception e) {
                        LOGGER.warning(String.format("Mapping rule: [%s] could not be executed: %s", (String) ((JSONObject)mr).get("mappping_rule"), e.getMessage()));
                    }
                } else {
                    mappedValue = getMappedValue(loginbuddyResponse, ctx, mappingKey, (String)mr);
                }
                result.put(mappingKey, mappedValue == null ? "" : mappedValue);
                mappedValue = "";
            }
        }
        return result;
    }

    private static String getMappedValue(JSONObject loginbuddyResponse, DocumentContext ctx, String mappingKey, String mappingRule) {
        String outputValue = "";
        // this must be a JSONPath based rule
        if (mappingRule.startsWith("$")) {
            int idx = -1;
            if (mappingRule.contains(":")) {
                idx = Integer.parseInt(Character.toString(mappingRule.charAt(mappingRule.indexOf("[") + 1)));
                mappingRule = mappingRule.substring(0, mappingRule.indexOf(":"));
            }
            outputValue = readString(ctx, mappingRule, "");
            if(idx >=0 && outputValue != null) {
                try {
                    outputValue = outputValue.split(" ")[idx];
                } catch (Exception e) {
                    outputValue = "";
                    LOGGER.warning(String.format("invalid indexed mapping: 'mappings.%s' --> 'message.%s': invalid index: %s", mappingKey, mappingRule, e.getMessage()));
                }
            }
        } else if (mappingRule.startsWith("asis:")) {
            outputValue = mappingRule.substring(5);
        } else if (mappingRule.trim().length() > 0) {
            Object value = loginbuddyResponse.get(mappingRule);
            outputValue = value == null ? "" : String.valueOf(value);
        }
        return outputValue;
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
