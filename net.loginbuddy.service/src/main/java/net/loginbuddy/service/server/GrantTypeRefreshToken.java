package net.loginbuddy.service.server;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.Pkce;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.service.util.SessionContext;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class GrantTypeRefreshToken implements GrantTypeHandler {

    private static final Logger LOGGER = Logger.getLogger(GrantTypeRefreshToken.class.getName());

    @Override
    public void handleGrantType(HttpServletRequest request, HttpServletResponse response, String... extras) throws IOException  {

        ParameterValidatorResult refreshTokenResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.REFRESH_TOKEN.getKey()));
        ParameterValidatorResult scopeResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.SCOPE.getKey()));

        if(refreshTokenResult.getResult().equals(ParameterValidatorResult.RESULT.VALID))
        {

// ***************************************************************
// ** Check if the refresh_token was obfuscated (encrypted)
// ***************************************************************

            if(refreshTokenResult.getValue().startsWith("lb.")) {

                String token[] = new String[0];
                try {
                    token = LoginbuddyUtil.UTIL.decrypt(refreshTokenResult.getValue()).split(":");
                } catch (Exception e) {
                    response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "the given refresh_token could not be decrypted").toJSONString());
                    return;
                }
                String provider = token[0];
                String clientId = token[1];
                String refreshToken = token[2];

                // check if the given client_id (RP of Loginbuddy) is the one that requested the refresh_token
                if(clientId.equals(extras[0])) {
                    List<NameValuePair> parameters = new ArrayList<>();
                    parameters.add(new BasicNameValuePair(Constants.CLIENT_ID.getKey(), clientId));
                    parameters.add(new BasicNameValuePair(Constants.CLIENT_SECRET.getKey(), LoginbuddyUtil.UTIL.getProviderConfigByProvider(provider).getClientSecret()));
                    parameters.add(new BasicNameValuePair(Constants.GRANT_TYPE.getKey(), Constants.GRANT_TYPE_REFRESH_TOKEN.getKey()));
                    parameters.add(new BasicNameValuePair(Constants.REFRESH_TOKEN.getKey(), refreshToken));
                    if(scopeResult.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
                        parameters.add(new BasicNameValuePair(Constants.SCOPE.getKey(), scopeResult.getValue()));
                    } else {
                        LOGGER.warning(String.format("parameter scope is ignored for refresh_token request to provider %s with client_id %s.\n", provider, clientId));
                    }
                    MsgResponse resp = HttpHelper.postMessage(parameters, LoginbuddyUtil.UTIL.getProviderConfigByProvider(provider).getTokenEndpoint(), "application/json");
                    response.setStatus(resp.getStatus());
                    response.setContentType(resp.getContentType());
                    response.getWriter().write(resp.getMsg());

                } else {
                    response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "the client is not authorized for this request").toJSONString());
                }
            } else {
                LOGGER.info(String.format("the given token is not supported by Loginbuddy: %s", refreshTokenResult.getValue()));
                response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "the given token is not supported by Loginbuddy").toJSONString());
            }
        } else {
            response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "either an invalid or missing refresh_token parameter").toJSONString());
        }
    }
}