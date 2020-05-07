package net.loginbuddy.service.server;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.Pkce;
import net.loginbuddy.common.util.Sanetizer;
import net.loginbuddy.service.config.ClientConfig;
import net.loginbuddy.service.config.LoginbuddyConfig;
import net.loginbuddy.service.config.discovery.DiscoveryConfig;
import net.loginbuddy.service.util.SessionContext;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Stream;

public abstract class AuthorizationHandler extends HttpServlet {

    private static Logger LOGGER = Logger.getLogger(String.valueOf(AuthorizationHandler.class));

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        // Check if this request includes a request_uri. If so, it is a PAR request and needs little attention only
        ParameterValidatorResult requestUriResult = ParameterValidator.getSingleValue(request.getParameterValues(Constants.REQUEST_URI.getKey()));
        if(requestUriResult.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
            String[] parRequestUriParts = requestUriResult.getValue().split(":");
            if(parRequestUriParts.length == 3) {
                SessionContext sessionCtx = (SessionContext) LoginbuddyCache.CACHE.get(parRequestUriParts[2]);
                if (sessionCtx == null || !parRequestUriParts[2].equals(sessionCtx.getId())) {
                    LOGGER.warning("The current session is invalid or it has expired! Given: '" + parRequestUriParts[2] + "'");
                    handleError(400, "The current session is invalid or it has expired!", response);
                    return;
                }
                // need to verify that the given requestUri is the one that belongs to this session
                if(requestUriResult.getValue() != null && requestUriResult.getValue().equals(sessionCtx.useParRequestUri())) {
                    handleAuthorizationResponse(request, response, sessionCtx, sessionCtx.get(Constants.CLIENT_PROVIDER.getKey()));
                    return;
                } else {
                    handleError(400, "invalid request_uri", response);
                    return;
                }
            } else {
                handleError(400, "invalid request_uri", response);
                return;
            }
        }

        ParameterValidatorResult clientIdResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CLIENT_ID.getKey()));
        ParameterValidatorResult clientSecretResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CLIENT_SECRET.getKey()));  // only valid for a PAR call
        ParameterValidatorResult clientResponseTypeResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.RESPONSE_TYPE.getKey()));
        ParameterValidatorResult clientRedirectUriResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.REDIRECT_URI.getKey()));
        ParameterValidatorResult clientProviderResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.PROVIDER.getKey()), "");
        ParameterValidatorResult clientStateResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.STATE.getKey()), "");
        ParameterValidatorResult clientCodeChallengeResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CODE_CHALLENGE.getKey()));
        ParameterValidatorResult clientCodeChallendeMethodResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CODE_CHALLENGE_METHOD.getKey()));
        ParameterValidatorResult scopeResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.SCOPE.getKey()));
        ParameterValidatorResult clientNonceResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.NONCE.getKey()));

        // these three parameters are forwarded to the provider, not handled by Loginbuddy
        ParameterValidatorResult clientPromptResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.PROMPT.getKey()), "");
        ParameterValidatorResult clientLoginHintResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.LOGIN_HINT.getKey()), "");
        ParameterValidatorResult clientIdTokenHintResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.ID_TOKEN_HINT.getKey()), "");

        // if Loginbuddys response should not include 'real' access_token or refresh_token it will create fake ones. Useful for demo purposes that should not display the original values
        ParameterValidatorResult obfuscateTokenResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.OBFUSCATE_TOKEN.getKey()), "false");


// ***************************************************************
// ** Let's start with checking for a valid client_id
// ***************************************************************

        if (!clientIdResult.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
            LOGGER.warning("Missing or multiple client_id parameters given!");
            handleError(400, "Missing or multiple client_id parameters given!", response);
            return;
        }

        ClientAuthenticator.ClientCredentialsResult clientValidationResult = handleClientValidation(clientIdResult, clientSecretResult, request.getHeader(Constants.AUTHORIZATION.getKey()));
        if (!clientValidationResult.isValid()) {
            LOGGER.warning(clientValidationResult.getErrorMsg());
            handleError(400, clientValidationResult.getErrorMsg(), response);
            return;
        }
        ClientConfig cc = clientValidationResult.getClientConfig();

