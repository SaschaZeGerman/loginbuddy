/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import net.loginbuddy.config.discovery.DiscoveryUtil;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

@WebServlet(name = "Token")
public class Token extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(Token.class.getName());

    private Map<String, GrantTypeHandler> token_handler;

    @Override
    public void init() throws ServletException {
        super.init();
        token_handler = new HashMap<>();
        for (String grantType : DiscoveryUtil.UTIL.getGrantTypesSupported()) {
            if (Constants.GRANT_TYPE_AUTHORIZATION_CODE.getKey().equalsIgnoreCase(grantType)) {
                token_handler.put(grantType, new GrantTypeAuthorizationCode());
                LOGGER.info(String.format("Registering handler for grant_type: %s\n", grantType));
            }
        }
    }

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

// ***************************************************************
// ** 1. Find the clientId. Either as POST parameter or in the Authorization header but not at both locations.
// ** 2. Lookup the client registration details to verify given credentials
// ***************************************************************

        String errorMsg;
        if ((errorMsg = ClientAuthenticator.validateClientCredentials(clientIdResult, clientSecretResult, request.getHeader(Constants.AUTHORIZATION.getKey())).getErrorMsg()) != null) {
            response.getWriter().write(HttpHelper.createJsonErrorResponse(errorMsg));
            return;
        }

// ***************************************************************
// ** Check for grant_type parameter and if the given one is supported
// ***************************************************************

        if (!grantTypeResult.getResult().equals(RESULT.VALID)) {
            response.getWriter().write(HttpHelper.createJsonErrorResponse("the given grant_type parameter is invalid or was provided multiple times"));
            return;
        } else if (Stream.of((DiscoveryUtil.UTIL.getGrantTypesSupported())).noneMatch(grantTypeResult.getValue()::equals)) {
            response.getWriter().write(HttpHelper.createJsonErrorResponse("the given grant_type is not supported", grantTypeResult.getValue()));
            return;
        }

// ***************************************************************
// ** Process the grant_type
// ***************************************************************

        token_handler.get(grantTypeResult.getValue()).handleGrantType(request, response);

    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Allow", "POST");
        response.sendError(405, "Method not allowed");
    }
}