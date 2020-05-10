package net.loginbuddy.service.config.properties;

import net.loginbuddy.service.config.Bootstrap;

public interface PropertyLoader extends Bootstrap {
    void loadProperties();
    void reloadProperties();
    Long getLongProperty(String propertyName);
    String getPropertiesAsJsonString();
}
