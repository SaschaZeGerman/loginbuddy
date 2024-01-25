package net.loginbuddy.config.loginbuddy.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.api.PostBody;
import net.loginbuddy.common.api.PostRequest;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.config.loginbuddy.Providers;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

public class RefreshTokenHandler implements GrantTypeHandler {

    private static final Logger LOGGER = Logger.getLogger(RefreshTokenHandler.class.getName());

    @Override
    public void handleGrantType(HttpServletRequest request, HttpServletResponse response, String... extras) throws IOException {

        ParameterValidatorResult refreshTokenResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.REFRESH_TOKEN.getKey()));
        ParameterValidatorResult scopeResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.SCOPE.getKey()));

        if (refreshTokenResult.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {

// ***************************************************************
// ** Check if the refresh_token was obfuscated (encrypted)
// ***************************************************************

            if (refreshTokenResult.getValue().startsWith("lb.")) {

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
                if (clientId.equals(extras[0])) {

                    Providers providers = LoginbuddyUtil.UTIL.getProviderConfigByProvider(provider);

                    String scopeForRTRequest = scopeResult.getResult().equals(ParameterValidatorResult.RESULT.VALID) ? scopeResult.getValue() : null;
                    if (scopeForRTRequest == null) {
                        LOGGER.warning(String.format("parameter scope is ignored for refresh_token request to provider %s with client_id %s.\n", provider, clientId));
                    }
                    List<NameValuePair> params = PostBody.create()
                            .addParameter(Constants.CLIENT_ID.getKey(), providers.getClientId())
                            .addParameter(Constants.CLIENT_SECRET.getKey(), providers.getClientSecret())
                            .addParameter(Constants.REDIRECT_URI.getKey(), providers.getRedirectUri())
                            .addParameter(Constants.GRANT_TYPE.getKey(), Constants.GRANT_TYPE_REFRESH_TOKEN.getKey())
                            .addParameter(Constants.REFRESH_TOKEN.getKey(), refreshToken)
                            .addParameter(Constants.SCOPE.getKey(), scopeForRTRequest)
                            .build();
                    try {
                        PostRequest pr = PostRequest.create(providers.getTokenEndpoint())
                                .setFormParametersPayload(params)
                                .setAcceptType("application/json");
                        if(providers.isDpopEnabled()) {
                            pr.setDpopHeader(
                                    providers.getDpopSigningAlg(),
                                    providers.getTokenEndpoint(),
                                    null,
                                    null,
                                    null);
                        }
                        MsgResponse tokenResponse = HttpHelper.postMessage(pr.build(), "application/json");

                        JSONObject tokenResponseObject = new DefaultTokenResponseHandler().handleRefreshTokenResponse(tokenResponse, providers, clientId);
                        response.setStatus(tokenResponse.getStatus());
                        response.setContentType(tokenResponse.getContentType());
                        response.getWriter().write(tokenResponseObject.toJSONString());
                    } catch (ParseException e) {
                        LOGGER.info(e.getMessage());
                        response.setStatus(400);
                        response.setContentType("application/json");
                        response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "the openid connect provider returned an unusable response").toJSONString());
                    } catch (Exception e) {
                        // caused by setDpopHeader ...
                        LOGGER.info(e.getMessage());
                        response.setStatus(500);
                        response.setContentType("application/json");
                        response.getWriter().write(HttpHelper.getErrorAsJson("server_error", "the dpop header could not be generated").toJSONString());
                    }
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