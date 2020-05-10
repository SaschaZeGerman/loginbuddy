package net.loginbuddy.service.config.discovery;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import net.loginbuddy.common.config.Constants;

import java.io.IOException;

public class DiscoveryConfigDeserializer extends StdDeserializer<DiscoveryConfig> {

    private com.fasterxml.jackson.databind.ObjectMapper MAPPER = new ObjectMapper();

    protected DiscoveryConfigDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public DiscoveryConfig deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JsonProcessingException {
        JsonNode node = p.getCodec().readTree(p);
        DiscoveryConfig config = new DiscoveryConfig();

        // required
        config.setIssuer(node.get(Constants.ISSUER.getKey()).asText());
        config.setAuthorizationEndpoint(node.get(Constants.AUTHORIZATION_ENDPOINT.getKey()).asText());
        config.setTokenEndpoint(node.get(Constants.TOKEN_ENDPOINT.getKey()).asText());
        config.setJwksUri(node.get(Constants.JWKS_URI.getKey()).asText());
        config.setResponseTypesSupported(toStringArrayValue(Constants.RESPONSE_TYPES_SUPPORTED.getKey(), node));
        config.setGrantTypesSupported(toStringArrayValue(Constants.GRANT_TYPES_SUPPORTED.getKey(), node));
        config.setSubjectTypesSupported(toStringArrayValue(Constants.SUBJECT_TYPES_SUPPORTED.getKey(), node));
        config.setIdTokenSigningAlgValuesSupported(toStringArrayValue(Constants.ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED.getKey(), node));

        // optional
        config.setTokenEndpointAuthMethodsSupported(toStringArrayValue(Constants.TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED.getKey(), node));
        config.setScopesSupported(toStringArrayValue(Constants.SCOPES_SUPPORTED.getKey(), node));
        config.setCodeChallengeMethodsSupported(toStringArrayValue(Constants.CODE_CHALLENGE_METHODS_SUPPORTED.getKey(), node));
        config.setServiceDocumentation(toStringValue(Constants.SERVICE_DOCUMENTATION.getKey(), node));
        config.setUserinfoEndpoint(toStringValue(Constants.USERINFO_ENDPOINT.getKey(), node));
        config.setPushedAuthorizationRequestEndpoint(toStringValue(Constants.PUSHED_AUTHORIZATION_REQUEST_ENDPOINT.getKey(), node));

        // optional, Loginbuddy specific
        if(node.get("management") != null) {
            config.setManagement(MAPPER.readValue(node.get("management").traverse(), Management.class));
        }
        config.setSigningAlgValuesSupported(toStringArrayValue("signing_alg_values_supported", node));
        return config;
    }

    private String toStringValue(String fieldName, JsonNode node) {
        return node.get(fieldName) == null ? null : node.get(fieldName).asText();
    }

    private String[] toStringArrayValue(String fieldName, JsonNode node) {
        if(node != null) {
            String[] result = new String[(node.withArray(fieldName)).size()];
            for (int i = 0; i < result.length; i++) {
                result[i] = node.withArray(fieldName).get(i).asText();
            }
            return result;
        }
        return null;
    }
}