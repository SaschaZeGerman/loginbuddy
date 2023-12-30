/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.server;

import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.config.loginbuddy.Clients;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;

import java.io.IOException;

@WebServlet(name = "Authorize")
public class Authorize extends AuthorizationHandler {

    @Override
    protected void handleError(int httpStatus, String errorMsg, HttpServletResponse response) throws IOException {
        if(httpStatus > 308) {
            response.sendError(httpStatus, errorMsg);
        } else {
            response.sendRedirect(errorMsg);
        }
    }

    @Override
    protected ClientAuthenticator.ClientCredentialsResult handleClientValidation(ParameterValidatorResult clientIdResult, ParameterValidatorResult clientSecretResult, String authorizationHeader) {
        Clients cc = LoginbuddyUtil.UTIL.getClientConfigByClientId(clientIdResult.getValue());
        if (cc == null) {
            return new ClientAuthenticator.ClientCredentialsResult("An invalid client_id was provided!", false, null);
        }
        return new ClientAuthenticator.ClientCredentialsResult(null, true, cc);
    }

    @Override
    protected ClientAuthenticator.ClientCredentialsResult handleClientValidation(ParameterValidatorResult clientIdResult, ParameterValidatorResult clientSecretResult, String authorizationHeader, String signedResponseAlg, boolean acceptDynamicProvider) {
        return handleClientValidation(clientIdResult, clientSecretResult, authorizationHeader); // ignoring other parameters on purpose
    }
}