/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.client;

import java.io.IOException;
import java.util.Date;
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
          sessionCtx.getString(Constants.TOKEN_ENDPOINT.getKey()), sessionCtx.getString(Constants.CODE_VERIFIER.getKey()));
      if (tokenResponse != null) {
        if (tokenResponse.getStatus() == 200) {
          if (tokenResponse.getContentType().startsWith("application/json")) {
            JSONObject tokenResponseObject = ((JSONObject) new JSONParser().parse(tokenResponse.getMsg()));
            LOGGER.fine(tokenResponseObject.toJSONString());
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
          } else {
            response.sendRedirect(getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()),
                "invalid_response",
                String.format("the provider returned a response with an unsupported content-type: %s", tokenResponse.getContentType())));
            return;
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
}