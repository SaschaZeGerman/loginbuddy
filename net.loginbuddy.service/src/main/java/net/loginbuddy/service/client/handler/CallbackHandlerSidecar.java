package net.loginbuddy.service.client.handler;

import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.config.JwsAlgorithm;
import net.loginbuddy.common.util.ExchangeBean;
import net.loginbuddy.common.util.Jwt;
import net.loginbuddy.config.loginbuddy.handler.LoginbuddyHandler;
import net.loginbuddy.service.util.SessionContext;
import org.jose4j.lang.JoseException;

import java.io.IOException;

public class CallbackHandlerSidecar extends CallbackHandlerCode {

    @Override
    protected void createUserInfoSession(SessionContext sessionCtx, String access_token, String tokenType, String dpopSigningAlg, LoginbuddyHandler loginbuddyHandler) {
        // do nothing on purpose
    }

    @Override
    void returnAuthorizationCode(HttpServletResponse response, SessionContext sessionCtx, ExchangeBean eb) throws Exception {

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
    }

    @Override
    protected void endFunHere(String error, String errorDescription, SessionContext sessionCtx, HttpServletResponse response) throws IOException {
        response.getWriter().write(HttpHelper.getErrorAsJson(error,errorDescription).toJSONString());
    }

    protected String getSignedResponse(String payload, String alg) throws JoseException {
        return Jwt.DEFAULT.createSignedJwt(payload, JwsAlgorithm.findMatchingAlg(alg)).getCompactSerialization();
    }
}
