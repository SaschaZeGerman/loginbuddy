package net.loginbuddy.config.loginbuddy;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler;
import com.fasterxml.jackson.databind.deser.ValueInstantiator;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.config.DynamicProviderRegistrationException;
import net.loginbuddy.common.config.Meta;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.loginbuddy.common.OnBehalfOf;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

public class LoginbuddyObjectMapper extends ObjectMapper {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(LoginbuddyObjectMapper.class));

    public LoginbuddyObjectMapper() {
        SimpleModule moduleClientsObjectDeserializer = new SimpleModule("ClientsObjectDeserializer", new Version(1, 0, 0, null, null, null));
        moduleClientsObjectDeserializer.addDeserializer(Clients.class, new ClientObjectDeserializer());

        SimpleModule moduleProvidersObjectDeserializer = new SimpleModule("ProvidersObjectDeserializer", new Version(1, 0, 0, null, null, null));
        moduleProvidersObjectDeserializer.addDeserializer(Providers.class, new ProviderObjectDeserializer());

        registerModule(moduleClientsObjectDeserializer);
        registerModule(moduleProvidersObjectDeserializer);

        addHandler(new ProvidersDeserializationProblemHandler());
    }

    public Loginbuddy readLoginbuddy(JSONObject config) throws JsonProcessingException {
        return readLoginbuddy(config, null);
    }

    public Loginbuddy readLoginbuddy(File dbLocation) throws IOException, ParseException {
        return readLoginbuddy((JSONObject) new JSONParser().parse(new FileReader(dbLocation)));
    }

    // used in loginbuddy-test
    public Loginbuddy readLoginbuddy(File configFile, File configTemplateFile) throws IOException, ParseException {
        return readLoginbuddy(
                (JSONObject) new JSONParser().parse(new FileReader(configFile)),
                configTemplateFile == null ? null : (JSONObject) new JSONParser().parse(new FileReader(configTemplateFile))
        );
    }

    public Loginbuddy readLoginbuddy(JSONObject config, JSONObject configTemplate) throws JsonProcessingException {

        // check if this configuration is build for Loginbuddys very first version
        if (config.get("loginbuddy") != null) {
            JSONObject fromLegacy = new JSONObject();
            fromLegacy.put("clients", ((JSONObject) config.get("loginbuddy")).get("clients"));
            fromLegacy.put("providers", ((JSONObject) config.get("loginbuddy")).get("providers"));
            config = fromLegacy;
        }
        if (configTemplate != null) {
            config = mergeProviders(config, configTemplate);
        } else {
            for (Object next : (JSONArray) config.get("providers")) {
                if (((JSONObject) next).get("template") != null) {
                    throw new IllegalArgumentException(String.format("At least one provider references a template but no templates were given! Provider: '%s'", ((JSONObject) next).get("provider")));
                }
            }
        }
        return readValue(config.toJSONString(), Loginbuddy.class);
    }

    // used in loginbuddy-test
    public List<Clients> readClients(File clients) throws IOException {
        return readValue(clients, new TypeReference<>() {
        });
    }

    public List<Clients> readClients(String clients) throws IOException {
        return readValue(clients, new TypeReference<>() {
        });
    }

    public Clients readClient(String clients) throws IOException {
        return readValue(clients, Clients.class);
    }

    private JSONObject mergeProviders(JSONObject configJson, JSONObject configTemplateJson) {

        JSONObject result = new JSONObject();

        // if no clients are defined, the 'loginbuddy-sidecar' client is added to pass validations when searching for providers that belog to a specific client
        // TODO is there a scenario where this could be an issue?
        JSONArray clients = (JSONArray) configJson.get("clients");
        if (clients.size() == 0) {

            JSONArray redirectUris = new JSONArray();
            redirectUris.add(Constants.SIDECAR_REDIRECT_URI.getKey());

            JSONObject sidecarClient = new JSONObject();
            sidecarClient.put(Constants.CLIENT_ID.getKey(), Constants.SIDECAR_CLIENT_ID.getKey());
            sidecarClient.put(Constants.CLIENT_TYPE.getKey(), Constants.CLIENT_TYPE_CONFIDENTIAL.getKey());
            sidecarClient.put(Constants.REDIRECT_URIS.getKey(), redirectUris);

            clients.add(sidecarClient);
        }
        result.put("clients", clients);

        JSONArray resultProviders = new JSONArray();
        Iterator iter = ((JSONArray) configJson.get("providers")).iterator();
        DocumentContext ctx = JsonPath.parse(((JSONArray) configTemplateJson.get("providers")).toJSONString());
        while (iter.hasNext()) {
            final JSONObject nextProvider = (JSONObject) iter.next();
            if (nextProvider.get("template") != null) {
                JSONArray templates = null;
                try {
                    templates = (JSONArray) new JSONParser().parse(ctx.read(String.format("$..[?(@.provider == '%s')]", nextProvider.get("template"))).toString());
                    if (templates != null && templates.size() > 0) {
                        JSONObject pt = (JSONObject) templates.get(0);
                        pt.putAll(nextProvider);
                        resultProviders.add(pt);
                    } else {
                        LOGGER.severe(String.format("The referenced template: '%s' is unknown for provider configuration: '%s'", nextProvider.get("template"), nextProvider.get("provider")));
                        throw new IllegalArgumentException(String.format("The referenced template: '%s' is unknown for provider configuration: '%s'", nextProvider.get("template"), nextProvider.get("provider")));
                    }
                } catch (ParseException e) {
                    LOGGER.severe(String.format("The template of '%s' could not be applied!", nextProvider.get("template")));
                    throw new IllegalArgumentException(String.format("The template of '%s' could not be applied!", nextProvider.get("template")));
                }
            } else {
                resultProviders.add(nextProvider);
            }
        }
        result.put("providers", resultProviders);
        return result;
    }

}

