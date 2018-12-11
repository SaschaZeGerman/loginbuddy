/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.provider;

import net.loginbuddy.cache.LoginbuddyCache;
import net.loginbuddy.config.Constants;
import net.loginbuddy.oauth.server.Token;
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
import java.util.Map;
import java.util.logging.Logger;
import java.util.stream.Stream;

public class LoginbuddyProviderUserinfo extends HttpServlet {

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

        Map<String, Object> sessionValues = null;
        if (!error) {
            // Let's see if we know this access_token
            sessionValues = (Map<String, Object>) LoginbuddyCache.getInstance().get(access_token);
            if (sessionValues == null || !access_token.equals(sessionValues.get(Constants.ACCESS_TOKEN.getKey()))) {
                LOGGER.warning("the access_token is invalid");
                fakeUserinfoResponse.put("error", "invalid_request");
                fakeUserinfoResponse.put("error_description", "the access_token is invalid");
                error = true;
            }
        }

        if (!error) {
            // Check if the access_token has not expired yet
            long expiration = Long.valueOf((String) sessionValues.get("access_token_expiration"));
            if (new Date().getTime() > expiration) {
                LOGGER.warning("the given access_token has expired");
                fakeUserinfoResponse.put("error", "invalid_request");
                fakeUserinfoResponse.put("error_description", "the given access_token has expired");
                error = true;
        }}

        String scope = "";
        if (!error) {
            // Check for at least scope 'util'
            scope = (String) sessionValues.get(Constants.SCOPE.getKey());
            if (Stream.of(scope.split(" ")).noneMatch("openid"::equals)) {
                LOGGER.warning("The given access_token has not been granted to access this API");
                fakeUserinfoResponse.put("error", "invalid_request");
                fakeUserinfoResponse.put("error_description", "The given access_token has not been granted to access this API");
                error = true;
            }
        }

        if (!error) {

            // Let's build the response message depending on scope values other than 'util'

            String clientId = (String) sessionValues.get(Constants.CLIENT_ID.getKey());
            String email = (String) sessionValues.get("email");

            // Create a fake PPID to be used with 'sub'
            String ppidSub = "fakeProviderSalt".concat(clientId).concat(email);
            MessageDigest md = null;
            try {
                md = MessageDigest.getInstance("SHA-1");
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            String sub = new String(Base64.getUrlEncoder().encode(md.digest(ppidSub.getBytes()))).replace("=", "").replace("-", "");

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