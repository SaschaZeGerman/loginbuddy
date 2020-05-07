package net.loginbuddy.service.config;

import net.loginbuddy.service.config.discovery.DiscoveryConfig;
import net.loginbuddy.service.config.discovery.DiscoveryUtil;

public interface LoginbuddyConfigLoader extends Bootstrap {
    void loadConfig();
    void reloadConfig();
    ConfigUtil getConfigUtil();
    PropertiesUtil getPropertiesUtil();
}
