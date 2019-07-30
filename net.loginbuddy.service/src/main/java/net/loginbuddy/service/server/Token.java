/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.server;

import java.util.Base64;
import java.util.stream.Stream;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.Pkce;
import net.loginbuddy.service.util.SessionContext;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import sun.misc.BASE64Decoder;

@WebServlet(name = "Token")
public class Token extends Overlord {

  private static final Logger LOGGER = Logger.getLogger(String.valueOf(Token.class));

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    // Whatever happens, we'll return JSON
    //
    response.setContentType("application/json");
    response.addHeader("Cache-Control", "no-store");
    response.addHeader("Pragma", "no-cache");
    JSONObject resp = new JSONObject();

    // find the clientId. Either as POST parameter or in the Authorization header but not at both locations
    //
    String usedAuthMethod = Constants.CLIENT_SECRET_POST.getKey(); // our assumed default
    String clientId = request.getParameter("client_id");
    String authHeader = request.getHeader("Authorization");
    String clientCreds = null;

    if ((clientId == null && authHeader == null) || (clientId != null && authHeader != null) ) {
      LOGGER.warning("Missing or duplicate client credentials!");
      resp.put("error", "invalid_request");
      resp.put("error_description", "Missing or duplicate client credentials!");
      response.setStatus(400);
      response.getWriter().write(resp.toJSONString());
      return;
    } else if (clientId == null) {
      if (authHeader.split(" ").length == 2 && authHeader.split(" ")[0].equalsIgnoreCase("basic")) {
        clientCreds = new String(Base64.getDecoder().decode(authHeader.split(" ")[1]));
        clientId = clientCreds.split(":")[0];
        usedAuthMethod = Constants.CLIENT_SECRET_BASIC.getKey();
      } else {
        LOGGER.warning("Missing client credentials!");
        resp.put("error", "invalid_request");
        resp.put("error_description", "Missing client credentials!");
        response.setStatus(400);
        response.getWriter().write(resp.toJSONString());
        return;
      }
    }

    // Find the client configuration
    //
    if (loadClientConfig(clientId)) {

      // let's check supported methods (if any were configured. Otherwise we'll accept the one that was used)
      String supportedMethods = getTokenEndpointAuthMethodsSupported();
      if (supportedMethods == null) {
        supportedMethods = usedAuthMethod;
      }

      if (Stream.of((supportedMethods).split("[,; ]")).anyMatch(usedAuthMethod::equalsIgnoreCase)) {
        // Public clients cannot use a Basic authorization header. They miss the 'secret' portion of the string 'client_id:'
        //
        if (Constants.CLIENT_TYPE_PUBLIC.getKey().equals(clientType) && Constants.CLIENT_SECRET_BASIC.getKey()
            .equals(usedAuthMethod)) {
          LOGGER.warning("Unsupported authentication method for public clients was used!");
          resp.put("error", "invalid_request");
          resp.put("error_description", "Unsupported authentication method for public clients was used!");
          response.setStatus(400);
          response.getWriter().write(resp.toJSONString());
          return;
        } else if (Constants.CLIENT_TYPE_CONFIDENTIAL.getKey().equalsIgnoreCase(clientType)) {
          String clientSecret = Constants.CLIENT_SECRET_POST.getKey().equals(usedAuthMethod) ? request.getParameter(Constants.CLIENT_SECRET.getKey()) : clientCreds.split(":")[1];
          if (clientSecret == null || clientSecret.trim().length() == 0) {
            LOGGER.warning("Missing client_secret");
            resp.put("error", "invalid_request");
            resp.put("error_description", "Missing client_secret!");
            response.setStatus(400);
            response.getWriter().write(resp.toJSONString());
            return;
          } else if (!clientConfig.getClientSecret().equals(clientSecret)) {
            resp.put("error", "invalid_request");
            resp.put("error_description", "Invalid client credentials given");
            response.setStatus(400);
            response.getWriter().write(resp.toJSONString());
            return;
          }
        }
      } else {
        LOGGER.warning("Unsupported authentication method was used!");
        resp.put("error", "invalid_request");
        resp.put("error_description", "Unsupported authentication method was used!");
        response.setStatus(400);
        response.getWriter().write(resp.toJSONString());
        return;
      }
    } else {
      LOGGER.warning("Missing or duplicate client credentials!");
      resp.put("error", "invalid_request");
      resp.put("error_description", "Missing or duplicate client credentials!");
      response.setStatus(400);
      response.getWriter().write(resp.toJSONString());
      return;
    }

