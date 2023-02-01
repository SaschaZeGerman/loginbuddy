package net.loginbuddy.config;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.cache.LoginbuddyContext;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import net.loginbuddy.common.util.Pkce;
import net.loginbuddy.common.util.PkcePair;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.config.loginbuddy.Providers;
import net.loginbuddy.config.properties.PropertiesUtil;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class HeadOfInitialize {

  private static final Logger LOGGER = Logger.getLogger(String.valueOf(HeadOfInitialize.class));

  public static String processInitializeRequest(
          LoginbuddyContext sessionCtx,
          ParameterValidatorResult providerResult,
          ParameterValidatorResult issuerResult,
          ParameterValidatorResult discoveryUrlResult) {

// ***************************************************************
// ** Check if a provider was chosen
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
        providers = registerProvider(issuerResult.getValue(), discoveryUrlResult.getValue(), sessionCtx);
        if (providers != null) {
          selectedProvider = providers.getProvider(); // overwriting the provider from 'dynamic_provider' to the 'real' value (provider==issuer)
        }
      } else {
        // null if the given provider is not available to the client
        providers = LoginbuddyUtil.UTIL.getProviders(sessionCtx.getString(Constants.CLIENT_CLIENT_ID.getKey()), selectedProvider);
      }

      // TODO if above if statement would be 'true' but accept_dynamic is false something is wrong ...
      if (providers == null || !providers.isUsable()) {
        LOGGER.warning("The given provider is unknown or invalid");
        return HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "invalid_request", "The given provider is unknown or invalid");
      } else {
        sessionCtx.put(Constants.CLIENT_PROVIDER.getKey(), selectedProvider);
      }
    } catch (Exception e) {
      LOGGER.severe(String.format("The system may not be configured yet! Error: '%s'", e.getMessage()));
      return HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "server_error", String.format("The system may not be configured yet! Error: '%s'", e.getMessage()));
    }

// ***************************************************************
// ** Retrieve the OpenID Configuration
// ***************************************************************

    JSONObject oidcConfig = null;
    String oidcConfigUrl = providers.getOpenidConfigurationUri();
    if (oidcConfigUrl != null) {
      try {
        MsgResponse msg = HttpHelper.getAPI(oidcConfigUrl);
        if (msg.getStatus() == 200) {
          try {
            oidcConfig = (JSONObject) new JSONParser().parse(msg.getMsg());
          } catch (ParseException e) {
            // should never happen
            LOGGER.warning("For some unknown reason the OpenID Configuration could not be parsed as JSON object!");
            return
                HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "message_error",
                    "The OpenID Configuration could not be parsed!");
          }
        } else {
          LOGGER.warning(
              String.format("Requesting the OpenID Configuration caused an error. Given URL: %s, http status: %s", oidcConfigUrl, msg.getStatus()));
          return HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "message_error",
              "The OpenID Configuration could not be retrieved!");
        }
      } catch (IOException e) {
        LOGGER.warning(
            String.format("The OpenID Connect configuration could not be retrieved. Given URL: %s, error message: %s", oidcConfigUrl, e.getMessage()));
        return HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "message_error",
            "The OpenID Configuration could not be retrieved!");
      }
    }

// ***************************************************************
// ** Build the authorization URL and retrieve protocol endpoints
// ***************************************************************

    StringBuilder authorizeUrl = new StringBuilder();
    String providerTokenEndpoint, providerJwksEndpoint, providerUserinfoEndpoint;
    if (oidcConfig != null) {
      authorizeUrl.append(oidcConfig.get(Constants.AUTHORIZATION_ENDPOINT.getKey()).toString());
      providerTokenEndpoint = oidcConfig.get(Constants.TOKEN_ENDPOINT.getKey()).toString();
      providerJwksEndpoint = oidcConfig.get(Constants.JWKS_URI.getKey()).toString();
      providerUserinfoEndpoint = oidcConfig.get(Constants.USERINFO_ENDPOINT.getKey()).toString();

    } else {
      // using the configured URLs since a well-known endpoint has not been configured
      authorizeUrl.append(providers.getAuthorizationEndpoint());
      providerTokenEndpoint = providers.getTokenEndpoint();
      providerJwksEndpoint = providers.getJwksUri();
      providerUserinfoEndpoint = providers.getUserinfoEndpoint();
    }

