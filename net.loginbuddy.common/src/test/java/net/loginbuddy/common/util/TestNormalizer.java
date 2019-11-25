package net.loginbuddy.common.util;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.logging.Logger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestNormalizer {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(TestNormalizer.class));

    private static JSONObject userinfo, loginbuddy;

    @BeforeClass
    public static void setup() {
        try {
            loginbuddy = (JSONObject)new JSONParser().parse(getLoginbuddyResponse());
            userinfo = (JSONObject) new JSONParser().parse("{\"details_provider\":{\"userinfo\":{\"sub\": \"248289761001\",\"name\": \"Jane Doe\",\"given_name\": \"Jane\",\"family_name\": \"Doe\",\"preferred_username\": \"j.doe\",\"email\": \"janedoe@example.com\",\"picture\": \"http://example.com/janedoe/me.jpg\"}}}");
        } catch (ParseException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void normalizeSimpleLoginbuddyResponse() {
        try {
            JSONObject mappings = (JSONObject) new JSONParser().parse(Normalizer.getDefaultMapping());
            assertEquals(
                    "{\"sub\":\"1018256\"," +
                            "\"email_verified\":\"false\"," +
                            "\"provider\":\"example\"," +
                            "\"name\":\"John Doe\"," +
                            "\"preferred_username\":\"John 'the man' Doe\"," +
                            "\"given_name\":\"John\"," +
                            "\"family_name\":\"Doe\"," +
                            "\"picture\":\"https:\\/\\/example.com\\/\\/photo.jpg\"," +
                            "\"email\":\"email@example.com\"}",
                    Normalizer.normalizeDetails(mappings, loginbuddy, null).toJSONString());
        } catch (ParseException e) {
            fail();
        }
    }

    @Test
    public void testNormalizeGitHub() {
        try {
            JSONObject mappings = (JSONObject) new JSONParser().parse("{\"name\": \"$.details_provider.userinfo.name\",\"given_name\": \"$.details_provider.userinfo.name:[0]\",\"family_name\": \"$.details_provider.userinfo.name:[1]\",\"picture\": \"\",\"email\": \"$.details_provider.userinfo.email\"}");
            assertEquals(
                    "{\"name\":\"John Doe\",\"given_name\":\"John\",\"family_name\":\"Doe\",\"picture\":\"\",\"email\":\"\"}",
                    Normalizer.normalizeDetails(mappings, getUserinfoGitHub(), "access_token").toJSONString());
        } catch (ParseException e) {
            fail();
        }
    }

    @Test
    public void testNormalizeGitHubUnknownUserinfoClaim() {
        try {
            JSONObject mappings = (JSONObject) new JSONParser().parse("{\"name\": \"unknown\",\"given_name\": \"$.details_provider.userinfo.name:[0]\",\"family_name\": \"$.details_provider.userinfo.name:[1]\",\"picture\": \"\",\"email\": \"$.details_provider.userinfo.email\"}");
            assertEquals("{\"name\":\"\",\"given_name\":\"John\",\"family_name\":\"Doe\",\"picture\":\"\",\"email\":\"\"}", Normalizer.normalizeDetails(mappings, getUserinfoGitHub(), "access_token").toJSONString());
        } catch (ParseException e) {
            fail();
        }
    }

    @Test
    public void testNormalizeGitHubInvalidMappingIndex() {
        try {
            JSONObject mappings = (JSONObject) new JSONParser().parse("{\"name\": \"$.details_provider.userinfo.name\",\"given_name\": \"$.details_provider.userinfo.name:[0]\",\"family_name\": \"$.details_provider.userinfo.name:[2]\",\"picture\": \"\",\"email\": \"$.details_provider.userinfo.email\"}");
            assertEquals("{\"name\":\"John Doe\",\"given_name\":\"John\",\"family_name\":\"\",\"picture\":\"\",\"email\":\"\"}",
                    Normalizer.normalizeDetails(mappings, getUserinfoGitHub(), "access_token").toJSONString());
        } catch (ParseException e) {
            fail();
        }
    }

    @Test
    public void testNormalizeGitHubNoMappingValue() {
        try {
            JSONObject mappings = (JSONObject) new JSONParser().parse("{\"name\": \"\",\"given_name\": \"$.details_provider.userinfo.name:[0]\",\"family_name\": \"$.details_provider.userinfo.name:[1]\",\"picture\": \"\",\"email\": \"email\"}");
            assertEquals("{\"name\":\"\",\"given_name\":\"John\",\"family_name\":\"Doe\",\"picture\":\"\",\"email\":\"\"}", Normalizer.normalizeDetails(mappings, getUserinfoGitHub(), "access_token").toJSONString());
        } catch (ParseException e) {
            fail();
        }
    }

    @Test
    public void testNormalizeMappingNull() {
        // use default mapping
        try {
            JSONObject mappings = null;
            assertEquals(
                    "{\"sub\":\"\"," +
                            "\"email_verified\":\"\"," +
                            "\"provider\":\"\"," +
                            "\"name\":\"John Doe\"," +
                            "\"preferred_username\":\"\"," +
                            "\"given_name\":\"\"," +
                            "\"family_name\":\"\"," +
                            "\"picture\":\"\"," +
                            "\"email\":\"\"}",
                    Normalizer.normalizeDetails(mappings, getUserinfoGitHub(), "access_token").toJSONString());
        } catch (ParseException e) {
            fail();
        }
    }

    @Test
    public void testNormalizeMappingEmpty() {
        // use default
        try {
            JSONObject mappings = new JSONObject();
            assertEquals(
                    "{\"sub\":\"\"," +
                            "\"email_verified\":\"\"," +
                            "\"provider\":\"\"," +
                            "\"name\":\"John Doe\"," +
                            "\"preferred_username\":\"\"," +
                            "\"given_name\":\"\"," +
                            "\"family_name\":\"\"," +
                            "\"picture\":\"\"," +
                            "\"email\":\"\"}",
                    Normalizer.normalizeDetails(mappings, getUserinfoGitHub(), "access_token").toJSONString());
        } catch (ParseException e) {
            fail();
        }
    }

    @Test
    public void testNormalizeUserinfoNull() {
        try {
            JSONObject mappings = (JSONObject) new JSONParser().parse("{\"name\": \"\",\"given_name\": \"name[0]\",\"family_name\": \"name[1]\",\"picture\": \"\",\"email\": \"email\"}");
            assertEquals("{}", Normalizer.normalizeDetails(mappings, null, "access_token").toJSONString());
        } catch (ParseException e) {
            fail();
        }
    }

    @Test
    public void testNormalizeKeepAsIsValue() {
        try {
            JSONObject mappings = (JSONObject) new JSONParser().parse("{\"name\": \"\",\"given_name\": \"$.details_provider.userinfo.name:[0]\",\"family_name\": \"$.details_provider.userinfo.name:[1]\",\"picture\": \"asis:http://picture.example.com\",\"email\": \"$.details_provider.userinfo.email\"}");
            assertEquals("{\"name\":\"\",\"given_name\":\"John\",\"family_name\":\"Doe\",\"picture\":\"http:\\/\\/picture.example.com\",\"email\":\"\"}", Normalizer.normalizeDetails(mappings, getUserinfoGitHub(), "access_token").toJSONString());
        } catch (ParseException e) {
            fail();
        }
    }

    @Test
    public void testNormalizeKeepAsIsNoValue() {
        try {
            JSONObject mappings = (JSONObject) new JSONParser().parse("{\"name\": \"\",\"given_name\": \"$.details_provider.userinfo.name:[0]\",\"family_name\": \"$.details_provider.userinfo.name:[1]\",\"picture\": \"asis:\",\"email\": \"email\"}");
            assertEquals("{\"name\":\"\",\"given_name\":\"John\",\"family_name\":\"Doe\",\"picture\":\"\",\"email\":\"\"}", Normalizer.normalizeDetails(mappings, getUserinfoGitHub(), "access_token").toJSONString());
        } catch (ParseException e) {
            fail();
        }
    }

    private JSONObject getUserinfoGitHub() throws ParseException {
        return (JSONObject) new JSONParser().parse("{\"details_provider\":{\"userinfo\":{\n"
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
                + "    }}}");
    }

    @Test
    public void testNormalizeResponseDefault() {

        JSONObject actual = Normalizer.normalizeDetails(null, userinfo, "access_token");

        JSONObject nd = new JSONObject();
        nd.put("sub", "248289761001");
        nd.put("email_verified", "");
        nd.put("name", "Jane Doe");
        nd.put("provider", "");
        nd.put("preferred_username", "j.doe");
        nd.put("given_name", "Jane");
        nd.put("family_name", "Doe");
        nd.put("picture", "http://example.com/janedoe/me.jpg");
        nd.put("email", "janedoe@example.com");

        assertEquals(nd.toJSONString(), actual.toJSONString());
    }

    @Test
    public void testNormalizeProfileResponseLinkedIn() {

        try {

            JSONObject linkedinProfileMapping = (JSONObject) new JSONParser().parse("{\"sub\":\"$.details_provider.userinfo.id\", \"name\": \"\",\"given_name\": \"$.details_provider.userinfo.localizedFirstName\",\"family_name\": \"$.details_provider.userinfo.localizedLastName\",\"picture\": \"$.details_provider.userinfo.profilePicture.displayImage~.elements[0].identifiers[0].identifier\", \"email\": {\"resource\": \"\",\"mapping_rule\": \"$.elements[0].handle~.emailAddress\"}, \"email_verified\":\"asis:true\", \"provider\":\"$.details_provider.provider\", \"preferred_username\": \"$.details_provider.userinfo.preferred_username\"}");
            JSONObject profileResponse = (JSONObject) new JSONParser().parse(getLinkedInProfileResponse());
            JSONObject actual = Normalizer.normalizeDetails(linkedinProfileMapping, profileResponse, "access_token");

            JSONObject nd = new JSONObject();
            nd.put("sub", "248289761001");
            nd.put("provider", "linkedin");
            nd.put("email_verified", "true");
            nd.put("name", "");
            nd.put("given_name", "Jane");
            nd.put("family_name", "Doe");
            nd.put("picture", "http://example.com/janedoe/me.jpg");
            nd.put("email", "");
            nd.put("preferred_username", "");

            assertEquals(nd.toJSONString(), actual.toJSONString());
        } catch (ParseException e) {
            fail();
        }
    }

    @Test
    public void testNormalizeEmailResponseLinkedIn() {

        // to retrieve the linkedIn email address another request is required
        // this test simulates a successful email response. This way we can check if the JSON path expression is correct that is used in Normalizer.normalizeDetails
        try {

            JSONObject linkedinEmailMapping = (JSONObject) new JSONParser().parse("{\"email\":\"$.elements[0].handle~.emailAddress\"}");
            JSONObject emailResponse = (JSONObject) new JSONParser().parse(getLinkedInEmailResponse());
            JSONObject actual = Normalizer.normalizeDetails(linkedinEmailMapping, emailResponse, "access_token");

            JSONObject nd = new JSONObject();
            nd.put("email", "janedoe@example.com");

            assertEquals(nd.toJSONString(), actual.toJSONString());
        } catch (ParseException e) {
            fail();
        }
    }

    private String getLinkedInProfileResponse() {
        return "{\"details_provider\":{\"provider\":\"linkedin\",\"userinfo\":{\n" +
                "  \"localizedLastName\": \"Doe\",\n" +
                "  \"profilePicture\": {\n" +
                "    \"displayImage\": \"urn:li:digitalmediaAsset:123456789\",\n" +
                "    \"displayImage~\": {\n" +
                "      \"paging\": {\n" +
                "        \"count\": 10,\n" +
                "        \"start\": 0,\n" +
                "        \"links\": []\n" +
                "      },\n" +
                "      \"elements\": [\n" +
                "        {\n" +
                "          \"artifact\": \"urn:li:digitalmediaMediaArtifact:(urn:li:digitalmediaAsset:123456789,urn:li:digitalmediaMediaArtifactClass:profile-displayphoto-shrink_100_100)\",\n" +
                "          \"authorizationMethod\": \"PUBLIC\",\n" +
                "          \"data\": {\n" +
                "            \"com.linkedin.digitalmedia.mediaartifact.StillImage\": {\n" +
                "              \"storageSize\": {\n" +
                "                \"width\": 100,\n" +
                "                \"height\": 100\n" +
                "              },\n" +
                "              \"storageAspectRatio\": {\n" +
                "                \"widthAspect\": 1.0,\n" +
                "                \"heightAspect\": 1.0,\n" +
                "                \"formatted\": \"1.00:1.00\"\n" +
                "              },\n" +
                "              \"mediaType\": \"image/jpeg\",\n" +
                "              \"rawCodecSpec\": {\n" +
                "                \"name\": \"jpeg\",\n" +
                "                \"type\": \"image\"\n" +
                "              },\n" +
                "              \"displaySize\": {\n" +
                "                \"uom\": \"PX\",\n" +
                "                \"width\": 100.0,\n" +
                "                \"height\": 100.0\n" +
                "              },\n" +
                "              \"displayAspectRatio\": {\n" +
                "                \"widthAspect\": 1.0,\n" +
                "                \"heightAspect\": 1.0,\n" +
                "                \"formatted\": \"1.00:1.00\"\n" +
                "              }\n" +
                "            }\n" +
                "          },\n" +
                "          \"identifiers\": [\n" +
                "            {\n" +
                "              \"identifier\": \"http://example.com/janedoe/me.jpg\",\n" +
                "              \"file\": \"urn:li:digitalmediaFile:(urn:li:digitalmediaAsset:123456789,urn:li:digitalmediaMediaArtifactClass:profile-displayphoto-shrink_100_100,0)\",\n" +
                "              \"index\": 0,\n" +
                "              \"mediaType\": \"image/jpeg\",\n" +
                "              \"identifierType\": \"EXTERNAL_URL\",\n" +
                "              \"identifierExpiresInSeconds\": 1234567890\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"artifact\": \"urn:li:digitalmediaMediaArtifact:(urn:li:digitalmediaAsset:123456789,urn:li:digitalmediaMediaArtifactClass:profile-displayphoto-shrink_200_200)\",\n" +
                "          \"authorizationMethod\": \"PUBLIC\",\n" +
                "          \"data\": {\n" +
                "            \"com.linkedin.digitalmedia.mediaartifact.StillImage\": {\n" +
                "              \"storageSize\": {\n" +
                "                \"width\": 200,\n" +
                "                \"height\": 200\n" +
                "              },\n" +
                "              \"storageAspectRatio\": {\n" +
                "                \"widthAspect\": 1.0,\n" +
                "                \"heightAspect\": 1.0,\n" +
                "                \"formatted\": \"1.00:1.00\"\n" +
                "              },\n" +
                "              \"mediaType\": \"image/jpeg\",\n" +
                "              \"rawCodecSpec\": {\n" +
                "                \"name\": \"jpeg\",\n" +
                "                \"type\": \"image\"\n" +
                "              },\n" +
                "              \"displaySize\": {\n" +
                "                \"uom\": \"PX\",\n" +
                "                \"width\": 200.0,\n" +
                "                \"height\": 200.0\n" +
                "              },\n" +
                "              \"displayAspectRatio\": {\n" +
                "                \"widthAspect\": 1.0,\n" +
                "                \"heightAspect\": 1.0,\n" +
                "                \"formatted\": \"1.00:1.00\"\n" +
                "              }\n" +
                "            }\n" +
                "          },\n" +
                "          \"identifiers\": [\n" +
                "            {\n" +
                "              \"identifier\": \"https://media.licdn.com/dms/image/123456789/profile-displayphoto-shrink_200_200/0?e=1234567890&v=beta&t=ieXRzm_waQRV4-V-SohBS5i6cMoWuF8qfo4MlaWlMF8\",\n" +
                "              \"file\": \"urn:li:digitalmediaFile:(urn:li:digitalmediaAsset:123456789,urn:li:digitalmediaMediaArtifactClass:profile-displayphoto-shrink_200_200,0)\",\n" +
                "              \"index\": 0,\n" +
                "              \"mediaType\": \"image/jpeg\",\n" +
                "              \"identifierType\": \"EXTERNAL_URL\",\n" +
                "              \"identifierExpiresInSeconds\": 1234567890\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"artifact\": \"urn:li:digitalmediaMediaArtifact:(urn:li:digitalmediaAsset:123456789,urn:li:digitalmediaMediaArtifactClass:profile-displayphoto-shrink_400_400)\",\n" +
                "          \"authorizationMethod\": \"PUBLIC\",\n" +
                "          \"data\": {\n" +
                "            \"com.linkedin.digitalmedia.mediaartifact.StillImage\": {\n" +
                "              \"storageSize\": {\n" +
                "                \"width\": 400,\n" +
                "                \"height\": 400\n" +
                "              },\n" +
                "              \"storageAspectRatio\": {\n" +
                "                \"widthAspect\": 1.0,\n" +
                "                \"heightAspect\": 1.0,\n" +
                "                \"formatted\": \"1.00:1.00\"\n" +
                "              },\n" +
                "              \"mediaType\": \"image/jpeg\",\n" +
                "              \"rawCodecSpec\": {\n" +
                "                \"name\": \"jpeg\",\n" +
                "                \"type\": \"image\"\n" +
                "              },\n" +
                "              \"displaySize\": {\n" +
                "                \"uom\": \"PX\",\n" +
                "                \"width\": 400.0,\n" +
                "                \"height\": 400.0\n" +
                "              },\n" +
                "              \"displayAspectRatio\": {\n" +
                "                \"widthAspect\": 1.0,\n" +
                "                \"heightAspect\": 1.0,\n" +
                "                \"formatted\": \"1.00:1.00\"\n" +
                "              }\n" +
                "            }\n" +
                "          },\n" +
                "          \"identifiers\": [\n" +
                "            {\n" +
                "              \"identifier\": \"https://media.licdn.com/dms/image/123456789/profile-displayphoto-shrink_400_400/0?e=1234567890&v=beta&t=VinU9aryUGkawtIpk8b1lVu-Mb21GTJngNExZxZSJ1U\",\n" +
                "              \"file\": \"urn:li:digitalmediaFile:(urn:li:digitalmediaAsset:123456789,urn:li:digitalmediaMediaArtifactClass:profile-displayphoto-shrink_400_400,0)\",\n" +
                "              \"index\": 0,\n" +
                "              \"mediaType\": \"image/jpeg\",\n" +
                "              \"identifierType\": \"EXTERNAL_URL\",\n" +
                "              \"identifierExpiresInSeconds\": 1234567890\n" +
                "            }\n" +
                "          ]\n" +
                "        },\n" +
                "        {\n" +
                "          \"artifact\": \"urn:li:digitalmediaMediaArtifact:(urn:li:digitalmediaAsset:123456789,urn:li:digitalmediaMediaArtifactClass:profile-displayphoto-shrink_800_800)\",\n" +
                "          \"authorizationMethod\": \"PUBLIC\",\n" +
                "          \"data\": {\n" +
                "            \"com.linkedin.digitalmedia.mediaartifact.StillImage\": {\n" +
                "              \"storageSize\": {\n" +
                "                \"width\": 800,\n" +
                "                \"height\": 800\n" +
                "              },\n" +
                "              \"storageAspectRatio\": {\n" +
                "                \"widthAspect\": 1.0,\n" +
                "                \"heightAspect\": 1.0,\n" +
                "                \"formatted\": \"1.00:1.00\"\n" +
                "              },\n" +
                "              \"mediaType\": \"image/jpeg\",\n" +
                "              \"rawCodecSpec\": {\n" +
                "                \"name\": \"jpeg\",\n" +
                "                \"type\": \"image\"\n" +
                "              },\n" +
                "              \"displaySize\": {\n" +
                "                \"uom\": \"PX\",\n" +
                "                \"width\": 800.0,\n" +
                "                \"height\": 800.0\n" +
                "              },\n" +
                "              \"displayAspectRatio\": {\n" +
                "                \"widthAspect\": 1.0,\n" +
                "                \"heightAspect\": 1.0,\n" +
                "                \"formatted\": \"1.00:1.00\"\n" +
                "              }\n" +
                "            }\n" +
                "          },\n" +
                "          \"identifiers\": [\n" +
                "            {\n" +
                "              \"identifier\": \"https://media.licdn.com/dms/image/123456789/profile-displayphoto-shrink_800_800/0?e=1234567890&v=beta&t=hp_WeNdlWUfK4wlVdVXHyzfMcf6QYuGMzGTYAwXOBr0\",\n" +
                "              \"file\": \"urn:li:digitalmediaFile:(urn:li:digitalmediaAsset:123456789,urn:li:digitalmediaMediaArtifactClass:profile-displayphoto-shrink_800_800,0)\",\n" +
                "              \"index\": 0,\n" +
                "              \"mediaType\": \"image/jpeg\",\n" +
                "              \"identifierType\": \"EXTERNAL_URL\",\n" +
                "              \"identifierExpiresInSeconds\": 1234567890\n" +
                "            }\n" +
                "          ]\n" +
                "        }\n" +
                "      ]\n" +
                "    }\n" +
                "  },\n" +
                "  \"id\": \"248289761001\",\n" +
                "  \"localizedFirstName\": \"Jane\"\n" +
                "}}}";
    }

    private String getLinkedInEmailResponse() {
        return "{\n" +
                "  \"elements\": [\n" +
                "    {\n" +
                "      \"handle~\": {\n" +
                "        \"emailAddress\": \"janedoe@example.com\"\n" +
                "      },\n" +
                "      \"handle\": \"urn:li:emailAddress:-123456789\"\n" +
                "    }\n" +
                "  ]\n" +
                "}";
    }

    private static String getLoginbuddyResponse() {
        return "{\"access_token\": \"ya29.ImCyBxbbmYU5vCm8LyE5rhSIFBn4cGdZvYkC8yGe93UqmEHn0\",\n" +
                "\t\"details_provider\": {\n" +
                "\t\t\"id_token_payload\": {\n" +
                "\t\t\t\"at_hash\": \"SZSvbjrY7b8cQ\",\n" +
                "\t\t\t\"sub\": \"1018256\",\n" +
                "\t\t\t\"email_verified\": true,\n" +
                "\t\t\t\"iss\": \"https:\\/\\/example.com\",\n" +
                "\t\t\t\"given_name\": \"John\",\n" +
                "\t\t\t\"locale\": \"en-GB\",\n" +
                "\t\t\t\"nonce\": \"3fddc3a345c8516c4\",\n" +
                "\t\t\t\"picture\": \"https:\\/\\/example.com\\/\\/photo.jpg\",\n" +
                "\t\t\t\"aud\": \"446042603example.com\",\n" +
                "\t\t\t\"azp\": \"446042603example.com\",\n" +
                "\t\t\t\"name\": \"John Doe\",\n" +
                "\t\t\t\"exp\": 1574485037,\n" +
                "\t\t\t\"family_name\": \"Doe\",\n" +
                "\t\t\t\"iat\": 1574481437,\n" +
                "\t\t\t\"email\": \"email@example.com\"\n" +
                "\t\t},\n" +
                "\t\t\"provider\": \"example\",\n" +
                "\t\t\"userinfo\": {\"sub\": \"1018256\",\n" +
                "\t\t\t\"name\": \"John Doe\",\n" +
                "\t\t\t\"given_name\": \"John\",\n" +
                "\t\t\t\"family_name\": \"Doe\",\n" +
                "\t\t\t\"picture\": \"https:\\/\\/example.com\\/\\/photo.jpg\",\n" +
                "\t\t\t\"email\": \"email@example.com\"\n" +
                "\t\t\t\"email_verified\": false,\n" +
                "\t\t\t\"preferred_username\": \"John 'the man' Doe\",\n" +
                "\t\t\t\"locale\": \"en-GB\"\n" +
                "\t\t}\n" +
                "\t},\n" +
                "\t\"scope\": \"openid profile email\",\n" +
                "\t\"id_token\": \"eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c\",\n" +
                "\t\"token_type\": \"Bearer\",\n" +
                "\t\"details_loginbuddy\": {\n" +
                "\t\t\"aud\": \"clientIdForTestingPurposes\",\n" +
                "\t\t\"iss\": \"https:\\/\\/local.loginbuddy.net\",\n" +
                "\t\t\"nonce\": \"3fddc3a3-d63f-438a-813b-c2f45c8516c4\",\n" +
                "\t\t\"iat\": 1574481437\n" +
                "\t},\n" +
                "\t\"expires_in\": 3600\n" +
                "}";
    }
}