    String grant_type = request.getParameter("grant_type");
    if (grant_type == null || grant_type.trim().length() == 0
        || request.getParameterValues(Constants.GRANT_TYPE.getKey()).length > 1) {
      LOGGER.warning("The given grant_type parameter is invalid or was provided multiple times");
      resp.put("error", "invalid_request");
      resp.put("error_description", "The given grant_type parameter is invalid or was provided multiple times");
      response.setStatus(400);
      response.getWriter().write(resp.toJSONString());
      return;
    } else if (Stream.of((getGrantTypesSupported()).split("[,; ]"))
        .noneMatch(grant_type::equals)) {
      LOGGER.warning(String.format("The given grant_type is not supported: '%s'", grant_type));
      resp.put("error", "invalid_request");
      resp.put("error_description", String.format("The given grant_type is not supported: '%s'", grant_type));
      response.setStatus(400);
      response.getWriter().write(resp.toJSONString());
      return;
    }

    String code = request.getParameter("code");
    if (code == null || code.trim().length() == 0 || request.getParameterValues(Constants.CODE.getKey()).length > 1) {
      LOGGER.warning("The given code parameter is invalid or was provided multiple times");
      resp.put("error", "invalid_request");
      resp.put("error_description", "The given code parameter is invalid or was provided multiple times");
      response.setStatus(400);
      response.getWriter().write(resp.toJSONString());
      return;
    }

    SessionContext sessionCtx = (SessionContext) LoginbuddyCache.getInstance().remove(code);
    if (sessionCtx == null) {
      LOGGER.warning("The given code is invalid or has expired");
      resp.put("error", "invalid_request");
      resp.put("error_description", "The given code is invalid or has expired");
      response.setStatus(400);
      response.getWriter().write(resp.toJSONString());
      return;
    } else {

      boolean checkRedirectUri = sessionCtx.get(Constants.CHECK_REDIRECT_URI.getKey(), Boolean.class);
      if(checkRedirectUri) {
        String redirectUri = request.getParameter(Constants.REDIRECT_URI.getKey());
        if(redirectUri == null || redirectUri.trim().length() == 0 || request.getParameterValues(Constants.REDIRECT_URI.getKey()).length > 2) {
          LOGGER.warning("Missing or duplicate redirect_uri!");
          resp.put("error", "invalid_request");
          resp.put("error_description", "Missing or duplicate redirect_uri!");
          response.setStatus(400);
          response.getWriter().write(resp.toJSONString());
          return;
        } else {
          if(!redirectUri.equals(sessionCtx.getString(Constants.CLIENT_REDIRECT.getKey()))) {
            LOGGER.warning("Invalid redirect_uri!");
            resp.put("error", "invalid_request");
            resp.put("error_description", "Invalid redirect_uri!");
            response.setStatus(400);
            response.getWriter().write(resp.toJSONString());
            return;
          }
        }
      }

      String clientCodeChallenge = sessionCtx.getString(Constants.CLIENT_CODE_CHALLENGE.getKey());
      if (clientCodeChallenge != null) {
        String clientCodeVerifier = request.getParameter(Constants.CODE_VERIFIER.getKey());
        if (clientCodeVerifier != null && request.getParameterValues(Constants.CODE_VERIFIER.getKey()).length == 1) {
          if (!Pkce
              .validate(clientCodeChallenge, sessionCtx.getString(Constants.CLIENT_CODE_CHALLENGE_METHOD.getKey()),
                  clientCodeVerifier)) {
            LOGGER.warning("The code_verifier is invalid!");
            resp.put("error", "invalid_request");
            resp.put("error_description", "The code_verifier is invalid!");
            response.setStatus(400);
            response.getWriter().write(resp.toJSONString());
            return;
          }
        } else {
          LOGGER.warning("The code_verifier parameter is invalid!");
          resp.put("error", "invalid_request");
          resp.put("error_description", "The code_verifier parameter is invalid!");
          response.setStatus(400);
          response.getWriter().write(resp.toJSONString());
          return;
        }
      }
      response.getWriter().write(sessionCtx.getString("eb"));
    }
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setHeader("Allow", "POST");
    response.sendError(405, "Method not allowed");
  }
}