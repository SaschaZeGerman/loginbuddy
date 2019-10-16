/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.server;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import net.loginbuddy.common.util.Pkce;
import net.loginbuddy.common.util.Sanetizer;
import net.loginbuddy.service.config.ClientConfig;
import net.loginbuddy.service.config.LoginbuddyConfig;
import net.loginbuddy.service.util.SessionContext;

@WebServlet(name = "Authorize")
public class Authorize extends HttpServlet {

  private static Logger LOGGER = Logger.getLogger(String.valueOf(Authorize.class));

  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    ParameterValidatorResult clientIdResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.CLIENT_ID.getKey()));
    ParameterValidatorResult clientResponseTypeResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.RESPONSE_TYPE.getKey()));
    ParameterValidatorResult clientRedirectUriResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.REDIRECT_URI.getKey()));
    ParameterValidatorResult clientProviderResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.PROVIDER.getKey()), "");
    ParameterValidatorResult clientStateResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.STATE.getKey()), "");
    ParameterValidatorResult clientCodeChallengeResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.CODE_CHALLENGE.getKey()));
    ParameterValidatorResult clientCodeChallendeMethodResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.CODE_CHALLENGE_METHOD.getKey()));
    ParameterValidatorResult scopeResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.SCOPE.getKey()));
    ParameterValidatorResult clientNonceResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.NONCE.getKey()));

    // these three parameters are forwarded to the provider, not handled by loginbuddy
    ParameterValidatorResult clientPromptResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.PROMPT.getKey()), "");
    ParameterValidatorResult clientLoginHintResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.LOGIN_HINT.getKey()), "");
    ParameterValidatorResult clientIdTokenHintResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.ID_TOKEN_HINT.getKey()), "");

// ***************************************************************
// ** Let's start with checking for a valid client_id
// ***************************************************************

    if (!clientIdResult.getResult().equals(RESULT.VALID)) {
      LOGGER.warning("Missing or invalid or multiple client_id parameters given!");
      response.sendError(400, "Missing or invalid or multiple client_id parameters given!");
      return;
    }

    ClientConfig cc = LoginbuddyConfig.getInstance().getConfigUtil()
        .getClientConfigByClientId(clientIdResult.getValue());
    if (cc == null) {
      LOGGER.warning("An invalid client_id was provided");
      response.sendError(400, "An invalid client_id was provided!");
      return;
    }

// ***************************************************************
// ** Check the given redirect_uri. Confidential clients only need to have one registered but not passed in
// ***************************************************************

    if (clientRedirectUriResult.getResult().equals(RESULT.MULTIPLE)) {
      LOGGER.warning("Too many redirect_uri parameters given!");
      response.sendError(400, "Too many redirect_uri parameters given!");
      return;
    }

    boolean checkRedirectUri = true;
    String clientRedirectUri = clientRedirectUriResult.getValue();
    if (clientRedirectUri == null) {
      if (Constants.CLIENT_TYPE_PUBLIC.getKey().equals(cc.getClientType())) {
        LOGGER.warning("Missing redirect_uri parameter!");
        response.sendError(400, "Missing redirect_uri parameter!");
        return;
      } else if (cc.getRedirectUri().split("[,; ]").length != 1) {
        LOGGER.warning("Missing redirect_uri parameter!");
        response.sendError(400, "Missing redirect_uri parameter!");
        return;
      } else {
        // confidential clients only need a registered redirectUri and not need to request it UNLESS multiple ones were registered
        clientRedirectUri = cc.getRedirectUri();
        checkRedirectUri = false; // it was not given, so no need to check for it at the token endpoint
      }
    }
    if (Stream.of(cc.getRedirectUri().split("[,; ]")).noneMatch(clientRedirectUri::equals)) {
      LOGGER.warning(String.format("Invalid redirect_uri: %s", clientRedirectUri));
      response.sendError(400, String.format("Invalid redirect_uri: %s", Sanetizer.sanetizeUrl(clientRedirectUri, 256)));
      return;
    }

// ***************************************************************
// ** As of here we can return errors to the clients redirect_uri
// ***************************************************************

