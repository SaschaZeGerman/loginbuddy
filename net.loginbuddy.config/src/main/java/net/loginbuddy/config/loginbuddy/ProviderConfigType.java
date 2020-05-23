package net.loginbuddy.config.loginbuddy;

public enum ProviderConfigType {

  MINIMAL, // issuer only, with dynamic registration (self issued)
  DEFAULT, // with oidc configuration
  FULL // all details are given

}
