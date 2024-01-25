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

        ParameterValidatorResult accessToken = ParameterValidator.getSingleValue(request.getParameterValues(Constants.ACCESS_TOKEN.getKey()));
        String authorizationHeader = request.getHeader(Constants.AUTHORIZATION.getKey());

        String tokenHint;
        String[] token = HttpHelper.extractAccessToken(accessToken, authorizationHeader).split("[.]");

        // encrypted token (i.e.: lb.access_token_value)
        if (token.length == 2 && "lb".equals(token[0])) {
            try {
                tokenHint = LoginbuddyUtil.UTIL.decrypt(String.format("%s.%s", token[0], token[1]));
                // the original access_token may be a reference token or a JWT
                token = tokenHint.split(":")[1].split("[.]");
            } catch (Exception e) {
                response.setStatus(400);
                response.setContentType("application/json");
                response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "the given token could not be encrypted").toJSONString());

            }
        }

        // for JWT based token the signature was used as key for the cache
        if (token.length == 3) {
            tokenHint = token[2];
        } else {
            tokenHint = token[0];
        }

        MsgResponse msg;
        JSONObject userInfoSession = (JSONObject) LoginbuddyCache.CACHE.get(tokenHint);
        if (userInfoSession == null) {
            msg = new MsgResponse();
            msg.setStatus(400);
            msg.setContentType("application/json");
            msg.setMsg(HttpHelper.getErrorAsJson("invalid_request", "the given token is unknown").toJSONString());
        } else {
            try {
                String dpopNonce = (String)userInfoSession.get(Constants.DPOP_NONCE_HEADER.getKey());
                String dpopNonceProvider = (String)userInfoSession.get(Constants.DPOP_NONCE_HEADER_PROVIDER.getKey());
                HttpGet req = getUserinfo(userInfoSession, tokenHint, dpopNonce, dpopNonceProvider);
                msg = HttpHelper.getAPI(req);
                if (msg.getStatus() == 401) {
                    if (msg.getHeader("WWW-Authenticate") != null && msg.getHeader("WWW-Authenticate").contains("use_dpop_nonce")) {
                        req = getUserinfo(userInfoSession, tokenHint, msg.getHeader(Constants.DPOP_NONCE_HEADER.getKey()));
                        msg = HttpHelper.getAPI(req);
                    }
                }
            } catch (Exception e) {
                response.setStatus(500);
                response.setContentType("application/json");
                response.getWriter().write(HttpHelper.getErrorAsJson("server_error", "the userinfo request failed").toJSONString());
                return;
            }
        }

        response.setStatus(msg.getStatus());
        response.setContentType(msg.getContentType());
        response.getWriter().write(msg.getMsg());
    }

    private HttpGet getUserinfo(JSONObject userInfoSession, String tokenHint, String dpopNonce) throws Exception {
        return getUserinfo(userInfoSession, tokenHint, dpopNonce, Sanetizer.getDomain((String) userInfoSession.get(Constants.USERINFO_ENDPOINT.getKey())));
    }

    private HttpGet getUserinfo(JSONObject userInfoSession, String tokenHint, String dpopNonce, String dpopNonceProvider) throws Exception {
        String tokenType = (String) userInfoSession.get(Constants.TOKEN_TYPE.getKey());
        LoginbuddyHandler loginbuddyHandler = (LoginbuddyHandler)userInfoSession.get(Constants.ISSUER_HANDLER.getKey());
        return Constants.BEARER.getKey().equalsIgnoreCase(tokenType) ?
                GetRequest.create(loginbuddyHandler.getUserinfoApi( (String) userInfoSession.get(Constants.USERINFO_ENDPOINT.getKey()), true ))
                        .setBearerAccessToken(tokenHint)
                        .build() :
                GetRequest.create(loginbuddyHandler.getUserinfoApi( (String) userInfoSession.get(Constants.USERINFO_ENDPOINT.getKey()), true ))
                        .setAccessToken((String) userInfoSession.get(Constants.TOKEN_TYPE.getKey()), tokenHint)
                        .setDpopHeader(
                                (String) userInfoSession.get(Constants.DPOP_SIGNING_ALG.getKey()),
                                loginbuddyHandler.getUserinfoApi( (String) userInfoSession.get(Constants.USERINFO_ENDPOINT.getKey()), false ),
                                tokenHint,
                                dpopNonce,
                                dpopNonceProvider)
                        .build();
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
