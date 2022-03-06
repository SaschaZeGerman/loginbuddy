package net.loginbuddy.config.loginbuddy;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.loginbuddy.common.config.Constants;
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
        registerModule(moduleClientsObjectDeserializer);
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
        }
        return readValue(config.toJSONString(), Loginbuddy.class);
    }

    public List<Clients> readClients(File clients) throws IOException {
        return readValue(clients, new TypeReference<>() {});
    }

    public List<Clients> readClients(String clients) throws IOException {
        return readValue(clients, new TypeReference<>() {});
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
                        LOGGER.severe(String.format("The referenced template '%s' is unknown. The provider configuration '%s' is being ignored!", nextProvider.get("template"), nextProvider.get("provider")));
                    }
                } catch (ParseException e) {
                    LOGGER.severe(String.format("The template of '%s' could not be applied!", nextProvider.get("template")));
                }
            } else {
                resultProviders.add(nextProvider);
            }
        }
        result.put("providers", resultProviders);
        return result;
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

            result.setAcceptDynamicProvider(obj.get("accept_dynamic_provider") != null && (Boolean) obj.get("accept_dynamic_provider"));

            String signAlg = (String) obj.get("signed_response_alg");
            if (signAlg != null) {
                if (("ES256".equalsIgnoreCase(signAlg) || "RS256".equalsIgnoreCase(signAlg))) {
                    result.setSignedResponseAlg(signAlg);
                } else {
                    LOGGER.warning("Ignoring signed_response_alg due to unsupported value");
                }
            }

            List<OnBehalfOf> onBehalfOfs = new ArrayList<>();
            if (obj.get("on_behalf_of") != null) {
                for (Object onBehalfOf : ((JSONArray) obj.get("on_behalf_of"))) {
                    String tokenType = (String)((JSONObject)onBehalfOf).get("token_type");
                    String alg = (String)((JSONObject)onBehalfOf).get("alg");
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