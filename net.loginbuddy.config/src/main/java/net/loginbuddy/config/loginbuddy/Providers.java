/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.config.loginbuddy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.config.Meta;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.Serializable;
import java.util.Objects;
import java.util.logging.Logger;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Providers implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(Providers.class.getName());

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

    @JsonProperty("_meta")
    @JsonIgnore(false)
    private Meta meta;

    @JsonProperty("response_type")
    private String responseType;

    @JsonProperty("template")
    private String template;

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
    private JSONObject mappings;

    @JsonProperty("pushed_authorization_request_endpoint")
    private String pushedAuthorizationRequestEndpoint;

    @JsonProperty("dpop_signing_alg")
    private String dpopSigningAlg;

    public Providers() {
        pkce = true;
        scope = Constants.OPENID_SCOPE.getKey();
        responseType = Constants.CODE.getKey();
        meta = new Meta();
    }

    public Providers(String issuer, String clientId, String redirectUri) {
        this();
        this.issuer = issuer;
        this.clientId = clientId;
        this.redirectUri = redirectUri;
    }

    public Providers(String issuer, String clientId, String redirectUri, String clientSecret) {
        this(issuer, clientId, redirectUri);
        this.clientSecret = clientSecret;
    }

    public String getProvider() {
        return provider;
    }

    public String getIssuer() {
        return issuer;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public Meta getMeta() {
        return meta;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public String getOpenidConfigurationUri() {
        return openidConfigurationUri;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public String getUserinfoEndpoint() {
        return userinfoEndpoint;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public String getScope() {
        return scope;
    }

    public String getResponseType() {
        return responseType;
    }

    public boolean getPkce() {
        return pkce;
    }

    public String getResponseMode() {
        return responseMode;
    }

    public JSONObject getMappings() {
        try {
            if (mappings == null) {
                return new JSONObject();
            }
            return (JSONObject) new JSONParser().parse(mappings.toString());
        } catch (ParseException e) {
            LOGGER.warning(String.format("The mapping object is invalid: %s", mappings == null ? "" : mappings.toString()));
            return new JSONObject();
        }
    }

    public String getPushedAuthorizationRequestEndpoint() {
        return pushedAuthorizationRequestEndpoint;
    }

    public String getDpopSigningAlg() {
        return dpopSigningAlg;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public void setOpenidConfigurationUri(String openidConfigurationUri) {
        this.openidConfigurationUri = openidConfigurationUri;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public void setUserinfoEndpoint(String userinfoEndpoint) {
        this.userinfoEndpoint = userinfoEndpoint;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public void setResponseMode(String responseMode) {
        this.responseMode = responseMode;
    }

    public void setPkce(boolean pkce) {
        this.pkce = pkce;
    }

    public void setMappings(JSONObject mappings) {
        this.mappings = mappings;
    }

    public void setPushedAuthorizationRequestEndpoint(String pushedAuthorizationRequestEndpoint) {
        this.pushedAuthorizationRequestEndpoint = pushedAuthorizationRequestEndpoint;
    }

    public String getTemplate() {
        return template;
    }

    public void setTemplate(String template) {
        this.template = template;
    }

    public void setDpopSigningAlg(String dpopSigningAlg) {
        this.dpopSigningAlg = dpopSigningAlg;
    }

    @JsonIgnore()
    public boolean isDpopEnabled() {
        return dpopSigningAlg != null;
    }

    @JsonIgnore()
    @Deprecated
    public JSONObject mappingsAsJsonNode() {
        return mappings;
    }

    @JsonIgnore()
    public boolean isUsable() {
        return meta.getStatus().size() == 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Providers providers = (Providers) o;
        return provider.equalsIgnoreCase(providers.provider);
    }

    @Override
    public int hashCode() {
        return Objects.hash(provider);
    }
}