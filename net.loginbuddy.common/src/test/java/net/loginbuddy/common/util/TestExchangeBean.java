package net.loginbuddy.common.util;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Test;

import static org.junit.Assert.fail;

/**
 * This is more like a speudo test. Just verifying that elements exist
 */
public class TestExchangeBean {

  @Test
  public void testComplete() {
    ExchangeBean eb = new ExchangeBean();
    JSONObject tokenResponse = new JSONObject();
    tokenResponse.put("access_token", "access-token-test");
    tokenResponse.put("refresh_token", "refresh-token-test");
    tokenResponse.put("scope", "scope1 scopeb");
    tokenResponse.put("token_type", "Bearer");
    tokenResponse.put("expires_in", 3600);
    tokenResponse.put("id_token", "id-token-test");

    eb.setTokenResponse(tokenResponse);

    eb.setIss("http://test.loginbuddy.iss.com");
    eb.setNonce("nonce-test");
    eb.setAud("aud-test");
    eb.setIat(1234567890);
    eb.setProvider("provider-test");
    try {
      eb.setUserinfo((JSONObject)new JSONParser().parse("{\"user\": \"content-of-userinfo-response\"}"));
      eb.setIdTokenPayload((JSONObject)new JSONParser().parse("{\"idtoken\": \"content-of-userinfo-response\"}"));
    } catch (ParseException e) {
      fail();
    }
    // System.out.println(eb.toString());
 }

  @Test
  public void testMinimum() {
    ExchangeBean eb = new ExchangeBean();
    JSONObject tokenResponse = new JSONObject();
    tokenResponse.put("access_token", "access-token-test");
    tokenResponse.put("token_type", "Bearer");
    tokenResponse.put("expires_in", 3600);

    eb.setTokenResponse(tokenResponse);
    eb.setIss("http://test.loginbuddy.iss.com");
    eb.setAud("aud-test");
    eb.setIat(1234567890);
    eb.setProvider("provider-test");
    try {
      eb.setUserinfo((JSONObject)new JSONParser().parse("{\"user\": \"content-of-userinfo-response\"}"));
    } catch (ParseException e) {
      fail();
    }
//    System.out.println(eb.toString());
  }

}
