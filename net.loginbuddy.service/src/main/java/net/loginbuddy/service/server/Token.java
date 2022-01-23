/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.server;

import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import net.loginbuddy.common.util.Pkce;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.service.util.SessionContext;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.stream.Stream;

@WebServlet(name = "Token")
public class Token extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(Token.class));

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

// ***************************************************************
// ** Whatever happens, we'll return JSON
// ***************************************************************

        response.setContentType("application/json");
        response.addHeader("Cache-Control", "no-store");
        response.addHeader("Pragma", "no-cache");
        response.setStatus(400);

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

// ***************************************************************
// ** 1. Find the clientId. Either as POST parameter or in the Authorization header but not at both locations.
// ** 2. Lookup the client registration details to verify given credentials
// ***************************************************************

        String errorMsg;
        if ((errorMsg = ClientAuthenticator.validateClientCredentials(clientIdResult, clientSecretResult, request.getHeader(Constants.AUTHORIZATION.getKey())).getErrorMsg()) != null) {
            response.getWriter().write(Overlord.createJsonErrorResponse(errorMsg));
            return;
        }

// ***************************************************************
// ** Check for grant_type parameter and if the given one is supported
// ***************************************************************

        if (!grantTypeResult.getResult().equals(RESULT.VALID)) {
            response.getWriter().write(Overlord.createJsonErrorResponse("the given grant_type parameter is invalid or was provided multiple times"));
            return;
        } else if (Stream.of((DiscoveryUtil.UTIL.getGrantTypesSupported())).noneMatch(grantTypeResult.getValue()::equals)) {
            response.getWriter().write(Overlord.createJsonErrorResponse("the given grant_type is not supported", grantTypeResult.getValue()));
            return;
        }

// ***************************************************************
// ** Check for a valid code parameter
// ***************************************************************

        if (!codeResult.getResult().equals(RESULT.VALID)) {
            response.getWriter().write(Overlord.createJsonErrorResponse(
                    "the given code parameter is invalid or was provided multiple times"));
            return;
        }

// ***************************************************************
// ** Check for the current session and remove it. An authorization code can be used only once!
// ***************************************************************

        SessionContext sessionCtx = (SessionContext) LoginbuddyCache.CACHE.remove(codeResult.getValue());
        if (sessionCtx == null) {
            response.getWriter().write(Overlord.createJsonErrorResponse("the given code is invalid or has expired"));
        } else {
            boolean checkRedirectUri = sessionCtx.get(Constants.CHECK_REDIRECT_URI.getKey(), Boolean.class);
            if (checkRedirectUri) {
                if (!redirectUriResult.getResult().equals(RESULT.VALID)) {
                    response.getWriter().write(Overlord.createJsonErrorResponse("missing or duplicate redirect_uri"));
                    return;
                } else {
                    if (!redirectUriResult.getValue().equals(sessionCtx.getString(Constants.CLIENT_REDIRECT.getKey()))) {
                        response.getWriter().write(Overlord.createJsonErrorResponse("invalid redirect_uri", redirectUriResult.getValue()));
                        return;
                    }
                }
            }

// ***************************************************************
// ** If the client initially used PKCE, it now has to use PKCE also
// ***************************************************************

            String clientCodeChallenge = sessionCtx.getString(Constants.CLIENT_CODE_CHALLENGE.getKey());
            if (clientCodeChallenge != null) {
                if (codeVerifierResult.getResult().equals(RESULT.VALID)) {
                    if (!Pkce.validate(clientCodeChallenge, sessionCtx.getString(Constants.CLIENT_CODE_CHALLENGE_METHOD.getKey()),
                            codeVerifierResult.getValue())) {
                        response.getWriter().write(Overlord.createJsonErrorResponse("the code_verifier is invalid"));
                        return;
                    }
                } else {
                    response.getWriter().write(Overlord.createJsonErrorResponse("the code_verifier parameter is invalid"));
                    return;
                }
            }

// ***************************************************************
// ** Send the token response to the client
// ***************************************************************

            response.setStatus(200);
            String eb = sessionCtx.getString("eb");
            if (!(eb == null || eb.startsWith("{"))) {
                response.setContentType("application/jwt");
            }
            response.getWriter().write(sessionCtx.getString("eb"));
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Allow", "POST");
        response.sendError(405, "Method not allowed");
    }
}