package net.loginbuddy.config.loginbuddy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * This is a simple example of a custom loader. It enables updates of configurations at runtime which is not supported by default!
 * It leverages a simple file as it 'configuration database'. As a developer you may connect to a database if you wish.
 *
 * This specific class is used with JUnit tests but also with the api test suite.
 *
 * The class name is specified here: {loginbuddy}/apitest/docker/loginbuddy.properties
 *
 * When the test suite launches the class 'net.loginbuddy.service.server.Overlord' instantiate this class.
 *
 * For more details see {loginbuddy}/apitest/README.md
 */
public class CustomLoginbuddyConfigLoader implements LoginbuddyLoader {

    private Logger LOGGER = Logger.getLogger(String.valueOf(CustomLoginbuddyConfigLoader.class));

    private Loginbuddy lb;
    private ObjectMapper mapper;

    private String dbLocation;

    public CustomLoginbuddyConfigLoader() {
        // this file location is used with Loginbuddys api tests ({loginbuddy}/apitest/docker)
        this("/usr/local/tomcat/webapps/ROOT/WEB-INF/testCustomLoginbuddyConfig.json");
    }

    public CustomLoginbuddyConfigLoader(String dbLocation) {
        mapper = new ObjectMapper();
        this.dbLocation = dbLocation;
        try {
            load();
        } catch (Exception e) {
            LOGGER.severe(String.format("Custom loader could not be initialized! Error: '%s'", e.getMessage()));
        }
    }

    @Override
    public void load() throws Exception {
        lb = mapper.readValue(new File(dbLocation), Loginbuddy.class);
        LOGGER.info(String.format("Loading custom test loginbuddy configuration: '%s'", dbLocation));
    }

    @Override
    public void reload() throws Exception {
        load();
    }

    @Override
    public <T> T save(T configuration) throws Exception {
        if (configuration instanceof List && ((List) configuration).size() > 0) {
            if (((List) configuration).get(0) instanceof Clients) {
                lb.setClients((List<Clients>) configuration);
            } else if (((List) configuration).get(0) instanceof Providers) {
                lb.setProviders((List<Providers>) configuration);
            }
            updateDbFileAndLoad();
        }
        return configuration;
    }

    @Override
    public <T> T update(T configuration) throws Exception {
        if (configuration instanceof List && ((List) configuration).size() > 0) {
            if (((List) configuration).get(0) instanceof Clients) {
                Set<Clients> all = new HashSet<>();
                all.addAll((List<Clients>)configuration);
                all.retainAll(lb.getClients());
                all.addAll(lb.getClients());
                lb.setClients(new ArrayList<>(all));
                updateDbFileAndLoad();
                return (T)lb.getClients();
            } else if (((List) configuration).get(0) instanceof Providers) {
                Set<Providers> all = new HashSet<>();
                all.addAll((List<Providers>)configuration);
                all.retainAll(lb.getProviders());
                all.addAll(lb.getProviders());
                lb.setProviders(new ArrayList<>(all));
                updateDbFileAndLoad();
                return (T)lb.getProviders();
            }
        }
        return configuration;
    }

    private void updateDbFileAndLoad() throws Exception {

        JsonNode node = mapper.valueToTree(lb);
        node.traverse();

        JSONObject output = new JSONObject();
        output.put("clients", new JSONParser().parse(node.get("clients").toString()));
        output.put("providers", new JSONParser().parse(node.get("providers").toString()));
        JSONParser p = new JSONParser();
        for (Object obj : ((JSONArray) output.get("providers"))) {
            ((JSONObject) obj).put("mappings", p.parse(((JSONObject) obj).get("mappings").toString()));
        }

        FileWriter fw = new FileWriter(new File(dbLocation));
        output.writeJSONString(fw);
        fw.flush();
        fw.close();
        load();
    }

    @Override
    public Loginbuddy getLoginbuddy() {
        return lb;
    }

    @Override
    public boolean isConfigured() {
        return true;
    }
}