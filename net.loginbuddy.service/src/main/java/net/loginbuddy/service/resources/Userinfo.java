package net.loginbuddy.service.resources;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.GetRequest;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.Sanetizer;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.config.loginbuddy.handler.LoginbuddyHandler;
import net.loginbuddy.service.server.Overlord;
import org.apache.http.client.methods.HttpGet;
import org.json.simple.JSONObject;

import java.io.IOException;

public class Userinfo extends Overlord {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ParameterValidatorResult accessTokenParameter = ParameterValidator.getSingleValue(request.getParameterValues(Constants.ACCESS_TOKEN.getKey()));
        String authorizationHeader = request.getHeader(Constants.AUTHORIZATION.getKey());

        // if it was encrypted by Loginbuddy it will look like lb.ey.....
        String givenToken = HttpHelper.extractAccessToken(accessTokenParameter, authorizationHeader);

        String originalAccessToken;

        if (givenToken.startsWith("lb.")) {
            try {
                String decryptedToken = LoginbuddyUtil.UTIL.decrypt(givenToken);
                String originalProvider = decryptedToken.split("[:]")[0];  // no use for this right now ... !?
                originalAccessToken = decryptedToken.split("[:]")[1];
            } catch (Exception e) {
                response.setStatus(400);
                response.setContentType("application/json");
                response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "the given token could not be decrypted").toJSONString());
                return;
            }
        } else {
            originalAccessToken = givenToken;
        }

        String sessionKey;

        // for JWT based token the signature was used as key for the cache
        if (originalAccessToken.split("[.]").length == 3) {
            sessionKey = originalAccessToken.split("[.]")[2];
        } else {
            sessionKey = originalAccessToken;
        }

        MsgResponse userinfoResp;
        JSONObject userInfoSession = (JSONObject) LoginbuddyCache.CACHE.get(sessionKey);
        if (userInfoSession == null) {
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "the given token is unknown").toJSONString());
        } else {
            try {
                String dpopNonce = (String)userInfoSession.get(Constants.DPOP_NONCE_HEADER.getKey());
                String dpopNonceProvider = (String)userInfoSession.get(Constants.DPOP_NONCE_HEADER_PROVIDER.getKey());
                HttpGet req = getUserInfoRequest(userInfoSession, originalAccessToken, dpopNonce, dpopNonceProvider);
                userinfoResp = HttpHelper.getAPI(req);
                if (userinfoResp.getStatus() == 401) {
                    if (userinfoResp.getHeader("WWW-Authenticate") != null && userinfoResp.getHeader("WWW-Authenticate").contains("use_dpop_nonce")) {
                        req = getUserInfoRequest(userInfoSession, originalAccessToken, userinfoResp.getHeader(Constants.DPOP_NONCE_HEADER.getKey()));
                        userinfoResp = HttpHelper.getAPI(req);
                    }
                }
                response.setStatus(userinfoResp.getStatus());
                response.setContentType(userinfoResp.getContentType());
                response.getWriter().write(userinfoResp.getMsg());
            } catch (Exception e) {
                response.setStatus(500);
                response.setContentType("application/json");
                response.getWriter().write(HttpHelper.getErrorAsJson("server_error", "the userinfo request failed").toJSONString());
            }
        }
    }

    private HttpGet getUserInfoRequest(JSONObject userInfoSession, String providerAccessToken, String dpopNonce) throws Exception {
        return getUserInfoRequest(userInfoSession, providerAccessToken, dpopNonce, Sanetizer.getDomain((String) userInfoSession.get(Constants.USERINFO_ENDPOINT.getKey())));
    }

    private HttpGet getUserInfoRequest(JSONObject userInfoSession, String providerAccessToken, String dpopNonce, String dpopNonceProvider) throws Exception {
        String tokenType = (String) userInfoSession.get(Constants.TOKEN_TYPE.getKey());
        LoginbuddyHandler loginbuddyHandler = (LoginbuddyHandler)userInfoSession.get(Constants.ISSUER_HANDLER.getKey());
        return Constants.BEARER.getKey().equalsIgnoreCase(tokenType) ?
                GetRequest.create(loginbuddyHandler.getUserinfoApi( (String) userInfoSession.get(Constants.USERINFO_ENDPOINT.getKey()), true ))
                        .setBearerAccessToken(providerAccessToken)
                        .build() :
                GetRequest.create(loginbuddyHandler.getUserinfoApi( (String) userInfoSession.get(Constants.USERINFO_ENDPOINT.getKey()), true ))
                        .setAccessToken((String) userInfoSession.get(Constants.TOKEN_TYPE.getKey()), providerAccessToken)
                        .setDpopHeader(
                                (String) userInfoSession.get(Constants.DPOP_SIGNING_ALG.getKey()),
                                loginbuddyHandler.getUserinfoApi( (String) userInfoSession.get(Constants.USERINFO_ENDPOINT.getKey()), false ),
                                providerAccessToken,
                                dpopNonce,
                                dpopNonceProvider)
                        .build();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
