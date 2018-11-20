package net.loginbuddy.config;

public enum Constants {
    ACCESS_TOKEN("access_token"),
    AUTHORIZATION("Authorization"),
    AUTHORIZATION_CODE("authorization_code"),
    AUTHORIZATION_ENDPOINT("authorization_endpoint"),
    TOKEN_ENDPOINT("token_endpoint"),
    USERINFO_ENDPOINT("userinfo_endpoint"),
    BEARER("Bearer "),
    CLIENT_ID("client_id"),
    CLIENT_SECRET("client_secret"),
    CODE("code"),
    GRANT_TYPE("grant_type"),
    CODE_VERIFIER("code_verifier"),
    ID_TOKEN("id_token"),
    PROVIDER("provider"),
    REDIRECT_URI("redirect_uri"),
    RESPONSE_TYPE("response_type"),
    SCOPE("scope"),
    STATE("state"),
    NONCE("nonce"),
    OPENID_SCOPE("openid profile email");

    private final String key;

    Constants(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}
