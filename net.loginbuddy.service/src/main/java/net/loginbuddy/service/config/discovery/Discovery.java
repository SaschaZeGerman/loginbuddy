package net.loginbuddy.service.config.discovery;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.service.config.Bootstrap;

import java.util.logging.Logger;

/**
 * Values to access the discovery document.
 * More details in the document here: https://openid.net/specs/openid-connect-discovery-1_0.html
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Discovery implements Bootstrap {

    private Logger LOGGER = Logger.getLogger(String.valueOf(Discovery.class));

    private com.fasterxml.jackson.databind.ObjectMapper MAPPER = new ObjectMapper();

    @JsonIgnore(false)
    private String issuer;

    @JsonProperty("authorization_endpoint")
    @JsonIgnore(false)
    private String authorizationEndpoint;

    @JsonProperty("token_endpoint")
    @JsonIgnore(false)
    private String tokenEndpoint;

    @JsonProperty("userinfo_endpoint")
    private String userinfoEndpoint;

    @JsonProperty("jwks_uri")
    @JsonIgnore(false)
    private String jwksUri;

    @JsonProperty("scopes_supported")
    private String[] scopesSupported;

    @JsonProperty("response_types_supported")
    @JsonIgnore(false)
    private String[] responseTypesSupported;

    @JsonProperty("grant_types_supported")
    @JsonIgnore(false)
    private String[] grantTypesSupported;  // required because the default would include 'implicit' which is not supported

    @JsonProperty("subject_types_supported")
    @JsonIgnore(false)
    private String[] subjectTypesSupported;

    @JsonProperty("id_token_signing_alg_values_supported")
    @JsonIgnore(false)
    private String[] idTokenSigningAlgValuesSupported;

    @JsonProperty("token_endpoint_auth_methods_supported")
    private String[] tokenEndpointAuthMethodsSupported;

    @JsonProperty("service_documentation")
    private String serviceDocumentation;

    @JsonProperty("code_challenge_methods_supported")
    private String[] codeChallengeMethodsSupported;

    @JsonProperty("pushed_authorization_request_endpoint")
    private String pushedAuthorizationRequestEndpoint;

    // *** values specific to Loginbuddy *** //
//    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Management management;

    @JsonProperty("signing_alg_values_supported")
    private String[] signingAlgValuesSupported;

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String[] getResponseTypesSupported() {
        return responseTypesSupported;
    }

    public void setResponseTypesSupported(String[] responseTypesSupported) {
        this.responseTypesSupported = responseTypesSupported;
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

    public Management getManagement() {
        return management;
    }

    public void setManagement(Management management) {
        this.management = management;
    }

    public String getRedirectUri() {
        return getRedirectUri("/callback");
    }

    public String getRedirectUri(String path) {
        return getIssuer() + path;
    }

    @JsonIgnore
    public String getScopesSupportedAsString() {
        return HttpHelper.stringArrayToString(getScopesSupported());
    }

    @JsonIgnore
    public String getSigningAlgValuesSupportedAsString() {
        return HttpHelper.stringArrayToString(getSigningAlgValuesSupported());
    }

    @JsonIgnore
    public String getTokenEndpointAuthMethodsSupportedAsString() {
        return HttpHelper.stringArrayToString(getTokenEndpointAuthMethodsSupported());
    }

    @Override
    @JsonIgnore
    public boolean isConfigured() {
        return issuer != null;
    }
}