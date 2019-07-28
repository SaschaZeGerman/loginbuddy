/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.server;

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

@WebServlet(name = "Token")
public class Token extends Overlord {

  private static final Logger LOGGER = Logger.getLogger(String.valueOf(Token.class));

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    // Whatever happens, we'll return JSON
    response.setContentType("application/json");
    response.addHeader("Cache-Control", "no-store");
    response.addHeader("Pragma", "no-cache");
    JSONObject resp = new JSONObject();

    String code = request.getParameter("code");
    if (code == null || code.trim().length() == 0 || request.getParameterValues(Constants.CODE.getKey()).length > 1) {
      LOGGER.warning("The given code parameter is invalid or was provided multiple times");
      resp.put("error", "invalid_request");
      resp.put("error_description", "The given code parameter is invalid or was provided multiple times");
      response.setStatus(400);
      response.getWriter().write(resp.toJSONString());
      return;
    }

    String client_id = request.getParameter("client_id");
    if (client_id == null || client_id.trim().length() == 0
        || request.getParameterValues(Constants.CLIENT_ID.getKey()).length > 1) {
      LOGGER.warning("The given client_id parameter is invalid or was provided multiple times");
      resp.put("error", "invalid_request");
      resp.put("error_description", "The given client_id parameter is invalid or was provided multiple times");
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
    } else if (Stream.of(((String) oidcConfig.get("grant_types_supported")).split(" ")).noneMatch(grant_type::equals)) {
      LOGGER.warning(String.format("The given grant_type is not supported: '%s'", grant_type));
      resp.put("error", "invalid_request");
      resp.put("error_description", String.format("The given grant_type: '%s' is not supported", grant_type));
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
      String clientCodeChallenge = sessionCtx.getString(Constants.CLIENT_CODE_CHALLENGE.getKey());
      if (clientCodeChallenge != null) {
        String clientCodeVerifier = request.getParameter(Constants.CODE_VERIFIER.getKey());
        if (clientCodeVerifier != null && request.getParameterValues(Constants.CODE_VERIFIER.getKey()).length == 1) {
          if (!Pkce.validate(clientCodeChallenge, sessionCtx.getString(Constants.CLIENT_CODE_CHALLENGE_METHOD.getKey()),
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
    doPost(request, response);
  }
}