package net.loginbuddy.service.util;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import net.loginbuddy.common.util.Pkce;
import net.loginbuddy.common.util.PkcePair;
import net.loginbuddy.service.config.LoginbuddyConfig;
import net.loginbuddy.service.config.ProviderConfig;
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

  public static String processInitializeRequest(SessionContext sessionCtx, ParameterValidatorResult providerResult, ParameterValidatorResult issuerResult,
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

    // TODO Verify that the selected provider is valid for this client
    ProviderConfig providerConfig = null;
    try {

      // dynamic registration of unknown provider
      // we need: 'dynamic_provider', and 'issuer', client must be registered to accept dynamic provider
      if (checkForDynamicProvider(selectedProvider, issuerResult, discoveryUrlResult, sessionCtx.getBoolean(Constants.CLIENT_ACCEPT_DYNAMIC_PROVIDER.getKey()))) {
        providerConfig = registerProvider(issuerResult.getValue(), discoveryUrlResult.getValue(), sessionCtx);
        if (providerConfig != null) {
          selectedProvider = providerConfig.getProvider(); // overwriting the provider from 'dynamic_provider' to the 'real' value (provider==issuer)
        }
      } else {
        providerConfig = LoginbuddyConfig.getInstance().getConfigUtil().getProviderConfigByProvider(selectedProvider);
      }

      if (providerConfig == null) {
        LOGGER.warning("The given provider is unknown or invalid");
        return HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "invalid_request", "The given provider is unknown or invalid");
      } else {
        sessionCtx.put(Constants.CLIENT_PROVIDER.getKey(), selectedProvider);
      }
    } catch (
        Exception e) {
      // should never occur
      e.printStackTrace();
      LOGGER.severe("The system has not been configured yet!");
      return HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "server_error", "The system has not been configured yet!");
    }

// ***************************************************************
// ** Retrieve the OpenID Configuration
// ***************************************************************

    JSONObject oidcConfig = null;
    String oidcConfigUrl = providerConfig.getOpenidConfigurationUri();
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
      authorizeUrl.append(providerConfig.getAuthorizationEndpoint());
      providerTokenEndpoint = providerConfig.getTokenEndpoint();
      providerJwksEndpoint = providerConfig.getJwksUri();
      providerUserinfoEndpoint = providerConfig.getUserinfoEndpoint();
    }

// ***************************************************************
// ** Prepare provider endpoints to be used via loginbuddy-selfissued if dynamic provider registration was used
// ***************************************************************

    // if this provider was dynamically registered we'll delegate other requests to loginbuddy-selfissued later also
    boolean isHandlerLoginbuddy = sessionCtx.get(Constants.ISSUER_HANDLER.getKey()).equals(Constants.ISSUER_HANDLER_LOGINBUDDY.getKey());
    sessionCtx.put(Constants.TOKEN_ENDPOINT.getKey(), isHandlerLoginbuddy ? providerTokenEndpoint : String
        .format("https://loginbuddy-selfissued:445/selfissued/token?%s=%s", Constants.TARGET_PROVIDER.getKey(), HttpHelper.urlEncode(providerTokenEndpoint)));
    sessionCtx.put(Constants.JWKS_URI.getKey(), isHandlerLoginbuddy ? providerJwksEndpoint : String
        .format("https://loginbuddy-selfissued:445/selfissued/jwks?%s=%s", Constants.TARGET_PROVIDER.getKey(), HttpHelper.urlEncode(providerJwksEndpoint)));
    sessionCtx.put(Constants.USERINFO_ENDPOINT.getKey(), isHandlerLoginbuddy ? providerUserinfoEndpoint : String
        .format("https://loginbuddy-selfissued:445/selfissued/userinfo?%s=%s", Constants.TARGET_PROVIDER.getKey(), HttpHelper.urlEncode(providerUserinfoEndpoint)));

// ***************************************************************
// ** use PKCE only if the provider supports it. Unfortunately, some providers fail if unsupported parameters are being send
// ***************************************************************

    String pkce = null;
    if (providerConfig.getPkce()) {
      PkcePair pair = Pkce.create(Pkce.CODE_CHALLENGE_METHOD_S256);
      sessionCtx.put(Constants.CODE_VERIFIER.getKey(), pair.getVerifier());
      pkce = String.format("&%s=%s&%s=S256", Constants.CODE_CHALLENGE.getKey(), pair.getChallenge(),
          Constants.CODE_CHALLENGE_METHOD.getKey());
    }

// ***************************************************************
// ** some providers do expect 'response_mode' for certain SCOPE combinations. Include it if specified
// ***************************************************************

    String responseMode = providerConfig.getResponseMode();
    if (Constants.RESPONSE_MODE_QUERY.getKey().equalsIgnoreCase(responseMode) || Constants.RESPONSE_MODE_FORM_POST.getKey().equalsIgnoreCase(responseMode)) {
      responseMode = "&response_mode=" + responseMode;
    }

