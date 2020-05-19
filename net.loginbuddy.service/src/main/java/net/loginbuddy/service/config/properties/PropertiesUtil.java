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
        try {
            setDefaultLoader();
        } catch (Exception e) {
            LOGGER.severe(String.format("Properties could not be loaded! Error: '%s'", e.getMessage()));
            e.printStackTrace();
        }
    }

    public void setDefaultLoader() throws Exception {
        setLoader(new DefaultLoader());
    }

    public void setLoader(PropertyLoader loader) throws Exception {
        this.loader = loader;
        this.loader.load();
    }

    @Override
    public boolean isConfigured() {
        return loader != null && loader.isConfigured();
    }

    public long getLongProperty(String property) {
        LOGGER.fine(String.format("Requested long property: '%s'", property));
        return Long.parseLong((String) loader.getProperties().get(property));
    }

    public String getStringProperty(String property) {
        LOGGER.fine(String.format("Requested string property: '%s'", property));
        return String.valueOf(loader.getProperties().get(property));
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

    public Properties setProperties(Properties props) throws Exception {
        return loader.save(props);
    }

    public Properties updateProperties(Properties props) throws Exception {
        return loader.update(props);
    }
}