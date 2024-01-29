/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.service.server.handler.GrantTypeHandler;
import net.loginbuddy.service.server.handler.RefreshTokenHandler;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class TokenSidecar extends Token  {

    private static final Logger LOGGER = Logger.getLogger(TokenSidecar.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        try {
            Sidecar.checkClientConnection(request);
        } catch (IllegalAccessException e) {
            LOGGER.warning(e.getMessage());
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write(e.getMessage());
            return;
        }
        super.doPost(request, response);
    }

    @Override
    protected Map<String, GrantTypeHandler> getGrantTypeHandlers() {
        Map<String, GrantTypeHandler> tokenHandler = new HashMap<>();
        tokenHandler.put(Constants.GRANT_TYPE_REFRESH_TOKEN.getKey(), new RefreshTokenHandler());
        LOGGER.info(String.format("Registering handler for grant_type: %s\n", Constants.GRANT_TYPE_REFRESH_TOKEN.getKey()));
        return tokenHandler;
    }

    @Override
    protected ClientAuthenticator.ClientCredentialsResult authenticateClient(HttpServletRequest request, HttpServletResponse response) throws IOException {
        ParameterValidatorResult clientIdResult = ParameterValidator.getSingleValue(new String[]{Constants.SIDECAR_CLIENT_ID.getKey()});
        return Sidecar.getClientForToken(clientIdResult);
    }
}