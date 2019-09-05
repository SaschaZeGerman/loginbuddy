/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.sidecar;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
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
import net.loginbuddy.service.util.SessionContext;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Callback  extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(String.valueOf(Callback.class));

  @Override
  protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
    doGet(req, resp);
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    try {
      SidecarMaster.checkClientConnection(request);
    } catch (IllegalAccessException e) {
      LOGGER.warning(e.getMessage());
      response.setStatus(400);
      response.setContentType("application/json");
      response.getWriter().write(e.getMessage());
      return;
    }

    try {

      ParameterValidatorResult sessionIdResult = ParameterValidator
          .getSingleValue(request.getParameterValues(Constants.STATE.getKey()));
      ParameterValidatorResult codeResult = ParameterValidator
          .getSingleValue(request.getParameterValues(Constants.CODE.getKey()));
      ParameterValidatorResult errorResult = ParameterValidator
          .getSingleValue(request.getParameterValues(Constants.ERROR.getKey()));
      ParameterValidatorResult errorDescriptionResult = ParameterValidator
          .getSingleValue(request.getParameterValues(Constants.ERROR_DESCRIPTION.getKey()), "");

// ***************************************************************
// ** Check for the current session
// ***************************************************************

      if (!sessionIdResult.getResult().equals(RESULT.VALID)) {
        LOGGER.warning("Missing or invalid state parameter returned from provider!");
        response.getWriter().write(HttpHelper.getErrorAsJson("invalid_response", "Missing or invalid state parameter returned from provider").toJSONString());
        return;
      }

      SessionContext sessionCtx = (SessionContext) LoginbuddyCache.getInstance().remove(sessionIdResult.getValue());
      if (sessionCtx == null || !sessionIdResult.getValue().equals(sessionCtx.getId())) {
        LOGGER.warning("The current session is invalid or it has expired! Given: '" + sessionIdResult.getValue() + "'");
        response.getWriter().write(HttpHelper.getErrorAsJson("invalid_session", "the current session is invalid or it has expired").toJSONString());
        return;
      }

// ***************************************************************
// ** End the fun here if the provider send back an error
// ***************************************************************

      if (errorResult.getValue() != null) {
        response.getWriter().write(HttpHelper.getErrorAsJson(errorResult.getValue(), errorDescriptionResult.getValue()).toJSONString());
        return;
      }

// ***************************************************************
// ** Check if we expected this call
// ***************************************************************

      if (!Constants.ACTION_CALLBACK.getKey().equals(sessionCtx.getString(Constants.ACTION_EXPECTED.getKey()))) {
        LOGGER.warning(
            "The current action was not expected! Given: '" + sessionCtx.getString(Constants.ACTION_EXPECTED.getKey())
                + "', expected: '" + Constants.ACTION_CALLBACK.getKey() + "'");
        response.getWriter().write(HttpHelper.getErrorAsJson("invalid_session", "the request was not expected").toJSONString());
        return;
      }

// ***************************************************************
// ** If we did not get a valid code parameter we are done
// ***************************************************************

      if (!codeResult.getResult().equals(RESULT.VALID)) {
        LOGGER.warning("Missing code parameter returned from provider!");
        response.getWriter().write(HttpHelper.getErrorAsJson("invalid_session", "missing or invalid code parameter").toJSONString());
        return;
      }

// ***************************************************************
// ** Find the chosen provider of this session and get a token. Also start preparing the response for the client
// ***************************************************************

      String provider = sessionCtx.getString(Constants.CLIENT_PROVIDER.getKey());

      ExchangeBean eb = new ExchangeBean();
      eb.setIss(LoginbuddyConfig.getInstance().getDiscoveryUtil().getIssuer());
      eb.setIat(new Date().getTime() / 1000);
      eb.setAud(sessionCtx.getString(Constants.CLIENT_CLIENT_ID.getKey()));
      eb.setNonce(sessionCtx.getString(Constants.CLIENT_NONCE.getKey()));
      eb.setProvider(provider);

      String access_token = null;
      String id_token = null;

      ProviderConfig providerConfig = null;
      if (Constants.ISSUER_HANDLER_LOGINBUDDY.getKey().equalsIgnoreCase(sessionCtx.getString(Constants.ISSUER_HANDLER.getKey()))) {
        providerConfig = LoginbuddyConfig.getInstance().getConfigUtil().getProviderConfigByProvider(provider);
      } else {
        providerConfig = new ProviderConfig();
        // dynamically registered providers are in a separate container and not available here. Get details out of the session
        providerConfig.setClientId(sessionCtx.getString(Constants.PROVIDER_CLIENT_ID.getKey()));
        providerConfig.setClientSecret(sessionCtx.getString(Constants.PROVIDER_CLIENT_SECRET.getKey()));
        providerConfig.setRedirectUri(sessionCtx.getString(Constants.PROVIDER_REDIRECT_URI.getKey()));
        providerConfig.setIssuer(provider);
      }

// ***************************************************************
// ** Exchange the code for a token response
// ***************************************************************

      MsgResponse tokenResponse = HttpHelper.postTokenExchange(providerConfig.getClientId(), providerConfig.getClientSecret(), providerConfig.getRedirectUri(), codeResult.getValue(),
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
              MsgResponse jwks = HttpHelper.getAPI(sessionCtx.getString(Constants.JWKS_URI.getKey()));
              JSONObject idTokenPayload = new Jwt().validateJwt(id_token, jwks.getMsg(), providerConfig.getIssuer(),
                  providerConfig.getClientId(), sessionCtx.getString(Constants.CLIENT_NONCE.getKey()));
              eb.setIdTokenPayload(idTokenPayload);
            } catch (Exception e) {
              LOGGER.warning(String.format("No id_token was issued or it was invalid! Details: %s", e.getMessage()));
            }
          } else {
            response.getWriter()
                .write(HttpHelper.getErrorAsJson("invalid_response", String.format("the provider returned a response with an unsupported content-type: %s", tokenResponse.getContentType()))
                    .toJSONString());
            return;
          }
        } else {
          // need to handle error cases
          if (tokenResponse.getContentType().startsWith("application/json")) {
            JSONObject err = (JSONObject) new JSONParser().parse(tokenResponse.getMsg());
            response.getWriter().write(HttpHelper.getErrorAsJson((String) err.get("error"), (String) err.get("error_description")).toJSONString());
            return;
          }
        }
      } else {
        response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "the code exchange failed. An access_token could not be retrieved").toJSONString());
        return;
      }

// ***************************************************************
// ** Now, let's get the userinfo response
// ***************************************************************

      MsgResponse userinfoResp = HttpHelper.getAPI(access_token, sessionCtx.getString(Constants.USERINFO_ENDPOINT.getKey()));
        if (userinfoResp.getStatus() == 200) {
          if (userinfoResp.getContentType().startsWith("application/json")) {
            JSONObject userinfoRespObject = (JSONObject) new JSONParser().parse(userinfoResp.getMsg());
            eb.setUserinfo(userinfoRespObject);
            eb.setNormalized(HttpHelper.normalizeDetails(provider, providerConfig.getMappingsAsJson(), userinfoRespObject));
          }
      } // TODO : handle non 200 response

// ***************************************************************
// ** Issue our own authorization_code and add details for the final client response
// ***************************************************************

      response.setStatus(200);
      response.getWriter().write(eb.toString());

    } catch (Exception e) {
      LOGGER.warning("authorization request failed!");
      e.printStackTrace();
      response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "authorization request failed").toJSONString());
    }
  }
}