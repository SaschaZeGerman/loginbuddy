package net.loginbuddy.service.config;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

public class DiscoveryConfig {

    private String jsonString;

    @JsonProperty("issuer")
    @JsonIgnore(false)
    private String issuer;

    @JsonProperty("response_types_supported")
    @JsonIgnore(false)
    private String[] responseTypeSupported;

    @JsonProperty("authorization_endpoint")
    @JsonIgnore(false)
    private String authorizationEndpoint;

    @JsonProperty("jwks_uri")
    @JsonIgnore(false)
    private String jwksUri;

    @JsonProperty("id_token_signing_alg_values_supported")
    @JsonIgnore(false)
    private String[] idTokenSigningAlgValuesSupported;

    @JsonProperty("subject_types_supported")
    @JsonIgnore(false)
    private String[] subjectTypesSupported;

    @JsonProperty("grant_types_supported")
    private String[] grantTypesSupported;

    @JsonProperty("token_endpoint_auth_methods_supported")
    private String[] tokenEndpointAuthMethodsSupported;

    @JsonProperty("scopes_supported")
    private String[] scopesSupported;

    @JsonProperty("token_endpoint")
    private String tokenEndpoint;

    @JsonProperty("service_documentation")
    private String serviceDocumentation;

    @JsonProperty("code_challenge_methods_supported")
    private String[] codeChallengeMethodsSupported;

    @JsonProperty("userinfo_endpoint")
    private String userinfoEndpoint;

    @JsonProperty("signing_alg_values_supported")
    private String[] signingAlgValuesSupported;

    @JsonProperty("pushed_authorization_request_endpoint")
    private String pushedAuthorizationRequestEndpoint;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String[] getResponseTypeSupported() {
        return responseTypeSupported;
    }

    public void setResponseTypeSupported(String[] responseTypeSupported) {
        this.responseTypeSupported = responseTypeSupported;
    }

    public String[] getGrantTypesSupported() {
        return grantTypesSupported;
    }

    public void setGrantTypesSupported(String[] grantTypesSupported) {
        this.grantTypesSupported = grantTypesSupported;
    }

    public String[] getTokenEndpointAuthMethodsSupported() {
        return tokenEndpointAuthMethodsSupported;
    }

    public void setTokenEndpointAuthMethodsSupported(String[] tokenEndpointAuthMethodsSupported) {
        this.tokenEndpointAuthMethodsSupported = tokenEndpointAuthMethodsSupported;
    }

    public String[] getScopesSupported() {
        return scopesSupported;
    }

    public void setScopesSupported(String[] scopesSupported) {
        this.scopesSupported = scopesSupported;
    }

    public String getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public String getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(String tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public String getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(String jwksUri) {
        this.jwksUri = jwksUri;
    }

    public String[] getIdTokenSigningAlgValuesSupported() {
        return idTokenSigningAlgValuesSupported;
    }

    public void setIdTokenSigningAlgValuesSupported(String[] idTokenSigningAlgValuesSupported) {
        this.idTokenSigningAlgValuesSupported = idTokenSigningAlgValuesSupported;
    }

    public String getServiceDocumentation() {
        return serviceDocumentation;
    }

    public void setServiceDocumentation(String serviceDocumentation) {
        this.serviceDocumentation = serviceDocumentation;
    }

    public String[] getSubjectTypesSupported() {
        return subjectTypesSupported;
    }

    public void setSubjectTypesSupported(String[] subjectTypesSupported) {
        this.subjectTypesSupported = subjectTypesSupported;
    }

    public String[] getCodeChallengeMethodsSupported() {
        return codeChallengeMethodsSupported;
    }

    public void setCodeChallengeMethodsSupported(String[] codeChallengeMethodsSupported) {
        this.codeChallengeMethodsSupported = codeChallengeMethodsSupported;
    }

    public String getUserinfoEndpoint() {
        return userinfoEndpoint;
    }

    public void setUserinfoEndpoint(String userinfoEndpoint) {
        this.userinfoEndpoint = userinfoEndpoint;
    }

    public String getPushedAuthorizationRequestEndpoint() {
        return pushedAuthorizationRequestEndpoint;
    }

    public void setPushedAuthorizationRequestEndpoint(String pushedAuthorizationRequestEndpoint) {
        this.pushedAuthorizationRequestEndpoint = pushedAuthorizationRequestEndpoint;
    }

    void setJsonString(String jsonString) {
        this.jsonString = jsonString;
    }

    @Override
    public String toString() {
        return jsonString;
    }

    public String[] getSigningAlgValuesSupported() {
        return signingAlgValuesSupported;
    }

    public void setSigningAlgValuesSupported(String[] signingAlgValuesSupported) {
        if (signingAlgValuesSupported == null || signingAlgValuesSupported.length == 0) {
            this.signingAlgValuesSupported = new String[0];
        } else {
            this.signingAlgValuesSupported = signingAlgValuesSupported;
        }
    }
}
