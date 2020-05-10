package net.loginbuddy.service.config.properties;

import net.loginbuddy.service.config.Bootstrap;

import java.util.logging.Logger;

public enum PropertiesUtil implements Bootstrap {

    UTIL;

    private Logger LOGGER = Logger.getLogger(String.valueOf(PropertiesUtil.class));

    private PropertyLoader loader;

    PropertiesUtil() {
        loader = new DefaultPropertyLoader();
        loader.loadProperties();
    }

    public void setLoader(PropertyLoader loader) {
        this.loader = loader;
        this.loader.reloadProperties();
    }

    public long getLongProperty(String property) {
        LOGGER.fine(String.format("Requested property: '%s'", property));
        return loader.getLongProperty(property);
    }

    public String getPropertiesAsJsonString() {
        return loader.getPropertiesAsJsonString();
    }

    @Override
    public boolean isConfigured() {
        return loader != null && loader.isConfigured();
    }
}