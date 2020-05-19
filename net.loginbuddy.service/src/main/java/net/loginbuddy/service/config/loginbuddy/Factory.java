package net.loginbuddy.service.config.loginbuddy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.naming.ResourceRef;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import java.io.File;
import java.io.FileReader;
import java.util.Hashtable;
import java.util.logging.Logger;

public class Factory implements ObjectFactory {

    private Logger LOGGER = Logger.getLogger(String.valueOf(Factory.class));

    private com.fasterxml.jackson.databind.ObjectMapper MAPPER = new ObjectMapper();

    private String path;

    @Override
    public Object getObjectInstance(Object o, Name name, Context context, Hashtable<?, ?> hashtable) throws Exception {
        path = (String) ((ResourceRef) o).get("path").getContent();
        LOGGER.info(String.format("Loading default loginbuddy configuration: '%s'", path));

        // To support configurations that start with {"loginbuddy": {...}} we will check for that here.
        // Otherwise all older configurations would have to be updated which may be painful
        JSONObject obj = (JSONObject)new JSONParser().parse(new FileReader(new File(this.path)));
        if(obj.get("loginbuddy") != null) {
            return MAPPER.readValue(((JSONObject)obj.get("loginbuddy")).toJSONString(), Loginbuddy.class);
        } else {
            return MAPPER.readValue(obj.toJSONString(), Loginbuddy.class);
        }
    }
}