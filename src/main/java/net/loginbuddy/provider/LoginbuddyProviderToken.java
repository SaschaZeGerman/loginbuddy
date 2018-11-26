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
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class LoginbuddyProviderToken extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(String.valueOf(LoginbuddyProviderToken.class));

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {
        throw new UnsupportedOperationException("GET is not supported for this operation");
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // TODO: Handle multiple grant_types. But, since this is all fake, we'll just support 'authorization_code'
        String grant_type = request.getParameter(Constants.GRANT_TYPE.getKey());
        if(!"authorization_code".equalsIgnoreCase(grant_type)) {
            throw new UnsupportedOperationException("The given grant_type is not supported or the parameter is missing");
        }

        String clientId = request.getParameter(Constants.CLIENT_ID.getKey());
        String code = request.getParameter(Constants.CODE.getKey());

        // find the session and fail if it is unknown
        Map<String, Object> sessionValues = (Map<String, Object>)LoginbuddyCache.getInstance().getCache().get(code);
        if (sessionValues == null) {
            throw new IllegalStateException("The given authorization_code is invalid or has expired or none was given");
        }

        // Need to check if the given clientId is the one associated with the given authorization_code
        if (clientId == null || !clientId.equals(sessionValues.get(Constants.CLIENT_ID.getKey())) ) {
            throw new IllegalStateException("The given client_id is not valid for the given authorization_code");
        }

        // TODO: Validate the client_secret. But, since this is all fake, we'll just check if it exists
        if(request.getParameter(Constants.CLIENT_SECRET.getKey()) == null) {
            throw new IllegalArgumentException("The client_secret is missing");
        }

        // Validate 'code_verifier' if PKCE was used (which is the default for loginbuddy)
        String code_challenge = (String)sessionValues.get(Constants.CODE_CHALLENGE.getKey());
        if(code_challenge != null) {
            String code_verifier = request.getParameter(Constants.CODE_VERIFIER.getKey());
            if (code_verifier == null) {
                throw new IllegalArgumentException("Missing code_verifier");
            } else {
                MessageDigest digest = null;
                try {
                    digest = MessageDigest.getInstance("SHA-256"); // the only algorithm this fake provider supports
                } catch (NoSuchAlgorithmException e) {
                    // this should never ever happen!
                    throw new IOException(e.getCause());
                }
                byte[] encodedhash = digest.digest(code_verifier.getBytes(StandardCharsets.UTF_8));
                if (!code_challenge.equals(new String(Base64.getUrlEncoder().encode(encodedhash)).replace("=", ""))) {
                    throw new IllegalArgumentException("The given code_verifier is invalid");
                }
            }
        }

        // Create values for the token response
        String access_token = "FAKE_".concat(UUID.randomUUID().toString());
        String refresh_token = "FAKE_".concat(UUID.randomUUID().toString());
        String id_token = "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJoZWxsbyI6ImxvZ2luYnVkZHkifQ.z9r5WoqNycrF7YLOZTZpMarwUeTopU1UZzRAU7beFTc"; // completely fake

        // Add to the sessionValues
        sessionValues.put("access_token", access_token);
        sessionValues.put("refresh_token",refresh_token);
        sessionValues.put("id_token",id_token);
        sessionValues.put("access_token_expiration", String.valueOf(new Date().getTime()+3600000)); // getTime should be 10-digits (seconds) but it is millis (13-digits)
        sessionValues.put("refresh_token_expiration", String.valueOf(new Date().getTime()+7200000)); // getTime should be 10-digits (seconds) but it is millis (13-digits)

        // Remove the code as key, it should be usable once only! And that was now
        LoginbuddyCache.getInstance().getCache().remove(code);

        // associate with access_token. We'll ignore the fresh_token for now. Remember, this is all 'fake'
        LoginbuddyCache.getInstance().getCache().put(access_token, sessionValues);

        // create the response message that includes the issued token
        JSONObject fakeProviderResponse = new JSONObject();
        fakeProviderResponse.put("access_token", access_token);
        fakeProviderResponse.put("refresh_token", refresh_token);
        fakeProviderResponse.put("token_type", "Bearer");
        fakeProviderResponse.put("expires_in", 3600);
        fakeProviderResponse.put("id_token", id_token);

        response.setStatus(200);
        response.setContentType("application/json");
        response.getWriter().println(fakeProviderResponse);
    }
}