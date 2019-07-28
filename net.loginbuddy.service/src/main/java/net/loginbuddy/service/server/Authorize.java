/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Stream;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.Pkce;
import net.loginbuddy.service.config.ClientConfig;
import net.loginbuddy.service.config.LoginbuddyConfig;
import net.loginbuddy.service.util.SessionContext;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

@WebServlet(name = "Providers")
public class Authorize extends Overlord {

    private static Logger LOGGER = Logger.getLogger(String.valueOf(Authorize.class));

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String clientId = request.getParameter(Constants.CLIENT_ID.getKey());
        if (clientId == null || clientId.trim().length() == 0 || request.getParameterValues(Constants.CLIENT_ID.getKey()).length > 1) {
            LOGGER.warning("Missing or invalid client_id parameter!");
            response.sendError(400, "Missing or invalid client_id parameter!");
            return;
        }

        String clientRedirectUri = request.getParameter(Constants.REDIRECT_URI.getKey());
        if (clientRedirectUri == null || clientRedirectUri.trim().length() == 0 || request.getParameterValues(Constants.REDIRECT_URI.getKey()).length > 1) {
            LOGGER.warning("Missing or invalid redirect_uri parameter!");
            response.sendError(400, "Missing or invalid redirect_uri parameter!");
            return;
        }
        String clientRedirectUriError;
        if (clientRedirectUri.contains("?")) {
            clientRedirectUriError = clientRedirectUri.concat("&");

        } else {
            clientRedirectUriError = clientRedirectUri.concat("?");
        }
        try {
            ClientConfig clientConfig = LoginbuddyConfig.getInstance().getConfigUtil().getClientConfigByClientId(clientId);
            if (clientConfig == null || !clientConfig.getRedirectUri().matches(clientRedirectUri)) {
                LOGGER.warning("The given client_id or redirect_uri is unknown or invalid");
                response.sendError(400, "The given client_id or redirect_uri is unknown or invalid");
                return;
            }
        } catch (Exception e) {
            // should never occur
            LOGGER.severe("The system has not been configured yet!");
            response.sendError(500, "The system has not been configured yet!");
            return;
        }

        String clientState = request.getParameter(Constants.STATE.getKey());
        if (clientState == null || clientState.trim().length() == 0 || request.getParameterValues(Constants.STATE.getKey()).length > 1) {
            LOGGER.warning("Missing or invalid state parameter!");
            response.sendRedirect(clientRedirectUriError.concat("error=invalid_request&error_description=missing+or+invalid+state+parameter"));
            return;
        }

        String clientProvider = request.getParameter(Constants.PROVIDER.getKey());
        if (clientProvider != null && request.getParameterValues("provider").length > 1) {
            LOGGER.warning("Invalid provider parameter!");
            response.sendRedirect(clientRedirectUriError.concat("state=").concat(clientState).concat("&error=invalid_request&error_description=invalid+provider+parameter"));
            return;
        }
        if (clientProvider == null || clientProvider.trim().length() == 0) {
            clientProvider = "";
        }

        String clientCodeChallenge = request.getParameter(Constants.CODE_CHALLENGE.getKey());
        if (clientCodeChallenge != null && (request.getParameterValues(Constants.CODE_CHALLENGE.getKey()).length > 1 || !Pkce.verifyChallenge(clientCodeChallenge))) {
            LOGGER.warning("Invalid code_challenge!");
            response.sendRedirect(clientRedirectUriError.concat("state=").concat(clientState).concat("&error=invalid_request&error_description=invalid+code_challenge"));
            return;
        }

        String clientCodeChallengeMethod = request.getParameter(Constants.CODE_CHALLENGE_METHOD.getKey());
        if ( (clientCodeChallengeMethod != null && request.getParameterValues(Constants.CODE_CHALLENGE_METHOD.getKey()).length > 1) || Pkce.CODE_CHALLENGE_METHOD_PLAIN.equals(clientCodeChallengeMethod)) {
            LOGGER.warning("Invalid or unsupported code_challenge_method parameter or value!");
            response.sendRedirect(clientRedirectUriError.concat("state=").concat(clientState).concat("&error=invalid_request&error_description=invalid+or+unsupported+code_challenge_method+parameter+or+value"));
            return;
        }

        String clientResponseType = request.getParameter(Constants.RESPONSE_TYPE.getKey());
        if (clientResponseType == null || clientResponseType.trim().length() == 0
            || request.getParameterValues(Constants.RESPONSE_TYPE.getKey()).length > 1) {
            LOGGER.warning("The given response_type parameter is invalid or was provided multiple times");
            response.sendRedirect(clientRedirectUriError.concat("state=").concat(clientState).concat("&error=invalid_request&error_description=invalid+or+unsupported+response_type+parameter+or+value"));
            return;
        } else if (Stream.of(((String) oidcConfig.get("response_types_supported")).split(" ")).noneMatch(clientResponseType::equals)) {
            LOGGER.warning(String.format("The given response_type is not supported: '%s'", clientResponseType));
            response.sendRedirect(clientRedirectUriError.concat("state=").concat(clientState).concat(String.format("&error=invalid_request&error_description=unsupported+response_type:+%s", URLEncoder.encode(clientResponseType, "UTF-8"))));
            return;
        }

        String clientScope = request.getParameter(Constants.SCOPE.getKey());
        String clientNonce = request.getParameter(Constants.NONCE.getKey());
        String clientPrompt = request.getParameter(Constants.PROMPT.getKey());
        String clientLoginHint = request.getParameter(Constants.LOGIN_HINT.getKey());
        String clientIdTokenHint = request.getParameter(Constants.ID_TOKEN_HINT.getKey());

        SessionContext sessionCtx = new SessionContext();
        sessionCtx.sessionInit(clientId, clientScope, clientResponseType, clientCodeChallenge, clientCodeChallengeMethod, clientRedirectUri, clientNonce, clientState, clientProvider, clientPrompt, clientLoginHint, clientIdTokenHint);

        LoginbuddyCache.getInstance().put(sessionCtx.getId(), sessionCtx);

        if ("".equals(clientProvider)) {
            request.getRequestDispatcher("/iapis/providers.jsp?session=".concat(sessionCtx.getId())).forward(request, response);
        } else {
            response.sendRedirect("initialize?session=".concat(sessionCtx.getId()).concat("&provider=").concat(URLEncoder.encode(clientProvider, "UTF-8")));
        }
    }
}