/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.sidecar;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.Pkce;
import net.loginbuddy.common.util.PkcePair;
import net.loginbuddy.service.config.LoginbuddyConfig;
import net.loginbuddy.service.config.ProviderConfig;
import net.loginbuddy.service.util.SessionContext;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

/**
 * This class accepts connections only from localhost. The intention is easy access if loginbuddy is used as a sidecar
 * deployment where it becomes part of the actual web application. This setup makes it possible to run loginbuddy
 * transparent to the outside world. At the same time, its external API are still available if necessary.
 */
public class Initialize extends SidecarMaster {

  private static final Logger LOGGER = Logger.getLogger(String.valueOf(Initialize.class));

  // initiate authorization flow with provider
  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    doPost(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

    try {
      checkClientConnection(request);
    } catch (IllegalAccessException e) {
      LOGGER.warning(e.getMessage());
      response.setStatus(400);
      response.setContentType("application/json");
      response.getWriter().write(e.getMessage());
      return;
    }

    ParameterValidatorResult providerResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.PROVIDER.getKey()));
    ParameterValidatorResult nonceResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.NONCE.getKey()));
    ParameterValidatorResult stateResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.STATE.getKey()));
    ParameterValidatorResult promptResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.PROMPT.getKey()), "");
    ParameterValidatorResult idTokenHintResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.ID_TOKEN_HINT.getKey()), "");
    ParameterValidatorResult loginHintResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.LOGIN_HINT.getKey()), "");
    ParameterValidatorResult acceptDynamicProvider = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.ACCEPT_DYNAMIC_PROVIDER.getKey()), "false");

    SessionContext sessionCtx = new SessionContext();

    // TODO Verify that the selected provider is valid for this client
    ProviderConfig providerConfig = null;
    try {
      providerConfig = LoginbuddyConfig.getInstance().getConfigUtil()
          .getProviderConfigByProvider(providerResult.getValue());
      if (providerConfig == null) {
        LOGGER.warning("The given provider is unknown or invalid");
        response.getWriter()
            .write(HttpHelper.getErrorAsJson("invalid_request", "The given provider is unknown or invalid").toJSONString());
        return;
      }
    } catch (Exception e) {
      // should never occur
      LOGGER.severe("The system has not been configured yet");
      response.getWriter()
          .write(HttpHelper.getErrorAsJson("invalid_system", "The system has not been configured yet").toJSONString());
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
          response.getWriter().write(HttpHelper.getErrorAsJson("invalid_target",
              "For some unknown reason the OpenID Configuration could not be parsed as JSON object").toJSONString());
          return;
        }
        authorizeUrl.append(msg.get(Constants.AUTHORIZATION_ENDPOINT.getKey()).toString());
        sessionCtx.put(Constants.TOKEN_ENDPOINT.getKey(), msg.get(Constants.TOKEN_ENDPOINT.getKey()).toString());
        sessionCtx.put(Constants.USERINFO_ENDPOINT.getKey(), msg.get(Constants.USERINFO_ENDPOINT.getKey()).toString());
        sessionCtx.put(Constants.JWKS_URI.getKey(), msg.get(Constants.JWKS_URI.getKey()).toString());
      } else {
        LOGGER.warning(
            String.format("The OpenID Connect configuration could not be retrieved. Given URL: %s", oidcConfigUrl));
        response.getWriter().write(HttpHelper.getErrorAsJson("invalid_target",
            String.format("The OpenID Connect configuration could not be retrieved. Given URL: %s", oidcConfigUrl))
            .toJSONString());
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
    PkcePair pair = Pkce.create(Pkce.CODE_CHALLENGE_METHOD_S256);
    String pkce = null;
    if (providerConfig.getPkce()) {
      sessionCtx.put(Constants.CODE_VERIFIER.getKey(), pair.getVerifier());
      pkce = String.format("&%s=%s&%s=S256", Constants.CODE_CHALLENGE.getKey(), pair.getChallenge(),
          Constants.CODE_CHALLENGE_METHOD.getKey());
    }

    String scope = providerConfig.getScope() == null ? Constants.OPENID_SCOPE.getKey() : providerConfig.getScope();
    boolean adp = Boolean.parseBoolean( (acceptDynamicProvider.getValue() == null) ? "false" : "true".equalsIgnoreCase(acceptDynamicProvider.getValue()) ? "true" : "false");

    sessionCtx.setSessionInit(
        providerConfig.getClientId(),
        scope,
        providerConfig.getResponseType(),
        pair.getChallenge(),
        Pkce.CODE_CHALLENGE_METHOD_S256,
        "",
        nonceResult.getValue(),
        stateResult.getValue(),
        providerResult.getValue(),
        promptResult.getValue(),
        loginHintResult.getValue(),
        idTokenHintResult.getValue(),
        false,
        "",
        adp);

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

    response.setStatus(201);
    response.setContentType("text/plain");
    response.addHeader("Location", authorizeUrl.toString());
  }
}