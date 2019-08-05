package net.loginbuddy.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

public class TestNormalize {

  private static final Logger LOGGER = Logger.getLogger(String.valueOf(TestNormalize.class));

  @Test
  public void testNormalizeGitHub() {
    try {
      JSONObject mappings = (JSONObject)new JSONParser().parse("{\"name\": \"name\",\"given_name\": \"name[0]\",\"family_name\": \"name[1]\",\"picture\": \"\",\"email\": \"email\"}");
      assertEquals("{\"name\":\"John Doe\",\"given_name\":\"John\",\"family_name\":\"Doe\",\"picture\":\"\",\"email\":\"\"}", normalizeDetails(mappings, getUserinfoGitHub()).toJSONString());
    } catch (ParseException e) {
      fail();
    }
  }

  @Test
  public void testNormalizeGitHubUnknownUserinfoClaim() {
    try {
      JSONObject mappings = (JSONObject)new JSONParser().parse("{\"name\": \"unknown\",\"given_name\": \"name[0]\",\"family_name\": \"name[1]\",\"picture\": \"\",\"email\": \"email\"}");
      assertEquals("{\"name\":\"\",\"given_name\":\"John\",\"family_name\":\"Doe\",\"picture\":\"\",\"email\":\"\"}", normalizeDetails(mappings, getUserinfoGitHub()).toJSONString());
    } catch (ParseException e) {
      fail();
    }
  }

  @Test
  public void testNormalizeGitHubInvalidMappingIndex() {
    try {
      JSONObject mappings = (JSONObject)new JSONParser().parse("{\"name\": \"name\",\"given_name\": \"name[0]\",\"family_name\": \"name[2]\",\"picture\": \"\",\"email\": \"email\"}");
      assertEquals("{\"name\":\"John Doe\",\"given_name\":\"John\",\"family_name\":\"\",\"picture\":\"\",\"email\":\"\"}", normalizeDetails(mappings, getUserinfoGitHub()).toJSONString());
    } catch (ParseException e) {
      fail();
    }
  }

  @Test
  public void testNormalizeGitHubNoMappingValue() {
    try {
      JSONObject mappings = (JSONObject)new JSONParser().parse("{\"name\": \"\",\"given_name\": \"name[0]\",\"family_name\": \"name[1]\",\"picture\": \"\",\"email\": \"email\"}");
      assertEquals("{\"name\":\"\",\"given_name\":\"John\",\"family_name\":\"Doe\",\"picture\":\"\",\"email\":\"\"}", normalizeDetails(mappings, getUserinfoGitHub()).toJSONString());
    } catch (ParseException e) {
      fail();
    }
  }

  @Test
  public void testNormalizeMappingNull() {
    try {
      JSONObject mappings = null;
      assertEquals("{}", normalizeDetails(mappings, getUserinfoGitHub()).toJSONString());
    } catch (ParseException e) {
      fail();
    }
  }

  @Test
  public void testNormalizeMappingEmpty() {
    try {
      JSONObject mappings = new JSONObject();
      assertEquals("{}", normalizeDetails(mappings, getUserinfoGitHub()).toJSONString());
    } catch (ParseException e) {
      fail();
    }
  }

  @Test
  public void testNormalizeUserinfoNull() {
    try {
      JSONObject mappings = (JSONObject)new JSONParser().parse("{\"name\": \"\",\"given_name\": \"name[0]\",\"family_name\": \"name[1]\",\"picture\": \"\",\"email\": \"email\"}");
      assertEquals("{}", normalizeDetails(mappings, null).toJSONString());
    } catch (ParseException e) {
      fail();
    }
  }

  @Test
  public void testNormalizeKeepAsIsValue() {
    try {
      JSONObject mappings = (JSONObject)new JSONParser().parse("{\"name\": \"\",\"given_name\": \"name[0]\",\"family_name\": \"name[1]\",\"picture\": \"asis:http://picture.example.com\",\"email\": \"email\"}");
      assertEquals("{\"name\":\"\",\"given_name\":\"John\",\"family_name\":\"Doe\",\"picture\":\"http:\\/\\/picture.example.com\",\"email\":\"\"}", normalizeDetails(mappings, getUserinfoGitHub()).toJSONString());
    } catch (ParseException e) {
      fail();
    }
  }

  @Test
  public void testNormalizeKeepAsIsNoValue() {
    try {
      JSONObject mappings = (JSONObject)new JSONParser().parse("{\"name\": \"\",\"given_name\": \"name[0]\",\"family_name\": \"name[1]\",\"picture\": \"asis:\",\"email\": \"email\"}");
      assertEquals("{\"name\":\"\",\"given_name\":\"John\",\"family_name\":\"Doe\",\"picture\":\"\",\"email\":\"\"}", normalizeDetails(mappings, getUserinfoGitHub()).toJSONString());
    } catch (ParseException e) {
      fail();
    }
  }

