/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.config.loginbuddy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Clients {

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

    public Clients() {
        acceptDynamicProvider = false;
    }

    public Clients(String clientId, String clientType) {
        this();
        this.clientId = clientId;
        this.clientType = clientType;
    }

    //    public Clients(Clients c) {
//        this.redirectUri = c.redirectUri;
//        this.clientId = c.clientId;
//        this.clientType = c.clientType;
//        this.clientUri = c.clientUri;
//        this.clientSecret = c.clientSecret;
//        this.clientProviders = c.clientProviders;
//        this.acceptDynamicProvider = c.acceptDynamicProvider;
//        this.signedResponseAlg = c.signedResponseAlg;
//    }

    public String getRedirectUri() {
        return redirectUri;
    }

//    public void setRedirectUri(String redirectUri) {
//        this.redirectUri = redirectUri;
//    }

    public String getClientUri() {
        return clientUri;
    }

//    public void setClientUri(String clientUri) {
//        this.clientUri = clientUri;
//    }

    public String getClientId() { return clientId; }

//    public void setClientId(String clientId) { this.clientId = clientId; }

    public String getClientSecret() {
        return clientSecret;
    }

//    public void setClientSecret(String clientSecret) {
//        this.clientSecret = clientSecret;
//    }

    public String getClientType() {
        return clientType;
    }

//    public void setClientType(String clientType) {
//        this.clientType = clientType;
//    }

    public String[] getClientProviders() {
        return clientProviders;
    }

//    public void setClientProviders(String[] clientProviders) {
//        this.clientProviders = clientProviders;
//    }

    public boolean isAcceptDynamicProvider() {
        // TODO do not depend on the variable
        return acceptDynamicProvider && Boolean.parseBoolean(System.getenv("SUPPORT_OIDCDR"));
    }

//    public void setAcceptDynamicProvider(boolean acceptDynamicProvider) {
        // TODO do not depend on the variable
//        this.acceptDynamicProvider = acceptDynamicProvider && Boolean.parseBoolean(System.getenv("SUPPORT_OIDCDR"));
//    }

    public String getSignedResponseAlg() {
        return signedResponseAlg;
    }

//    public void setSignedResponseAlg(String signedResponseAlg) {
//        this.signedResponseAlg = signedResponseAlg;
//    }
}
