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
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.service.server.Overlord;
import org.apache.http.client.methods.HttpGet;
import org.json.simple.JSONObject;

import java.io.IOException;

public class Userinfo extends Overlord {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ParameterValidatorResult accessToken = ParameterValidator.getSingleValue(request.getParameterValues(Constants.ACCESS_TOKEN.getKey()));
        String authorizationHeader = request.getHeader(Constants.AUTHORIZATION.getKey());

        String hint;
        String[] token = HttpHelper.extractAccessToken(accessToken, authorizationHeader).split("[.]");

        // encrypted token (i.e.: lb.access_token_value)
        if (token.length == 2 && "lb".equals(token[0])) {
            try {
                hint = LoginbuddyUtil.UTIL.decrypt(String.format("%s.%s", token[0], token[1]));
                // the original access_token may be a reference token or a JWT
                token = hint.split(":")[1].split("[.]");
            } catch (Exception e) {
                response.setStatus(400);
                response.setContentType("application/json");
                response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "the given token could not be encrypted").toJSONString());

            }
        }

        // for JWT based token the signature was used as key for the cache
        if (token.length == 3) {
            hint = token[2];
        } else {
            hint = token[0];
        }

        MsgResponse msg;
        JSONObject apis = (JSONObject) LoginbuddyCache.CACHE.get(hint);
        if (apis == null) {
            msg = new MsgResponse();
            msg.setStatus(400);
            msg.setContentType("application/json");
            msg.setMsg(HttpHelper.getErrorAsJson("invalid_request", "the given token is unknown").toJSONString());
        } else {
            try {
                String tokenType = (String) apis.get(Constants.TOKEN_TYPE.getKey());
                HttpGet req = Constants.BEARER.getKey().equalsIgnoreCase(tokenType) ?
                        GetRequest.create((String) apis.get(Constants.USERINFO_ENDPOINT.getKey()))
                                .setBearerAccessToken(hint)
                                .build() :
                        GetRequest.create((String) apis.get(Constants.USERINFO_ENDPOINT.getKey()))
                                .setAccessToken((String) apis.get(Constants.TOKEN_TYPE.getKey()), hint)
                                .setDpopHeader((String) apis.get(Constants.DPOP_SIGNING_ALG.getKey()), (String) apis.get(Constants.USERINFO_ENDPOINT.getKey()), hint, null)
                                .build();
                msg = HttpHelper.getAPI(req);
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

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}
