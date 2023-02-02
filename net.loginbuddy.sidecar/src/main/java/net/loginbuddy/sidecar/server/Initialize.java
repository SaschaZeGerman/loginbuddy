/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.sidecar.server;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.config.HeadOfInitialize;
import net.loginbuddy.sidecar.util.SessionContext;

import java.io.IOException;
import java.util.logging.Logger;

/**
 * This class accepts connections only from localhost. The intention is easy access if loginbuddy is used as a sidecar deployment where it becomes part of the actual web application. This setup makes
 * it possible to run loginbuddy transparent to the outside world. At the same time, its external API are still available if necessary.
 */
public class Initialize extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(Initialize.class));

    // initiate authorization flow with provider
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            SidecarMaster.checkClientConnection(request);
        } catch (IllegalAccessException e) {
            LOGGER.warning(e.getMessage());
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write(e.getMessage());
            return;
        }

        ParameterValidatorResult providerResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.PROVIDER.getKey()), "");
        ParameterValidatorResult clientStateResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.STATE.getKey()), "");
        ParameterValidatorResult clientScopeResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.SCOPE.getKey()), Constants.OPENID_SCOPE.getKey());
        ParameterValidatorResult clientNonceResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.NONCE.getKey()));
        ParameterValidatorResult issuerResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.ISSUER.getKey()));
        ParameterValidatorResult discoveryUrlResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.DISCOVERY_URL.getKey()));

        // these three parameters are forwarded to the provider, not handled by loginbuddy
        ParameterValidatorResult clientPromptResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.PROMPT.getKey()), "");
        ParameterValidatorResult clientLoginHintResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.LOGIN_HINT.getKey()), "");
        ParameterValidatorResult clientIdTokenHintResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.ID_TOKEN_HINT.getKey()), "");

        // this would usually be a configuration. In this case the web app may provide the value on the fly
        ParameterValidatorResult clientAcceptDynamicProvider = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.ACCEPT_DYNAMIC_PROVIDER.getKey()), "false");

        // this would usually be a configuration. In this case the web app may provide the value on the fly
        ParameterValidatorResult signedResponseAlg = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.SIGNED_RESPONSE_ALG.getKey()), "");

        // if Loginbuddys response should not include 'real' access_token or refresh_token it will create fake ones. Useful for demo purposes that should not display the original values
        ParameterValidatorResult obfuscateTokenResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.OBFUSCATE_TOKEN.getKey()), "false");

// ***************************************************************
// ** Create the session so that it can be handled throughout multiple requests
// ***************************************************************

        SessionContext sessionCtx = new SessionContext();
        sessionCtx.setSessionInit(
                Constants.SIDECAR_CLIENT_ID.getKey(),
                clientScopeResult.getValue(),
                Constants.CODE.getKey(),
                clientNonceResult.getValue(),
                clientStateResult.getValue(),
                "".equals(providerResult.getValue()) ? issuerResult.getResult().equals(ParameterValidatorResult.RESULT.VALID) ? Constants.DYNAMIC_PROVIDER.getKey() : "" : providerResult.getValue(),
                clientPromptResult.getValue(),
                clientLoginHintResult.getValue(),
                clientIdTokenHintResult.getValue(),
                false,
                Constants.SIDECAR_REDIRECT_URI.getKey(),
                Boolean.parseBoolean(clientAcceptDynamicProvider.getValue().equals("true") ? clientAcceptDynamicProvider.getValue() : "false"),
                signedResponseAlg.getValue(),
                obfuscateTokenResult.getBooleanValue());

        // this will be the authorization_url or an error_url
        String authorizationUrl = HeadOfInitialize.processInitializeRequest(sessionCtx, providerResult, issuerResult, discoveryUrlResult);
        if (authorizationUrl.contains("?error")) {
            response.setStatus(400);
        } else {
            response.setStatus(201);
        }
        response.setContentType("text/plain");
        response.addHeader("Location", authorizationUrl);
    }
}