class ProviderObjectDeserializer extends StdDeserializer<Providers> {

    private static final Logger LOGGER = Logger.getLogger(ProviderObjectDeserializer.class.getName());

    public ProviderObjectDeserializer() {
        this(null);
    }

    public ProviderObjectDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public Providers deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException {

        ObjectCodec codec = jsonParser.getCodec();
        JsonNode node = codec.readTree(jsonParser);

        JSONObject currentProvider = null;
        try {
            currentProvider = (JSONObject) new JSONParser().parse(node.toString());
        } catch (ParseException e) {
            LOGGER.severe(String.format("This should never happen: %s", e.getMessage()));
            throw new IllegalArgumentException("Provider could not be loaded due to an invalid JSON format!");
        }

        // Figure out what type of configuration we have (the same check as in LoginbuddyUtil)
        ProviderConfigType providerType = currentProvider.get(Constants.CLIENT_ID.getKey()) == null ? ProviderConfigType.MINIMAL : currentProvider.get("openid_configuration_uri") == null ? ProviderConfigType.FULL : ProviderConfigType.DEFAULT;

        Meta meta = new Meta();
        JSONObject oidcConfig = new JSONObject();
        String currentRedirectUri = (String) currentProvider.get(Constants.REDIRECT_URI.getKey());

        // DEFAULT or MINIMUM configuration, retrieve OIDC configuration
        if (ProviderConfigType.MINIMAL.equals(providerType) || ProviderConfigType.DEFAULT.equals(providerType)) {
            try {
                oidcConfig = retrieveOidcConfig((String) currentProvider.get("openid_configuration_uri"));
            } catch (Exception e) {
                LOGGER.warning(e.getMessage());
                meta.addStatus(Meta.STATUS_OIDC_CONFIG_ERROR, String.format("Retrieving OIDC configuration for provider: '%s' failed: %s", currentProvider.get(Constants.PROVIDER.getKey()), e.getMessage()));
            }
        }
        // MINIMUM configuration, register dynamically
        if (ProviderConfigType.MINIMAL.equals(providerType)) {
            String registerUrl = (String) oidcConfig.get(Constants.REGISTRATION_ENDPOINT.getKey());
            try {
                if (registerUrl == null || registerUrl.trim().length() == 0 || registerUrl.startsWith("http:")) {
                    LOGGER.warning(String.format("The provider does not support dynamic registration. Provider: %s", currentProvider.get(Constants.PROVIDER.getKey())));
                    meta.addStatus(Meta.STATUS_REGISTRATION_ERROR, String.format("The provider does not support dynamic registration. Provider: %s", currentProvider.get(Constants.PROVIDER.getKey())));
                    throw new DynamicProviderRegistrationException((String) currentProvider.get("The provider does not support dynamic registration"));
                }
                MsgResponse registration = HttpHelper.register(registerUrl, DiscoveryUtil.UTIL.getRedirectUri());
                JSONObject registrationMsg = (JSONObject) new JSONParser().parse(registration.getMsg());;
                if (registration.getStatus() == 200) {
                    currentProvider.put(Constants.CLIENT_ID.getKey(), registrationMsg.get(Constants.CLIENT_ID.getKey()));
                    currentProvider.put(Constants.CLIENT_SECRET.getKey(), registrationMsg.get(Constants.CLIENT_SECRET.getKey()));
                    LOGGER.info(String.format("Successfully registered with: %s!", currentProvider.get(Constants.PROVIDER.getKey())));
                    currentRedirectUri = DiscoveryUtil.UTIL.getRedirectUri();
                } else {
                    throw new DynamicProviderRegistrationException((String) currentProvider.get("error_description"));
                }
            } catch (Exception e) {
                LOGGER.warning(e.getMessage());
                meta.addStatus(Meta.STATUS_REGISTRATION_ERROR, String.format("Dynamic registration for provider: '%s' failed: %s", currentProvider.get(Constants.PROVIDER.getKey()), e.getMessage()));
            }
        }
        currentProvider.remove("openid_discovery_uri");
        oidcConfig.remove("openid_discovery_uri");

        currentProvider = augmentProviderConfiguration(oidcConfig, currentProvider, currentRedirectUri, false, false);

        Providers providers = new Providers((String) currentProvider.get(Constants.ISSUER.getKey()), (String) currentProvider.get(Constants.CLIENT_ID.getKey()), (String) currentProvider.get(Constants.REDIRECT_URI.getKey()), (String) currentProvider.get(Constants.CLIENT_SECRET.getKey()));
        providers.setProvider((String) currentProvider.get(Constants.PROVIDER.getKey()));
        providers.setOpenidConfigurationUri((String) currentProvider.get("openid_configuration_uri"));
        providers.setResponseType((String) currentProvider.get(Constants.RESPONSE_TYPE.getKey()));
        providers.setScope((String) currentProvider.get(Constants.SCOPE.getKey()));
        providers.setAuthorizationEndpoint((String) currentProvider.get(Constants.AUTHORIZATION_ENDPOINT.getKey()));
        providers.setPushedAuthorizationRequestEndpoint((String) currentProvider.get(Constants.PUSHED_AUTHORIZATION_REQUEST_ENDPOINT.getKey()));
        providers.setTokenEndpoint((String) currentProvider.get(Constants.TOKEN_ENDPOINT.getKey()));
        providers.setUserinfoEndpoint((String) currentProvider.get(Constants.USERINFO_ENDPOINT.getKey()));
        providers.setJwksUri((String) currentProvider.get(Constants.JWKS_URI.getKey()));
        providers.setResponseMode((String) currentProvider.get("response_mode"));
        providers.setPkce((Boolean) currentProvider.get(Constants.PKCE.getKey()));
        providers.setDpopSigningAlg((String) currentProvider.get(Constants.DPOP_SIGNING_ALG.getKey()));
        providers.setMappings((JSONObject) currentProvider.get("mappings"));
        providers.setTemplate((String) currentProvider.get("template"));

        Meta currentMeta = new ObjectMapper().readValue((String) currentProvider.get("_meta"), Meta.class);
        currentMeta.getStatus().putAll(meta.getStatus());

        providers.setMeta(currentMeta);

        return providers;
    }

