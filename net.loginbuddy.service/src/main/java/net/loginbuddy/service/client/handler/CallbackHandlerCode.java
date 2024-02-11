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
import net.loginbuddy.common.api.GetRequest;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.api.PostBody;
import net.loginbuddy.common.api.PostRequest;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.*;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.config.loginbuddy.Providers;
import net.loginbuddy.config.loginbuddy.common.DefaultTokenResponseHandler;
import net.loginbuddy.config.loginbuddy.handler.LoginbuddyHandler;
import net.loginbuddy.config.properties.PropertiesUtil;
import net.loginbuddy.service.util.SessionContext;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.List;
import java.util.logging.Logger;

import static net.loginbuddy.common.api.HttpHelper.postMessage;

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

        LoginbuddyHandler loginbuddyHandler = (LoginbuddyHandler) sessionCtx.get(Constants.ISSUER_HANDLER.getKey());
        Providers providers = loginbuddyHandler.getProviders(provider);

        String providerAccessToken = null;

// ***************************************************************
// ** Exchange the code for a token response
// ***************************************************************

        List<NameValuePair> params = PostBody.create()
                .addParameter(Constants.CLIENT_ID.getKey(), providers.getClientId())
                .addParameter(Constants.CLIENT_SECRET.getKey(), providers.getClientSecret())
                .addParameter(Constants.REDIRECT_URI.getKey(), providers.getRedirectUri())
                .addParameter(Constants.CODE.getKey(), codeResult.getValue())
                .addParameter(Constants.CODE_VERIFIER.getKey(), sessionCtx.getString(Constants.CODE_VERIFIER.getKey()))
                .addParameter(Constants.GRANT_TYPE.getKey(), Constants.AUTHORIZATION_CODE.getKey())
                .build();
        PostRequest pr = PostRequest.create(loginbuddyHandler.getTokenApi(sessionCtx.getString(Constants.TOKEN_ENDPOINT.getKey()), true))
                .setFormParametersPayload(params)
                .setAcceptType("application/json");
        if (providers.isDpopEnabled()) {
            pr.setDpopHeader(
                    providers.getDpopSigningAlg(),
                    loginbuddyHandler.getTokenApi(sessionCtx.getString(Constants.TOKEN_ENDPOINT.getKey()), false),
                    null,
                    sessionCtx.getString(Constants.DPOP_NONCE_HEADER.getKey()),
                    sessionCtx.getString(Constants.DPOP_NONCE_HEADER_PROVIDER.getKey()));
        }
        MsgResponse tokenResponse = HttpHelper.postMessage(pr.build(), "application/json");
        if (tokenResponse.getHeader(Constants.DPOP_NONCE_HEADER.getKey()) != null) {
            sessionCtx.put(Constants.DPOP_NONCE_HEADER.getKey(), tokenResponse.getHeader(Constants.DPOP_NONCE_HEADER.getKey()));
            sessionCtx.put(Constants.DPOP_NONCE_HEADER_PROVIDER.getKey(), Sanetizer.getDomain(loginbuddyHandler.getTokenApi(sessionCtx.getString(Constants.TOKEN_ENDPOINT.getKey()), false)));
        }
        JSONObject tokenResponseObject = (JSONObject) new JSONParser().parse(tokenResponse.getMsg());
        if (tokenResponse.getStatus() == 400) {
            if ("use_dpop_nonce".equalsIgnoreCase((String) tokenResponseObject.get("error"))) {
                pr.setDpopHeader(
                        providers.getDpopSigningAlg(),
                        sessionCtx.getString(loginbuddyHandler.getTokenApi(Constants.TOKEN_ENDPOINT.getKey(), false)),
                        null,
                        sessionCtx.getString(Constants.DPOP_NONCE_HEADER.getKey()),
                        sessionCtx.getString(Constants.DPOP_NONCE_HEADER_PROVIDER.getKey()));
                tokenResponse = postMessage(pr.build(), "application/json");
            }
        }

        String tokenType = Constants.BEARER.getKey();

        if (tokenResponse.getStatus() == 200) {
            tokenResponseObject = new DefaultTokenResponseHandler().handleCodeTokenExchangeResponse(
                    tokenResponse,
                    sessionCtx.getBoolean(Constants.OBFUSCATE_TOKEN.getKey()),
                    providers,
                    sessionCtx.getString(Constants.CLIENT_CLIENT_ID.getKey()),
                    sessionCtx.getString(Constants.CLIENT_NONCE.getKey()),
                    loginbuddyHandler.getJwksApi(sessionCtx.getString(Constants.JWKS_URI.getKey()), true)
            );
            tokenType = tokenResponseObject.get(Constants.TOKEN_TYPE.getKey()) != null ? (String) tokenResponseObject.get(Constants.TOKEN_TYPE.getKey()) : tokenType;
            if (tokenResponseObject.get("id_token_payload") != null) {
                eb.setIdTokenPayload((JSONObject) tokenResponseObject.remove("id_token_payload"));
            }
            providerAccessToken = (String) tokenResponseObject.remove("provider_access_token");
            eb.setTokenResponse(tokenResponseObject);
        } else {
            // need to handle error cases
            if (tokenResponse.getContentType().startsWith("application/json")) {
                endFunHere((String) tokenResponseObject.get("error"), (String) tokenResponseObject.get("error_description"), sessionCtx, response);
                return;
            }
        }

