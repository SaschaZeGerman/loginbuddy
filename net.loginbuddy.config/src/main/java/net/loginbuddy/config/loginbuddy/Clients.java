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
import net.loginbuddy.config.loginbuddy.common.OnBehalfOf;

import java.io.Serializable;
import java.util.Objects;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Clients implements Serializable {

    @JsonProperty("redirect_uri")
    @JsonIgnore(false)
    private String redirectUri;

    @JsonProperty("client_id")
    @JsonIgnore(false)
    private String clientId;

    @JsonProperty("client_type")
    @JsonIgnore(false)
    private String clientType;

    @JsonProperty("client_uri")
    private String clientUri;

    @JsonProperty("client_secret")
    private String clientSecret;

    @JsonProperty("providers")
    private String[] clientProviders;

    @JsonProperty("accept_dynamic_provider")
    private boolean acceptDynamicProvider;

    @JsonProperty("signed_response_alg")
    private String signedResponseAlg;

    @JsonProperty("on_behalf_of")
    private OnBehalfOf[] onBehalfOf;

    public Clients() {
        this.acceptDynamicProvider = false;
        this.onBehalfOf = new OnBehalfOf[0];
    }

    public Clients(String clientId, String clientType) {
        this();
        this.clientId = clientId;
        this.clientType = clientType;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getClientUri() {
        return clientUri;
    }

    public String getClientId() { return clientId; }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getClientType() {
        return clientType;
    }

    public String[] getClientProviders() {
        return clientProviders;
    }

    public boolean isAcceptDynamicProvider() {
        // TODO do not depend on the variable
        return acceptDynamicProvider && Boolean.parseBoolean(System.getenv("SUPPORT_OIDCDR"));
    }

    public String getSignedResponseAlg() {
        return signedResponseAlg;
    }

    public OnBehalfOf[] getOnBehalfOf() {
        return onBehalfOf;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public void setClientUri(String clientUri) {
        this.clientUri = clientUri;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public void setClientProviders(String[] clientProviders) {
        this.clientProviders = clientProviders;
    }

    public void setAcceptDynamicProvider(boolean acceptDynamicProvider) {
        this.acceptDynamicProvider = acceptDynamicProvider;
    }

    public void setSignedResponseAlg(String signedResponseAlg) {
        this.signedResponseAlg = signedResponseAlg;
    }

    public void setOnBehalfOf(OnBehalfOf[] onBehalfOf) {
        this.onBehalfOf = onBehalfOf;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Clients clients = (Clients) o;
        return clientId.equalsIgnoreCase(clients.clientId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(clientId);
    }
}