    private JSONObject retrieveOidcConfig(String oidcConfigUrl) {
        try {
            return (JSONObject) new JSONParser().parse(HttpHelper.getAPI(oidcConfigUrl).getMsg());
        } catch (Exception e) {
            throw new DynamicProviderRegistrationException(String.format("Requesting the OpenID Configuration failed: %s", e.getMessage()));
        }
    }

    /**
     * This template matches what is configured in config.json. At least for fields that should be provided through an OpenID Connect Discovery endpoint and the Registration
     *
     * @param oidcConfig
     * @param providerConfig
     * @param redirectUri
     * @param updateProvider
     * @param updateIssuer
     * @return
     */
    private JSONObject augmentProviderConfiguration(JSONObject oidcConfig, JSONObject providerConfig, String redirectUri, boolean updateProvider, boolean updateIssuer) {

        JSONObject completedConfig = new JSONObject();

        completedConfig.put("provider", providerConfig.get(Constants.PROVIDER.getKey()));
        completedConfig.put("issuer", providerConfig.get(Constants.ISSUER.getKey()));
        if (updateProvider || (providerConfig.get(Constants.PROVIDER.getKey()) == null)) {
            completedConfig.put("provider", oidcConfig.get(Constants.ISSUER.getKey())); // issuer as provider, this is on purpose
        }
        if (updateIssuer || (providerConfig.get(Constants.ISSUER.getKey()) == null)) {
            completedConfig.put("issuer", oidcConfig.get(Constants.ISSUER.getKey()));
        }
        completedConfig.put("client_id", providerConfig.get(Constants.CLIENT_ID.getKey()));
        completedConfig.put("client_secret", providerConfig.get(Constants.CLIENT_SECRET.getKey()));
        completedConfig.put("redirect_uri", redirectUri == null ? providerConfig.get(Constants.REDIRECT_URI.getKey()) : redirectUri);
        completedConfig.put("response_type", providerConfig.get(Constants.RESPONSE_TYPE.getKey()));
        completedConfig.put("scope", providerConfig.get(Constants.SCOPE.getKey()));
        completedConfig.put(Constants.AUTHORIZATION_ENDPOINT.getKey(), providerConfig.get(Constants.AUTHORIZATION_ENDPOINT.getKey()));
        completedConfig.put("pushed_authorization_request_endpoint", providerConfig.get(Constants.PUSHED_AUTHORIZATION_REQUEST_ENDPOINT.getKey()));
        completedConfig.put("token_endpoint", providerConfig.get(Constants.TOKEN_ENDPOINT.getKey()));
        completedConfig.put("userinfo_endpoint", providerConfig.get(Constants.USERINFO_ENDPOINT.getKey()));
        completedConfig.put("jwks_uri", providerConfig.get(Constants.JWKS_URI.getKey()));
        completedConfig.put("response_mode", providerConfig.get("response_mode"));
        completedConfig.put("pkce", providerConfig.get(Constants.PKCE.getKey()));
        completedConfig.put("dpop_signing_alg", providerConfig.get(Constants.DPOP_SIGNING_ALG.getKey()));
        if (providerConfig.get("mappings") != null) {
            completedConfig.put("mappings", (providerConfig.get("mappings")));
        }
        // keep the reference to a template if one was used
        if (providerConfig.get("template") != null) {
            completedConfig.put("template", providerConfig.get("template"));
        }

        // start the validation process
        Meta meta = new Meta();

        if (completedConfig.get(Constants.PROVIDER.getKey()) == null || completedConfig.get(Constants.ISSUER.getKey()) == null) {
            meta.addStatus(Meta.STATUS_INCOMPLETE, "provider and issuer are required provider configuration values!");
        }

        if (completedConfig.get(Constants.AUTHORIZATION_ENDPOINT.getKey()) == null) {
            if (oidcConfig != null) {
                completedConfig.put(Constants.AUTHORIZATION_ENDPOINT.getKey(), oidcConfig.get(Constants.AUTHORIZATION_ENDPOINT.getKey()));
            }
        }

        if (completedConfig.get(Constants.CLIENT_ID.getKey()) == null ||
                completedConfig.get(Constants.REDIRECT_URI.getKey()) == null ||
                completedConfig.get(Constants.AUTHORIZATION_ENDPOINT.getKey()) == null) {
            meta.addStatus(Meta.STATUS_INCOMPLETE, String.format("provider %s is misconfigured: client_id, redirect_uri and authorization_endpoint are required provider configuration values!", completedConfig.get(Constants.PROVIDER.getKey())));
        }

        String scope = (String) completedConfig.get(Constants.SCOPE.getKey());
        if (scope == null) {
            if ((oidcConfig != null && oidcConfig.get(Constants.SCOPES_SUPPORTED.getKey()) != null)) {
                JSONArray oidcScopesSelected = new JSONArray();
                JSONArray scopesSupported = (JSONArray) oidcConfig.get(Constants.SCOPES_SUPPORTED.getKey());
                if (scopesSupported != null) {
                    for (Object next : scopesSupported) {
                        if ("openid".equalsIgnoreCase((String) next)) {
                            oidcScopesSelected.add(next);
                        } else if ("email".equalsIgnoreCase((String) next)) {
                            oidcScopesSelected.add(next);
                        } else if ("profile".equalsIgnoreCase((String) next)) {
                            oidcScopesSelected.add(next);
                        }
                        if (oidcScopesSelected.size() >= 5) {
                            break; // no need to work through a list of 1000 scopes ....
                        }
                    }
                    if (oidcScopesSelected.size() > 0) {
                        scope = HttpHelper.jsonArrayToString(oidcScopesSelected);
                    } else {
                        LOGGER.warning(String.format("No suitable scope value is supported by the provider: %s", completedConfig.get(Constants.PROVIDER.getKey())));
                    }
                }
            } else {
                scope = "openid";
                LOGGER.info("Setting default scope 'openid'");
            }
        }
        completedConfig.put("scope", scope);

        if (completedConfig.get(Constants.PKCE.getKey()) == null) {
            completedConfig.put("pkce", true);
            if (oidcConfig != null) {
                if (oidcConfig.get(Constants.CODE_CHALLENGE_METHODS_SUPPORTED.getKey()) != null) {
                    boolean s256Supported = false;
                    JSONArray algs = (JSONArray) oidcConfig.get(Constants.CODE_CHALLENGE_METHODS_SUPPORTED.getKey());
                    for (Object alg : algs) {
                        if ("S256".equalsIgnoreCase((String) alg)) {
                            s256Supported = true;
                            break;
                        }
                    }
                    if(!s256Supported) {
                        completedConfig.put("pkce", false);
                    }
                }
            }
        }

        String responseType = (String) completedConfig.get(Constants.RESPONSE_TYPE.getKey());
        if (responseType == null) {
            if (oidcConfig != null && oidcConfig.get(Constants.RESPONSE_TYPES_SUPPORTED.getKey()) != null) {
                responseType = ((JSONArray) oidcConfig.get(Constants.RESPONSE_TYPES_SUPPORTED.getKey())).contains("code") ? "code" :
                        ((JSONArray) oidcConfig.get(Constants.RESPONSE_TYPES_SUPPORTED.getKey())).contains("id_token") ? "id_token" : null;
                if (responseType == null) {
                    LOGGER.warning(String.format("No suitable response_type is supported by this provider: %s", completedConfig.get(Constants.PROVIDER.getKey())));
                    meta.addStatus(Meta.STATUS_UNSUPPORTED, String.format("No suitable response_type is supported by this provider: %s", completedConfig.get(Constants.PROVIDER.getKey())));
                }
            } else {
                responseType = Constants.CODE.getKey();
                LOGGER.info("Setting default response_type 'code'");
            }
            completedConfig.put(Constants.RESPONSE_TYPE.getKey(), responseType);
        } else {
            if (!(Constants.ID_TOKEN.getKey().equalsIgnoreCase(responseType) || Constants.CODE.getKey().equalsIgnoreCase(responseType))) {
                meta.addStatus(Meta.STATUS_UNSUPPORTED, String.format("Unsupported response_type configured: '%s'", responseType));
            }
        }

        String tokenEndpoint = (String) completedConfig.get(Constants.TOKEN_ENDPOINT.getKey());
        if (tokenEndpoint == null) {
            if (oidcConfig != null) {
                completedConfig.put(Constants.TOKEN_ENDPOINT.getKey(), oidcConfig.get(Constants.TOKEN_ENDPOINT.getKey()));
            }
        }
        if (Constants.CODE.getKey().equalsIgnoreCase(responseType) && completedConfig.get(Constants.TOKEN_ENDPOINT.getKey()) == null) {
            meta.addStatus(Meta.STATUS_INCOMPLETE, "token_endpoint is required with response_type=code for provider configuration values!");
        }

        String responseMode = (String) completedConfig.get("response_mode");
        if (responseMode == null) {
            LOGGER.info("response_mode was not configured and is not set!");
        } else if (!(Constants.RESPONSE_MODE_QUERY.getKey().equalsIgnoreCase(responseMode) || Constants.RESPONSE_MODE_FORM_POST.getKey().equalsIgnoreCase(responseMode))) {
            meta.addStatus(Meta.STATUS_UNSUPPORTED, String.format("Unsupported response_mode configured: '%s'", responseMode));
        }

        if (completedConfig.get(Constants.USERINFO_ENDPOINT.getKey()) == null) {
            if (oidcConfig != null) {
                completedConfig.put(Constants.USERINFO_ENDPOINT.getKey(), oidcConfig.get(Constants.USERINFO_ENDPOINT.getKey()));
            }
            if (completedConfig.get(Constants.USERINFO_ENDPOINT.getKey()) == null) {
                LOGGER.warning("userinfo_endpoint is not configured! No userinfo will be retrieved!");
            }
        }

        if (completedConfig.get(Constants.JWKS_URI.getKey()) == null) {
            if (oidcConfig != null) {
                completedConfig.put(Constants.JWKS_URI.getKey(), oidcConfig.get(Constants.JWKS_URI.getKey()));
            }
            if (completedConfig.get(Constants.JWKS_URI.getKey()) == null) {
                LOGGER.warning("jwks_uri is not configured! No id_token validation possible!");
            }
        }

        if (completedConfig.get(Constants.PUSHED_AUTHORIZATION_REQUEST_ENDPOINT.getKey()) == null) {
            if (oidcConfig != null) {
                completedConfig.put(Constants.PUSHED_AUTHORIZATION_REQUEST_ENDPOINT.getKey(), oidcConfig.get(Constants.PUSHED_AUTHORIZATION_REQUEST_ENDPOINT.getKey()));
            }
            if (completedConfig.get(Constants.PUSHED_AUTHORIZATION_REQUEST_ENDPOINT.getKey()) == null) {
                LOGGER.warning("pushed_authorization_request_endpoint is not configured!");
            }
        }

        String dpopSigningAlgorithm = (String) completedConfig.get(Constants.DPOP_SIGNING_ALG.getKey());
        if (dpopSigningAlgorithm == null) {
            if (oidcConfig != null) {
                if (oidcConfig.get(Constants.DPOP_SIGNING_ALG_VALUES_SUPPORTED.getKey()) != null) {
                    JSONArray algs = (JSONArray) oidcConfig.get(Constants.DPOP_SIGNING_ALG_VALUES_SUPPORTED.getKey());
                    for (Object alg : algs) {
                        // we prefer ES256 over RS256
                        if ("ES256".equalsIgnoreCase((String) alg)) {
                            dpopSigningAlgorithm = "ES256";
                            break;
                        } else if ("RS256".equalsIgnoreCase((String) alg) && dpopSigningAlgorithm == null) {
                            dpopSigningAlgorithm = "RS256";
                        }
                    }
                }
            }
        }
        if (dpopSigningAlgorithm != null) {
            if (!("ES256".equalsIgnoreCase(dpopSigningAlgorithm) || "RS256".equalsIgnoreCase(dpopSigningAlgorithm))) {
                LOGGER.info(String.format("No suitable algorithm for DPoP is configured for this provider: %s", completedConfig.get(Constants.PROVIDER.getKey())));
            } else {
                completedConfig.put(Constants.DPOP_SIGNING_ALG.getKey(), dpopSigningAlgorithm);
            }
        }

        try {
            completedConfig.put("_meta", new ObjectMapper().writeValueAsString(meta));
        } catch (JsonProcessingException e) {
            LOGGER.warning("Meta information could not be added to current provider configuration");
        }

        return completedConfig;
    }
}

