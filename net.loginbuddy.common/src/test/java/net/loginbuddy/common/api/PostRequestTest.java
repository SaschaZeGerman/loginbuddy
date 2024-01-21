package net.loginbuddy.common.api;

import org.apache.http.client.methods.HttpPost;
import org.jose4j.base64url.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Test;

import static org.junit.Assert.*;

public class PostRequestTest {

    @Test
    public void testPostRequestSimple() {
        try {
            HttpPost httpPost = PostRequest.create("https://localhost:8443/token").setDpopHeader("RS256", "https://localhost:8443/token", "myAccessToken", "dpop-nonce-value", "localhost:8443").build();
            assertEquals("POST", httpPost.getMethod());
            assertEquals("https://localhost:8443/token",httpPost.getURI().toString());
            String dpopHeader = httpPost.getFirstHeader("dpop").getValue();
            assertNotNull(dpopHeader);
            JSONObject dpopProofPayload = (JSONObject)new JSONParser().parse(new String(Base64.decode(dpopHeader.split("[.]")[1])));
            assertEquals("POST", dpopProofPayload.get("htm"));
            assertEquals("https://localhost:8443/token", dpopProofPayload.get("htu"));
            assertEquals("dpop-nonce-value", dpopProofPayload.get("nonce"));
            assertEquals("duQDMgs1kN5rJ8XEG5n1ClU97kGkhWOzSzZy9S9RKrE", dpopProofPayload.get("ath"));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
