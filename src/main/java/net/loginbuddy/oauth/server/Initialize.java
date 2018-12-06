/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.oauth.server;

import net.loginbuddy.cache.LoginbuddyCache;
import net.loginbuddy.config.Constants;
import net.loginbuddy.config.ProviderConfig;
import net.loginbuddy.config.LoginbuddyConfig;
import net.loginbuddy.oauth.util.OpenIDConfiguration;
import org.json.simple.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;

@WebServlet(name = "Initialize")
public class Initialize extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(Initialize.class));

    // initiate authorization flow with provider
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String session = request.getParameter("session");
        if (session == null || session.trim().length() == 0 || request.getParameterValues("session").length > 1) {
            response.sendError(400, "Missing session, cannot initiate the authorization flow!");
            return;
        }

        Map<String, Object> sessionValues = (Map<String, Object>) LoginbuddyCache.getInstance().getCache().get(session);
        if (sessionValues == null || !session.equals(sessionValues.get(Constants.SESSION.getKey()))) {
            response.sendError(400, "The current session is invalid or it has expired!");
            return;
        }

        String providerSession = ((String)sessionValues.get("clientProvider")).trim();
        if("".equals(providerSession)) {
            providerSession = request.getParameter(Constants.PROVIDER.getKey());
            if (providerSession != null && request.getParameterValues(Constants.PROVIDER.getKey()).length > 1) {
                response.sendError(400, "Invalid provider parameter");
                return;
            }
            if (providerSession == null || providerSession.trim().length() == 0) {
                response.sendError(400, "No provider has been selected");
                return;
            }
        }

        ProviderConfig providerConfig = null;
        try {
            providerConfig = LoginbuddyConfig.getInstance().getConfigUtil().getProviderConfigByProvider(providerSession);
            if (providerConfig == null) {
                response.sendError(400, "The given provider is unknown or invalid");
                return;
            }
        } catch (Exception e) {
            // should never occur
            LOGGER.severe("The system has not been configured yet!");
            response.sendError(500, "The system has not been configured yet!");
            return;
        }

        sessionValues.put("clientProvider", providerSession);

        StringBuilder authorizeUrl = new StringBuilder();

        // check for 'util configuration' first
        if (providerConfig.getOpenidConfigurationUri() != null) {
            JSONObject openIdConfig = OpenIDConfiguration.getOpenIDConfiguration(providerConfig.getOpenidConfigurationUri());
            if (openIdConfig != null) {
                providerConfig.setAuthorizationEndpoint(openIdConfig.get("authorization_endpoint").toString());
                sessionValues.put(Constants.TOKEN_ENDPOINT.getKey(), openIdConfig.get("token_endpoint").toString());
                sessionValues.put(Constants.USERINFO_ENDPOINT.getKey(), openIdConfig.get("userinfo_endpoint").toString());
            }
        }

        // RFC 7636, generate PKCE values
        String code_verifier = UUID.randomUUID().toString().replace("-", "");
        code_verifier = new String(Base64.getEncoder().encode(code_verifier.getBytes())).replace("=", "");
        sessionValues.put("code_verifier", code_verifier);

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // this should never ever happen!
            throw new IOException(e.getCause());
        }

        byte[] encodedhash = digest.digest(code_verifier.getBytes(StandardCharsets.UTF_8));
        String code_challenge = new String(Base64.getUrlEncoder().encode(encodedhash)).replace("=", "");

        authorizeUrl.append(providerConfig.getAuthorizationEndpoint())
            .append("?").append(Constants.CLIENT_ID.getKey())
            .append("=").append(providerConfig.getClientId())
            .append("&").append(Constants.RESPONSE_TYPE.getKey())
            .append("=").append(providerConfig.getResponseType())
            .append("&").append(Constants.SCOPE.getKey())
            .append("=").append(URLEncoder.encode(Constants.OPENID_SCOPE.getKey(), "UTF-8"))
            .append("&").append(Constants.NONCE.getKey())
            .append("=").append(sessionValues.get(Constants.NONCE.getKey()))
            .append("&").append(Constants.REDIRECT_URI.getKey())
            .append("=").append(URLEncoder.encode(providerConfig.getRedirectUri(), "utf-8"))
            .append("&").append("code_challenge=").append(code_challenge)
            .append("&").append("code_challenge_method=S256")
            .append("&").append(Constants.STATE.getKey())
            .append("=").append(sessionValues.get(Constants.SESSION.getKey()));

        LoginbuddyCache.getInstance().getCache().put((String)sessionValues.get(Constants.SESSION.getKey()), sessionValues);

        response.sendRedirect(authorizeUrl.toString());

    }
}