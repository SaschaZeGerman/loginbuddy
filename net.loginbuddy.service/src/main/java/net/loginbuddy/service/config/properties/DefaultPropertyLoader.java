package net.loginbuddy.service.config.properties;

import org.json.simple.JSONObject;

import java.io.File;
import java.io.FileReader;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

class DefaultPropertyLoader implements PropertyLoader {

    private Logger LOGGER = Logger.getLogger(String.valueOf(DefaultPropertyLoader.class));

    private Properties props;

    @Override
    public void loadProperties() {
        props = new Properties();
        try {
            File file = new File(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("loginbuddy.properties")).toURI());
            props.load(new FileReader(file));
            LOGGER.info(String.format("Default Properties loaded: ", file.getAbsoluteFile()));
        } catch (Exception e) {
            LOGGER.severe(String.format("Properties could not be loaded! Error: '%s'", e.getMessage()));
            props = null;
        }
    }

    @Override
    public void reloadProperties() {
        loadProperties();
    }

    @Override
    public Long getLongProperty(String propertyName) {
        return Long.parseLong((String) props.get(propertyName));
    }

    @Override
    public String getPropertiesAsJsonString() {
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
        return props != null;
    }
}
