/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.client;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
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
import net.loginbuddy.service.config.LoginbuddyConfig;
import net.loginbuddy.service.config.ProviderConfig;
import net.loginbuddy.service.server.Overlord;
import net.loginbuddy.service.util.SessionContext;
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


public class Callback extends Overlord {

  private static final Logger LOGGER = Logger.getLogger(String.valueOf(Callback.class));

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    try {

      String sessionId = request.getParameter(Constants.STATE.getKey());
      if (sessionId == null || sessionId.trim().length() == 0
          || request.getParameterValues(Constants.STATE.getKey()).length > 1) {
        LOGGER.warning("Missing or invalid state parameter returned from provider!");
        response.sendError(400, "Missing or invalid state parameter");
        return;
      }

      SessionContext sessionCtx = (SessionContext) LoginbuddyCache.getInstance().remove(sessionId);
      if (sessionCtx == null || !sessionId.equals(sessionCtx.getId())) {
        LOGGER.warning("The current session is invalid or it has expired! Given: '" + sessionId + "'");
        response.sendError(400, "The current session is invalid or it has expired!");
        return;
      }

      if (!Constants.ACTION_CALLBACK.getKey().equals(sessionCtx.getString(Constants.ACTION_EXPECTED.getKey()))) {
        LOGGER.warning(
            "The current action was not expected! Given: '" + sessionCtx.getString(Constants.ACTION_EXPECTED.getKey())
                + "', expected: '" + Constants.ACTION_CALLBACK.getKey() + "'");
        response.sendError(400, "The current action was not expected!");
        return;
      }

      String clientRedirectUri = sessionCtx.getString(Constants.CLIENT_REDIRECT.getKey());
      if (clientRedirectUri.contains("?")) {
        clientRedirectUri = clientRedirectUri.concat("&");

      } else {
        clientRedirectUri = clientRedirectUri.concat("?");
      }
      String clientState = sessionCtx.getString(Constants.CLIENT_STATE.getKey());

      String error = request.getParameter(Constants.ERROR.getKey());
      String errorDescription = null;
      if (error != null) {
        errorDescription = request.getParameter("error_description");
        clientRedirectUri = getErrorForRedirect(clientRedirectUri, clientState, error, errorDescription);
        response.sendRedirect(clientRedirectUri);
        return;
      }

      String providersAuthCode = request.getParameter(Constants.CODE.getKey());
      if (providersAuthCode == null || providersAuthCode.trim().length() == 0
          || request.getParameterValues(Constants.CODE.getKey()).length > 1) {
        clientRedirectUri = clientRedirectUri
            .concat("error=invalid_request&error_description=Missing+or+invalid+code+parameter");
        response.sendRedirect(clientRedirectUri);
        return;
      }

      String provider = sessionCtx.getString(Constants.CLIENT_PROVIDER.getKey());
      String code_verifier = sessionCtx.getString(Constants.CODE_VERIFIER.getKey());

      ProviderConfig providerConfig = LoginbuddyConfig.getInstance().getConfigUtil()
          .getProviderConfigByProvider(provider);

      String tokenEndpoint = null;
      String userInfoEndpoint = null;
      String jwksUri = null;

      if (providerConfig.getOpenidConfigurationUri() != null) {
        tokenEndpoint = sessionCtx.getString(Constants.TOKEN_ENDPOINT.getKey());
        userInfoEndpoint = sessionCtx.getString(Constants.USERINFO_ENDPOINT.getKey());
        jwksUri = sessionCtx.getString(Constants.JWKS_URI.getKey());
      } else {
        tokenEndpoint = providerConfig.getTokenEndpoint();
        userInfoEndpoint = providerConfig.getUserinfoEndpoint();
        jwksUri = providerConfig.getJwksUri();
      }

      ExchangeBean eb = new ExchangeBean();
      eb.setIss((String) oidcConfig.get(Constants.ISSUER.getKey()));
      eb.setIat(new Date().getTime() / 1000);
      eb.setAud(sessionCtx.getString(Constants.CLIENT_ID.getKey()));
      eb.setNonce(sessionCtx.getString(Constants.NONCE.getKey()));
      eb.setProvider(provider);

      String access_token = null;
      String id_token = null;

      MsgResponse tokenResponse = postTokenExchange(providerConfig.getClientId(), providerConfig.getClientSecret(),
          providerConfig.getRedirectUri(), providersAuthCode, tokenEndpoint, code_verifier);
      if (tokenResponse != null) {
        if (tokenResponse.getStatus() == 200) {
          if (tokenResponse.getContentType().startsWith("application/json")) {
            JSONObject tokenResponseObject = ((JSONObject) new JSONParser().parse(tokenResponse.getMsg()));
            LOGGER.info(tokenResponseObject.toJSONString());
            access_token = tokenResponseObject.get("access_token").toString();
            eb.setTokenResponse(tokenResponseObject);
            try {
              id_token = tokenResponseObject.get("id_token").toString();
              MsgResponse jwks = getAPI(jwksUri);
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
            error = (String) err.get("error");
            errorDescription = (String) err.get("error_description");
            clientRedirectUri = getErrorForRedirect(clientRedirectUri, clientState, error, errorDescription);
            response.sendRedirect(clientRedirectUri);
            return;
          }
        }
      } else {
        clientRedirectUri = clientRedirectUri.concat(
            "error=invalid_request&error_description=The+code+exchange+failed.+An+access_token+could+not+be+retrieved");
        response.sendRedirect(clientRedirectUri);
        return;
      }

      // Call /userinfo only if it was configured in config.json
      if (userInfoEndpoint != null && !"".equals(userInfoEndpoint)) {
        MsgResponse userinfoResp = getProtectedAPI(access_token, userInfoEndpoint);
        if (userinfoResp != null) {
          if (userinfoResp.getStatus() == 200) {
            if (userinfoResp.getContentType().startsWith("application/json")) {
              JSONObject userinfoRespObject = (JSONObject) new JSONParser().parse(userinfoResp.getMsg());
              eb.setUserinfo(userinfoRespObject);
            }
          }
        }
      }

      String authorizationCode = UUID.randomUUID().toString();
      sessionCtx.put("eb", eb.toString());
      sessionCtx.put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_TOKEN_EXCHANGE.getKey());
      LoginbuddyCache.getInstance().put(authorizationCode, sessionCtx);

      clientRedirectUri = clientState == null ? clientRedirectUri.concat("code=").concat(authorizationCode) : clientRedirectUri.concat("code=").concat(authorizationCode).concat("&state=").concat(clientState);
      response.sendRedirect(clientRedirectUri);

    } catch (Exception e) {
      LOGGER.warning("authorization request failed!");
      e.printStackTrace();
      response.getWriter().println("authorization request failed!");
    }
  }

  private String getErrorForRedirect(String clientRedirectUri, String clientState, String error,
      String errorDescription) throws UnsupportedEncodingException {
    if (errorDescription == null) {
      errorDescription = "An error was returned by the provider without any description";
    }
    error = URLEncoder.encode(error, "UTF-8");
    errorDescription = URLEncoder.encode(errorDescription, "UTF-8");

    if (clientState != null) {
      return clientRedirectUri.concat("error=").concat(error).concat("&error_description=").concat(errorDescription)
          .concat("&state=").concat(clientState);
    } else {
      return clientRedirectUri.concat("error=").concat(error).concat("&error_description=")
          .concat(errorDescription);
    }
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

  private MsgResponse getProtectedAPI(String accessToken, String targetApi) {
    try {
      HttpGet req = new HttpGet(targetApi);
      HttpClient httpClient = HttpClientBuilder.create().build();
      req.setHeader(Constants.AUTHORIZATION.getKey(), Constants.BEARER.getKey() + accessToken);

      HttpResponse response = httpClient.execute(req);
      return new MsgResponse(response.getHeaders("Content-Type")[0].getValue(),
          EntityUtils.toString(response.getEntity()), response.getStatusLine().getStatusCode());
    } catch (Exception e) {
      LOGGER.warning("Call to targetApi failed!");
      e.printStackTrace();
      return null;
    }
  }

  private MsgResponse getAPI(String targetApi) {
    try {
      HttpGet req = new HttpGet(targetApi);
      HttpClient httpClient = HttpClientBuilder.create().build();

      HttpResponse response = httpClient.execute(req);
      return new MsgResponse(response.getHeaders("Content-Type")[0].getValue(),
          EntityUtils.toString(response.getEntity()), response.getStatusLine().getStatusCode());
    } catch (Exception e) {
      LOGGER.warning("The API response could not be retrieved. Given URL: '" + targetApi + "'");
      e.printStackTrace();
      return null;
    }
  }

}