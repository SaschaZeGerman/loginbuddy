package net.loginbuddy.config;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.api.PostRequest;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.cache.LoginbuddyContext;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.storage.LoginbuddyStorage;
import net.loginbuddy.common.util.*;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.loginbuddy.Loginbuddy;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.config.loginbuddy.Providers;
import net.loginbuddy.config.loginbuddy.handler.LoginbuddyHandler;
import net.loginbuddy.config.loginbuddy.handler.OidcdrLoginbuddyHandler;
import net.loginbuddy.config.properties.PropertiesUtil;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static net.loginbuddy.common.api.HttpHelper.postMessage;

public class HeadOfInitialize {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(HeadOfInitialize.class));

    public static String processInitializeRequest(
            LoginbuddyContext sessionCtx,
            ParameterValidatorResult providerResult,
            ParameterValidatorResult issuerResult,
            ParameterValidatorResult discoveryUrlResult) {

// ***************************************************************
// ** Check if a provider was selected
// ***************************************************************

        String selectedProvider = (sessionCtx.getString(Constants.CLIENT_PROVIDER.getKey())).trim();
        if ("".equals(selectedProvider)) {
            if (providerResult.getResult().equals(RESULT.VALID)) {
                selectedProvider = providerResult.getValue();
            } else {
                LOGGER.warning("No provider has been selected or an invalid parameters has been given");
                return HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "invalid_request",
                        "No provider has been selected or an invalid parameters has been given");
            }
        }

// ***************************************************************
// ** Find the provider configuration. We may register one dynamically
// ***************************************************************

        Providers providers = null;
        try {

            // dynamic registration of unknown provider
            // we need: 'dynamic_provider', and 'issuer', client must be registered to accept dynamic provider
            if (HttpHelper.checkForDynamicProvider(selectedProvider, issuerResult, discoveryUrlResult, sessionCtx.getBoolean(Constants.CLIENT_ACCEPT_DYNAMIC_PROVIDER.getKey()))) {
                LoginbuddyHandler loginbuddyHandler = new OidcdrLoginbuddyHandler();
                Map<String, Providers> dynamicProviders = (Map<String, Providers>) LoginbuddyStorage.STORAGE.get(Constants.PROVIDER_DYNAMIC_REGISTRATION.getKey());
                if(dynamicProviders == null) {
                    dynamicProviders = new HashMap<>();
                }
                if(dynamicProviders.get(issuerResult.getValue()) == null) {
                    MsgResponse msg = registerProviderViaOIDCDR(issuerResult.getValue(), discoveryUrlResult.getValue(), loginbuddyHandler);
                    if (msg.getStatus() == 200) {
                        providers = LoginbuddyUtil.UTIL.getProviderConfigFromJsonString(msg.getMsg());
                        dynamicProviders.put(issuerResult.getValue(), providers);
                        LoginbuddyStorage.STORAGE.put(Constants.PROVIDER_DYNAMIC_REGISTRATION.getKey(), dynamicProviders);
                    } else {
                        LOGGER.warning(msg.getMsg());
                        JSONObject resp = (JSONObject) new JSONParser().parse(msg.getMsg());
                        return HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "invalid_request", (String) resp.get("error_description"));
                    }
                } else {
                    providers = dynamicProviders.get(issuerResult.getValue());
                }
                sessionCtx.put(Constants.ISSUER_HANDLER.getKey(), loginbuddyHandler);
            } else {
                // null if the given provider is not available to the client
                providers = LoginbuddyUtil.UTIL.getProviders(sessionCtx.getString(Constants.CLIENT_CLIENT_ID.getKey()), selectedProvider);
            }

            // TODO if above if statement would be 'true' but accept_dynamic is false something is wrong ...
            if (providers == null || !providers.isUsable()) {
                LOGGER.warning("The given provider is unknown or invalid");
                return HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "invalid_request", "The given provider is unknown or invalid");
            } else {
                selectedProvider = providers.getProvider(); // overwriting the dynamic provider from 'dynamic_provider' to the 'real' value (provider==issuer) for the case dynamic registration was used
                sessionCtx.put(Constants.CLIENT_PROVIDER.getKey(), selectedProvider);
            }
        } catch (Exception e) {
            LOGGER.severe(String.format("The system may not be configured yet! Error: '%s'", e.getMessage()));
            return HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "server_error", String.format("The system may not be configured yet! Error: '%s'", e.getMessage()));
        }

