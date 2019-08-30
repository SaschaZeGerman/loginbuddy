/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.server;

import java.io.IOException;
import java.util.Base64;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import net.loginbuddy.common.util.Pkce;
import net.loginbuddy.service.config.ClientConfig;
import net.loginbuddy.service.config.LoginbuddyConfig;
import net.loginbuddy.service.util.SessionContext;
import org.json.simple.JSONObject;

@WebServlet(name = "Token")
public class Token extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(String.valueOf(Token.class));

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    // Whatever happens, we'll return JSON
    //
    response.setContentType("application/json");
    response.addHeader("Cache-Control", "no-store");
    response.addHeader("Pragma", "no-cache");
    response.setStatus(400);

    // find the clientId. Either as POST parameter or in the Authorization header but not at both locations
    //

    ParameterValidatorResult clientIdResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.CLIENT_ID.getKey()));
    ParameterValidatorResult clientSecretResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.CLIENT_SECRET.getKey()));
    ParameterValidatorResult grantTypeResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.GRANT_TYPE.getKey()));
    ParameterValidatorResult codeResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.CODE.getKey()));
    ParameterValidatorResult redirectUriResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.REDIRECT_URI.getKey()));
    ParameterValidatorResult codeVerifierResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.CODE_VERIFIER.getKey()));

    String clientId = clientIdResult.getValue();
    String authHeader = request.getHeader("Authorization");
    String clientCreds = null;
    String usedAuthMethod = Constants.CLIENT_SECRET_POST.getKey(); // our assumed default

    if ((clientId == null && authHeader == null) || (clientId != null && authHeader != null)) {
      response.getWriter().write(createJsonErrorResponse("missing or duplicate client credentials"));
      return;
    } else if (clientId == null) {
      if (authHeader.split(" ").length == 2 && authHeader.split(" ")[0].equalsIgnoreCase("basic")) {
        clientCreds = new String(Base64.getDecoder().decode(authHeader.split(" ")[1]));
        clientId = clientCreds.split(":")[0];
        usedAuthMethod = Constants.CLIENT_SECRET_BASIC.getKey();
      } else {
        response.getWriter().write(createJsonErrorResponse("missing client credentials"));
        return;
      }
    }

    ClientConfig cc = LoginbuddyConfig.getInstance().getConfigUtil().getClientConfigByClientId(clientId);
    if (cc != null) {
      // let's check supported methods (if any were configured. Otherwise we'll accept the one that was used)
      String supportedMethods = LoginbuddyConfig.getInstance().getDiscoveryUtil()
          .getTokenEndpointAuthMethodsSupportedAsString();
      if (supportedMethods == null) {
        supportedMethods = usedAuthMethod;
      }

      if (Stream.of((supportedMethods).split("[,; ]")).anyMatch(usedAuthMethod::equalsIgnoreCase)) {
        // Public clients cannot use a Basic authorization header. They miss the 'secret' portion of the string 'client_id:'
        //
        if (Constants.CLIENT_TYPE_PUBLIC.getKey().equals(cc.getClientType()) && Constants.CLIENT_SECRET_BASIC.getKey()
            .equals(usedAuthMethod)) {
          response.getWriter().write(createJsonErrorResponse(
              "unsupported authentication method for public clients was used"));
          return;
        } else if (Constants.CLIENT_TYPE_CONFIDENTIAL.getKey().equalsIgnoreCase(cc.getClientType())) {
          String clientSecret = Constants.CLIENT_SECRET_POST.getKey().equals(usedAuthMethod) ? clientSecretResult.getValue()
              : clientCreds.split(":")[1];
          if (clientSecret == null || clientSecret.trim().length() == 0) {
            response.getWriter().write(createJsonErrorResponse("missing client_secret"));
            return;
          } else if (!cc.getClientSecret().equals(clientSecret)) {
            response.getWriter().write(createJsonErrorResponse("invalid client credentials given"));
            return;
          }
        }
      } else {
        response.getWriter().write(createJsonErrorResponse("unsupported authentication method was used"));
        return;
      }
    } else {
      response.getWriter().write(createJsonErrorResponse("invalid client credentials"));
      return;
    }

    if (!grantTypeResult.getResult().equals(RESULT.VALID)) {
      response.getWriter().write(createJsonErrorResponse(
          "the given grant_type parameter is invalid or was provided multiple times"));
      return;
    } else if (Stream.of((LoginbuddyConfig.getInstance().getDiscoveryUtil().getGrantTypesSupported()))
        .noneMatch(grantTypeResult.getValue()::equals)) {
      response.getWriter()
          .write(createJsonErrorResponse("the given grant_type is not supported", grantTypeResult.getValue()));
      return;
    }

    if (!codeResult.getResult().equals(RESULT.VALID)) {
      response.getWriter().write(createJsonErrorResponse(
          "the given code parameter is invalid or was provided multiple times"));
      return;
    }

    SessionContext sessionCtx = (SessionContext) LoginbuddyCache.getInstance().remove(codeResult.getValue());
    if (sessionCtx == null) {
      response.getWriter().write(createJsonErrorResponse("the given code is invalid or has expired"));
    } else {
      boolean checkRedirectUri = sessionCtx.get(Constants.CHECK_REDIRECT_URI.getKey(), Boolean.class);
      if (checkRedirectUri) {
        if (!redirectUriResult.getResult().equals(RESULT.VALID)) {
          response.getWriter().write(createJsonErrorResponse("missing or duplicate redirect_uri"));
          return;
        } else {
          if (!redirectUriResult.getValue().equals(sessionCtx.getString(Constants.CLIENT_REDIRECT.getKey()))) {
            response.getWriter().write(createJsonErrorResponse("invalid redirect_uri", redirectUriResult.getValue()));
            return;
          }
        }
      }

      String clientCodeChallenge = sessionCtx.getString(Constants.CLIENT_CODE_CHALLENGE.getKey());
      if (clientCodeChallenge != null) {
        if (codeVerifierResult.getResult().equals(RESULT.VALID)) {
          if (!Pkce.validate(clientCodeChallenge, sessionCtx.getString(Constants.CLIENT_CODE_CHALLENGE_METHOD.getKey()),
              codeVerifierResult.getValue())) {
            response.getWriter().write(createJsonErrorResponse("the code_verifier is invalid"));
            return;
          }
        } else {
          response.getWriter().write(createJsonErrorResponse("the code_verifier parameter is invalid"));
          return;
        }
      }
      response.setStatus(200);
      response.getWriter().write(sessionCtx.getString("eb"));
    }
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setHeader("Allow", "POST");
    response.sendError(405, "Method not allowed");
  }

  private String createJsonErrorResponse(String value) {
    return createJsonErrorResponse(value, "");
  }

  private String createJsonErrorResponse(String value, String toLogger) {
    if (toLogger != null && toLogger.trim().length() > 0) {
      LOGGER.warning(value.concat(": ").concat(toLogger));
    } else {
      LOGGER.warning(value);
    }

    JSONObject o = new JSONObject();
    o.put("error", "invalid_request");
    o.put("error_description", value);
    return o.toJSONString();
  }
}