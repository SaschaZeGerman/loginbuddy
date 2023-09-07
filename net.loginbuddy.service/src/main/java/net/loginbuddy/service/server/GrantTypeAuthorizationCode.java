package net.loginbuddy.service.server;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.Pkce;
import net.loginbuddy.service.util.SessionContext;

import java.io.IOException;

public class GrantTypeAuthorizationCode implements GrantTypeHandler {

    @Override
    public void handleGrantType(HttpServletRequest request, HttpServletResponse response, String... extras) throws IOException  {

        ParameterValidatorResult codeResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CODE.getKey()));
        ParameterValidatorResult redirectUriResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.REDIRECT_URI.getKey()));
        ParameterValidatorResult codeVerifierResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CODE_VERIFIER.getKey()));

// ***************************************************************
// ** Check for a valid code parameter
// ***************************************************************

        if (!codeResult.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
            response.getWriter().write(HttpHelper.createJsonErrorResponse(
                    "the given code parameter is invalid or was provided multiple times"));
            return;
        }

// ***************************************************************
// ** Check for the current session and remove it. An authorization code can be used only once!
// ***************************************************************

        SessionContext sessionCtx = (SessionContext) LoginbuddyCache.CACHE.remove(codeResult.getValue());
        if (sessionCtx == null) {
            response.getWriter().write(HttpHelper.createJsonErrorResponse("the given code is invalid or has expired"));
        } else {
            boolean checkRedirectUri = sessionCtx.get(Constants.CHECK_REDIRECT_URI.getKey(), Boolean.class);
            if (checkRedirectUri) {
                if (!redirectUriResult.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
                    response.getWriter().write(HttpHelper.createJsonErrorResponse("missing or duplicate redirect_uri"));
                    return;
                } else {
                    if (!redirectUriResult.getValue().equals(sessionCtx.getString(Constants.CLIENT_REDIRECT.getKey()))) {
                        response.getWriter().write(HttpHelper.createJsonErrorResponse("invalid redirect_uri", redirectUriResult.getValue()));
                        return;
                    }
                }
            }

// ***************************************************************
// ** If the client initially used PKCE, it now has to use PKCE also
// ***************************************************************

            String clientCodeChallenge = sessionCtx.getString(Constants.CLIENT_CODE_CHALLENGE.getKey());
            if (clientCodeChallenge != null) {
                if (codeVerifierResult.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
                    if (!Pkce.validate(clientCodeChallenge, sessionCtx.getString(Constants.CLIENT_CODE_CHALLENGE_METHOD.getKey()),
                            codeVerifierResult.getValue())) {
                        response.getWriter().write(HttpHelper.createJsonErrorResponse("the code_verifier is invalid"));
                        return;
                    }
                } else {
                    response.getWriter().write(HttpHelper.createJsonErrorResponse("the code_verifier parameter is invalid"));
                    return;
                }
            }

// ***************************************************************
// ** Send the token response to the client
// ***************************************************************

            response.setStatus(200);
            String eb = sessionCtx.getString("eb");
            if (!(eb == null || eb.startsWith("{"))) {
                response.setContentType("application/jwt");
            }
            response.getWriter().write(sessionCtx.getString("eb"));
        }

    }

}