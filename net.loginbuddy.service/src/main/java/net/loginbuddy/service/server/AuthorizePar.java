package net.loginbuddy.service.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;

import java.io.IOException;

public class AuthorizePar extends AuthorizationHandler {

    @Override
    protected void handleError(int httpStatus, String errorMsg, HttpServletResponse response) throws IOException {
        if(301 == httpStatus || 302 == httpStatus) {
            response.setStatus(400);
        } else {
            response.setStatus(httpStatus);
        }
        response.setContentType("application/json");
        response.addHeader("Cache-Control", "no-store");
        response.addHeader("Pragma", "no-cache");
        response.getWriter().write(HttpHelper.createJsonErrorResponse(errorMsg));
    }

    @Override
    protected ClientAuthenticator.ClientCredentialsResult handleClientValidation(ParameterValidatorResult clientIdResult, ParameterValidatorResult clientSecretResult, String authorizationHeader, String signedResponseAlg, boolean acceptDynamicProvider) {
        return ClientAuthenticator.validateClientCredentials(clientIdResult, clientSecretResult, authorizationHeader); // ignoring other parameters on purpose
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // the parameter 'request_uri' MUST NOT be submitted
        ParameterValidatorResult requestUriResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.REQUEST_URI.getKey()));

        if (!requestUriResult.getResult().equals(ParameterValidatorResult.RESULT.NONE)) {
            handleError(400, "request_uri must not be submitted", response);
            return;
        }

        request.setAttribute("par", true);
        super.doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setHeader("Allow", "POST");
        response.sendError(405, "Method not allowed");
    }
}
