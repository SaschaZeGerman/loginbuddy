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
    BEARER("Bearer"),
    CHECK_REDIRECT_URI("checkRedirectUri"),
    CLIENT_ID("client_id"),
    CLIENT_SECRET_EXPIRES_AT("client_secret_expires_at"),
    CLIENT_SECRET("client_secret"),
    CLIENT_SECRET_BASIC("client_secret_basic"),
    CLIENT_SECRET_POST("client_secret_post"),
    CLIENT_TYPE_CONFIDENTIAL("confidential"),
    CLIENT_TYPE_PUBLIC("public"),
    CODE("code"),
    CODE_VERIFIER("code_verifier"),
    CODE_CHALLENGE("code_challenge"),
    CODE_CHALLENGE_METHOD("code_challenge_method"),
    CODE_CHALLENGE_METHODS_SUPPORTED("code_challenge_methods_supported"),
    CONFIGURATION_ENDPOINT("configuration_endpoint"),
    DISCOVERY_URL("discovery_url"),
    DYNAMIC_PROVIDER("dynamic_provider"),
    ERROR("error"),
    ERROR_DESCRIPTION("error_description"),
    EXPIRES_IN("expires_in"),
    GRANT_TYPE("grant_type"),
    GRANT_TYPE_CLIENT_CREDENTIALS("client_credentials"),
    GRANT_TYPES_SUPPORTED("grant_types_supported"),
    ID_TOKEN("id_token"),
    ID_TOKEN_HINT("id_token_hint"),
    ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED("id_token_signing_alg_values_supported"),
    ISSUER("issuer"),
    ISSUER_SELFISSUED("https://self-issued.me"),
    ISSUER_HANDLER("issuer_handler"),
    ISSUER_HANDLER_OIDCDR("issuer_handler_oidcdr"),
    ISSUER_HANDLER_LOGINBUDDY("issuer_handler_loginbuddy"),
    JWKS_URI("jwks_uri"),
    LOGIN_HINT("login_hint"),
    SIGNED_RESPONSE_ALG("signed_response_alg"),
    NONCE("nonce"),
    OPENID_SCOPE("openid profile email"),
    OBFUSCATE_TOKEN("obfuscate_token"),
    PAR_REQUEST_URI("parRequestUri"),
    PROVIDER("provider"),
    PROVIDER_ADDITION("provider_addition"),
    PROMPT("prompt"),
    PUSHED_AUTHORIZATION_REQUEST_ENDPOINT("pushed_authorization_request_endpoint"),
    PKCE("pkce"),
    REDIRECT_URI("redirect_uri"),
    REGISTRATION_ENDPOINT("registration_endpoint"),
    REQUEST_URI("request_uri"),
    REFRESH_TOKEN("refresh_token"),
    RESOURCE("resource"),
    RESPONSE_TYPE("response_type"),
    RESPONSE_TYPES_SUPPORTED("response_types_supported"),
    RESPONSE_MODE_QUERY("query"),
    RESPONSE_MODE_FORM_POST("form_post"),
    SERVICE_DOCUMENTATION("service_documentation"),
    SESSION("session"),
    SCOPE("scope"),
    SCOPES_SUPPORTED("scopes_supported"),
    STATE("state"),
    SUBJECT_TYPES_SUPPORTED("subject_types_supported"),
    TOKEN_ENDPOINT("token_endpoint"),
    TOKEN_ENDPOINT_AUTH_METHOD("token_endpoint_auth_method"),
    TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED("token_endpoint_auth_methods_supported"),
    TOKEN_TYPE("token_type"),
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
    CLIENT_SIGNED_RESPONSE_ALG("clientSignedResponseAlg"),
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

    @Override
    public String toString() {
        return getKey();
    }
}
