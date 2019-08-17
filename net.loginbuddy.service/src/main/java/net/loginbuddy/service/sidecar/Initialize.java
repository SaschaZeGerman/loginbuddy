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
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    super.doGet(request, response);

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

    SessionContext sessionCtx = new SessionContext();

    ProviderConfig providerConfig = null;
    try {
      providerConfig = LoginbuddyConfig.getInstance().getConfigUtil()
          .getProviderConfigByProvider(providerResult.getValue());
    } catch (Exception e) {
      // should never occur
      LOGGER.severe("The system has not been configured yet");
      response.getWriter().write(getErrorAsJson("invalid_system", "system not configured yet").toJSONString());
      return;
    }

    StringBuilder authorizeUrl = new StringBuilder();

    // using the well-known endpoint
    String oidcConfigUrl = providerConfig.getOpenidConfigurationUri();
    if (oidcConfigUrl != null) {
      MsgResponse openIdConfig = getAPI(oidcConfigUrl);
      if (openIdConfig != null && openIdConfig.getStatus() == 200) {
        JSONObject msg = null;
        try {
          msg = (JSONObject) new JSONParser().parse(openIdConfig.getMsg());
        } catch (ParseException e) {
          // should never happen
          LOGGER.warning("For some unknown reason the OpenID Configuration could not be parsed as JSON object!");
          response.getWriter().write(getErrorAsJson("invalid_target",
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
        response.getWriter().write(getErrorAsJson("invalid_target",
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

    PkcePair pair = Pkce.create(Pkce.CODE_CHALLENGE_METHOD_S256);
    sessionCtx.put(Constants.CODE_VERIFIER.getKey(), pair.getVerifier());

    String scope = providerConfig.getScope() == null ? Constants.OPENID_SCOPE.getKey() : providerConfig.getScope();

    sessionCtx.setSessionInit(
        providerConfig.getClientId(),
        scope,
        providerConfig.getResponseType(),
        pair.getChallenge(),
        "S256",
        "",
        nonceResult.getValue(),
        stateResult.getValue(),
        providerResult.getValue(),
        promptResult.getValue(),
        loginHintResult.getValue(),
        idTokenHintResult.getValue(),
        false,
        "");

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
        .append("&").append(Constants.CODE_CHALLENGE.getKey()).append("=")
        .append(pair.getChallenge()) // won't produce null unless we ask for method=plain which we do not do
        .append("&").append(Constants.CODE_CHALLENGE_METHOD.getKey()).append("=S256")
        .append("&").append(Constants.PROMPT.getKey()).append("=")
        .append(URLEncoder.encode(promptResult.getValue(), "utf-8"))
        .append("&").append(Constants.LOGIN_HINT.getKey()).append("=")
        .append(URLEncoder.encode(loginHintResult.getValue(), "utf-8"))
        .append("&").append(Constants.ID_TOKEN_HINT.getKey()).append("=")
        .append(URLEncoder.encode(idTokenHintResult.getValue(), "utf-8"))
        .append("&").append(Constants.STATE.getKey())
        .append("=").append(sessionCtx.getId());

    sessionCtx.setSessionCallback();

    LoginbuddyCache.getInstance().put(sessionCtx.getId(), sessionCtx);

    response.setStatus(201);
    response.setContentType("text/plain");
    response.addHeader("Location", authorizeUrl.toString());
  }
}