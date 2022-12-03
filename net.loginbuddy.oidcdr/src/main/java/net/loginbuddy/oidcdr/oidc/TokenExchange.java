package net.loginbuddy.oidcdr.oidc;

import jakarta.servlet.http.HttpServlet;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Logger;

public class TokenExchange extends HttpServlet {

    private static Logger LOGGER = Logger.getLogger(String.valueOf(TokenExchange.class));

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            OIDCDRMaster.checkClientConnection(request);
        } catch (IllegalAccessException e) {
            LOGGER.warning(e.getMessage());
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write(e.getMessage());
            return;
        }

        ParameterValidatorResult targetEndpointResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.TARGET_PROVIDER.getKey()));
        ParameterValidatorResult authCodeResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CODE.getKey()));
        ParameterValidatorResult clientIdResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CLIENT_ID.getKey()));
        ParameterValidatorResult clientSecretResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CLIENT_SECRET.getKey()));
        ParameterValidatorResult redirectUriResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.REDIRECT_URI.getKey()));
        ParameterValidatorResult grantTypeResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.GRANT_TYPE.getKey()));
        ParameterValidatorResult codeVerifierResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CODE_VERIFIER.getKey()));

        MsgResponse msg = HttpHelper
                .postTokenExchange(
                        clientIdResult.getValue(),
                        clientSecretResult.getValue(),
                        redirectUriResult.getValue(),
                        authCodeResult.getValue(),
                        targetEndpointResult.getValue(),
                        codeVerifierResult.getValue());

        // TODO: validate msg as good as possible
        response.setStatus(msg.getStatus());
        response.setContentType(msg.getContentType());
        response.getWriter().write(msg.getMsg());

    }
}