// ***************************************************************
// ** Build the rdirect_uri for success and error cases including the optional state parameter
// ***************************************************************

    String clientRedirectUriValid;
    if (clientRedirectUri.contains("?")) {
      clientRedirectUriValid = clientRedirectUri.concat("&");
    } else {
      clientRedirectUriValid = clientRedirectUri.concat("?");
    }

    if (clientStateResult.getResult().equals(RESULT.MULTIPLE)) {
      LOGGER.warning("Multiple state parameters received!");
      response.sendRedirect(HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request", "multiple state parameters received"));
      return;
    }

    clientRedirectUriValid = "".equals(clientStateResult.getValue()) ? clientRedirectUriValid
        : clientRedirectUriValid.concat("state=").concat(clientStateResult.getValue()).concat("&");

// ***************************************************************
// ** Check the given response type
// ***************************************************************

    if (!clientResponseTypeResult.getResult().equals(RESULT.VALID)) {
      LOGGER.warning("The given response_type parameter is invalid or was provided multiple times");
      response.sendRedirect(HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request",
          "invalid or unsupported response_type parameter or value"));
      return;
    } else if (Stream.of((LoginbuddyConfig.getInstance().getDiscoveryUtil().getResponseTypesSupported()))
        .noneMatch(clientResponseTypeResult.getValue()::equals)) {
      LOGGER.warning(
          String.format("The given response_type is not supported: %s", clientResponseTypeResult.getValue()));
      response.sendRedirect(
          HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request", String.format("unsupported response_type: %s", Sanetizer.sanetize(clientResponseTypeResult.getValue()))));
      return;
    }

    if (clientProviderResult.getResult().equals(RESULT.MULTIPLE)) {
      LOGGER.warning("Multiple provider parameter!");
      response
          .sendRedirect(HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request", "multiple provider parameter"));
      return;
    }

// ***************************************************************
// ** PKCE: if it was used it has to be used at the /token endpoint. Remember if it was used
// ***************************************************************

    if (clientCodeChallengeResult.getResult().equals(RESULT.MULTIPLE)) {
      LOGGER.warning("Multiple code_challenge parameters found!");
      response.sendRedirect(
          HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request", "multiple code_challenge parameters found"));
      return;
    }
    if (clientCodeChallengeResult.getResult().equals(RESULT.VALID) && !Pkce
        .verifyChallenge(clientCodeChallengeResult.getValue())) {
      LOGGER.warning("Invalid code_challenge!");
      response.sendRedirect(HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request", "invalid code_challenge"));
      return;
    }

    if (clientCodeChallendeMethodResult.getResult().equals(RESULT.MULTIPLE)) {
      LOGGER.warning("Multiple code_challenge_method parameters found!");
      response.sendRedirect(HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request",
          "multiple code_challenge_method parameters found!"));
      return;
    }

    if (clientCodeChallendeMethodResult.getResult().equals(RESULT.VALID) && !Pkce.CODE_CHALLENGE_METHOD_S256
        .equals(clientCodeChallendeMethodResult.getValue())) {
      LOGGER.warning("Unsupported code_challenge_method parameter!");
      response.sendRedirect(HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request",
          "unsupported code_challenge_method parameter"));
      return;
    }

// ***************************************************************
// ** Check the given scope
// ***************************************************************

    // TODO somehow tie incoming and outgoing SCOPE values together. 'Initialize' uses SCOPEs independent of these ones
    String clientScope = scopeResult.getValue();
    if (clientScope == null) {
      if (Constants.CLIENT_TYPE_CONFIDENTIAL.getKey().equals(cc.getClientType())) {
        clientScope = LoginbuddyConfig.getInstance().getDiscoveryUtil().getScopesSupportedAsString();
      } else {
        LOGGER.warning("Invalid or unsupported scope parameter!");
        response.sendRedirect(HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request", "invalid or unsupported scope parameter"));
        return;
      }
    }

    Set<String> scopes = new TreeSet<>(
        Arrays.asList((LoginbuddyConfig.getInstance().getDiscoveryUtil().getScopesSupportedAsString()).split("[,; ]")));
    scopes.retainAll(Arrays.asList(clientScope.split("[,; ]")));
    if (scopes.size() == 0) {
      LOGGER.warning("Invalid or unsupported scope!");
      response.sendRedirect(HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request", "invalid or unsupported scope value"));
      return;
    }

// ***************************************************************
// ** Create the session so that it can be handled through out multiple requests
// ***************************************************************

    SessionContext sessionCtx = new SessionContext();
    sessionCtx.setSessionInit(clientIdResult.getValue(), clientScope, clientResponseTypeResult.getValue(),
        clientCodeChallengeResult.getValue(), clientCodeChallendeMethodResult.getValue(),
        clientRedirectUri, clientNonceResult.getValue(), clientStateResult.getValue(), clientProviderResult.getValue(),
        clientPromptResult.getValue(), clientLoginHintResult.getValue(), clientIdTokenHintResult.getValue(),
        checkRedirectUri, clientRedirectUriValid, cc.isAcceptDynamicProvider());

    LoginbuddyCache.getInstance().put(sessionCtx.getId(), sessionCtx, LoginbuddyConfig.getInstance().getPropertiesUtil().getLongProperty("lifetime.oauth.authcode.loginbuddy.flow"));

// ***************************************************************
// ** Present the provider selection page if non was given in this request. Otherwise, fast forward
// ***************************************************************

    if ("".equals(clientProviderResult.getValue())) {
      request.getRequestDispatcher(String.format("/iapis/providers.jsp?session=%s", sessionCtx.getId()))
          .forward(request, response);
    } else {
      String hostname = LoginbuddyConfig.getInstance().getDiscoveryUtil().getIssuer();
      response.sendRedirect(String.format("%s/initialize?session=%s", hostname, (sessionCtx.getId())));
    }
  }
}