class ClientObjectDeserializer extends StdDeserializer<Clients> {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(ClientObjectDeserializer.class));

    public ClientObjectDeserializer() {
        this(null);
    }

    protected ClientObjectDeserializer(Class<Clients> t) {
        super(t);
    }

    @Override
    public Clients deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {

        ObjectCodec codec = jsonParser.getCodec();
        JsonNode node = codec.readTree(jsonParser);

        Clients clients = new Clients();

        JSONObject currentClient = null;
        try {
            currentClient = (JSONObject) new JSONParser().parse(node.toString());
        } catch (ParseException e) {
            LOGGER.severe(String.format("This should never happen: %s", e.getMessage()));
            throw new IllegalArgumentException("Client could not be loaded due to an invalid JSON format!");
        }

        // required configuration
        Set<String> redirectUris = new HashSet<>();
        if (currentClient.get("redirect_uris") != null) {
            for (Object redirectUri : ((JSONArray) currentClient.get("redirect_uris"))) {
                redirectUris.add((String) redirectUri);
            }
        }
        clients.setRedirectUris(redirectUris);

        // merge redirect_uri into redirect_uris. redirect_uri is valid for older configurations
        if (currentClient.get("redirect_uri") != null) {
            redirectUris.addAll(Arrays.asList(((String) currentClient.get("redirect_uri")).split("[;, ]")));
        }

        if (clients.getRedirectUrisCount() == 0) {
            clients.getMeta().addStatus(Meta.STATUS_INCOMPLETE, "Missing redirect_uri, at least one must be defined");
        }

        if (currentClient.get("client_id") != null) {
            clients.setClientId((String) currentClient.get("client_id"));
        } else {
            clients.getMeta().addStatus(Meta.STATUS_INCOMPLETE, "Missing client_id");
        }

        if (
                Constants.CLIENT_TYPE_CONFIDENTIAL.getKey().equalsIgnoreCase((String) currentClient.get("client_type")) ||
                        Constants.CLIENT_TYPE_PUBLIC.getKey().equalsIgnoreCase((String) currentClient.get("client_type"))) {
            clients.setClientType((String) currentClient.get("client_type"));
        } else {
            LOGGER.warning(String.format("Missing or invalid client_type: '%s'", currentClient.get("client_type")));
            clients.getMeta().addStatus(Meta.STATUS_INCOMPLETE, "Missing or invalid client_type");
        }

        if (currentClient.get("client_uri") != null) {
            clients.setClientUri((String) currentClient.get("client_uri"));
        }

        if (currentClient.get("client_secret") != null) {
            clients.setClientSecret((String) currentClient.get("client_secret"));
        } else if (Constants.CLIENT_TYPE_CONFIDENTIAL.getKey().equals(clients.getClientType())) {
            clients.getMeta().addStatus(Meta.STATUS_INCOMPLETE, "Missing client_secret");
        }

        List<String> providers = new ArrayList<>();
        if (currentClient.get("providers") != null) {
            for (Object provider : ((JSONArray) currentClient.get("providers"))) {
                providers.add((String) provider);
            }
        }
        clients.setClientProviders(providers);

        clients.setAcceptDynamicProvider(
                currentClient.get("accept_dynamic_provider") != null &&
                        (Boolean) currentClient.get("accept_dynamic_provider"));

        String signAlg = (String) currentClient.get("signed_response_alg");
        if (signAlg != null) {
            if (("ES256".equalsIgnoreCase(signAlg) || "RS256".equalsIgnoreCase(signAlg))) {
                clients.setSignedResponseAlg(signAlg);
            } else {
                LOGGER.warning(String.format("Unsupported signed_response_alg configured: '%s'", signAlg));
                clients.getMeta().addStatus(Meta.STATUS_INCOMPLETE, "Unsupported signed_response_alg configured!");
            }
        }

        List<OnBehalfOf> onBehalfOfs = new ArrayList<>();
        if (currentClient.get("on_behalf_of") != null) {
            for (Object onBehalfOf : ((JSONArray) currentClient.get("on_behalf_of"))) {
                String tokenType = (String) ((JSONObject) onBehalfOf).get("token_type");
                String alg = (String) ((JSONObject) onBehalfOf).get("alg");
                onBehalfOfs.add(new OnBehalfOf(tokenType, alg));
            }
        }
        clients.setOnBehalfOf(onBehalfOfs);

        if (currentClient.get("client_name") != null) {
            clients.setClientName((String) currentClient.get("client_name"));
        } else {
            clients.setClientName((String) currentClient.get("client_id"));
        }
        if (currentClient.get("tos_uri") != null) {
            clients.setClientTosUri((String) currentClient.get("tos_uri"));
        }
        if (currentClient.get("policy_uri") != null) {
            clients.setClientPolicyUri((String) currentClient.get("policy_uri"));
        }
        if (currentClient.get("logo_uri") != null) {
            clients.setClientLogoUri((String) currentClient.get("logo_uri"));
        }

        Set<String> contacts = new HashSet<>();
        if (currentClient.get("contacts") != null) {
            for (Object contact : ((JSONArray) currentClient.get("contacts"))) {
                contacts.add((String) contact);
            }
        }
        clients.setClientContacts(contacts);

        return clients;
    }
}

