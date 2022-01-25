/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.client;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.*;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.loginbuddy.Clients;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.config.loginbuddy.Providers;
import net.loginbuddy.config.properties.PropertiesUtil;
import net.loginbuddy.service.util.SessionContext;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.UUID;
import java.util.logging.Logger;

public class CallbackHandlerCode extends Callback implements CallbackHandler {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(CallbackHandlerCode.class));

    public void handleCallback(HttpServletRequest request, HttpServletResponse response, SessionContext sessionCtx, ExchangeBean eb, String provider) throws Exception {

// ***************************************************************
// ** If we did not get a valid code parameter we are done
// ***************************************************************
        ParameterValidatorResult codeResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CODE.getKey()));
        if (!codeResult.getResult().equals(RESULT.VALID)) {
            LOGGER.warning("Missing code parameter returned from provider!");
            response.sendRedirect(HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "invalid_session", "missing or invalid code parameter"));
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
        String id_token = null;

// ***************************************************************
// ** Exchange the code for a token response
// ***************************************************************

        MsgResponse tokenResponse = HttpHelper.postTokenExchange(providers.getClientId(), providers.getClientSecret(), providers.getRedirectUri(), codeResult.getValue(),
                sessionCtx.getString(Constants.TOKEN_ENDPOINT.getKey()), sessionCtx.getString(Constants.CODE_VERIFIER.getKey()));
        JSONObject idTokenPayload = null;
        if (tokenResponse != null) {
            if (tokenResponse.getStatus() == 200) {
                if (tokenResponse.getContentType().startsWith("application/json")) {
                    JSONObject tokenResponseObject = ((JSONObject) new JSONParser().parse(tokenResponse.getMsg()));
                    LOGGER.fine(tokenResponseObject.toJSONString());
                    access_token = tokenResponseObject.get(Constants.ACCESS_TOKEN.getKey()).toString();
                    if(sessionCtx.getBoolean(Constants.OBFUSCATE_TOKEN.getKey())) {
                        tokenResponseObject.put(Constants.ACCESS_TOKEN.getKey(), UUID.randomUUID().toString().substring(0,8));
                        if(tokenResponseObject.get(Constants.REFRESH_TOKEN.getKey()) != null) {
                            tokenResponseObject.put(Constants.REFRESH_TOKEN.getKey(), UUID.randomUUID().toString().substring(0,8));
                        }
                    }
                    eb.setTokenResponse(tokenResponseObject);
                    try {
                        id_token = tokenResponseObject.get(Constants.ID_TOKEN.getKey()).toString();
                        MsgResponse jwks = HttpHelper.getAPI(sessionCtx.getString(Constants.JWKS_URI.getKey()));
                        idTokenPayload = Jwt.DEFAULT.validateIdToken(id_token, jwks.getMsg(), providers.getIssuer(),
                                providers.getClientId(), sessionCtx.getString(Constants.CLIENT_NONCE.getKey()));
                        // check if the client is configured to get an id_token re-signed by Loginbuddy, on behalf of the original issuer
                        Clients currentClient = LoginbuddyUtil.UTIL.getClientConfigByClientId(sessionCtx.getString(Constants.CLIENT_CLIENT_ID.getKey()));
                        if (Arrays.stream(currentClient.getOnBehalfOf()).anyMatch(n -> n.equalsIgnoreCase("id_token"))) {
                            JSONObject onBehalfOf = new JSONObject();
                            onBehalfOf.put("iss", idTokenPayload.get("iss"));
                            onBehalfOf.put("aud", idTokenPayload.get("aud"));
                            onBehalfOf.put("nonce", idTokenPayload.get("nonce"));
                            idTokenPayload.put("on_behalf_of", onBehalfOf);
                            idTokenPayload.put("iss", DiscoveryUtil.UTIL.getIssuer());
                            idTokenPayload.put("aud", currentClient.getClientId());
                            idTokenPayload.put("nonce", sessionCtx.getString(Constants.CLIENT_NONCE.getKey()));
                            tokenResponseObject.put("id_token", Jwt.DEFAULT.createSignedJwt(idTokenPayload.toJSONString(), sessionCtx.getString(Constants.CLIENT_SIGNED_RESPONSE_ALG.getKey())).getCompactSerialization());
                        }
                        eb.setIdTokenPayload(idTokenPayload);
                    } catch (Exception e) {
                        LOGGER.warning(String.format("No id_token was issued or it was invalid! Details: %s", e.getMessage()));
                    }
                } else {
                    response.sendRedirect(HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()),
                            "invalid_response",
                            String.format("the provider returned a response with an unsupported content-type: %s", tokenResponse.getContentType())));
                    return;
                }
            } else {
                // need to handle error cases
                if (tokenResponse.getContentType().startsWith("application/json")) {
                    JSONObject err = (JSONObject) new JSONParser().parse(tokenResponse.getMsg());
                    response.sendRedirect(HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), (String) err.get("error"), (String) err.get("error_description")));
                    return;
                }
            }
        } else {
            response.sendRedirect(HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "invalid_request", "the code exchange failed. An access_token could not be retrieved"));
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

        returnAuthorizationCode(response, sessionCtx, eb);
    }
}