// ***************************************************************
// ** Build the authorization URL and set protocol client and endpoint values
// ***************************************************************

        StringBuilder authorizeUrl = new StringBuilder();
        authorizeUrl.append(providers.getAuthorizationEndpoint());
        sessionCtx.put(Constants.PROVIDER_CLIENT_ID.getKey(), providers.getClientId());
        sessionCtx.put(Constants.PROVIDER_CLIENT_SECRET.getKey(), providers.getClientSecret());
        sessionCtx.put(Constants.PROVIDER_REDIRECT_URI.getKey(), providers.getRedirectUri());
        sessionCtx.put(Constants.TOKEN_ENDPOINT.getKey(), providers.getTokenEndpoint());
        sessionCtx.put(Constants.JWKS_URI.getKey(), providers.getJwksUri());
        sessionCtx.put(Constants.USERINFO_ENDPOINT.getKey(), providers.getUserinfoEndpoint());

// ***************************************************************
// ** use PKCE only if the provider supports it. Unfortunately, some providers fail if unsupported parameters are being send
// ***************************************************************

        String pkce = null;
        if (providers.getPkce()) {
            PkcePair pair = Pkce.create(Pkce.CODE_CHALLENGE_METHOD_S256);
            sessionCtx.put(Constants.CODE_VERIFIER.getKey(), pair.getVerifier());
            pkce = String.format("&%s=%s&%s=S256", Constants.CODE_CHALLENGE.getKey(), pair.getChallenge(),
                    Constants.CODE_CHALLENGE_METHOD.getKey());
        }

// ***************************************************************
// ** some providers do expect 'response_mode' for certain SCOPE combinations. Include it if specified
// ***************************************************************

        String responseMode = providers.getResponseMode();
        if (Constants.RESPONSE_MODE_QUERY.getKey().equalsIgnoreCase(responseMode) || Constants.RESPONSE_MODE_FORM_POST.getKey().equalsIgnoreCase(responseMode)) {
            responseMode = "&response_mode=" + responseMode;
        }

// ***************************************************************
// ** Create the authorization URL to redirect the client
// ***************************************************************

        // do not include empty parameters. Some providers will fail the request if included
        String prompt = "".equals(sessionCtx.getString(Constants.CLIENT_PROMPT.getKey())) ? "" : "&" + Constants.PROMPT.getKey() + "=" + HttpHelper.urlEncode(sessionCtx.getString(Constants.CLIENT_PROMPT.getKey()));
        String loginHint = "".equals(sessionCtx.getString(Constants.CLIENT_LOGIN_HINT.getKey())) ? "" : "&" + Constants.LOGIN_HINT.getKey() + "=" + HttpHelper.urlEncode(sessionCtx.getString(Constants.CLIENT_LOGIN_HINT.getKey()));
        String idTokenHint = "".equals(sessionCtx.getString(Constants.CLIENT_ID_TOKEN_HINT.getKey())) ? "" : "&" + Constants.ID_TOKEN_HINT.getKey() + "=" + HttpHelper.urlEncode(sessionCtx.getString(Constants.CLIENT_ID_TOKEN_HINT.getKey()));

        StringBuilder queryParams = new StringBuilder();
        queryParams.append(Constants.CLIENT_ID.getKey()).append("=").append(HttpHelper.urlEncode(providers.getClientId())).
                append("&").append(Constants.RESPONSE_TYPE.getKey()).append("=").append(HttpHelper.urlEncode(providers.getResponseType()))
                .append("&").append(Constants.SCOPE.getKey()).append("=").append(HttpHelper.urlEncode(providers.getScope()))
                .append("&").append(Constants.NONCE.getKey()).append("=").append(HttpHelper.urlEncode((String) sessionCtx.get(Constants.CLIENT_NONCE.getKey())))
                .append("&").append(Constants.REDIRECT_URI.getKey()).append("=").append(HttpHelper.urlEncode(providers.getRedirectUri()))
                .append(pkce == null ? "" : pkce)
                .append(responseMode == null ? "" : responseMode)
                .append(prompt)
                .append(loginHint)
                .append(idTokenHint)
                .append("&").append(Constants.STATE.getKey()).append("=").append(sessionCtx.getId())
                .append(providers.isDpopEnabled() ? String.format("&dpop_jkt=%s", HttpHelper.urlEncode(Jwt.DEFAULT.getDpopJkt(providers.getDpopSigningAlg()))) : "");

        authorizeUrl.append("?");