// ***************************************************************
// ** Now, let's get the userinfo response
// ***************************************************************

        String userinfo = sessionCtx.getString(Constants.USERINFO_ENDPOINT.getKey());
        if (userinfo != null) {
            try {
                HttpGet req = getUserInfoReq(sessionCtx, tokenType, loginbuddyHandler, userinfo, providerAccessToken, providers);
                MsgResponse userinfoResp = HttpHelper.getAPI(req);
                if (userinfoResp.getHeader(Constants.DPOP_NONCE_HEADER.getKey()) != null) {
                    sessionCtx.put(Constants.DPOP_NONCE_HEADER.getKey(), userinfoResp.getHeader(Constants.DPOP_NONCE_HEADER.getKey()));
                    sessionCtx.put(Constants.DPOP_NONCE_HEADER_PROVIDER.getKey(), Sanetizer.getDomain(loginbuddyHandler.getUserinfoApi(userinfo, false)));
                }
                if (userinfoResp.getStatus() == 401) {
                    if (userinfoResp.getHeader("www-authenticate") != null && userinfoResp.getHeader("www-authenticate").contains("use_dpop_nonce")) {
                        req = getUserInfoReq(sessionCtx, tokenType, loginbuddyHandler, userinfo, providerAccessToken, providers);
                        userinfoResp = HttpHelper.getAPI(req);
                    }
                }
                if (userinfoResp.getStatus() == 200) {
                    JSONObject userinfoRespObject = (JSONObject) new JSONParser().parse(userinfoResp.getMsg());
                    eb.setUserinfo(userinfoRespObject);
                } else {
                    // TODO : handle non 200 response
                    LOGGER.warning(String.format("Userinfo request failed: %s", userinfoResp.getMsg()));
                }
            } catch (Exception e) {
                LOGGER.warning(String.format("Retrieving userinfo failed! %s", e.getMessage()));
            }
        }
        eb.setNormalized(Normalizer.normalizeDetails(providers.getMappings(), eb.getEbAsJson(), providerAccessToken));

        createUserInfoSession(sessionCtx, providerAccessToken, tokenType, providers.getDpopSigningAlg(), loginbuddyHandler);

        returnAuthorizationCode(response, sessionCtx, eb);

    }

    private static HttpGet getUserInfoReq(SessionContext sessionCtx, String tokenType, LoginbuddyHandler loginbuddyHandler, String userinfo, String providerAccessToken, Providers providers) throws Exception {
        return Constants.BEARER.getKey().equalsIgnoreCase(tokenType) ?
                GetRequest.create(loginbuddyHandler.getUserinfoApi(userinfo, true))
                        .setBearerAccessToken(providerAccessToken)
                        .build() :
                GetRequest.create(loginbuddyHandler.getUserinfoApi(userinfo, true))
                        .setAccessToken(tokenType, providerAccessToken)
                        .setDpopHeader(
                                providers.getDpopSigningAlg(),
                                loginbuddyHandler.getUserinfoApi(userinfo, false),
                                providerAccessToken,
                                sessionCtx.getString(Constants.DPOP_NONCE_HEADER.getKey()),
                                sessionCtx.getString(Constants.DPOP_NONCE_HEADER_PROVIDER.getKey()))
                        .build();
    }

    protected void createUserInfoSession(SessionContext sessionCtx, String providerAccessToken, String tokenType, String dpopSigningAlg, LoginbuddyHandler loginbuddyHandler) {

// ***************************************************************
// ** Create a session to be used if the client wants to call the providers Userinfo endpoint itself. Loginbuddy will proxy those calls
// ***************************************************************

        JSONObject userInfoSession = new JSONObject();
        userInfoSession.put(Constants.USERINFO_ENDPOINT.getKey(), sessionCtx.getString(Constants.USERINFO_ENDPOINT.getKey()));
        userInfoSession.put(Constants.JWKS_URI.getKey(), sessionCtx.getString(Constants.JWKS_URI.getKey()));
        userInfoSession.put(Constants.TOKEN_TYPE.getKey(), tokenType);
        userInfoSession.put(Constants.DPOP_SIGNING_ALG.getKey(), dpopSigningAlg);
        userInfoSession.put(Constants.DPOP_NONCE_HEADER.getKey(), sessionCtx.getString(Constants.DPOP_NONCE_HEADER.getKey()));
        userInfoSession.put(Constants.DPOP_NONCE_HEADER_PROVIDER.getKey(), sessionCtx.getString(Constants.DPOP_NONCE_HEADER_PROVIDER.getKey()));
        userInfoSession.put(Constants.ISSUER_HANDLER.getKey(), loginbuddyHandler);
        String sessionKey = providerAccessToken;
        if (providerAccessToken.split("[.]").length == 3) {
            sessionKey = providerAccessToken.split("[.]")[2];
        }
        LoginbuddyCache.CACHE.put(sessionKey, userInfoSession, PropertiesUtil.UTIL.getLongProperty("lifetime.proxy.userinfo"));
    }
}