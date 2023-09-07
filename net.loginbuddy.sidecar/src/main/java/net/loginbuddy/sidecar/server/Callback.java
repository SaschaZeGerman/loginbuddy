/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.sidecar.server;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.*;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.config.loginbuddy.Providers;
import net.loginbuddy.config.loginbuddy.common.OnBehalfOf;
import net.loginbuddy.config.loginbuddy.common.OnBehalfOfResult;
import net.loginbuddy.sidecar.util.SessionContext;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

public class Callback extends CallbackParent {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(Callback.class));

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {

            SessionContext sessionCtx = checkForSessionAndErrors(request, response);
            if (sessionCtx == null) {
                return;
            }

            if (sessionCtx.getString(Constants.CLIENT_STATE.getKey()) != null) {
                response.setHeader("X-State", sessionCtx.getString(Constants.CLIENT_STATE.getKey()));
            }
            response.setContentType("application/json");

            if (sessionCtx.get("error") != null) {
                response.getWriter().write(
                        HttpHelper.getErrorAsJson(
                                sessionCtx.get(Constants.ERROR.getKey(), String.class),
                                sessionCtx.get(Constants.ERROR_DESCRIPTION.getKey(), String.class)
                        ).toJSONString());
                return;
            }

// ***************************************************************
// ** If we did not get a valid code parameter we are done
// ***************************************************************
            ParameterValidatorResult codeResult = ParameterValidator
                    .getSingleValue(request.getParameterValues(Constants.CODE.getKey()));
            if (!codeResult.getResult().equals(RESULT.VALID)) {
                LOGGER.warning("Missing code parameter returned from provider!");
                response.getWriter().write(HttpHelper.getErrorAsJson("invalid_session", "missing or invalid code parameter").toJSONString());
                return;
            }

// ***************************************************************
// ** Find the chosen provider of this session and get a token. Also start preparing the response for the client
// ***************************************************************

            String provider = sessionCtx.getString(Constants.CLIENT_PROVIDER.getKey());

            ExchangeBean eb = new ExchangeBean();
            eb.setIss(DiscoveryUtil.UTIL.getIssuer());
            eb.setIat(new Date().getTime() / 1000);
            eb.setAud(sessionCtx.getString(Constants.CLIENT_CLIENT_ID.getKey()));
            eb.setNonce(sessionCtx.getString(Constants.CLIENT_NONCE.getKey()));
            eb.setProvider(provider);

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
                        JSONObject tokenResponseObject = ((JSONObject) new JSONParser().parse(tokenResponse.getMsg()));
                        LOGGER.fine(tokenResponseObject.toJSONString());
                        access_token = tokenResponseObject.get(Constants.ACCESS_TOKEN.getKey()).toString();
                        if (sessionCtx.getBoolean(Constants.OBFUSCATE_TOKEN.getKey())) {
                            tokenResponseObject.put(Constants.ACCESS_TOKEN.getKey(), LoginbuddyUtil.UTIL.encrypt(String.format("%s:%s", providers.getProvider(), access_token)));
                            if (tokenResponseObject.get(Constants.REFRESH_TOKEN.getKey()) != null) {
                                tokenResponseObject.put(Constants.REFRESH_TOKEN.getKey(), LoginbuddyUtil.UTIL.encrypt(String.format("%s:%s", providers.getProvider(), tokenResponseObject.get(Constants.REFRESH_TOKEN.getKey()))));
                            }
                        }
                        String id_token = tokenResponseObject.get(Constants.ID_TOKEN.getKey()) == null ? null : (String) tokenResponseObject.get(Constants.ID_TOKEN.getKey());
                        if (id_token != null) {
                            try {
                                MsgResponse jwks = HttpHelper.getAPI(sessionCtx.getString(Constants.JWKS_URI.getKey()));
                                JSONObject idTokenPayload = Jwt.DEFAULT.validateIdToken(id_token, jwks.getMsg(), providers.getIssuer(), providers.getClientId(), sessionCtx.getString(Constants.CLIENT_NONCE.getKey()));
                                // check if the client is configured to get an id_token re-signed by Loginbuddy, on behalf of the original issuer
                                OnBehalfOfResult resigningResult = OnBehalfOf.signOnBehalfOf(
                                        sessionCtx.getString(Constants.CLIENT_CLIENT_ID.getKey()),
                                        sessionCtx.getString(Constants.CLIENT_NONCE.getKey()),
                                        "id_token",
                                        idTokenPayload,
                                        id_token
                                );
                                tokenResponseObject.put("id_token", resigningResult.getIdToken());
                                eb.setIdTokenPayload(resigningResult.getIdTokenPayload());
                            } catch (Exception e) {
                                LOGGER.warning(String.format("No id_token was issued or it was invalid! Details: %s", e.getMessage()));
                            }
                        } else {
                            LOGGER.warning("No id_token was issued");
                        }
                        eb.setTokenResponse(tokenResponseObject);
                    } else {
                        response.getWriter()
                                .write(HttpHelper.getErrorAsJson("invalid_response", String.format("the provider returned a response with an unsupported content-type: %s", tokenResponse.getContentType()))
                                        .toJSONString());
                        return;
                    }
                } else {
                    // need to handle error cases
                    if (tokenResponse.getContentType().startsWith("application/json")) {
                        JSONObject err = (JSONObject) new JSONParser().parse(tokenResponse.getMsg());
                        response.getWriter().write(HttpHelper.getErrorAsJson((String) err.get("error"), (String) err.get("error_description")).toJSONString());
                        return;
                    }
                }
            } else {
                response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "the code exchange failed. An access_token could not be retrieved").toJSONString());
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
// ** Return the details to the client
// ***************************************************************

            response.setStatus(200);
            if (!("".equals(sessionCtx.getString(Constants.CLIENT_SIGNED_RESPONSE_ALG.getKey())))) {
                response.setContentType("application/jwt");
                response.getWriter().write(getSignedResponse(eb.toString(), sessionCtx.getString(Constants.CLIENT_SIGNED_RESPONSE_ALG.getKey())));
            } else {
                response.getWriter().write(eb.toString());
            }

        } catch (Exception e) {
            LOGGER.warning(String.format("authorization request failed: %s", e.getMessage()));
            response.setStatus(400);
            response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "authorization request failed").toJSONString());
        }
    }
}