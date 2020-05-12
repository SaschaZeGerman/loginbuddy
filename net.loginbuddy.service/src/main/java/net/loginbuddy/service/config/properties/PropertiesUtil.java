package net.loginbuddy.service.config.properties;

import net.loginbuddy.service.config.Bootstrap;
import org.json.simple.JSONObject;

import java.util.Properties;
import java.util.logging.Logger;

public enum PropertiesUtil implements Bootstrap {

    UTIL;

    private Logger LOGGER = Logger.getLogger(String.valueOf(PropertiesUtil.class));

    private PropertyLoader loader;

    PropertiesUtil() {
        loader = new DefaultLoader();
        loader.load();
    }

    public void setLoader(PropertyLoader loader) {
        this.loader = loader;
        this.loader.reload();
    }

    public long getLongProperty(String property) {
        LOGGER.fine(String.format("Requested property: '%s'", property));
        return Long.parseLong((String) loader.getProperties().get(property));
    }

    public String getPropertiesAsJsonString() {
        Properties props = loader.getProperties();
        String nextProp;
        JSONObject output = new JSONObject();
        for (Object prop : props.keySet()) {
            nextProp = (String) prop;
            output.put(nextProp.replace(".", "_"), props.getProperty(nextProp));
        }
        return output.toJSONString();
    }

    @Override
    public boolean isConfigured() {
        return loader != null && loader.isConfigured();
    }
}