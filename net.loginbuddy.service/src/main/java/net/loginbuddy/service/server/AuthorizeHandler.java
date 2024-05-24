package net.loginbuddy.service.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.Pkce;
import net.loginbuddy.common.util.Sanetizer;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.loginbuddy.Clients;
import net.loginbuddy.config.properties.PropertiesUtil;
import net.loginbuddy.service.util.SessionContext;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.Arrays;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Logger;
import java.util.stream.Stream;

public abstract class AuthorizeHandler extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(AuthorizeHandler.class.getName());

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

// ***************************************************************
// ** Potentially used with sidecar requests, ignored otherwise since these values would be configured in client configurations
// ***************************************************************

        ParameterValidatorResult signedResponseAlgResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.SIGNED_RESPONSE_ALG.getKey()));
        ParameterValidatorResult acceptDynamicProviderResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.ACCEPT_DYNAMIC_PROVIDER.getKey()));

// ***************************************************************
// ** Let's start with checking for valid client credentials
// ***************************************************************

        ParameterValidatorResult clientIdResult = getClientIdResult(request, response);
        // simple check first
        if (!clientIdResult.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
            LOGGER.warning("Missing or multiple client_id parameters given!");
            handleError(400, "Missing or multiple client_id parameters given!", response);
            return;
        }
        // only needed for a PAR call (.../pauthorize)
        ParameterValidatorResult clientSecretResult = ParameterValidator.getSingleValue(request.getParameterValues(Constants.CLIENT_SECRET.getKey()));
        ClientAuthenticator.ClientCredentialsResult clientValidationResult =
                handleClientValidation(clientIdResult, clientSecretResult, request.getHeader(Constants.AUTHORIZATION.getKey()), signedResponseAlgResult.getValue(), acceptDynamicProviderResult.getBooleanValue());
        if (!clientValidationResult.isValid()) {
            LOGGER.warning(clientValidationResult.getErrorMsg());
            handleError(400, clientValidationResult.getErrorMsg(), response);
            return;
        }
        Clients cc = clientValidationResult.getClients();

        // Check if this request includes a request_uri. If so, it is a PAR request and needs some attention.
        // The authorization response is also created and returned to the client; processing ends here
        if (handleParRequestUriRequest(request, response, clientIdResult)) return;

        ParameterValidatorResult clientResponseTypeResult = getResponseTypeResult(request, response);
        ParameterValidatorResult clientRedirectUriResult = getRedirectUriResult(request, response);
        ParameterValidatorResult clientProviderResult = ParameterValidator.getSingleValue(request.getParameterValues(Constants.PROVIDER.getKey()), "");
        ParameterValidatorResult clientStateResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.STATE.getKey()), "");
        ParameterValidatorResult scopeResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.SCOPE.getKey()));
        ParameterValidatorResult clientNonceResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.NONCE.getKey()));

        // these four parameters are forwarded to the provider, not handled by Loginbuddy
        ParameterValidatorResult clientPromptResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.PROMPT.getKey()), "");
        ParameterValidatorResult clientLoginHintResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.LOGIN_HINT.getKey()), "");
        ParameterValidatorResult clientIdTokenHintResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.ID_TOKEN_HINT.getKey()), "");
        ParameterValidatorResult authorizationDetailsResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.AUTHORIZATION_DETAILS.getKey()), "");
        if(!"".equals(authorizationDetailsResult.getValue())) {
            try {
                new JSONParser().parse(authorizationDetailsResult.getValue());
            } catch (ParseException e) {
                handleError(400, "The given authorization_details are not in JSON format!", response);
                return;
            }
        }

        // if Loginbuddys response should not include 'real' access_token or refresh_token it will create fake ones. Useful for demo purposes that should not display the original values
        ParameterValidatorResult obfuscateTokenResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.OBFUSCATE_TOKEN.getKey()), "false");

        ParameterValidatorResult clientCodeChallengeResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CODE_CHALLENGE.getKey()));
        ParameterValidatorResult clientCodeChallendeMethodResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CODE_CHALLENGE_METHOD.getKey()));

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
            } else if (cc.getRedirectUrisCount() != 1) {
                LOGGER.warning("Missing redirect_uri parameter!");
                handleError(400, "Missing redirect_uri parameter!", response);
                return;
            } else {
                // confidential clients only need a registered redirectUri and not need to request it UNLESS multiple ones were registered
                clientRedirectUri = cc.getRedirectUri();
                checkRedirectUri = false; // it was not given, so no need to check for it at the token endpoint
            }
        }
        if (!cc.isRegisteredRedirectUri(clientRedirectUri)) {
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
        if (clientRedirectUri.endsWith("?")) {
            // do nothing, attach parameters directly
            clientRedirectUriValid = clientRedirectUri;
        } else if (clientRedirectUri.contains("?")) {
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
        } else if (Stream.of((DiscoveryUtil.UTIL.getResponseTypesSupported()))
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
            handleError(301, HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request", "multiple code_challenge_method parameters found!"), response);
            return;
        }

        if (clientCodeChallendeMethodResult.getResult().equals(ParameterValidatorResult.RESULT.VALID) && !Pkce.CODE_CHALLENGE_METHOD_S256
                .equals(clientCodeChallendeMethodResult.getValue())) {
            LOGGER.warning("Unsupported code_challenge_method parameter!");
            handleError(301, HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request", "unsupported code_challenge_method parameter"), response);
            return;
        }

// ***************************************************************
// ** Check the given scope
// ***************************************************************

        // TODO somehow tie incoming and outgoing SCOPE values together. 'Initialize' uses SCOPEs independent of these ones
        String clientScope = scopeResult.getValue();
        if (clientScope == null) {
            if (Constants.CLIENT_TYPE_CONFIDENTIAL.getKey().equals(cc.getClientType())) {
                clientScope = DiscoveryUtil.UTIL.getScopesSupportedAsString();
            } else {
                LOGGER.warning("Invalid or unsupported scope parameter!");
                handleError(301, HttpHelper.getErrorForRedirect(clientRedirectUriValid, "invalid_request", "invalid or unsupported scope parameter"), response);
                return;
            }
        }

        Set<String> scopes = new TreeSet<>(
                Arrays.asList((DiscoveryUtil.UTIL.getScopesSupportedAsString()).split("[,; ]")));
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
            Set<String> signedResponseAlgs = new TreeSet<>(Arrays.asList((DiscoveryUtil.UTIL.getSigningAlgValuesSupportedAsString()).split("[,; ]")));
            if (signedResponseAlgs.contains(cc.getSignedResponseAlg())) {
                signResponseAlg = cc.getSignedResponseAlg();
            }
            // TODO: do we need logging for informational purposes?
        }
        boolean acceptDynamicProvider = cc.isAcceptDynamicProvider();
        createSessionAndResponse(
                request,
                response,
                clientIdResult.getValue(),
                clientScope,
                clientResponseTypeResult.getValue(),
                clientCodeChallengeResult.getValue(),
                clientCodeChallendeMethodResult.getValue(),
                clientRedirectUri,
                clientNonceResult.getValue(),
                clientStateResult.getValue(),
                clientProviderResult,
                clientPromptResult.getValue(),
                clientLoginHintResult.getValue(),
                clientIdTokenHintResult.getValue(),
                checkRedirectUri,
                clientRedirectUriValid,
                acceptDynamicProvider,
                signResponseAlg,
                obfuscateTokenResult.getBooleanValue(),
                authorizationDetailsResult.getValue());
    }

    protected void createSessionAndResponse(HttpServletRequest request, HttpServletResponse response, String clientId, String clientScope, String clientResponseType, String clientCodeChallenge, String clientCodeChallendeMethod, String clientRedirectUri, String clientNonce, String clientState, ParameterValidatorResult clientProviderResult, String clientPrompt, String clientLoginHint, String clientIdTokenHint, boolean checkRedirectUri, String clientRedirectUriValid, boolean acceptDynamicProvider, String signResponseAlg, boolean obfuscateToken, String authorizationDetails) throws IOException, ServletException {

        // ***************************************************************
        // ** Create the session so that it can be handled throughout multiple requests
        // ***************************************************************

        boolean isPar = Boolean.valueOf(request.getAttribute("par") == null ? false : (Boolean) request.getAttribute("par"));
        SessionContext sessionCtx = new SessionContext();
        String parRequestUri = null;
        if (isPar) {
            parRequestUri = String.format("urn:loginbuddy:%s", sessionCtx.getId());
        }
        sessionCtx.setSessionInit(
                clientId,
                clientScope,
                clientResponseType,
                clientCodeChallenge,
                clientCodeChallendeMethod,
                clientRedirectUri,
                clientNonce,
                clientState,
                clientProviderResult.getValue(),
                clientPrompt,
                clientLoginHint,
                clientIdTokenHint,
                checkRedirectUri,
                clientRedirectUriValid,
                acceptDynamicProvider,
                signResponseAlg,
                obfuscateToken,
                parRequestUri,
                authorizationDetails);

        LoginbuddyCache.CACHE.put(sessionCtx.getId(), sessionCtx, PropertiesUtil.UTIL.getLongProperty("lifetime.oauth.authcode.loginbuddy.flow"));

// ***************************************************************
// ** Present the provider selection page if none was given in this request. Otherwise, fast forward
// ***************************************************************

        if (isPar) {
            response.setContentType("application/json");
            response.setStatus(201);
            JSONObject obj = new JSONObject();
            obj.put(Constants.REQUEST_URI.getKey(), parRequestUri);
            obj.put(Constants.EXPIRES_IN.getKey(), PropertiesUtil.UTIL.getLongProperty("lifetime.oauth.authcode.loginbuddy.flow"));
            response.getWriter().write(obj.toJSONString());
        } else {
            handleAuthorizationResponse(request, response, sessionCtx, clientProviderResult.getValue());
        }
    }

    private boolean handleParRequestUriRequest(HttpServletRequest request, HttpServletResponse response, ParameterValidatorResult clientIdResult) throws IOException, ServletException {
        ParameterValidatorResult requestUriResult = ParameterValidator.getSingleValue(request.getParameterValues(Constants.REQUEST_URI.getKey()));
        if (requestUriResult.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
            String[] parRequestUriParts = requestUriResult.getValue().split(":");
            if (parRequestUriParts.length == 3) {
                SessionContext sessionCtx = (SessionContext) LoginbuddyCache.CACHE.get(parRequestUriParts[2]);
                if (sessionCtx == null || !parRequestUriParts[2].equals(sessionCtx.getId())) {
                    LOGGER.warning("The current session is invalid or it has expired! Given: '" + parRequestUriParts[2] + "'");
                    handleError(400, "The current session is invalid or it has expired!", response);
                    return true;
                }
                // need to verify that the given requestUri is the one that belongs to this session
                if (requestUriResult.getValue() != null && requestUriResult.getValue().equals(sessionCtx.useParRequestUri())) {
                    if (clientIdResult.getValue().equals(sessionCtx.getString(Constants.CLIENT_CLIENT_ID.getKey()))) {
                        LoginbuddyCache.CACHE.put(sessionCtx.getId(), sessionCtx, PropertiesUtil.UTIL.getLongProperty("lifetime.oauth.authcode.loginbuddy.flow"));
                        handleAuthorizationResponse(request, response, sessionCtx, sessionCtx.get(Constants.CLIENT_PROVIDER.getKey()));
                    } else {
                        handleError(400, "invalid client_id", response);
                    }
                } else {
                    handleError(400, "invalid request_uri", response);
                }
            } else {
                handleError(400, "invalid request_uri", response);
            }
            return true;
        }
        return false;
    }

    protected ParameterValidatorResult getClientIdResult(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return ParameterValidator.getSingleValue(request.getParameterValues(Constants.CLIENT_ID.getKey()));
    }

    protected ParameterValidatorResult getResponseTypeResult(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return ParameterValidator.getSingleValue(request.getParameterValues(Constants.RESPONSE_TYPE.getKey()));
    }

    protected ParameterValidatorResult getRedirectUriResult(HttpServletRequest request, HttpServletResponse response) throws IOException {
        return ParameterValidator.getSingleValue(request.getParameterValues(Constants.REDIRECT_URI.getKey()));
    }

    protected void handleAuthorizationResponse(HttpServletRequest request, HttpServletResponse response, SessionContext sessionCtx, Object value) throws ServletException, IOException {
        if ("".equals(value)) {
            // present a page to users to choose a provider, goto .../initialize (as below) afterwards
            request.getRequestDispatcher(String.format("/iapis/providers.jsp?session=%s", sessionCtx.getId()))
                    .forward(request, response);
        } else {
            String hostname = DiscoveryUtil.UTIL.getIssuer();
            response.sendRedirect(String.format("%s/initialize?session=%s", hostname, (sessionCtx.getId())));
        }
    }

    protected abstract void handleError(int httpStatus, String errorMsg, HttpServletResponse response) throws IOException;

    protected abstract ClientAuthenticator.ClientCredentialsResult handleClientValidation(ParameterValidatorResult clientIdResult, ParameterValidatorResult clientSecretResult, String authorizationHeader, String signedResponseAlg, boolean acceptDynamicProvider);
}