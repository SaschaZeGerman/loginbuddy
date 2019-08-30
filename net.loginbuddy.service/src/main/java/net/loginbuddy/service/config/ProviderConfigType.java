package net.loginbuddy.service.config;

public enum ProviderConfigType {

  MINIMAL, // issuer only, with dynamic registration (self issued)
  DEFAULT, // with oidc configuration
  FULL // all details are given

}
