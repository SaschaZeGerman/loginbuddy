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
import net.loginbuddy.config.loginbuddy.common.Meta;
import net.loginbuddy.config.loginbuddy.common.OnBehalfOf;

import java.io.Serializable;
import java.util.*;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Clients implements Serializable {

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
    private List<String> clientProviders;

    @JsonProperty("accept_dynamic_provider")
    private boolean acceptDynamicProvider;

    @JsonProperty("signed_response_alg")
    private String signedResponseAlg;

    @JsonProperty("on_behalf_of")
    private List<OnBehalfOf> onBehalfOf;

    @JsonProperty("client_name")
    private String clientName;

    @JsonProperty("tos_uri")
    private String clientTosUri;

    @JsonProperty("policy_uri")
    private String clientPolicyUri;

    @JsonProperty("logo_uri")
    private String clientLogoUri;

    @JsonProperty("contacts")
    private Set<String> clientContacts;

    @JsonProperty("redirect_uris")
    @JsonIgnore(false)
    private Set<String> redirectUris;

    @JsonProperty("_meta")
    @JsonIgnore(false)
    private Meta meta;

    public Clients() {
        acceptDynamicProvider = false;
        clientContacts = new HashSet<>();
        redirectUris = new HashSet<>();
        clientProviders = new ArrayList<>();
        onBehalfOf = new ArrayList<>();
        meta = new Meta();
    }

    public Clients(String clientId, String clientType) {
        this();
        this.clientId = clientId;
        this.clientType = clientType;
    }

    public String getRedirectUri() {
        return (String)redirectUris.toArray()[0];
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

    public List<String> getClientProviders() {
        return clientProviders;
    }

    public boolean isAcceptDynamicProvider() {
        // TODO do not depend on the variable
        return acceptDynamicProvider && Boolean.parseBoolean(System.getenv("SUPPORT_OIDCDR"));
    }

    public String getSignedResponseAlg() {
        return signedResponseAlg;
    }

    public List<OnBehalfOf> getOnBehalfOf() {
        return onBehalfOf;
    }

    public Meta getMeta() {
        return meta;
    }

    public void setRedirectUri(String redirectUri) {
        redirectUris.addAll(Arrays.asList(redirectUri.split("[,; ]")));
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

    public void setClientProviders(List<String> clientProviders) {
        this.clientProviders = clientProviders;
    }

    public void setAcceptDynamicProvider(boolean acceptDynamicProvider) {
        this.acceptDynamicProvider = acceptDynamicProvider;
    }

    public void setSignedResponseAlg(String signedResponseAlg) {
        this.signedResponseAlg = signedResponseAlg;
    }

    public void setOnBehalfOf(List<OnBehalfOf> onBehalfOf) {
        this.onBehalfOf = onBehalfOf;
    }

    public String getClientName() {
        return clientName;
    }

    public void setClientName(String clientName) {
        this.clientName = clientName;
    }

    public String getClientTosUri() {
        return clientTosUri;
    }

    public void setClientTosUri(String clientTosUri) {
        this.clientTosUri = clientTosUri;
    }

    public String getClientPolicyUri() {
        return clientPolicyUri;
    }

    public void setClientPolicyUri(String clientPolicyUri) {
        this.clientPolicyUri = clientPolicyUri;
    }

    public String getClientLogoUri() {
        return clientLogoUri;
    }

    public void setClientLogoUri(String clientLogoUri) {
        this.clientLogoUri = clientLogoUri;
    }

    public Set<String> getClientContacts() {
        return clientContacts;
    }

    @JsonIgnore
    public String getClientContactsAsString() {
        return String.join(",", clientContacts);
    }

    public void setClientContacts(Set<String> clientContacts) {
        this.clientContacts = clientContacts;
    }

    public boolean addContact(String contact) {
        return clientContacts.add(contact);
    }

    public boolean removeContact(String contact) {
        return clientContacts.remove(contact);
    }

    public Set<String> getRedirectUris() {
        return redirectUris;
    }

    public void setRedirectUris(Set<String> redirectUris) {
        this.redirectUris = redirectUris;
    }

    public void setMeta(Meta meta) {
        this.meta = meta;
    }

    public boolean addRedirectUri(String redirectUri) {
        return redirectUris.add(redirectUri);
    }

    public boolean removeRedirectUri(String redirectUri) {
        return redirectUris.remove(redirectUri);
    }

    @JsonIgnore()
    public int getRedirectUrisCount() {
        return redirectUris.size();
    }

    @JsonIgnore()
    public boolean isRegisteredRedirectUri(String redirectUri) {
        return redirectUris.contains(redirectUri);
    }

    @JsonIgnore()
    public boolean isUsable() {
        return meta.getStatus().size() == 0;
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
