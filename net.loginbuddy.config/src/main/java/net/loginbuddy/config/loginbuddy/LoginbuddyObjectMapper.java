package net.loginbuddy.config.loginbuddy;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
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

    private Logger LOGGER = Logger.getLogger(String.valueOf(LoginbuddyObjectMapper.class));

    public LoginbuddyObjectMapper() {
        SimpleModule moduleClientsObjectDeserializer = new SimpleModule("ClientsObjectDeserializer", new Version(1, 0, 0, null, null, null));
        moduleClientsObjectDeserializer.addDeserializer(Clients.class, new ClientObjectDeserializer());

        SimpleModule moduleProvidersObjectDeserializer = new SimpleModule("moduleProvidersObjectDeserializer", new Version(1, 0, 0, null, null, null));
        moduleProvidersObjectDeserializer.addDeserializer(Providers.class, new ProviderObjectDeserializer());

        registerModule(moduleClientsObjectDeserializer);
        registerModule(moduleProvidersObjectDeserializer);
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
            for(Object next : (JSONArray)config.get("providers")) {
                if(((JSONObject)next).get("template") != null) {
                    throw new IllegalArgumentException(String.format("At least one provider references a template but no templates were given! Provider: '%s'", ((JSONObject)next).get("provider")));
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
    public Providers deserialize(JsonParser jsonParser, DeserializationContext ctxt) throws IOException, JacksonException {

        try {
            ObjectCodec codec = jsonParser.getCodec();
            JsonNode node = codec.readTree(jsonParser);

            JSONObject obj = (JSONObject) new JSONParser().parse(node.toString());

            Providers result = new Providers();

            // required configuration

            result.setProvider((String) obj.get("provider"));
            result.setIssuer((String) obj.get("issuer"));
            result.setOpenidConfigurationUri((String) obj.get("openid_configuration_uri"));

            // MINIMUM configuration, configured for dynamic registration
            if (obj.size() == 3 && result.getProvider() != null && result.getIssuer() != null && result.getOpenidConfigurationUri() != null) {
                LOGGER.info("Minimum configuration, going to enhance it!");
                try {
                    JSONObject retrieveAndRegister = HttpHelper.retrieveAndRegister(result.getOpenidConfigurationUri(), DiscoveryUtil.UTIL.getRedirectUri());
                    if (retrieveAndRegister.get("error") == null) {
                        retrieveAndRegister.put("provider", result.getProvider());
                        retrieveAndRegister.put("issuer", result.getIssuer());
                        retrieveAndRegister.put("openid_configuration_uri", result.getOpenidConfigurationUri());
                        obj = retrieveAndRegister;
                        LOGGER.info(String.format("Successfully registered with: %s!", result.getProvider()));
                    } else {
                        LOGGER.warning(String.format("Could not register for: '%s'", retrieveAndRegister.get("error_description")));
                    }
                } catch(Exception e) {
                    LOGGER.severe(String.format("Dynamic registration for provider: '%s' failed: %s", result.getProvider(), e.getMessage()));
                    throw new IllegalArgumentException(String.format("Dynamic registration for provider: '%s' failed. See logs", result.getProvider()));
                }
            }
            // DEFAULT configuration, openid discovery is used
            else if (obj.get("client_id") != null && obj.get("openid_configuration_uri") != null) {
                JSONObject retrieveOidcConfig = retrieveOidcConfig((String) obj.get("openid_configuration_uri"));
                obj.put(Constants.AUTHORIZATION_ENDPOINT.getKey(), retrieveOidcConfig.get(Constants.AUTHORIZATION_ENDPOINT.getKey()));
                obj.put(Constants.TOKEN_ENDPOINT.getKey(), retrieveOidcConfig.get(Constants.TOKEN_ENDPOINT.getKey()));
                obj.put(Constants.USERINFO_ENDPOINT.getKey(), retrieveOidcConfig.get(Constants.USERINFO_ENDPOINT.getKey()));
                obj.put(Constants.JWKS_URI.getKey(), retrieveOidcConfig.get(Constants.JWKS_URI.getKey()));
            }

            // FULL configuration as of here

            result.setClientId((String) obj.get("client_id"));
            result.setRedirectUri((String) obj.get("redirect_uri"));

            // optional with defaults
            String responseType = (String) obj.get("response_type");
            if (responseType != null) {
                if ("id_token".equalsIgnoreCase(responseType) || "code".equalsIgnoreCase(responseType)) {
                    result.setResponseType((String) obj.get("response_type"));
                } else {
                    throw new IllegalArgumentException(String.format("Unsupported response_type configured: '%s'", responseType));
                }
            } else {
                result.setResponseType("code");
            }

            String scope = (String) obj.get("scope");
            if (scope != null) {
                result.setScope(scope);
            } else {
                result.setScope("openid");
            }

            String authorizationEndpoint = (String) obj.get("authorization_endpoint");
            if (authorizationEndpoint == null) {
                throw new IllegalArgumentException("Missing authorization_endpoint configuration");
            } else {
                result.setAuthorizationEndpoint(authorizationEndpoint);
            }

            String tokenEndpoint = (String) obj.get("token_endpoint");
            if (tokenEndpoint == null) {
                if ("code".equalsIgnoreCase(result.getResponseType())) {
                    throw new IllegalArgumentException("Missing token_endpoint configuration");
                }
            } else {
                result.setTokenEndpoint(tokenEndpoint);
            }

            String responseMode = (String) obj.get("response_mode");
            if (responseMode != null) {
                if ("query".equalsIgnoreCase(responseMode) || "form_post".equalsIgnoreCase(responseMode)) {
                    result.setResponseMode(responseMode);
                } else {
                    throw new IllegalArgumentException(String.format("Unsupported response_mode configured: '%s'", responseMode));
                }
            }

            // other values
            if (obj.get("userinfo_endpoint") != null) {
                result.setUserinfoEndpoint((String) obj.get("userinfo_endpoint"));
            }

            if (obj.get("client_secret") != null) {
                result.setClientSecret((String) obj.get("client_secret"));
            }

            if (obj.get("jwks_uri") != null) {
                result.setJwksUri((String) obj.get("jwks_uri"));
            } else {
                LOGGER.warning("jwks_uri is not configured! No id_token validation possible!");
            }

            result.setPkce(obj.get("pkce") == null || (Boolean) obj.get("pkce"));

            if (obj.get("mappings") != null) {
                result.setMappings( (JSONObject)obj.get("mappings") );
            }

            return result;

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject retrieveOidcConfig(String oidcConfigUrl) throws IOException, ParseException {
        JSONObject oidcConfig = null;
        MsgResponse msg = HttpHelper.getAPI(oidcConfigUrl);
        if (msg.getStatus() == 200) {
            return (JSONObject) new JSONParser().parse(msg.getMsg());
        } else {
            LOGGER.warning(
                    String.format("Requesting the OpenID Configuration failed. Given URL: %s, http status: %s", oidcConfigUrl, msg.getStatus()));
            return null;
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

        } catch (ParseException e) {
            e.printStackTrace();
        }
        return null;
    }
}