// ***************************************************************
// ** Check the given redirect_uri. Confidential clients only need to have one registered but not passed in
// ***************************************************************

        if (clientRedirectUriResult.getResult().equals(ParameterValidatorResult.RESULT.MULTIPLE)) {
            LOGGER.warning("Too many redirect_uri parameters given!");
            handleError(400, "Too many redirect_uri parameters given!", response);
            return;
        }

        boolean checkRedirectUri = true;
        String clientRedirectUri = clientRedirectUriResult.getValue();
        if (clientRedirectUri == null) {
            if (Constants.CLIENT_TYPE_PUBLIC.getKey().equals(cc.getClientType())) {
                LOGGER.warning("Missing redirect_uri parameter!");
                handleError(400, "Missing redirect_uri parameter!", response);
                return;
            } else if (cc.getRedirectUri().split("[,; ]").length != 1) {
                LOGGER.warning("Missing redirect_uri parameter!");
                handleError(400, "Missing redirect_uri parameter!", response);
                return;
            } else {
                // confidential clients only need a registered redirectUri and not need to request it UNLESS multiple ones were registered
                clientRedirectUri = cc.getRedirectUri();
                checkRedirectUri = false; // it was not given, so no need to check for it at the token endpoint
            }
        }
        if (Stream.of(cc.getRedirectUri().split("[,; ]")).noneMatch(clientRedirectUri::equals)) {
            LOGGER.warning(String.format("Invalid redirect_uri: %s", clientRedirectUri));
            handleError(400, String.format("Invalid redirect_uri: %s", Sanetizer.checkForUrlPattern(clientRedirectUri, 256)), response);
            return;
        }

// ***************************************************************
// ** As of here we can return errors to the clients redirect_uri
// ***************************************************************

