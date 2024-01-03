/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.loginbuddy.common.GrantTypeHandler;
import net.loginbuddy.config.loginbuddy.common.RefreshTokenHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Token extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(Token.class.getName());

    private Map<String, GrantTypeHandler> token_handler;

    @Override
    public void init() throws ServletException {
        super.init();
        token_handler = getGrantTypeHandlers();
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

// ***************************************************************
// ** Whatever happens, we'll return JSON
// ***************************************************************

        response.setContentType("application/json");
        response.addHeader("Cache-Control", "no-store");
        response.addHeader("Pragma", "no-cache");
        response.setStatus(400);

        ClientAuthenticator.ClientCredentialsResult clientCredentialsResult = authenticateClient(request, response);
        if (clientCredentialsResult.isValid()) {

// ***************************************************************
// ** Check for grant_type parameter and if the given one is supported
// ***************************************************************

            ParameterValidatorResult grantTypeResult = ParameterValidator.getSingleValue(request.getParameterValues(Constants.GRANT_TYPE.getKey()));
            if (!grantTypeResult.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
                response.getWriter().write(HttpHelper.createJsonErrorResponse("the given grant_type parameter is invalid or was provided multiple times"));
                return;
            } else if (token_handler.keySet().stream().noneMatch(grantTypeResult.getValue()::equals)) {
                response.getWriter().write(HttpHelper.createJsonErrorResponse("the given grant_type is not supported", grantTypeResult.getValue()));
                return;
            }
            token_handler.get(grantTypeResult.getValue()).handleGrantType(request, response, clientCredentialsResult.getClients().getClientId());
        } else {
            response.getWriter().write(HttpHelper.createJsonErrorResponse(clientCredentialsResult.getErrorMsg()));
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Allow", "POST");
        response.sendError(405, "Method not allowed");
    }

    protected ClientAuthenticator.ClientCredentialsResult authenticateClient(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ParameterValidatorResult clientIdResult = ParameterValidator.getSingleValue(request.getParameterValues(Constants.CLIENT_ID.getKey()));
        ParameterValidatorResult clientSecretResult = ParameterValidator.getSingleValue(request.getParameterValues(Constants.CLIENT_SECRET.getKey()));
        return ClientAuthenticator.validateClientCredentials(clientIdResult, clientSecretResult, request.getHeader(Constants.AUTHORIZATION.getKey()));
    }

    protected Map<String, GrantTypeHandler> getGrantTypeHandlers() {
        Map<String, GrantTypeHandler> tokenHandler = new HashMap<>();
        for (String grantType : DiscoveryUtil.UTIL.getGrantTypesSupported()) {
            if (Constants.GRANT_TYPE_AUTHORIZATION_CODE.getKey().equalsIgnoreCase(grantType)) {
                tokenHandler.put(grantType, new AuthorizationCodeHandler());
                LOGGER.info(String.format("Registering handler for grant_type: %s\n", grantType));
            } else if (Constants.GRANT_TYPE_REFRESH_TOKEN.getKey().equalsIgnoreCase(grantType)) {
                tokenHandler.put(grantType, new RefreshTokenHandler());
                LOGGER.info(String.format("Registering handler for grant_type: %s\n", grantType));
            }
        }
        return tokenHandler;
    }
}