class ProvidersDeserializationProblemHandler extends DeserializationProblemHandler {

    @Override
    public Object handleInstantiationProblem(DeserializationContext ctxt, Class<?> instClass, Object argument, Throwable t) throws IOException {
        if (t instanceof DynamicProviderRegistrationException) {
            System.out.println(t.getMessage());
        }
        return argument;
    }

    @Override
    public boolean handleUnknownProperty(DeserializationContext ctxt, JsonParser p, JsonDeserializer<?> deserializer, Object beanOrClass, String propertyName) throws IOException {
        return super.handleUnknownProperty(ctxt, p, deserializer, beanOrClass, propertyName);
    }

    @Override
    public Object handleWeirdKey(DeserializationContext ctxt, Class<?> rawKeyType, String keyValue, String failureMsg) throws IOException {
        return super.handleWeirdKey(ctxt, rawKeyType, keyValue, failureMsg);
    }

    @Override
    public Object handleWeirdStringValue(DeserializationContext ctxt, Class<?> targetType, String valueToConvert, String failureMsg) throws IOException {
        return super.handleWeirdStringValue(ctxt, targetType, valueToConvert, failureMsg);
    }

    @Override
    public Object handleWeirdNumberValue(DeserializationContext ctxt, Class<?> targetType, Number valueToConvert, String failureMsg) throws IOException {
        return super.handleWeirdNumberValue(ctxt, targetType, valueToConvert, failureMsg);
    }

