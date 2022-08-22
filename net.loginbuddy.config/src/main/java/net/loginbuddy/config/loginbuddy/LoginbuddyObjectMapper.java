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
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.loginbuddy.common.Meta;
import net.loginbuddy.config.loginbuddy.common.OnBehalfOf;
import net.loginbuddy.config.loginbuddy.exception.DynamicProviderRegistrationException;
import net.loginbuddy.config.loginbuddy.exception.RequiredConfigurationException;
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

    private Logger LOGGER = Logger.getLogger(String.valueOf(LoginbuddyObjectMapper.class));

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
        result.put("clients", configJson.get("clients"));
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

    private Logger LOGGER = Logger.getLogger(String.valueOf(ProviderObjectDeserializer.class));

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

        Providers providers = new Providers();

        JSONObject currentProvider = null;
        try {
            currentProvider = (JSONObject) new JSONParser().parse(node.toString());
        } catch (ParseException e) {
            LOGGER.severe(String.format("This should never happen: %s", e.getMessage()));
            throw new IllegalArgumentException("Provider could not be loaded due to an invalid JSON format!");
        }

        // required configuration
        if (currentProvider.get(Constants.PROVIDER.getKey()) == null || currentProvider.get(Constants.ISSUER.getKey()) == null) {
            providers.getMeta().addStatus(Meta.STATUS_INCOMPLETE, "provider and issuer are required provider configuration values!");
        }
        providers.setProvider((String) currentProvider.get(Constants.PROVIDER.getKey()));
        providers.setIssuer((String) currentProvider.get(Constants.ISSUER.getKey()));
        providers.setOpenidConfigurationUri((String) currentProvider.get("openid_configuration_uri"));

        // Figure out what type of configuration we have (the same check as in LoginbuddyUtil)
        ProviderConfigType providerType = currentProvider.get(Constants.CLIENT_ID.getKey()) == null ? ProviderConfigType.MINIMAL : providers.getOpenidConfigurationUri() == null ? ProviderConfigType.FULL : ProviderConfigType.DEFAULT;

        // DEFAULT, using OIDC configuration endpoint to get authorization server details
        if (ProviderConfigType.DEFAULT.equals(providerType)) {
            try {
                JSONObject retrieveOidcConfig = retrieveOidcConfig(providers.getOpenidConfigurationUri());
                currentProvider.put(Constants.AUTHORIZATION_ENDPOINT.getKey(), retrieveOidcConfig.get(Constants.AUTHORIZATION_ENDPOINT.getKey()));
                currentProvider.put(Constants.TOKEN_ENDPOINT.getKey(), retrieveOidcConfig.get(Constants.TOKEN_ENDPOINT.getKey()));
                currentProvider.put(Constants.USERINFO_ENDPOINT.getKey(), retrieveOidcConfig.get(Constants.USERINFO_ENDPOINT.getKey()));
                currentProvider.put(Constants.JWKS_URI.getKey(), retrieveOidcConfig.get(Constants.JWKS_URI.getKey()));
            } catch (Exception e) {
                LOGGER.warning(e.getMessage());
                providers.getMeta().addStatus(Meta.STATUS_OIDC_CONFIG_ERROR, String.format("Retrieving OIDC configuration for provider: '%s' failed: %s", providers.getProvider(), e.getMessage()));
            }
        } // MINIMUM configuration, configured for dynamic registration
        else if (ProviderConfigType.MINIMAL.equals(providerType)) {
            LOGGER.info(String.format("Registering for provider: '%s'", providers.getProvider()));
            try {
                currentProvider = HttpHelper.retrieveAndRegister(providers.getOpenidConfigurationUri(), DiscoveryUtil.UTIL.getRedirectUri());
                if (currentProvider.get("error") == null) {
                    currentProvider.put(Constants.PROVIDER.getKey(), Objects.requireNonNullElse((String) currentProvider.get(Constants.PROVIDER.getKey()), providers.getProvider()));
                    currentProvider.put(Constants.ISSUER.getKey(), Objects.requireNonNullElse((String) currentProvider.get(Constants.ISSUER.getKey()), providers.getIssuer()));
                    LOGGER.info(String.format("Successfully registered with: %s!", currentProvider.get(Constants.PROVIDER.getKey())));
                } else {
                    throw new DynamicProviderRegistrationException((String) currentProvider.get("error_description"));
                }
            } catch (Exception e) {
                LOGGER.warning(e.getMessage());
                providers.getMeta().addStatus(Meta.STATUS_REGISTRATION_ERROR, String.format("Dynamic registration for provider: '%s' failed: %s", providers.getProvider(), e.getMessage()));
            }
        }

        // FULL configuration as of here, check for required values
        if (currentProvider.get(Constants.CLIENT_ID.getKey()) == null ||
                currentProvider.get(Constants.REDIRECT_URI.getKey()) == null ||
                currentProvider.get(Constants.AUTHORIZATION_ENDPOINT.getKey()) == null) {
            providers.getMeta().addStatus(Meta.STATUS_INCOMPLETE, String.format("provider %s is misconfigured: client_id, redirect_uri and authorization_endpoint are required provider configuration values!", providers.getProvider()));
        }

        providers.setClientId((String) currentProvider.get(Constants.CLIENT_ID.getKey()));
        providers.setRedirectUri((String) currentProvider.get(Constants.REDIRECT_URI.getKey()));
        providers.setAuthorizationEndpoint((String) currentProvider.get(Constants.AUTHORIZATION_ENDPOINT.getKey()));

        // Optional, with defaults
        providers.setScope(Objects.requireNonNullElse((String) currentProvider.get(Constants.SCOPE.getKey()), "openid"));
        providers.setPkce(currentProvider.get("pkce") == null || (Boolean) currentProvider.get("pkce"));

        String responseType = (String) currentProvider.get(Constants.RESPONSE_TYPE.getKey());
        if (responseType == null) {
            providers.setResponseType(Constants.CODE.getKey());
        } else if (Constants.ID_TOKEN.getKey().equalsIgnoreCase(responseType) || Constants.CODE.getKey().equalsIgnoreCase(responseType)) {
            providers.setResponseType(responseType);
        } else {
            providers.getMeta().addStatus(Meta.STATUS_UNSUPPORTED, String.format("Unsupported response_type configured: '%s'", responseType));
        }

        if (Constants.CODE.getKey().equalsIgnoreCase(responseType) && currentProvider.get(Constants.TOKEN_ENDPOINT.getKey()) == null) {
            providers.getMeta().addStatus(Meta.STATUS_INCOMPLETE, "token_endpoint is required with response_type=code for provider configuration values!");
        }
        if (currentProvider.get(Constants.TOKEN_ENDPOINT.getKey()) != null) {
            providers.setTokenEndpoint((String) currentProvider.get(Constants.TOKEN_ENDPOINT.getKey()));
        }

        String responseMode = (String) currentProvider.get("response_mode");
        if (responseMode == null) {
            providers.setResponseMode(Constants.RESPONSE_MODE_QUERY.getKey());
        } else if (Constants.RESPONSE_MODE_QUERY.getKey().equalsIgnoreCase(responseMode) || Constants.RESPONSE_MODE_FORM_POST.getKey().equalsIgnoreCase(responseMode)) {
            providers.setResponseMode(responseMode);
        } else {
            providers.getMeta().addStatus(Meta.STATUS_UNSUPPORTED, String.format("Unsupported response_mode configured: '%s'", responseMode));
        }

        // other values
        if (currentProvider.get("userinfo_endpoint") != null) {
            providers.setUserinfoEndpoint((String) currentProvider.get("userinfo_endpoint"));
        } else {
            LOGGER.warning("userinfo_endpoint is not configured! No userinfo will be retrieved!");
        }

        if (currentProvider.get("client_secret") != null) {
            providers.setClientSecret((String) currentProvider.get("client_secret"));
        }

        if (currentProvider.get("jwks_uri") != null) {
            providers.setJwksUri((String) currentProvider.get("jwks_uri"));
        } else {
            LOGGER.warning("jwks_uri is not configured! No id_token validation possible!");
        }

        if (currentProvider.get("mappings") != null) {
            providers.setMappings((JSONObject) currentProvider.get("mappings"));
        }

        // keep the reference to a template if one was used
        if (currentProvider.get("template") != null) {
            providers.setTemplate((String) currentProvider.get("template"));
        }

        return providers;
    }

    private JSONObject retrieveOidcConfig(String oidcConfigUrl) throws IOException, ParseException {
        try {
            MsgResponse msg = HttpHelper.getAPI(oidcConfigUrl);
            if (msg.getStatus() == 200) {
                return (JSONObject) new JSONParser().parse(msg.getMsg());
            }
            throw new DynamicProviderRegistrationException(String.format("Given URL: %s, http status: %s", oidcConfigUrl, msg.getStatus()));
        } catch (Exception e) {
            throw new DynamicProviderRegistrationException(String.format("Requesting the OpenID Configuration failed: %s", e.getMessage()));
        }
    }
}

