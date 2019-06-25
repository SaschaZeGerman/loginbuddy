/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.common.config;

public enum Constants {

    // General values
    ACCESS_TOKEN("access_token"),
    AUTHORIZATION("Authorization"),
    AUTHORIZATION_CODE("authorization_code"),
    BASIC("Basic "),
    BEARER("Bearer "),
    CLIENT_ID("client_id"),
    CLIENT_SECRET("client_secret"),
    CODE("code"),
    CODE_VERIFIER("code_verifier"),
    CODE_CHALLENGE("code_challenge"),
    CODE_CHALLENGE_METHOD("code_challenge_method"),
    ERROR("error"),
    GRANT_TYPE("grant_type"),
    ID_TOKEN("id_token"),
    NONCE("nonce"),
    OPENID_SCOPE("openid profile email"),
    PROVIDER("provider"),
    REDIRECT_URI("redirect_uri"),
    RESPONSE_TYPE("response_type"),
    SESSION("session"),
    SCOPE("scope"),
    STATE("state"),
    AUTHORIZATION_ENDPOINT("authorization_endpoint"),
    TOKEN_ENDPOINT("token_endpoint"),
    USERINFO_ENDPOINT("userinfo_endpoint"),
    JWKS_URI("jwks_uri"),

    // used with example provider
    ACTION_EXPECTED("action_expected"),
    ACTION_LOGIN("login"),
    ACTION_AUTHENTICATE("authenticate"),
    ACTION_GRANT("grant"),

    // references client (application) values
    CLIENT_STATE("clientState"),
    CLIENT_REDIRECT("clientRedirectUri"),
    CLIENT_CODE_CHALLENGE("clientCodeChallenge"),
    CLIENT_CODE_CHALLENGE_METHOD("clientCodeChallengeMethod"),
    CLIENT_PROVIDER("clientProvider");

    private final String key;

    Constants(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}
