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
    ACCEPT_DYNAMIC_PROVIDER("accept_dynamic_provider"),
    AUTHORIZATION("Authorization"),
    AUTHORIZATION_CODE("authorization_code"),
    AUTHORIZATION_ENDPOINT("authorization_endpoint"),
    BASIC("Basic "),
    BEARER("Bearer "),
    CHECK_REDIRECT_URI("checkRedirectUri"),
    CLIENT_ID("client_id"),
    CLIENT_SECRET("client_secret"),
    CLIENT_SECRET_BASIC("client_secret_basic"),
    CLIENT_SECRET_POST("client_secret_post"),
    CLIENT_TYPE_CONFIDENTIAL("confidential"),
    CLIENT_TYPE_PUBLIC("public"),
    CODE("code"),
    CODE_VERIFIER("code_verifier"),
    CODE_CHALLENGE("code_challenge"),
    CODE_CHALLENGE_METHOD("code_challenge_method"),
    DISCOVERY_URL("discovery_url"),
    ERROR("error"),
    ERROR_DESCRIPTION("error_description"),
    GRANT_TYPE("grant_type"),
    ID_TOKEN("id_token"),
    ID_TOKEN_HINT("id_token_hint"),
    ISSUER("issuer"),
    ISSUER_SELFISSUED("https://self-issued.me"),
    ISSUER_HANDLER("issuer_handler"),
    ISSUER_HANDLER_OIDCDR("issuer_handler_oidcdr"),
    ISSUER_HANDLER_LOGINBUDDY("issuer_handler_loginbuddy"),
    JWKS_URI("jwks_uri"),
    LOGIN_HINT("login_hint"),
    NONCE("nonce"),
    OPENID_SCOPE("openid profile email"),
    PROVIDER("provider"),
    PROVIDER_ADDITION("provider_addition"),
    PROMPT("prompt"),
    PKCE("pkce"),
    REDIRECT_URI("redirect_uri"),
    RESPONSE_TYPE("response_type"),
    RESPONSE_MODE_QUERY("query"),
    RESPONSE_MODE_FORM_POST("form_post"),
    SESSION("session"),
    SCOPE("scope"),
    SCOPES_SUPPORTED("scopes_supported"),
    STATE("state"),
    TOKEN_ENDPOINT("token_endpoint"),
    USERINFO_ENDPOINT("userinfo_endpoint"),

    // used with example provider
    ACTION_EXPECTED("action_expected"),
    ACTION_USED_RESPONSE_TYPE("action_used_response_type"),
    ACTION_LOGIN("login"),
    ACTION_AUTHENTICATE("authenticate"),
    ACTION_GRANT("grant"),
    ACTION_INITIALIZE("initialize"),
    ACTION_CALLBACK("callback"),
    ACTION_TOKEN_EXCHANGE("token_exchange"),

    // references client (application) values
    CLIENT_STATE("clientState"),
    CLIENT_ACCEPT_DYNAMIC_PROVIDER("clientAcceptDynamicProvider"),
    CLIENT_REDIRECT("clientRedirectUri"),
    CLIENT_REDIRECT_VALID("clientRedirectUriValid"),
    CLIENT_CODE_CHALLENGE("clientCodeChallenge"),
    CLIENT_CODE_CHALLENGE_METHOD("clientCodeChallengeMethod"),
    CLIENT_PROVIDER("clientProvider"),
    CLIENT_SCOPE("clientScope"),
    CLIENT_RESPONSE_TYPE("clientResponseType"),
    CLIENT_PROMPT("clientPrompt"),
    CLIENT_LOGIN_HINT("clientLoginHint"),
    CLIENT_ID_TOKEN_HINT("clientIdtokenHint"),
    CLIENT_CLIENT_ID("clientClientId"),
    CLIENT_NONCE("clientNonce"),
    MAPPING_OIDC("{\"sub\":\"sub\", \"name\": \"name\",\"given_name\": \"given_name\",\"family_name\": \"family_name\",\"picture\": \"picture\",\"email\":\"email\", \"email_verified\":\"email_verified\", \"provider\":\"asis:provider\"}"),

    // references for dynamically registered providers
    TARGET_PROVIDER("target"),
    PROVIDER_CLIENT_ID("providerClientId"),
    PROVIDER_CLIENT_SECRET("providerClientSecret"),
    PROVIDER_REDIRECT_URI("providerRedirectUri");

    private final String key;

    Constants(String key) {
        this.key = key;
    }

    public String getKey() {
        return this.key;
    }
}
