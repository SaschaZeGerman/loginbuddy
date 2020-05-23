/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.client;

import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import net.loginbuddy.config.HeadOfInitialize;
import net.loginbuddy.service.util.SessionContext;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

import static net.loginbuddy.common.api.HttpHelper.getErrorForRedirect;

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
    ParameterValidatorResult issuerResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.ISSUER.getKey()));
    ParameterValidatorResult discoveryUrlResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.DISCOVERY_URL.getKey()));
    ParameterValidatorResult providerAddition = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.PROVIDER_ADDITION.getKey()));

// ***************************************************************
// ** Check for the current session
// ***************************************************************

    if (!sessionIdResult.getResult().equals(RESULT.VALID)) {
      LOGGER.warning("Missing session, cannot initiate the authorization flow!");
      response.sendError(400, "Missing session, cannot initiate the authorization flow!");
      return;
    }

    if (providerAddition.getResult().equals(RESULT.VALID)) {
      LOGGER.warning(String.format("Invalid request! Unused field had values: '%s'", providerAddition));
      response.sendError(400, "Invalid request, please try again!");
      return;
    }

    SessionContext sessionCtx = (SessionContext) LoginbuddyCache.CACHE.get(sessionIdResult.getValue());
    if (sessionCtx == null || !sessionIdResult.getValue().equals(sessionCtx.getId())) {
      LOGGER.warning("The current session is invalid or it has expired!");
      response.sendError(400, "The current session is invalid or it has expired!");
      return;
    }

// ***************************************************************
// ** Check if we expected this call
// ***************************************************************

    if (!Constants.ACTION_INITIALIZE.getKey().equals(sessionCtx.getString(Constants.ACTION_EXPECTED.getKey()))) {
      LOGGER.warning("The current action was not expected! Given: '" + sessionCtx.getString(Constants.ACTION_EXPECTED.getKey())
          + "', expected: '" + Constants.ACTION_INITIALIZE.getKey() + "'");
      response.sendRedirect(getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "invalid_session",
          "the request was not expected"));
      return;
    }

    // this will be the authorization_url or an error_url
    response.sendRedirect(HeadOfInitialize.processInitializeRequest(sessionCtx, providerResult, issuerResult, discoveryUrlResult));
  }
}