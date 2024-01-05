/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.client.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.*;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.config.loginbuddy.Providers;
import net.loginbuddy.config.loginbuddy.common.DefaultTokenResponseHandler;
import net.loginbuddy.config.properties.PropertiesUtil;
import net.loginbuddy.service.util.SessionContext;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.logging.Logger;

public class CallbackHandlerCode extends CallbackHandlerDefault {

    private static final Logger LOGGER = Logger.getLogger(CallbackHandlerCode.class.getName());

    @Override
    public void handleCallback(HttpServletRequest request, HttpServletResponse response, SessionContext sessionCtx, ExchangeBean eb, String provider) throws Exception {

// ***************************************************************
// ** If we did not get a valid code parameter we are done
// ***************************************************************
        ParameterValidatorResult codeResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CODE.getKey()));
        if (!codeResult.getResult().equals(RESULT.VALID)) {
            LOGGER.warning("Missing code parameter returned from provider!");
            endFunHere("invalid_session", "missing or invalid code parameter", sessionCtx, response);
            return;
        }

        Providers providers = null;
        if (Constants.ISSUER_HANDLER_LOGINBUDDY.getKey().equalsIgnoreCase(sessionCtx.getString(Constants.ISSUER_HANDLER.getKey()))) {
            providers = LoginbuddyUtil.UTIL.getProviderConfigByProvider(provider);
        } else {
            // dynamically registered providers are in a separate container and not available here. Get details out of the session
            providers = new Providers(
                    provider,
                    sessionCtx.getString(Constants.PROVIDER_CLIENT_ID.getKey()),
                    sessionCtx.getString(Constants.PROVIDER_REDIRECT_URI.getKey()),
                    sessionCtx.getString(Constants.PROVIDER_CLIENT_SECRET.getKey()));
        }

        String access_token = null;

// ***************************************************************
// ** Exchange the code for a token response
// ***************************************************************

        MsgResponse tokenResponse = HttpHelper.postTokenExchange(providers.getClientId(), providers.getClientSecret(), providers.getRedirectUri(), codeResult.getValue(),
                sessionCtx.getString(Constants.TOKEN_ENDPOINT.getKey()), sessionCtx.getString(Constants.CODE_VERIFIER.getKey()));
        if (tokenResponse != null) {
            if (tokenResponse.getStatus() == 200) {
                if (tokenResponse.getContentType().startsWith("application/json")) {
                    JSONObject tokenResponseObject = new DefaultTokenResponseHandler().handleCodeTokenExchangeResponse(
                            tokenResponse,
                            sessionCtx.getBoolean(Constants.OBFUSCATE_TOKEN.getKey()),
                            providers,
                            sessionCtx.getString(Constants.CLIENT_CLIENT_ID.getKey()),
                            sessionCtx.getString(Constants.CLIENT_NONCE.getKey()),
                            sessionCtx.getString(Constants.JWKS_URI.getKey())
                    );
                    if(tokenResponseObject.get("id_token_payload") != null) {
                        eb.setIdTokenPayload((JSONObject)tokenResponseObject.remove("id_token_payload"));
                    }
                    access_token = (String)tokenResponseObject.remove("provider_access_token");
                    eb.setTokenResponse(tokenResponseObject);
                } else {
                    endFunHere("invalid_response", String.format("the provider returned a response with an unsupported content-type: %s", tokenResponse.getContentType()), sessionCtx, response);
                    return;
                }
            } else {
                // need to handle error cases
                if (tokenResponse.getContentType().startsWith("application/json")) {
                    JSONObject err = (JSONObject) new JSONParser().parse(tokenResponse.getMsg());
                    endFunHere((String) err.get("error"), (String) err.get("error_description"), sessionCtx, response);
                    return;
                }
            }
        } else {
            endFunHere("invalid_request", "the code exchange failed. An access_token could not be retrieved", sessionCtx, response);
            return;
        }

// ***************************************************************
// ** Now, let's get the userinfo response
// ***************************************************************

        String userinfo = sessionCtx.getString(Constants.USERINFO_ENDPOINT.getKey());
        if (userinfo != null) {
            try {
                MsgResponse userinfoResp = HttpHelper.getAPI(access_token, userinfo);
                if (userinfoResp.getStatus() == 200) {
                    if (userinfoResp.getContentType().startsWith("application/json")) {
                        JSONObject userinfoRespObject = (JSONObject) new JSONParser().parse(userinfoResp.getMsg());
                        eb.setUserinfo(userinfoRespObject);
                    }
                } // TODO : handle non 200 response
            } catch (Exception e) {
                LOGGER.warning("Retrieving userinfo failed!");
            }
        }
        eb.setNormalized(Normalizer.normalizeDetails(providers.getMappings(), eb.getEbAsJson(), access_token));

        createUserInfoSession(sessionCtx, access_token);

        returnAuthorizationCode(response, sessionCtx, eb);
    }

    protected void createUserInfoSession(SessionContext sessionCtx, String access_token) {

// ***************************************************************
// ** Create a session to be used if the client wants to call the providers Userinfo endpoint itself. Loginbuddy will proxy those calls
// ***************************************************************

        JSONObject jo = new JSONObject();
        jo.put(Constants.USERINFO_ENDPOINT.getKey(), sessionCtx.getString(Constants.USERINFO_ENDPOINT.getKey()));
        jo.put(Constants.JWKS_URI.getKey(), sessionCtx.getString(Constants.JWKS_URI.getKey()));
        String[] hint = access_token.split("[.]");
        if (hint.length == 3) {
            LoginbuddyCache.CACHE.put(hint[2], jo, PropertiesUtil.UTIL.getLongProperty("lifetime.proxy.userinfo"));
        } else {
            LoginbuddyCache.CACHE.put(access_token, jo, PropertiesUtil.UTIL.getLongProperty("lifetime.proxy.userinfo"));
        }
    }
}