// ***************************************************************
// ** Build the redirect_uri for success and error cases including the optional state parameter
// ***************************************************************

        String clientRedirectUriValid;
        if (clientRedirectUri.contains("?")) {
            clientRedirectUriValid = clientRedirectUri.concat("&");
        } else {
            clientRedirectUriValid = clientRedirectUri.concat("?");
        }

        if (clientStateResult.getResult().equals(ParameterValidatorResult.RESULT.MULTIPLE)) {
            LOGGER.warning("Multiple state parameters received!");
            handleError(301, HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request", "multiple state parameters received"), response);
            return;
        }

        clientRedirectUriValid = "".equals(clientStateResult.getValue()) ? clientRedirectUriValid
                : clientRedirectUriValid.concat("state=").concat(clientStateResult.getValue()).concat("&");

// ***************************************************************
// ** Check the given response type
// ***************************************************************

        if (!clientResponseTypeResult.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
            LOGGER.warning("The given response_type parameter is invalid or was provided multiple times");
            handleError(301, HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request",
                    "invalid or unsupported response_type parameter or value"), response);
            return;
        } else if (Stream.of((DiscoveryConfig.CONFIG.getResponseTypesSupported()))
                .noneMatch(clientResponseTypeResult.getValue()::equals)) {
            LOGGER.warning(
                    String.format("The given response_type is not supported: %s", clientResponseTypeResult.getValue()));
            handleError(301, HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request", String.format("unsupported response_type: %s", Sanetizer.sanetize(clientResponseTypeResult.getValue()))), response);
            return;
        }

        if (clientProviderResult.getResult().equals(ParameterValidatorResult.RESULT.MULTIPLE)) {
            LOGGER.warning("Multiple provider parameter!");
            handleError(301, HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request", "multiple provider parameter"), response);
            return;
        }

// ***************************************************************
// ** PKCE: if it was used it has to be used at the /token endpoint. Remember if it was used
// ***************************************************************

        if (clientCodeChallengeResult.getResult().equals(ParameterValidatorResult.RESULT.MULTIPLE)) {
            LOGGER.warning("Multiple code_challenge parameters found!");
            handleError(301, HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request", "multiple code_challenge parameters found"), response);
            return;
        }
        if (clientCodeChallengeResult.getResult().equals(ParameterValidatorResult.RESULT.VALID) && !Pkce
                .verifyChallenge(clientCodeChallengeResult.getValue())) {
            LOGGER.warning("Invalid code_challenge!");
            handleError(301, HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request", "invalid code_challenge"), response);
            return;
        }

        if (clientCodeChallendeMethodResult.getResult().equals(ParameterValidatorResult.RESULT.MULTIPLE)) {
            LOGGER.warning("Multiple code_challenge_method parameters found!");
            handleError(301, HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request","multiple code_challenge_method parameters found!"), response);
            return;
        }

        if (clientCodeChallendeMethodResult.getResult().equals(ParameterValidatorResult.RESULT.VALID) && !Pkce.CODE_CHALLENGE_METHOD_S256
                .equals(clientCodeChallendeMethodResult.getValue())) {
            LOGGER.warning("Unsupported code_challenge_method parameter!");
            handleError(301, HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request","unsupported code_challenge_method parameter"), response);
            return;
        }

// ***************************************************************
// ** Check the given scope
// ***************************************************************

        // TODO somehow tie incoming and outgoing SCOPE values together. 'Initialize' uses SCOPEs independent of these ones
        String clientScope = scopeResult.getValue();
        if (clientScope == null) {
            if (Constants.CLIENT_TYPE_CONFIDENTIAL.getKey().equals(cc.getClientType())) {
                clientScope = DiscoveryConfig.CONFIG.getScopesSupportedAsString();
            } else {
                LOGGER.warning("Invalid or unsupported scope parameter!");
                handleError(301, HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request", "invalid or unsupported scope parameter"), response);
                return;
            }
        }

        Set<String> scopes = new TreeSet<>(
                Arrays.asList((DiscoveryConfig.CONFIG.getScopesSupportedAsString()).split("[,; ]")));
        scopes.retainAll(Arrays.asList(clientScope.split("[,; ]")));
        if (scopes.size() == 0) {
            LOGGER.warning("Invalid or unsupported scope!");
            handleError(301, HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request", "invalid or unsupported scope value"), response);
            return;
        }

// ***************************************************************
// ** Check if the client should receive a signed response
// ***************************************************************

        String signResponseAlg = "";
        if (cc.getSignedResponseAlg() != null) {
            Set<String> signedResponseAlgs = new TreeSet<>(Arrays.asList((DiscoveryConfig.CONFIG.getSigningAlgValuesSupportedAsString()).split("[,; ]")));
            if (signedResponseAlgs.contains(cc.getSignedResponseAlg())) {
                signResponseAlg = cc.getSignedResponseAlg();
            }
            // TODO: do we need logging for informational purposes?
        }

// ***************************************************************
// ** Create the session so that it can be handled throughout multiple requests
// ***************************************************************

        String parRequestUri = null;
        boolean isPar = Boolean.valueOf( request.getAttribute("par") == null ? false : (Boolean)request.getAttribute("par"));
        SessionContext sessionCtx = new SessionContext();
        if(isPar) {
            parRequestUri = String.format("urn:loginbuddy:%s", sessionCtx.getId());
        }
        sessionCtx.setSessionInit(clientIdResult.getValue(), clientScope, clientResponseTypeResult.getValue(),
                clientCodeChallengeResult.getValue(), clientCodeChallendeMethodResult.getValue(),
                clientRedirectUri, clientNonceResult.getValue(), clientStateResult.getValue(), clientProviderResult.getValue(),
                clientPromptResult.getValue(), clientLoginHintResult.getValue(), clientIdTokenHintResult.getValue(),
                checkRedirectUri, clientRedirectUriValid, cc.isAcceptDynamicProvider(), signResponseAlg, obfuscateTokenResult.getBooleanValue(), parRequestUri);

        LoginbuddyCache.CACHE.put(sessionCtx.getId(), sessionCtx, LoginbuddyConfig.CONFIGS.getPropertiesUtil().getLongProperty("lifetime.oauth.authcode.loginbuddy.flow"));

// ***************************************************************
// ** Present the provider selection page if non was given in this request. Otherwise, fast forward
// ***************************************************************

        if(isPar) {
            response.setContentType("application/json");
            response.setStatus(201);
            JSONObject obj = new JSONObject();
            obj.put(Constants.REQUEST_URI.getKey(), parRequestUri);
            obj.put(Constants.EXPIRES_IN.getKey(), LoginbuddyConfig.CONFIGS.getPropertiesUtil().getLongProperty("lifetime.oauth.authcode.loginbuddy.flow"));
            response.getWriter().write(obj.toJSONString());
        } else {
            handleAuthorizationResponse(request, response, sessionCtx, clientProviderResult.getValue());
        }
    }

    private void handleAuthorizationResponse(HttpServletRequest request, HttpServletResponse response, SessionContext sessionCtx, Object value) throws ServletException, IOException {
        if ("".equals(value)) {
            request.getRequestDispatcher(String.format("/iapis/providers.jsp?session=%s", sessionCtx.getId()))
                    .forward(request, response);
        } else {
            String hostname = DiscoveryConfig.CONFIG.getIssuer();
            response.sendRedirect(String.format("%s/initialize?session=%s", hostname, (sessionCtx.getId())));
        }
    }

    protected abstract void handleError(int httpStatus, String errorMsg, HttpServletResponse response) throws IOException;
    protected abstract ClientAuthenticator.ClientCredentialsResult handleClientValidation(ParameterValidatorResult clientIdResult, ParameterValidatorResult clientSecretResult, String authorizationHeader);
}