package net.loginbuddy.service.client.handler;

import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.config.JwsAlgorithm;
import net.loginbuddy.common.util.ExchangeBean;
import net.loginbuddy.common.util.Jwt;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.config.loginbuddy.common.OnBehalfOf;
import net.loginbuddy.config.loginbuddy.common.OnBehalfOfResult;
import net.loginbuddy.config.loginbuddy.handler.LoginbuddyHandler;
import net.loginbuddy.config.properties.PropertiesUtil;
import net.loginbuddy.service.util.SessionContext;
import org.jose4j.lang.JoseException;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.UUID;

public abstract class CallbackHandlerDefault  implements CallbackHandler {

    protected String getMessageForRedirect(String redirectUri, String urlSafeKey, String value) {
        return redirectUri.concat(urlSafeKey).concat("=").concat(HttpHelper.urlEncode(value));
    }

    void returnAuthorizationCode(HttpServletResponse response, SessionContext sessionCtx, ExchangeBean eb) throws Exception {

// ***************************************************************
// ** Issue our own authorization_code and add details for the final client response
// ***************************************************************

        String authorizationCode = UUID.randomUUID().toString();
        if (!("".equals(sessionCtx.getString(Constants.CLIENT_SIGNED_RESPONSE_ALG.getKey())))) {
            sessionCtx.put("eb", Jwt.DEFAULT.createSignedJwt(eb.toString(), JwsAlgorithm.findMatchingAlg(sessionCtx.getString(Constants.CLIENT_SIGNED_RESPONSE_ALG.getKey()))).getCompactSerialization());
        } else {
            sessionCtx.put("eb", eb.toString());
        }
        sessionCtx.put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_TOKEN_EXCHANGE.getKey());
        LoginbuddyCache.CACHE.put(authorizationCode, sessionCtx, PropertiesUtil.UTIL.getLongProperty("lifetime.oauth.authcode"));

        response.sendRedirect(getMessageForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), Constants.CODE.getKey(), authorizationCode));
    }

    protected void endFunHere(String error, String errorDescription, SessionContext sessionCtx, HttpServletResponse response) throws IOException {
        response.sendRedirect(
                HttpHelper.getErrorForRedirect(
                        sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()),
                        error,
                        errorDescription));
    }
}
