/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import net.loginbuddy.common.config.Constants;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.logging.Logger;

public class ProviderConfig {

  private static final Logger LOGGER = Logger.getLogger(String.valueOf(ProviderConfig.class));

  @JsonProperty("provider")
  @JsonIgnore(false)
  private String provider;

  @JsonProperty("issuer")
  @JsonIgnore(false)
  private String issuer;

  @JsonProperty("client_id")
  @JsonIgnore(false)
  private String clientId;

  @JsonProperty("redirect_uri")
  @JsonIgnore(false)
  private String redirectUri;

  @JsonProperty("client_secret")
  @JsonIgnore(false)
  private String clientSecret;

  @JsonProperty("response_type")
  private String responseType;

  @JsonProperty("scope")
  private String scope;

  @JsonProperty("authorization_endpoint")
  private String authorizationEndpoint;

  @JsonProperty("openid_configuration_uri")
  private String openidConfigurationUri;

  @JsonProperty("token_endpoint")
  private String tokenEndpoint;

  @JsonProperty("userinfo_endpoint")
  private String userinfoEndpoint;

  @JsonProperty("jwks_uri")
  private String jwksUri;

  @JsonProperty("response_mode")
  private String responseMode;

  @JsonProperty("pkce")
  private boolean pkce;

  @JsonProperty("mappings")
  private JsonNode mappings;

  public ProviderConfig() {
    this.pkce = true;
    this.scope = Constants.OPENID_SCOPE.getKey();
    this.responseType = Constants.CODE.getKey();
  }

  /**
   * Updates the config and removes the oidc config endpoint. This way we do not do another API call to retrieve it
   * again. It does NOT update 'issuer' or 'provider'
   */
  public void enhanceToFull(ProviderConfig config) {
    this.clientId = config.getClientId();
    this.clientSecret = config.getClientSecret();
    this.redirectUri = config.getRedirectUri();
    this.scope = config.getScope();
    this.authorizationEndpoint = config.getAuthorizationEndpoint();
    this.tokenEndpoint = config.getTokenEndpoint();
    this.userinfoEndpoint = config.getUserinfoEndpoint();
    this.jwksUri = config.getJwksUri();
    this.responseType = config.getResponseType();
    this.openidConfigurationUri = null;
  }

  public ProviderConfigType getProviderType() {
    return clientId == null ? ProviderConfigType.MINIMAL
        : openidConfigurationUri == null ? ProviderConfigType.FULL : ProviderConfigType.DEFAULT;
  }

  public String getProvider() {
    return provider;
  }

  public void setProvider(String provider) {
    this.provider = provider;
  }

  public String getIssuer() {
    return issuer;
  }

  public void setIssuer(String issuer) {
    this.issuer = issuer;
  }

  public String getClientId() {
    return clientId;
  }

  public void setClientId(String clientId) {
    this.clientId = clientId;
  }

  public String getClientSecret() {
    return clientSecret;
  }

  public void setClientSecret(String clientSecret) {
    this.clientSecret = clientSecret;
  }

  public String getRedirectUri() {
    return redirectUri;
  }

  public void setRedirectUri(String redirectUri) {
    this.redirectUri = redirectUri;
  }

  public String getAuthorizationEndpoint() {
    return authorizationEndpoint;
  }

  public void setAuthorizationEndpoint(String authorizationEndpoint) {
    this.authorizationEndpoint = authorizationEndpoint;
  }

  public String getOpenidConfigurationUri() {
    return openidConfigurationUri;
  }

  public void setOpenidConfigurationUri(String openidConfigurationUri) {
    this.openidConfigurationUri = openidConfigurationUri;
  }

  public String getTokenEndpoint() {
    return tokenEndpoint;
  }

  public void setTokenEndpoint(String tokenEndpoint) {
    this.tokenEndpoint = tokenEndpoint;
  }

  public String getUserinfoEndpoint() {
    return userinfoEndpoint;
  }

  public void setUserinfoEndpoint(String userinfoEndpoint) {
    this.userinfoEndpoint = userinfoEndpoint;
  }

  public String getJwksUri() {
    return jwksUri;
  }

  public void setJwksUri(String jwksUri) {
    this.jwksUri = jwksUri;
  }

  public String getScope() {
    return scope;
  }

  public void setScope(String scope) {
    this.scope = scope;
  }

  public String getResponseType() {
    return responseType;
  }

  public void setResponseType(String responseType) {
    this.responseType = responseType;
  }

  public boolean getPkce() {
    return pkce;
  }

  public void setPkce(boolean pkce) {
    this.pkce = pkce;
  }

  public String getMappings() {
    return mappings == null ? "{}" : mappings.toString();
  }

  public String getResponseMode() {
    return responseMode;
  }

  public void setResponseMode(String responseMode) {
    this.responseMode = responseMode;
  }

  public JSONObject getMappingsAsJson() {
    try {
      return (JSONObject) new JSONParser().parse(getMappings());
    } catch (ParseException e) {
      LOGGER.warning(String.format("The mapping object is invalid: %s", getMappings() == null ? "" : getMappings()));
      return new JSONObject();
    }
  }

  public void setMappings(JsonNode mappings) {
    this.mappings = mappings;
  }
}
