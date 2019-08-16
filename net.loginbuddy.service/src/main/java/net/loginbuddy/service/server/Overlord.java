package net.loginbuddy.service.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.service.config.LoginbuddyConfig;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class Overlord extends HttpServlet {

  private static Logger LOGGER = Logger.getLogger(String.valueOf(Overlord.class));

  @Override
  public void init() throws ServletException {
    super.init();
    // initialize the configuration. If this fails, there is no reason to continue
    if (LoginbuddyConfig.getInstance().isConfigured()) {
      LOGGER.info("Loginbuddy successfully started!");
    } else {
      LOGGER.severe("Stopping loginbuddy since its configuration could not be loaded! Fix that first!");
      System.exit(1);
    }
  }

  protected MsgResponse getAPI(String accessToken, String targetApi) throws IOException {
    HttpGet req = new HttpGet(targetApi);
    HttpClient httpClient = HttpClientBuilder.create().build();
    req.setHeader(Constants.AUTHORIZATION.getKey(), Constants.BEARER.getKey() + accessToken);

    HttpResponse response = httpClient.execute(req);
    return new MsgResponse(response.getHeaders("Content-Type")[0].getValue(),
        EntityUtils.toString(response.getEntity()), response.getStatusLine().getStatusCode());
  }

  protected MsgResponse getAPI(String targetApi) throws IOException {
    HttpGet req = new HttpGet(targetApi);
    HttpClient httpClient = HttpClientBuilder.create().build();

    HttpResponse response = httpClient.execute(req);
    return new MsgResponse(response.getHeaders("Content-Type")[0].getValue(),
        EntityUtils.toString(response.getEntity()), response.getStatusLine().getStatusCode());
  }

  protected JSONObject getErrorAsJson(String error, String errorDescription) {
    if ("".equals(error)) {
      error = "unknown";
    }
    if ("".equals(errorDescription)) {
      errorDescription = "An error without any description, sorry";
    }
    JSONObject result = new JSONObject();
    result.put("error", error);
    result.put("error_description", errorDescription);
    return result;
  }

  protected String getErrorForRedirect(String redirectUri, String error, String errorDescription)
      throws UnsupportedEncodingException {
    if ("".equals(errorDescription)) {
      errorDescription = "An error without any description, sorry";
    }
    error = URLEncoder.encode(error, "UTF-8");
    errorDescription = URLEncoder.encode(errorDescription, "UTF-8");

    return redirectUri.concat("error=").concat(error).concat("&error_description=").concat(errorDescription);
  }

  protected String getMessageForRedirect(String redirectUri, String urlSafeKey, String value)
      throws UnsupportedEncodingException {
    return redirectUri.concat(urlSafeKey).concat("=").concat(URLEncoder.encode(value, "UTF-8"));
  }

  /**
   * Mappings attributes so that receiving clients can expect the same details at the same location in the response
   * message
   */
  protected JSONObject normalizeDetails(String provider, JSONObject mappings, JSONObject userinfoRespObject) {
    JSONObject result = new JSONObject();
    try {
      mappings = (mappings == null || mappings.size() == 0) ? (JSONObject) new JSONParser()
          .parse(Constants.MAPPING_OIDC.getKey().replace("asis:provider", "asis:" + provider)) : mappings;
    } catch (ParseException e) {
      // should not occur!
      LOGGER.severe(
          "The default mapping for OpenID Connect claims is invalid! Continuing as if nothing has happened ... .");
    }
    if (userinfoRespObject != null && userinfoRespObject.size() > 0) {
      for (Object nextEntry : mappings.entrySet()) {
        Map.Entry entry = (Entry) nextEntry;
        String mappingKey = (String) entry.getKey();
        String mappingRule = (String) entry.getValue();
        String outputValue = "";
        if (mappingRule.contains("[")) {
          String userinfoClaim = (String) userinfoRespObject.get(mappingRule.substring(0, mappingRule.indexOf("[")));
          int idx = Integer.parseInt(Character.toString(mappingRule.charAt(mappingRule.indexOf("[") + 1)));
          try {
            outputValue = userinfoClaim.split(" ")[idx];
          } catch (Exception e) {
            LOGGER.warning(String
                .format("invalid indexed mapping: 'mappings.%s' --> 'userinfo.%s': invalid index: %s", mappingKey,
                    mappingRule, e.getMessage()));
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

  protected MsgResponse postTokenExchange(String clientId, String clientSecret, String redirectUri, String authCode,
      String tokenEndpoint, String codeVerifier) throws IOException {

    // build POST request
    List<NameValuePair> formParameters = new ArrayList<>();
    formParameters.add(new BasicNameValuePair(Constants.CODE.getKey(), authCode));
    formParameters.add(new BasicNameValuePair(Constants.CLIENT_ID.getKey(), clientId));
    formParameters.add(new BasicNameValuePair(Constants.CLIENT_SECRET.getKey(), clientSecret));
    formParameters.add(new BasicNameValuePair(Constants.REDIRECT_URI.getKey(), redirectUri));
    formParameters.add(new BasicNameValuePair(Constants.GRANT_TYPE.getKey(), Constants.AUTHORIZATION_CODE.getKey()));
    formParameters.add(new BasicNameValuePair(Constants.CODE_VERIFIER.getKey(), codeVerifier));

    HttpPost req = new HttpPost(tokenEndpoint);

    HttpClient httpClient = HttpClientBuilder.create().build();
    req.setEntity(new UrlEncodedFormEntity(formParameters));
    req.addHeader("Accept", "application/json");

    HttpResponse response = httpClient.execute(req);
    return new MsgResponse(response.getHeaders("Content-Type")[0].getValue(),
        EntityUtils.toString(response.getEntity()), response.getStatusLine().getStatusCode());
  }

  void notYetImplemented(HttpServletResponse response) throws IOException {
    response.setStatus(418);
    response.setContentType("application/json");
    response.getWriter().write("{\"sorry\":\"not yet implemented\"}");
  }
}