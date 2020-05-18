package net.loginbuddy.service.config.loginbuddy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.MethodNotSupportedException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileWriter;
import java.util.List;
import java.util.logging.Logger;

public class CustomLoginbuddyConfigLoader implements LoginbuddyLoader {

    private Logger LOGGER = Logger.getLogger(String.valueOf(CustomLoginbuddyConfigLoader.class));

    private Loginbuddy lb;
    private ObjectMapper mapper;

    private String dbLocation;

    public CustomLoginbuddyConfigLoader() {
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
                // new JSONParser().parse(node.toString())
                // new JSONParser().parse(node.get("providers").get(3).get("mappings").asText())
            }
        }

        JsonNode node = mapper.valueToTree(lb);
        node.traverse();

        JSONObject output = new JSONObject();
        output.put("clients", new JSONParser().parse(node.get("clients").toString()));
        output.put("providers", new JSONParser().parse(node.get("providers").toString()));
        JSONParser p = new JSONParser();
        for(Object obj : ((JSONArray)output.get("providers"))) {
            ((JSONObject)obj).put("mappings", p.parse(((JSONObject)obj).get("mappings").toString()));
        }

        FileWriter fw = new FileWriter(new File(dbLocation));
        output.writeJSONString(fw);
        fw.flush();
        fw.close();
        load();

        return configuration;
    }

    @Override
    public <T> T update(T configuration) throws MethodNotSupportedException {
        return null;
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