package net.loginbuddy.config;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ClientConfig {
    @JsonProperty("redirect_uri")
    private String redirectUri;
    @JsonProperty("client_uri")
    private String clientUri;

    public String getRedirectUri() {
        return redirectUri;
    }

    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    public String getClientUri() {
        return clientUri;
    }

    public void setClientUri(String clientUri) {
        this.clientUri = clientUri;
    }
}