    @Override
    public Object handleWeirdNativeValue(DeserializationContext ctxt, JavaType targetType, Object valueToConvert, JsonParser p) throws IOException {
        return super.handleWeirdNativeValue(ctxt, targetType, valueToConvert, p);
    }

    @Override
    public Object handleUnexpectedToken(DeserializationContext ctxt, JavaType targetType, JsonToken t, JsonParser p, String failureMsg) throws IOException {
        return super.handleUnexpectedToken(ctxt, targetType, t, p, failureMsg);
    }

    @Override
    public Object handleMissingInstantiator(DeserializationContext ctxt, Class<?> instClass, ValueInstantiator valueInsta, JsonParser p, String msg) throws IOException {
        return super.handleMissingInstantiator(ctxt, instClass, valueInsta, p, msg);
    }

    @Override
    public JavaType handleUnknownTypeId(DeserializationContext ctxt, JavaType baseType, String subTypeId, TypeIdResolver idResolver, String failureMsg) throws IOException {
        return super.handleUnknownTypeId(ctxt, baseType, subTypeId, idResolver, failureMsg);
    }

    @Override
    public JavaType handleMissingTypeId(DeserializationContext ctxt, JavaType baseType, TypeIdResolver idResolver, String failureMsg) throws IOException {
        return super.handleMissingTypeId(ctxt, baseType, idResolver, failureMsg);
    }
}