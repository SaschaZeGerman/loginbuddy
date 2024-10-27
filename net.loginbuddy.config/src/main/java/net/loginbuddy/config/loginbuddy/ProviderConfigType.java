package net.loginbuddy.config.loginbuddy;

import net.loginbuddy.common.config.Constants;
import org.json.simple.JSONObject;

public enum ProviderConfigType {

  MINIMAL, // issuer only, with dynamic registration (self issued)
  DEFAULT, // with oidc configuration
  FULL; // all details are given

  public static ProviderConfigType getProviderConfigType(JSONObject provider) {
    return provider.get(Constants.CLIENT_ID.getKey()) == null ? MINIMAL : provider.get(Constants.OPENID_CONFIGURATION_URI.getKey()) == null ? FULL : DEFAULT;
  }

  public static ProviderConfigType getProviderConfigType(Providers provider) {
    return provider.getClientId() == null ? MINIMAL : provider.getOpenidConfigurationUri() == null ? FULL : DEFAULT;
  }
}
