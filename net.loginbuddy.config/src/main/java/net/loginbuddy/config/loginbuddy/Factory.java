package net.loginbuddy.config.loginbuddy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.naming.ResourceRef;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import java.io.File;
import java.io.FileReader;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.logging.Logger;

public class Factory implements ObjectFactory {

    private Logger LOGGER = Logger.getLogger(String.valueOf(Factory.class));

    private com.fasterxml.jackson.databind.ObjectMapper MAPPER = new ObjectMapper();

    private String path, pathTemplate;

    @Override
    public Object getObjectInstance(Object o, Name name, Context context, Hashtable<?, ?> hashtable) throws Exception {
        path = (String) ((ResourceRef) o).get("path").getContent();
        LOGGER.info(String.format("Loading default loginbuddy configuration: '%s'", path));
        // To support configurations that start with {"loginbuddy": {...}} we will check for that here.
        // Otherwise all older configurations would have to be updated which may be painful
        JSONObject configJson = (JSONObject)new JSONParser().parse(new FileReader(new File(this.path)));
        if(configJson.get("loginbuddy") != null) {
            return MAPPER.readValue(((JSONObject)configJson.get("loginbuddy")).toJSONString(), Loginbuddy.class);
        } else {
            pathTemplate = (String) ((ResourceRef) o).get("pathTemplate").getContent();
            if(pathTemplate != null) {
                LOGGER.info(String.format("Loading template configuration: '%s'", pathTemplate));
                JSONObject configTemplateJson = (JSONObject)new JSONParser().parse(new FileReader(new File(pathTemplate)));
                JSONObject result = getMergedConfigObject(configJson, configTemplateJson);
                LOGGER.info(String.format("Loaded loginbuddy configuration with template. Config: '%s', template: '%s'", path, pathTemplate));
                return MAPPER.readValue(result.toJSONString(), Loginbuddy.class);
            } else {
                LOGGER.info(String.format("Loaded loginbuddy configuration. Config: '%s'", path));
                return MAPPER.readValue(configJson.toJSONString(), Loginbuddy.class);
            }
        }
    }

    public static JSONObject getMergedConfigObject(JSONObject configJson, JSONObject configTemplateJson) {
        JSONObject result = new JSONObject();
        result.put("clients", configJson.get("clients"));
        JSONArray resultProviders = new JSONArray();
        Iterator iter = ((JSONArray)configJson.get("providers")).iterator();
        while(iter.hasNext()) {
            final JSONObject nextProvider = (JSONObject)iter.next();
            if(nextProvider.get("template") != null) {
                JSONObject pt = (JSONObject)((JSONArray) configTemplateJson.get("providers")).stream()
                        .filter(provider -> ((String) ((JSONObject) provider).get("provider")).equalsIgnoreCase((String) nextProvider.get("template")))
                        .findFirst()
                        .orElse(null);
                pt.putAll(nextProvider);
                resultProviders.add(pt);
            } else {
                resultProviders.add(nextProvider);
            }
        }
        result.put("providers", resultProviders);
        return result;
    }
}