class ClientObjectDeserializer extends StdDeserializer<Clients> {

    private Logger LOGGER = Logger.getLogger(String.valueOf(ClientObjectDeserializer.class));

    public ClientObjectDeserializer() {
        this(null);
    }

    protected ClientObjectDeserializer(Class<Clients> t) {
        super(t);
    }

    @Override
    public Clients deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JacksonException {

        try {
            ObjectCodec codec = jsonParser.getCodec();
            JsonNode node = codec.readTree(jsonParser);

            JSONObject obj = (JSONObject) new JSONParser().parse(node.toString());

            Clients result = new Clients();

            Set<String> redirectUris = new HashSet<>();
            if (obj.get("redirect_uris") != null) {
                for (Object redirectUri : ((JSONArray) obj.get("redirect_uris"))) {
                    redirectUris.add((String) redirectUri);
                }
            }
            result.setRedirectUris(redirectUris);

            // merge redirect_uri into redirect_uris. redirect_uri is valid for older configurations
            if (obj.get("redirect_uri") != null) {
                redirectUris.addAll(Arrays.asList(((String) obj.get("redirect_uri")).split("[;, ]")));
            }

            if (result.getRedirectUrisCount() == 0) {
                throw new IllegalArgumentException("Missing redirect_uri, at least one must be defined");
            }

            if (obj.get("client_id") != null) {
                result.setClientId((String) obj.get("client_id"));
            } else {
                throw new IllegalArgumentException("Missing client_id");
            }

            if (
                    Constants.CLIENT_TYPE_CONFIDENTIAL.getKey().equalsIgnoreCase((String) obj.get("client_type")) ||
                            Constants.CLIENT_TYPE_PUBLIC.getKey().equalsIgnoreCase((String) obj.get("client_type"))) {
                result.setClientType((String) obj.get("client_type"));
            } else {
                throw new IllegalArgumentException("Missing or invalid client_type");
            }

            if (obj.get("client_uri") != null) {
                result.setClientUri((String) obj.get("client_uri"));
            }

            if (obj.get("client_secret") != null) {
                result.setClientSecret((String) obj.get("client_secret"));
            } else if (Constants.CLIENT_TYPE_CONFIDENTIAL.getKey().equals(result.getClientType())) {
                throw new IllegalArgumentException("Missing client_secret");
            }

            List<String> providers = new ArrayList<>();
            if (obj.get("providers") != null) {
                for (Object provider : ((JSONArray) obj.get("providers"))) {
                    providers.add((String) provider);
                }
            }
            result.setClientProviders(providers);

            result.setAcceptDynamicProvider(
                    obj.get("accept_dynamic_provider") != null &&
                            (Boolean) obj.get("accept_dynamic_provider"));

            String signAlg = (String) obj.get("signed_response_alg");
            if (signAlg != null) {
                if (("ES256".equalsIgnoreCase(signAlg) || "RS256".equalsIgnoreCase(signAlg))) {
                    result.setSignedResponseAlg(signAlg);
                } else {
                    throw new IllegalArgumentException(String.format("Unsupported signed_response_alg configured: '%s'", signAlg));
                }
            }

            List<OnBehalfOf> onBehalfOfs = new ArrayList<>();
            if (obj.get("on_behalf_of") != null) {
                for (Object onBehalfOf : ((JSONArray) obj.get("on_behalf_of"))) {
                    String tokenType = (String) ((JSONObject) onBehalfOf).get("token_type");
                    String alg = (String) ((JSONObject) onBehalfOf).get("alg");
                    onBehalfOfs.add(new OnBehalfOf(tokenType, alg));
                }
            }
            result.setOnBehalfOf(onBehalfOfs);

            if (obj.get("client_name") != null) {
                result.setClientName((String) obj.get("client_name"));
            } else {
                result.setClientName((String) obj.get("client_id"));
            }
            if (obj.get("tos_uri") != null) {
                result.setClientTosUri((String) obj.get("tos_uri"));
            }
            if (obj.get("policy_uri") != null) {
                result.setClientPolicyUri((String) obj.get("policy_uri"));
            }
            if (obj.get("logo_uri") != null) {
                result.setClientLogoUri((String) obj.get("logo_uri"));
            }

            Set<String> contacts = new HashSet<>();
            if (obj.get("contacts") != null) {
                for (Object contact : ((JSONArray) obj.get("contacts"))) {
                    contacts.add((String) contact);
                }
            }
            result.setClientContacts(contacts);

            return result;

        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }
}

class ProvidersDeserializationProblemHandler extends DeserializationProblemHandler {

    @Override
    public Object handleInstantiationProblem(DeserializationContext ctxt, Class<?> instClass, Object argument, Throwable t) throws IOException {
        if (t instanceof net.loginbuddy.config.loginbuddy.exception.DynamicProviderRegistrationException) {
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