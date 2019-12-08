/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.demoserver.provider;

import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class LoginbuddyProviderUserinfo extends LoginbuddyProviderCommon {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(LoginbuddyProviderUserinfo.class));

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Whatever happens, we'll return JSON
        response.setContentType("application/json");
        JSONObject fakeUserinfoResponse = new JSONObject();

        // do we have an error?
        boolean error = false;

        // TODO: Handle access_token validation including RFC specific error responses. Well, this is all fake, so ... do not worry about it now
        String access_token = request.getParameter(Constants.ACCESS_TOKEN.getKey());
        if (access_token == null || access_token.trim().length() == 0) {
            // not found, let's check the authorization header
            String authHeader = request.getHeader(Constants.AUTHORIZATION.getKey());
            if (authHeader != null && Stream.of(authHeader.split(" ")).anyMatch("bearer"::equalsIgnoreCase)) {
                access_token = authHeader.split(" ")[1];
            } else {
                LOGGER.warning("the access_token is missing");
                fakeUserinfoResponse.put("error", "invalid_request");
                fakeUserinfoResponse.put("error_description", "the access_token is missing");
                error = true;
            }
        }

        SessionContext sessionValues = null;
        if (!error) {
            // Let's see if we know this access_token
            sessionValues = (SessionContext) LoginbuddyCache.getInstance().get(access_token);
            if (sessionValues == null || !access_token.equals(sessionValues.get(Constants.ACCESS_TOKEN.getKey()))) {
                LOGGER.warning("the access_token is invalid");
                fakeUserinfoResponse.put("error", "invalid_request");
                fakeUserinfoResponse.put("error_description", "the access_token is invalid");
                error = true;
            }
        }

        if (!error) {
            // Check if the access_token has not expired yet
            long expiration = sessionValues.getLong("access_token_expiration");
            if (new Date().getTime() > expiration) {
                LOGGER.warning("the given access_token has expired");
                fakeUserinfoResponse.put("error", "invalid_request");
                fakeUserinfoResponse.put("error_description", "the given access_token has expired");
                error = true;
        }}

        String scope = "";
        if (!error) {
            // Check for at least scope 'openid'
            scope = sessionValues.getString(Constants.SCOPE.getKey());
            if (Stream.of(scope.split(" ")).noneMatch("openid"::equals)) {
                LOGGER.warning("The given access_token has not been granted to access this API");
                fakeUserinfoResponse.put("error", "invalid_request");
                fakeUserinfoResponse.put("error_description", "The given access_token has not been granted to access this API");
                error = true;
            }
        }

        if (!error) {

            // Let's build the response message depending on scope values other than 'openid'

            String clientId = sessionValues.getString(Constants.CLIENT_ID.getKey());
            String email = sessionValues.getString("email");
            String sub = getSub(clientId, email, false);

            fakeUserinfoResponse = new JSONObject();
            fakeUserinfoResponse.put("sub", sub);

            // add 'email' if it was requested in scope
            if (Stream.of(scope.split(" ")).anyMatch("email"::equals)) {
                fakeUserinfoResponse.put("email", email);
                fakeUserinfoResponse.put("email_verified", true);
            }

            // add 'profile' if it was requested in scope
            if (Stream.of(scope.split(" ")).anyMatch("profile"::equals)) {
                fakeUserinfoResponse.put("name", "Login Buddy");
                fakeUserinfoResponse.put("given_name", "Login");
                fakeUserinfoResponse.put("family_name", "Buddy");
                fakeUserinfoResponse.put("preferred_username", email);
            }
        }

        int status = 200;
        if(error) {
            status = 401;
        }
        response.setStatus(status);
        response.getWriter().println(fakeUserinfoResponse.toJSONString());
    }
}