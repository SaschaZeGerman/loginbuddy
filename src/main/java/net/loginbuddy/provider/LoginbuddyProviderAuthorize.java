/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

/*
 * This class simulates and OAuth 2.0 suthorization endpoint. It is meant for demo purposes
 *
 */
package net.loginbuddy.provider;

import net.loginbuddy.cache.LoginbuddyCache;
import net.loginbuddy.config.Constants;
import net.loginbuddy.oauth.util.Pkce;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class LoginbuddyProviderAuthorize extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(LoginbuddyProviderAuthorize.class));

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {

        try {

            /*
             * All the parameters below need to be validated. But, since this is just for demo purposes, they are mostly used 'as is'.
             * In the future this could be turned into a 'real' /authorization endpoint.
             *
             */
            String nonce = request.getParameter(Constants.NONCE.getKey());
            if (nonce == null || nonce.trim().length() == 0) {
                throw new IllegalArgumentException("The nonce is invalid or missing");
            }

            String state = request.getParameter(Constants.STATE.getKey());
            if (state == null || state.trim().length() == 0) {
                throw new IllegalArgumentException("The state is invalid or missing");
            }

            // Currently only response_type=code is supported. Let's check for that to simulate a more realistic behaviour
            String response_type = request.getParameter(Constants.RESPONSE_TYPE.getKey());
            if (!"code".equals(response_type)) {
                throw new IllegalArgumentException("The given response_type is not supported");
            }

            // TODO: Validate if this is a valid client_id
            String clientId = request.getParameter(Constants.CLIENT_ID.getKey());
            if (clientId == null || clientId.trim().length() == 0) {
                throw new IllegalArgumentException("The client_id is missing");
            }

            // TODO: Validate if the SCOPE for this client is valid
            String scope = request.getParameter(Constants.SCOPE.getKey());
            if (scope == null || scope.trim().length() == 0) {
                throw new IllegalArgumentException("The scope is missing");
            }

            // TODO: Validate it, needs to be 43-128 characters
            String code_challenge = request.getParameter(Constants.CODE_CHALLENGE.getKey());
            if (!Pkce.verifyChallenge(code_challenge)) {
                throw new IllegalArgumentException("invalid code_challenge");
            }

            // We always require S256
            String code_challenge_method = request.getParameter(Constants.CODE_CHALLENGE_METHOD.getKey());
            if(!Pkce.CODE_CHALLENGE_METHOD_S256.equalsIgnoreCase(code_challenge_method)) {
                throw new IllegalArgumentException("The given code_challenge_method is not supported!");
            }

            // TODO: Validate the redirect_uri to be one registered for the client_id.
            String redirectUri = request.getParameter(Constants.REDIRECT_URI.getKey());
            if(redirectUri == null || redirectUri.trim().length() == 0 || redirectUri.startsWith("http://")) {
                throw new IllegalArgumentException("The given redirect_uri is not valid. 'http' schema is not supported!");
            }

            // Need to remember all these values for the current session
            Map<String, Object> sessionValues = new HashMap<>();
            sessionValues.put(Constants.CLIENT_ID.getKey(), clientId);
            sessionValues.put(Constants.SCOPE.getKey(), scope);
            sessionValues.put(Constants.RESPONSE_TYPE.getKey(), response_type);
            sessionValues.put(Constants.CODE_CHALLENGE.getKey(), code_challenge);
            sessionValues.put(Constants.CODE_CHALLENGE_METHOD.getKey(), code_challenge_method);
            sessionValues.put(Constants.REDIRECT_URI.getKey(), redirectUri);
            sessionValues.put(Constants.NONCE.getKey(), nonce);
            sessionValues.put(Constants.STATE.getKey(), state);
            sessionValues.put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_LOGIN.getKey());

            LoginbuddyCache.getInstance().put("fakeProvider_".concat(state), sessionValues); // adding 'fakeProvider_' to not overlap with Loginbuddy client values

            // forward to a fake login page
            request.getRequestDispatcher("exampleProviderUsername.jsp").forward(request, response);
        } catch (Exception e) {
            LOGGER.warning("The authorization request was invalid");
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // TODO: since this is fake, we do not really validate the incoming parameters ... just assuming they are available ... exactly once ... otherwise, add validation!

        String path = request.getRequestURL().toString();
        String state = request.getParameter("state");
        String action = request.getParameter("action");

        Map<String, Object> sessionValues = null;
        if (state != null && state.trim().length() > 0) {
            sessionValues = (Map<String, Object>) LoginbuddyCache.getInstance().remove("fakeProvider_".concat(state));
            if (sessionValues == null) {
                LOGGER.warning("Unknown or expired session!");
                response.sendError(400, "Unknown or expired session!");
                return;
            } else {
                // let's check if the given state is the one associated with the sessionValues
                String sessionState = (String)sessionValues.get(Constants.STATE.getKey());
                if (!state.equals(sessionState)) {
                    LOGGER.warning("Sessions at FakeProvider are mixed up!");
                    response.sendError(400, "Sessions at FakeProvider are mixed up!");
                    return;
                }
                // let's check if the given action is the one we expect (unless it is 'cancel')
                String actionExpected = (String)sessionValues.get(Constants.ACTION_EXPECTED.getKey());
                if (!actionExpected.equals(action) && !"cancel".equalsIgnoreCase(action)) {
                    LOGGER.warning("The current action was not expected! Given: '" + action + "', expected: '" + actionExpected + "'");
                    response.sendError(400, "The current action was not expected!");
                    return;
                }
            }
        } else {
            LOGGER.warning("Invalid or missing state parameter!");
            response.sendError(400, "Invalid of missing state parameter!");
            return;
        }

        try {
            // Handle the provided 'email address'. In a real life scenario it would have to be validated
            if (path.endsWith("/login")) {
                String email = request.getParameter("email");
                if (email != null && email.trim().length() > 0) {
                    // add the email to the current session, but also check if it is the expected one
                    sessionValues.put("email", email);
                    sessionValues.put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_AUTHENTICATE.getKey());
                    LoginbuddyCache.getInstance().put("fakeProvider_".concat(state), sessionValues);
                    request.getRequestDispatcher("exampleProviderAuthenticate.jsp").forward(request, response);
                } // TODO: else { ... return an error and request the email-address or allow to cancel ... }
                else if("cancel".equalsIgnoreCase(action)) {

                    String clientRedirectUri = (String)sessionValues.get(Constants.REDIRECT_URI.getKey());
                    String clientState = (String)sessionValues.get(Constants.STATE.getKey());

                    if (clientRedirectUri.contains("?")) {
                        clientRedirectUri += "&state=" + clientState;
                    } else {
                        clientRedirectUri += "?state=" + clientState;
                    }
                    clientRedirectUri += "&error=login_cancelled&error_description=the+resource_owner+cancelled+the+login+process";
                    response.sendRedirect(clientRedirectUri);
                }
            } else if (path.endsWith("/authenticate")) {
                String password = request.getParameter("password");
                if (password != null && password.trim().length() > 0) {
                    sessionValues.put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_GRANT.getKey());
                    LoginbuddyCache.getInstance().put("fakeProvider_".concat(state), sessionValues);
                    request.getRequestDispatcher("exampleProviderConsent.jsp").forward(request, response);
                } // TODO: else { ... return an error and request the password or allow to cancel ... }
                else if("cancel".equalsIgnoreCase(action)) {

                    String clientRedirectUri = (String)sessionValues.get(Constants.REDIRECT_URI.getKey());
                    String clientState = (String)sessionValues.get(Constants.STATE.getKey());

                    if (clientRedirectUri.contains("?")) {
                        clientRedirectUri += "&state=" + clientState;
                    } else {
                        clientRedirectUri += "?state=" + clientState;
                    }
                    clientRedirectUri += "&error=login_cancelled&error_description=the+resource_owner+cancelled+the+authentication+process";
                    response.sendRedirect(clientRedirectUri);
                }
            } else if (path.endsWith("/consent")) {
                if ("grant".equals(action)) {

                    // now it is time to issue an authorization code
                    String code = String.valueOf(UUID.randomUUID().toString());

                    sessionValues.put("grant", String.valueOf(new Date().getTime())); // TODO: remember when this grant was given! If we had a 'grant' table, it would go in there
                    LoginbuddyCache.getInstance().put(code, sessionValues);

                    String clientRedirectUri = (String)sessionValues.get(Constants.REDIRECT_URI.getKey());
                    String clientState = (String)sessionValues.get(Constants.STATE.getKey());

                    if (clientRedirectUri.contains("?")) {
                        clientRedirectUri += "&state=" + clientState;
                    } else {
                        clientRedirectUri += "?state=" + clientState;
                    }
                    clientRedirectUri += "&code=" + URLEncoder.encode(code, "UTF-8");

                    response.sendRedirect(clientRedirectUri);
                } // TODO: else { ... return an error and request the consent or allow to cancel ... }
                else if("cancel".equalsIgnoreCase(action)) {

                    String clientRedirectUri = (String)sessionValues.get(Constants.REDIRECT_URI.getKey());
                    String clientState = (String)sessionValues.get(Constants.STATE.getKey());

                    if (clientRedirectUri.contains("?")) {
                        clientRedirectUri += "&state=" + clientState;
                    } else {
                        clientRedirectUri += "?state=" + clientState;
                    }
                    clientRedirectUri += "&error=access_denied&error_description=the+resource_owner+denied_access";
                    response.sendRedirect(clientRedirectUri);
                }
            } else {
                LOGGER.warning("Unknown API was called!");
                response.sendError(400, "Unknown API was called!");
            }
        } catch (ServletException e) {
            LOGGER.warning("Something in the FakeProvider went badly wrong!");
            e.printStackTrace();
            response.sendError(500, "Something in the FakeProvider went badly wrong!");
        }
    }
}