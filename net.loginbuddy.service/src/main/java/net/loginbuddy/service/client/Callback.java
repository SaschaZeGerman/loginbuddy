/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.client;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ExchangeBean;
import net.loginbuddy.common.util.Jwt;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import net.loginbuddy.service.config.LoginbuddyConfig;
import net.loginbuddy.service.config.ProviderConfig;
import net.loginbuddy.service.server.Overlord;
import net.loginbuddy.service.util.SessionContext;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;


public class Callback extends Overlord {

  private static final Logger LOGGER = Logger.getLogger(String.valueOf(Callback.class));

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    try {

      ParameterValidatorResult sessionIdResult = ParameterValidator
          .getSingleValue(request.getParameterValues(Constants.STATE.getKey()));
      ParameterValidatorResult codeResult = ParameterValidator
          .getSingleValue(request.getParameterValues(Constants.CODE.getKey()));
      ParameterValidatorResult errorResult = ParameterValidator
          .getSingleValue(request.getParameterValues(Constants.ERROR.getKey()));
      ParameterValidatorResult errorDescriptionResult = ParameterValidator
          .getSingleValue(request.getParameterValues(Constants.ERROR_DESCRIPTION.getKey()), "");

      if (!sessionIdResult.getResult().equals(RESULT.VALID)) {
        LOGGER.warning("Missing or invalid state parameter returned from provider!");
        response.sendError(400, "Missing or invalid state parameter");
        return;
      }

      SessionContext sessionCtx = (SessionContext) LoginbuddyCache.getInstance().remove(sessionIdResult.getValue());
      if (sessionCtx == null || !sessionIdResult.getValue().equals(sessionCtx.getId())) {
        LOGGER.warning("The current session is invalid or it has expired! Given: '" + sessionIdResult.getValue() + "'");
        response.sendError(400, "The current session is invalid or it has expired!");
        return;
      }

      if (errorResult.getValue() != null) {
        response.sendRedirect(
            getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), errorResult.getValue(),
                errorDescriptionResult.getValue()));
        return;
      }

      if (!Constants.ACTION_CALLBACK.getKey().equals(sessionCtx.getString(Constants.ACTION_EXPECTED.getKey()))) {
        LOGGER.warning(
            "The current action was not expected! Given: '" + sessionCtx.getString(Constants.ACTION_EXPECTED.getKey())
                + "', expected: '" + Constants.ACTION_CALLBACK.getKey() + "'");
        response.sendRedirect(
            getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "invalid_session",
                "the request was not expected"));
        return;
      }

      if (!codeResult.getResult().equals(RESULT.VALID)) {
        LOGGER.warning("Missing code parameter returned from provider!");
        response.sendRedirect(
            getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "invalid_session",
                "missing or invalid code parameter"));
        return;
      }

      String provider = sessionCtx.getString(Constants.CLIENT_PROVIDER.getKey());
      String code_verifier = sessionCtx.getString(Constants.CODE_VERIFIER.getKey());

      ProviderConfig providerConfig = LoginbuddyConfig.getInstance().getConfigUtil()
          .getProviderConfigByProvider(provider);

      ExchangeBean eb = new ExchangeBean();
      eb.setIss(LoginbuddyConfig.getInstance().getDiscoveryUtil().getIssuer());
      eb.setIat(new Date().getTime() / 1000);
      eb.setAud(sessionCtx.getString(Constants.CLIENT_ID.getKey()));
      eb.setNonce(sessionCtx.getString(Constants.NONCE.getKey()));
      eb.setProvider(provider);

      String access_token = null;
      String id_token = null;

      MsgResponse tokenResponse = postTokenExchange(providerConfig.getClientId(), providerConfig.getClientSecret(),
          providerConfig.getRedirectUri(), codeResult.getValue(),
          sessionCtx.getString(Constants.TOKEN_ENDPOINT.getKey()), code_verifier);
      if (tokenResponse != null) {
        if (tokenResponse.getStatus() == 200) {
          if (tokenResponse.getContentType().startsWith("application/json")) {
            JSONObject tokenResponseObject = ((JSONObject) new JSONParser().parse(tokenResponse.getMsg()));
//            LOGGER.info(tokenResponseObject.toJSONString()); // turn this on for debugging purposes
            access_token = tokenResponseObject.get("access_token").toString();
            eb.setTokenResponse(tokenResponseObject);
            try {
              id_token = tokenResponseObject.get("id_token").toString();
              MsgResponse jwks = getAPI(sessionCtx.getString(Constants.JWKS_URI.getKey()));
              JSONObject idTokenPayload = new Jwt()
                  .validateJwt(id_token, jwks.getMsg(), providerConfig.getIssuer(), providerConfig.getClientId(),
                      sessionCtx.getString(Constants.NONCE.getKey()));
              eb.setIdTokenPayload(idTokenPayload);
            } catch (Exception e) {
              LOGGER.warning("No id_token was issued or it was invalid!");
            }
          }
        } else {
          // need to handle error cases
          if (tokenResponse.getContentType().startsWith("application/json")) {
            JSONObject err = (JSONObject) new JSONParser().parse(tokenResponse.getMsg());
            response.sendRedirect(getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()),
                (String) err.get("error"),
                (String) err.get("error_description")));
            return;
          }
        }
      } else {
        response.sendRedirect(
            getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "invalid_request",
                "the code exchange failed. An access_token could not be retrieved"));
        return;
      }

      MsgResponse userinfoResp = getAPI(access_token, sessionCtx.getString(Constants.USERINFO_ENDPOINT.getKey()));
      if (userinfoResp != null) {
        if (userinfoResp.getStatus() == 200) {
          if (userinfoResp.getContentType().startsWith("application/json")) {
            JSONObject userinfoRespObject = (JSONObject) new JSONParser().parse(userinfoResp.getMsg());
            eb.setUserinfo(userinfoRespObject);
            eb.setNormalized(normalizeDetails(provider, providerConfig.getMappingsAsJson(), userinfoRespObject));
          }
        }
      }

      String authorizationCode = UUID.randomUUID().toString();
      sessionCtx.put("eb", eb.toString());
      sessionCtx.put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_TOKEN_EXCHANGE.getKey());
      LoginbuddyCache.getInstance().put(authorizationCode, sessionCtx);

      response.sendRedirect(
          getMessageForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "code",
              authorizationCode));

    } catch (Exception e) {
      LOGGER.warning("authorization request failed!");
      e.printStackTrace();
      response.sendError(400, "authorization request failed!");
    }
  }

  /**
   * Mappings attributes so that receiving clients can expect the same details at the same location in the response
   * message
   */
  private JSONObject normalizeDetails(String provider, JSONObject mappings, JSONObject userinfoRespObject) {
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

  private MsgResponse postTokenExchange(String clientId, String clientSecret, String redirectUri, String authCode,
      String tokenEndpoint, String codeVerifier) {

    // build POST request
    List<NameValuePair> formParameters = new ArrayList<>();
    formParameters.add(new BasicNameValuePair(Constants.CODE.getKey(), authCode));
    formParameters.add(new BasicNameValuePair(Constants.CLIENT_ID.getKey(), clientId));
    formParameters.add(new BasicNameValuePair(Constants.CLIENT_SECRET.getKey(), clientSecret));
    formParameters.add(new BasicNameValuePair(Constants.REDIRECT_URI.getKey(), redirectUri));
    formParameters.add(new BasicNameValuePair(Constants.GRANT_TYPE.getKey(), Constants.AUTHORIZATION_CODE.getKey()));
    formParameters.add(new BasicNameValuePair(Constants.CODE_VERIFIER.getKey(), codeVerifier));

    try {
      HttpPost req = new HttpPost(tokenEndpoint);

      HttpClient httpClient = HttpClientBuilder.create().build();
      req.setEntity(new UrlEncodedFormEntity(formParameters));
      req.addHeader("Accept", "application/json");

      HttpResponse response = httpClient.execute(req);
      return new MsgResponse(response.getHeaders("Content-Type")[0].getValue(),
          EntityUtils.toString(response.getEntity()), response.getStatusLine().getStatusCode());
    } catch (Exception e) {
      LOGGER.warning("Token exchange request failed!");
      e.printStackTrace();
      return null;
    }
  }
}