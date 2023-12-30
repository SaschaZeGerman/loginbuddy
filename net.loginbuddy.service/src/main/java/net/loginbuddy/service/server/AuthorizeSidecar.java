package net.loginbuddy.service.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.config.HeadOfInitialize;
import net.loginbuddy.config.loginbuddy.Clients;
import net.loginbuddy.service.util.SessionContext;

import java.io.IOException;
import java.util.logging.Logger;

public class AuthorizeSidecar extends AuthorizationHandler {

    private static final Logger LOGGER = Logger.getLogger(AuthorizeSidecar.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        try {
            SidecarMaster.checkClientConnection(request);
        } catch (IllegalAccessException e) {
            LOGGER.warning(e.getMessage());
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write(e.getMessage());
            return;
        }

        super.doGet(request, response);
    }

    @Override
    protected void handleError(int httpStatus, String errorMsg, HttpServletResponse response) throws IOException {
        System.out.println(httpStatus);
        System.out.println(errorMsg);
    }

    @Override
    protected ClientAuthenticator.ClientCredentialsResult handleClientValidation(ParameterValidatorResult clientIdResult, ParameterValidatorResult clientSecretResult, String authorizationHeader) {
        return handleClientValidation(clientIdResult, clientSecretResult, authorizationHeader, null, false);
    }

    @Override
    protected ClientAuthenticator.ClientCredentialsResult handleClientValidation(ParameterValidatorResult clientIdResult, ParameterValidatorResult clientSecretResult, String authorizationHeader, String signedResponseAlg, boolean acceptDynamicProvider) {
        Clients c = new Clients();
        c.setClientId(Constants.SIDECAR_CLIENT_ID.getKey());
        c.setClientType(Constants.CLIENT_TYPE_CONFIDENTIAL.getKey());
        c.setRedirectUri(Constants.SIDECAR_REDIRECT_URI.getKey());
        c.setSignedResponseAlg(signedResponseAlg);
        c.setAcceptDynamicProvider(acceptDynamicProvider);
        return new ClientAuthenticator.ClientCredentialsResult(null, true, c);
    }

    @Override
    protected ParameterValidatorResult getClientIdResult(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // this should not be passed in, will be ignored
        return ParameterValidator.getSingleValue(new String[]{Constants.SIDECAR_CLIENT_ID.getKey()});
    }

    @Override
    protected ParameterValidatorResult getResponseTypeResult(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // this should not be passed in, will be ignored
        return ParameterValidator.getSingleValue(new String[]{Constants.CODE.getKey()});
    }

    @Override
    protected ParameterValidatorResult getRedirectUriResult(HttpServletRequest request, HttpServletResponse response) throws IOException {
        // this should not be passed in, will be ignored
        return ParameterValidator.getSingleValue(new String[]{Constants.SIDECAR_REDIRECT_URI.getKey()});
    }

    @Override
    protected ParameterValidatorResult getProviderResult(HttpServletRequest request) {
        return ParameterValidator.getSingleValue(request.getParameterValues(Constants.PROVIDER.getKey()), "");
//        ParameterValidatorResult issuerResult = ParameterValidator.getSingleValue(request.getParameterValues(Constants.ISSUER.getKey()));
//        ParameterValidatorResult providerResult = ParameterValidator.getSingleValue(request.getParameterValues(Constants.PROVIDER.getKey()));
//        String providerValue = "".equals(providerResult.getValue()) ? issuerResult.getResult().equals(ParameterValidatorResult.RESULT.VALID) ? Constants.DYNAMIC_PROVIDER.getKey() : "" : providerResult.getValue();
//        return ParameterValidator.getSingleValue(new String[]{providerValue});
    }

    @Override
    protected void createSessionAndResponse(HttpServletRequest request, HttpServletResponse response, String clientId, String clientScope, String clientResponseType, String clientCodeChallenge, String clientCodeChallendeMethod, String clientRedirectUri, String clientNonce, String clientState, ParameterValidatorResult clientProviderResult, String clientPrompt, String clientLoginHint, String clientIdTokenHint, boolean checkRedirectUri, String clientRedirectUriValid, boolean acceptDynamicProvider, String signResponseAlg, boolean obfuscateToken) throws IOException, ServletException {

        // ***************************************************************
        // ** Create the session so that it can be handled throughout multiple requests
        // ***************************************************************

        ParameterValidatorResult issuerResult = ParameterValidator.getSingleValue(request.getParameterValues(Constants.ISSUER.getKey()));
        SessionContext sessionCtx = new SessionContext();
        sessionCtx.setSessionInit(
                Constants.SIDECAR_CLIENT_ID.getKey(),
                clientScope,
                clientResponseType,
                null,
                null,
                null,
                clientNonce,
                clientState,
                "".equals(clientProviderResult.getValue()) ? issuerResult.getResult().equals(ParameterValidatorResult.RESULT.VALID) ? Constants.DYNAMIC_PROVIDER.getKey() : "" : clientProviderResult.getValue(),
                clientPrompt,
                clientLoginHint,
                clientIdTokenHint,
                false,
                clientRedirectUri,
                acceptDynamicProvider,
                signResponseAlg,
                obfuscateToken,
                null);

        // this will be the authorization_url or an error_url
        String authorizationUrl = HeadOfInitialize.processInitializeRequest(
                sessionCtx,
                clientProviderResult,
                ParameterValidator.getSingleValue(request.getParameterValues(Constants.ISSUER.getKey())),
                ParameterValidator.getSingleValue(request.getParameterValues(Constants.DISCOVERY_URL.getKey()))
        );
        if (authorizationUrl.contains("?error")) {
            response.setStatus(400);
        } else {
            response.setStatus(201);
        }
        response.setContentType("text/plain");
        response.addHeader("Location", authorizationUrl);
    }
}