// ***************************************************************
// ** prioritise PAR if the OP supports it
// ***************************************************************

        if (providers.getPushedAuthorizationRequestEndpoint() != null) {
            try {
                if (providers.getClientSecret() != null) {
                    queryParams.append("&").append(Constants.CLIENT_SECRET.getKey()).append("=").append(HttpHelper.urlEncode(providers.getClientSecret()));
                }
                LoginbuddyHandler loginbuddyHandler = (LoginbuddyHandler)sessionCtx.get(Constants.ISSUER_HANDLER.getKey());
                HttpPost req = providers.isDpopEnabled() ?
                        PostRequest.create(loginbuddyHandler.getPAuthorizeApi(providers.getPushedAuthorizationRequestEndpoint(), true))
                                .setAcceptType("application/json")
                                .setUrlEncodedParametersPayload(queryParams.toString())
                                .setDpopHeader(
                                        providers.getDpopSigningAlg(),
                                        loginbuddyHandler.getPAuthorizeApi(providers.getPushedAuthorizationRequestEndpoint(), false),
                                        null,
                                        sessionCtx.getString(Constants.DPOP_NONCE_HEADER.getKey()),
                                        sessionCtx.getString(Constants.DPOP_NONCE_HEADER_PROVIDER.getKey()))
                                .build() :
                        PostRequest.create(loginbuddyHandler.getPAuthorizeApi(providers.getPushedAuthorizationRequestEndpoint(), true))
                                .setAcceptType("application/json")
                                .setUrlEncodedParametersPayload(queryParams.toString())
                                .build();
                MsgResponse msgResponse = postMessage(req, "application/json");
                if (msgResponse.getHeader(Constants.DPOP_NONCE_HEADER.getKey()) != null) {
                    sessionCtx.put(Constants.DPOP_NONCE_HEADER.getKey(), msgResponse.getHeader(Constants.DPOP_NONCE_HEADER.getKey()));
                    sessionCtx.put(Constants.DPOP_NONCE_HEADER_PROVIDER.getKey(), Sanetizer.getDomain(loginbuddyHandler.getPAuthorizeApi(providers.getPushedAuthorizationRequestEndpoint(), false)));
                }
                JSONObject obj = (JSONObject) new JSONParser().parse(msgResponse.getMsg());
                if (msgResponse.getStatus() > 204) {
                    LOGGER.warning(String.format("The PAR request failed: %s\n", obj.get("error_description")));
                    return HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), (String) obj.get("error"), (String) obj.get("error_description"));
                }
                authorizeUrl.append(Constants.REQUEST_URI.getKey()).append("=").append(HttpHelper.urlEncode((String) obj.get(Constants.REQUEST_URI.getKey())));
                authorizeUrl.append("&").append(Constants.CLIENT_ID.getKey()).append("=").append(HttpHelper.urlEncode(providers.getClientId()));
            } catch (Exception e) {
                LOGGER.warning(String.format("PAR request failed, attempting to use an authorization_code flow. Error: %s", e.getMessage()));
                authorizeUrl.append(queryParams);
            }
        } else {
            authorizeUrl.append(queryParams);
        }

// ***************************************************************
// ** Finalize and update the session details
// ***************************************************************

        sessionCtx.setSessionCallback(Constants.valueOf(providers.getResponseType().toUpperCase()));

        LoginbuddyCache.CACHE.put(sessionCtx.getId(), sessionCtx, PropertiesUtil.UTIL.getLongProperty("lifetime.oauth.authcode.provider.flow"));

        return authorizeUrl.toString();
    }

    /**
     * Register loginbuddy at the provider and remember a few details for later use
     */
    private static MsgResponse registerProviderViaOIDCDR(String issuer, String discoveryUrl, LoginbuddyHandler loginbuddyHandler)
            throws IOException {

        List<NameValuePair> formParameters = new ArrayList<>();
        formParameters.add(new BasicNameValuePair(Constants.ISSUER.getKey(), issuer));
        formParameters.add(new BasicNameValuePair(Constants.DISCOVERY_URL.getKey(), discoveryUrl));
        formParameters.add(new BasicNameValuePair(Constants.REDIRECT_URI.getKey(), DiscoveryUtil.UTIL.getRedirectUri()));
        return postMessage(formParameters, loginbuddyHandler.getRegistrationApi(), "application/json");
    }
}
