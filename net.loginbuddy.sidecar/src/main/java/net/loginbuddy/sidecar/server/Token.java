/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.sidecar.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.config.loginbuddy.common.GrantTypeHandler;
import net.loginbuddy.config.loginbuddy.common.RefreshTokenHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

@WebServlet(name = "Token")
public class Token extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(Token.class.getName());

    private Map<String, GrantTypeHandler> token_handler;

    @Override
    public void init() throws ServletException {
        super.init();
        token_handler = new HashMap<>();
        token_handler.put(Constants.GRANT_TYPE_REFRESH_TOKEN.getKey(), new RefreshTokenHandler());
        LOGGER.info(String.format("Registering handler for grant_type: %s\n", Constants.GRANT_TYPE_REFRESH_TOKEN.getKey()));
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

// ***************************************************************
// ** Whatever happens, we'll return JSON
// ***************************************************************

        response.setContentType("application/json");
        response.addHeader("Cache-Control", "no-store");
        response.addHeader("Pragma", "no-cache");
        response.setStatus(400);

        ParameterValidatorResult grantTypeResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.GRANT_TYPE.getKey()));

// ***************************************************************
// ** Process the grant_type
// ***************************************************************

        token_handler.get(grantTypeResult.getValue()).handleGrantType(request, response, Constants.SIDECAR_CLIENT_ID.getKey());
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Allow", "POST");
        response.sendError(405, "Method not allowed");
    }
}