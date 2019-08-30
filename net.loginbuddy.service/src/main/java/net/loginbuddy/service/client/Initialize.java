/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.client;

import static net.loginbuddy.common.api.HttpHelper.getErrorForRedirect;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.logging.Logger;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import net.loginbuddy.common.util.Pkce;
import net.loginbuddy.common.util.PkcePair;
import net.loginbuddy.service.config.LoginbuddyConfig;
import net.loginbuddy.service.config.ProviderConfig;
import net.loginbuddy.service.util.SessionContext;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@WebServlet(name = "Initialize")
public class Initialize extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(String.valueOf(Initialize.class));

  // initiate authorization flow with provider
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    doPost(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

    ParameterValidatorResult providerResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.PROVIDER.getKey()));
    ParameterValidatorResult sessionIdResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.SESSION.getKey()));

    if (!sessionIdResult.getResult().equals(RESULT.VALID)) {
      LOGGER.warning("Missing session, cannot initiate the authorization flow!");
      response.sendError(400, "Missing session, cannot initiate the authorization flow!");
      return;
    }

    SessionContext sessionCtx = (SessionContext) LoginbuddyCache.getInstance().get(sessionIdResult.getValue());
    if (sessionCtx == null || !sessionIdResult.getValue().equals(sessionCtx.getId())) {
      LOGGER.warning("The current session is invalid or it has expired!");
      response.sendError(400, "The current session is invalid or it has expired!");
      return;
    }

    if (!Constants.ACTION_INITIALIZE.getKey().equals(sessionCtx.getString(Constants.ACTION_EXPECTED.getKey()))) {
      LOGGER.warning(
          "The current action was not expected! Given: '" + sessionCtx.getString(Constants.ACTION_EXPECTED.getKey())
              + "', expected: '" + Constants.ACTION_INITIALIZE.getKey() + "'");
      response.sendRedirect(
          getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "invalid_session",
              "the request was not expected"));
      return;
    }

    String providerSession = (sessionCtx.getString(Constants.CLIENT_PROVIDER.getKey())).trim();
    if ("".equals(providerSession)) {
      if (providerResult.getResult().equals(RESULT.VALID)) {
        providerSession = providerResult.getValue();
      } else {
        LOGGER.warning("No provider has been selected or an invalid parameters has been given");
        response.sendRedirect(
            getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "invalid_request",
                "No provider has been selected or an invalid parameters has been given"));
        return;
      }
    }

    // TODO Verify that the selected provider is valid for this client
    ProviderConfig providerConfig = null;
    try {
      providerConfig = LoginbuddyConfig.getInstance().getConfigUtil().getProviderConfigByProvider(providerSession);
      if (providerConfig == null) {
        LOGGER.warning("The given provider is unknown or invalid");
        response.sendRedirect(
            getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "invalid_request",
                "The given provider is unknown or invalid"));
        return;
      } else {
        sessionCtx.put(Constants.CLIENT_PROVIDER.getKey(), providerSession);
      }
    } catch (Exception e) {
      // should never occur
      LOGGER.severe("The system has not been configured yet!");
      response.sendRedirect(
          getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "server_error",
              "The system has not been configured yet!"));
      return;
    }

    StringBuilder authorizeUrl = new StringBuilder();

    // using the well-known endpoint
    String oidcConfigUrl = providerConfig.getOpenidConfigurationUri();
    if (oidcConfigUrl != null) {
      MsgResponse openIdConfig = HttpHelper.getAPI(oidcConfigUrl);
      if (openIdConfig != null && openIdConfig.getStatus() == 200) {
        JSONObject msg = null;
        try {
          msg = (JSONObject) new JSONParser().parse(openIdConfig.getMsg());
        } catch (ParseException e) {
          // should never happen
          LOGGER.warning("For some unknown reason the OpenID Configuration could not be parsed as JSON object!");
          response.sendRedirect(
              getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "message_error",
                  "The OpenID Configuration could not be parsed!"));
          return;
        }
        authorizeUrl.append(msg.get(Constants.AUTHORIZATION_ENDPOINT.getKey()).toString());
        sessionCtx.put(Constants.TOKEN_ENDPOINT.getKey(), msg.get(Constants.TOKEN_ENDPOINT.getKey()).toString());
        sessionCtx.put(Constants.USERINFO_ENDPOINT.getKey(), msg.get(Constants.USERINFO_ENDPOINT.getKey()).toString());
        sessionCtx.put(Constants.JWKS_URI.getKey(), msg.get(Constants.JWKS_URI.getKey()).toString());
      } else {
        LOGGER.warning(String.format("The OpenID Connect configuration could not be retrieved. Given URL: %s", oidcConfigUrl));
        response.sendRedirect(
            getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "message_error",
                "The OpenID Configuration could not be retrieved!"));
        return;
      }
    } else {
      // using the configured URLs since a well-known endpoint has not been configured
      authorizeUrl.append(providerConfig.getAuthorizationEndpoint());
      sessionCtx.put(Constants.TOKEN_ENDPOINT.getKey(), providerConfig.getTokenEndpoint());
      sessionCtx.put(Constants.USERINFO_ENDPOINT.getKey(), providerConfig.getUserinfoEndpoint());
      sessionCtx.put(Constants.JWKS_URI.getKey(), providerConfig.getJwksUri());
    }

    // use PKCE only if the provider supports it. Unfortunately, some providers fail if unsupported parameters are being send
    String pkce = null;
    if (providerConfig.getPkce()) {
      PkcePair pair = Pkce.create(Pkce.CODE_CHALLENGE_METHOD_S256);
      sessionCtx.put(Constants.CODE_VERIFIER.getKey(), pair.getVerifier());
      pkce = String.format("&%s=%s&%s=S256", Constants.CODE_CHALLENGE.getKey(), pair.getChallenge(),
          Constants.CODE_CHALLENGE_METHOD.getKey());
    }

    String scope = providerConfig.getScope() == null ? Constants.OPENID_SCOPE.getKey() : providerConfig.getScope();

    authorizeUrl.append("?")
        .append(Constants.CLIENT_ID.getKey())
        .append("=").append(providerConfig.getClientId())
        .append("&").append(Constants.RESPONSE_TYPE.getKey())
        .append("=").append(providerConfig.getResponseType())
        .append("&").append(Constants.SCOPE.getKey())
        .append("=").append(URLEncoder.encode(scope, "UTF-8"))
        .append("&").append(Constants.NONCE.getKey())
        .append("=").append(sessionCtx.get(Constants.NONCE.getKey()))
        .append("&").append(Constants.REDIRECT_URI.getKey())
        .append("=").append(URLEncoder.encode(providerConfig.getRedirectUri(), "utf-8"))
        .append(pkce == null ? "" : pkce)
        .append("&").append(Constants.PROMPT.getKey()).append("=")
        .append(sessionCtx.getString(Constants.CLIENT_PROMPT.getKey()))
        .append("&").append(Constants.LOGIN_HINT.getKey()).append("=")
        .append(sessionCtx.getString(Constants.CLIENT_LOGIN_HINT.getKey()))
        .append("&").append(Constants.ID_TOKEN_HINT.getKey()).append("=")
        .append(sessionCtx.getString(Constants.CLIENT_ID_TOKEN_HINT.getKey()))
        .append("&").append(Constants.STATE.getKey())
        .append("=").append(sessionCtx.getId());

    sessionCtx.setSessionCallback();

    LoginbuddyCache.getInstance().put(sessionCtx.getId(), sessionCtx);

    response.sendRedirect(authorizeUrl.toString());
  }
}