// ***************************************************************
// ** Prepare provider endpoints to be used via loginbuddy-oidcdr if dynamic provider registration was used
// ***************************************************************

    // if this provider was dynamically registered we'll delegate other requests to loginbuddy-oidcdr later also
    boolean isHandlerLoginbuddy = sessionCtx.get(Constants.ISSUER_HANDLER.getKey()).equals(Constants.ISSUER_HANDLER_LOGINBUDDY.getKey());
    sessionCtx.put(Constants.TOKEN_ENDPOINT.getKey(), isHandlerLoginbuddy ? providerTokenEndpoint : String
        .format("https://loginbuddy-oidcdr:445/oidcdr/token?%s=%s", Constants.TARGET_PROVIDER.getKey(), HttpHelper.urlEncode(providerTokenEndpoint)));
    sessionCtx.put(Constants.JWKS_URI.getKey(), isHandlerLoginbuddy ? providerJwksEndpoint : String
        .format("https://loginbuddy-oidcdr:445/oidcdr/jwks?%s=%s", Constants.TARGET_PROVIDER.getKey(), HttpHelper.urlEncode(providerJwksEndpoint)));
    sessionCtx.put(Constants.USERINFO_ENDPOINT.getKey(), isHandlerLoginbuddy ? providerUserinfoEndpoint : String
        .format("https://loginbuddy-oidcdr:445/oidcdr/userinfo?%s=%s", Constants.TARGET_PROVIDER.getKey(), HttpHelper.urlEncode(providerUserinfoEndpoint)));

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
    String cp = "".equals(sessionCtx.getString(Constants.CLIENT_PROMPT.getKey())) ? "" : "&" + Constants.PROMPT.getKey() + "=" + HttpHelper.urlEncode(sessionCtx.getString(Constants.CLIENT_PROMPT.getKey()));
    String lh = "".equals(sessionCtx.getString(Constants.CLIENT_LOGIN_HINT.getKey())) ? "" : "&" + Constants.LOGIN_HINT.getKey() + "=" + HttpHelper.urlEncode(sessionCtx.getString(Constants.CLIENT_LOGIN_HINT.getKey()));
    String ith = "".equals(sessionCtx.getString(Constants.CLIENT_ID_TOKEN_HINT.getKey())) ? "" : "&" + Constants.ID_TOKEN_HINT.getKey() + "=" + HttpHelper.urlEncode(sessionCtx.getString(Constants.CLIENT_ID_TOKEN_HINT.getKey()));

    StringBuilder queryParams = new StringBuilder();
    queryParams.append(Constants.CLIENT_ID.getKey()).append("=").append(HttpHelper.urlEncode(providers.getClientId())).
            append("&").append(Constants.RESPONSE_TYPE.getKey()).append("=").append(HttpHelper.urlEncode(providers.getResponseType()))
            .append("&").append(Constants.SCOPE.getKey()).append("=").append(HttpHelper.urlEncode(providers.getScope()))
            .append("&").append(Constants.NONCE.getKey()).append("=").append(HttpHelper.urlEncode((String)sessionCtx.get(Constants.CLIENT_NONCE.getKey())))
            .append("&").append(Constants.REDIRECT_URI.getKey()).append("=").append(HttpHelper.urlEncode(providers.getRedirectUri()))
            .append(pkce == null ? "" : pkce)
            .append(responseMode == null ? "" : responseMode)
            .append(cp)
            .append(lh)
            .append(ith)
            .append("&").append(Constants.STATE.getKey()).append("=").append(sessionCtx.getId());

    authorizeUrl.append("?");

// ***************************************************************
// ** prioritise PAR if the OP supports it
// ***************************************************************

    if(providers.getPushedAuthorizationRequestEndpoint() != null) {
      try {
        if(providers.getClientSecret() != null) {
          queryParams.append("&").append(Constants.CLIENT_SECRET.getKey()).append("=").append(HttpHelper.urlEncode(providers.getClientSecret()));
        }
        MsgResponse msgResponse = HttpHelper.postMessage(queryParams.toString(), providers.getPushedAuthorizationRequestEndpoint(), "application/json");
        JSONObject obj = (JSONObject)new JSONParser().parse(msgResponse.getMsg());
        authorizeUrl.append(Constants.REQUEST_URI.getKey()).append("=").append(HttpHelper.urlEncode((String)obj.get(Constants.REQUEST_URI.getKey())));
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
  private static Providers registerProvider(String issuer, String discoveryUrl, LoginbuddyContext sessionCtx)
      throws IOException {

    List<NameValuePair> formParameters = new ArrayList<>();
    formParameters.add(new BasicNameValuePair(Constants.ISSUER.getKey(), issuer));
    formParameters.add(new BasicNameValuePair(Constants.DISCOVERY_URL.getKey(), discoveryUrl));
    // TODO take loginbuddys redirect_uri for dynamic registrations from a net.loginbuddy.service.config file
    formParameters.add(new BasicNameValuePair(Constants.REDIRECT_URI.getKey(), DiscoveryUtil.UTIL.getRedirectUri()));

    MsgResponse msg = HttpHelper
        .postMessage(formParameters, "https://loginbuddy-oidcdr:445/oidcdr/register", "application/json");
    if (msg.getStatus() == 200) {
      Providers providers = LoginbuddyUtil.UTIL.getProviderConfigFromJsonString(msg.getMsg());
      sessionCtx.put(Constants.ISSUER_HANDLER.getKey(), Constants.ISSUER_HANDLER_OIDCDR.getKey());
      sessionCtx.put(Constants.PROVIDER_CLIENT_ID.getKey(), providers.getClientId()); // we have to store this in the session to make it available later
      sessionCtx.put(Constants.PROVIDER_CLIENT_SECRET.getKey(), providers.getClientSecret()); // we have to store this in the session to make it available later
      sessionCtx.put(Constants.PROVIDER_REDIRECT_URI.getKey(), providers.getRedirectUri()); // we have to store this in the session to make it available later
      return providers;
    } else {
      LOGGER.warning(msg.getMsg());
      return null;
    }
  }
}