  private JSONObject getUserinfoGitHub() throws ParseException {
    return (JSONObject) new JSONParser().parse("{\n"
        + "      \"gists_url\": \"https:\\/\\/api.github.com\\/users\\/JohnDoe\\/gists{\\/gist_id}\",\n"
        + "      \"repos_url\": \"https:\\/\\/api.github.com\\/users\\/JohnDoe\\/repos\",\n"
        + "      \"two_factor_authentication\": false,\n"
        + "      \"following_url\": \"https:\\/\\/api.github.com\\/users\\/JohnDoe\\/following{\\/other_user}\",\n"
        + "      \"bio\": null,\n"
        + "      \"created_at\": \"1900-00-00T00:00:00Z\",\n"
        + "      \"login\": \"JohnDoe\",\n"
        + "      \"type\": \"User\",\n"
        + "      \"blog\": \"https:\\/\\/john.doe.example.com\",\n"
        + "      \"private_gists\": 0,\n"
        + "      \"total_private_repos\": 0,\n"
        + "      \"subscriptions_url\": \"https:\\/\\/api.github.com\\/users\\/JohnDoe\\/subscriptions\",\n"
        + "      \"updated_at\": \"1900-01-01T00:00:00Z\",\n"
        + "      \"site_admin\": false,\n"
        + "      \"disk_usage\": 1000,\n"
        + "      \"collaborators\": 0,\n"
        + "      \"company\": null,\n"
        + "      \"owned_private_repos\": 0,\n"
        + "      \"id\": 000000000,\n"
        + "      \"public_repos\": 1,\n"
        + "      \"gravatar_id\": \"\",\n"
        + "      \"plan\": {\n"
        + "        \"private_repos\": 10000,\n"
        + "        \"name\": \"free\",\n"
        + "        \"collaborators\": 0,\n"
        + "        \"space\": 0000000000\n"
        + "      },\n"
        + "      \"email\": null,\n"
        + "      \"organizations_url\": \"https:\\/\\/api.github.com\\/users\\/JohnDoe\\/orgs\",\n"
        + "      \"hireable\": null,\n"
        + "      \"starred_url\": \"https:\\/\\/api.github.com\\/users\\/JohnDoe\\/starred{\\/owner}{\\/repo}\",\n"
        + "      \"followers_url\": \"https:\\/\\/api.github.com\\/users\\/JohnDoe\\/followers\",\n"
        + "      \"public_gists\": 0,\n"
        + "      \"url\": \"https:\\/\\/api.github.com\\/users\\/JohnDoe\",\n"
        + "      \"received_events_url\": \"https:\\/\\/api.github.com\\/users\\/JohnDoe\\/received_events\",\n"
        + "      \"followers\": 0,\n"
        + "      \"avatar_url\": \"https:\\/\\/avatars2.githubusercontent.com\\/u\\/00000000?v=1\",\n"
        + "      \"events_url\": \"https:\\/\\/api.github.com\\/users\\/JohnDoe\\/events{\\/privacy}\",\n"
        + "      \"html_url\": \"https:\\/\\/github.com\\/JohnDoe\",\n"
        + "      \"following\": 0,\n"
        + "      \"name\": \"John Doe\",\n"
        + "      \"location\": null,\n"
        + "      \"node_id\": \"MDQ6V........wNDEx\"\n"
        + "    }");
  }

  // TODO: use the method found at @see net.loginbuddy.service.client.Callback
  // TODO: this is jsut a copy to get the method implemented
  private JSONObject normalizeDetails(JSONObject mappings, JSONObject userinfoRespObject) {
    JSONObject result = new JSONObject();
    if(mappings != null && userinfoRespObject != null) {
      for(Object nextEntry : mappings.entrySet()) {
        Map.Entry entry = (Entry)nextEntry;
        String mappingKey = (String)entry.getKey();
        String mappingRule = (String)entry.getValue();
        String outputValue = "";
        if(mappingRule.contains("[")) {
          String userinfoClaim = (String)userinfoRespObject.get(mappingRule.substring(0, mappingRule.indexOf("[")));
          int idx = Integer.parseInt(Character.toString(mappingRule.charAt(mappingRule.indexOf("[")+1)));
          try {
            outputValue = userinfoClaim.split(" ")[idx];
          } catch(Exception e) {
            LOGGER.warning(String.format("invalid indexed mapping: 'mappings.%s' --> 'userinfo.%s': invalid index: %s",mappingKey, mappingRule, e.getMessage()));
          }
        } else if(mappingRule.startsWith("asis:")) {
          outputValue = mappingRule.substring(5);
        }
        else if(mappingRule.trim().length() > 0){
          Object value = userinfoRespObject.get(mappingRule);
          outputValue = value == null ? "" : String.valueOf(value);
        }
        result.put(mappingKey, outputValue == null ? "" : outputValue);
      }
    }
    return result;
  }
}