// ***************************************************************
// ** Create the authorization URL to redirect the client
// ***************************************************************

    // do not include empty parameters. Some providers will fail the request if included
    String cp = "".equals(sessionCtx.getString(Constants.CLIENT_PROMPT.getKey())) ? "" : "&" + Constants.PROMPT.getKey() + "=" + sessionCtx.getString(Constants.CLIENT_PROMPT.getKey());
    String lh = "".equals(sessionCtx.getString(Constants.CLIENT_LOGIN_HINT.getKey())) ? "" : "&" + Constants.LOGIN_HINT.getKey() + "=" + sessionCtx.getString(Constants.CLIENT_LOGIN_HINT.getKey());
    String ith = "".equals(sessionCtx.getString(Constants.CLIENT_ID_TOKEN_HINT.getKey())) ? "" : "&" + Constants.ID_TOKEN_HINT.getKey() + "=" + sessionCtx.getString(Constants.CLIENT_ID_TOKEN_HINT.getKey());

    authorizeUrl.append("?").append(Constants.CLIENT_ID.getKey()).append("=").append(providerConfig.getClientId()).
        append("&").append(Constants.RESPONSE_TYPE.getKey()).append("=").append(providerConfig.getResponseType())
        .append("&").append(Constants.SCOPE.getKey()).append("=").append(HttpHelper.urlEncode(providerConfig.getScope()))
        .append("&").append(Constants.NONCE.getKey()).append("=").append(sessionCtx.get(Constants.CLIENT_NONCE.getKey()))
        .append("&").append(Constants.REDIRECT_URI.getKey()).append("=").append(HttpHelper.urlEncode(providerConfig.getRedirectUri()))
        .append(pkce == null ? "" : pkce)
        .append(responseMode == null ? "" : responseMode)
        .append(cp)
        .append(lh)
        .append(ith)
        .append("&").append(Constants.STATE.getKey()).append("=").append(sessionCtx.getId());

// ***************************************************************
// ** Finalize and update the session details
// ***************************************************************

    sessionCtx.setSessionCallback();

    LoginbuddyCache.getInstance().put(sessionCtx.getId(), sessionCtx, LoginbuddyConfig.getInstance().getPropertiesUtil().getLongProperty("lifetime.oauth.authcode.provider.flow"));

    return authorizeUrl.toString();
  }

  /**
   * Check if the user chose to dynamically set a provider
   */
  private static boolean checkForDynamicProvider(String provider, ParameterValidatorResult issuer,
      ParameterValidatorResult discoveryUrlResult, boolean acceptDynamicProvider) {
    boolean result = false;
    if (acceptDynamicProvider) {
      result = "dynamic_provider".equalsIgnoreCase(provider);
      result = result && issuer.getResult().equals(RESULT.VALID);
      result = result && HttpHelper.couldBeAUrl(issuer.getValue());
      if (discoveryUrlResult.getResult().equals(RESULT.VALID)) {
        result = result && HttpHelper.couldBeAUrl(discoveryUrlResult.getValue());
      }
    }
    return result;
  }

  /**
   * Register loginbuddy at the provider and remember a few details for later use
   */
  private static ProviderConfig registerProvider(String issuer, String discoveryUrl, SessionContext sessionCtx)
      throws IOException {

    List<NameValuePair> formParameters = new ArrayList<>();
    formParameters.add(new BasicNameValuePair(Constants.ISSUER.getKey(), issuer));
    formParameters.add(new BasicNameValuePair(Constants.DISCOVERY_URL.getKey(), discoveryUrl));
    formParameters.add(new BasicNameValuePair(Constants.REDIRECT_URI.getKey(),
        LoginbuddyConfig.getInstance().getDiscoveryUtil().getRedirectUri()));

    MsgResponse msg = HttpHelper
        .postMessage(formParameters, "https://loginbuddy-selfissued:445/selfissued/register", "application/json");
    if (msg.getStatus() == 200) {
      ProviderConfig providerConfig = LoginbuddyConfig.getInstance().getConfigUtil().getProviderConfigFromJsonString(msg.getMsg());
      sessionCtx.put(Constants.ISSUER_HANDLER.getKey(), Constants.ISSUER_HANDLER_SELFISSUED.getKey());
      sessionCtx.put(Constants.PROVIDER_CLIENT_ID.getKey(), providerConfig.getClientId()); // we have to store this in the session to make it available later
      sessionCtx.put(Constants.PROVIDER_CLIENT_SECRET.getKey(), providerConfig.getClientSecret()); // we have to store this in the session to make it available later
      sessionCtx.put(Constants.PROVIDER_REDIRECT_URI.getKey(), providerConfig.getRedirectUri()); // we have to store this in the session to make it available later
      return providerConfig;
    } else {
      LOGGER.warning(msg.getMsg());
      return null;
    }
  }
}
