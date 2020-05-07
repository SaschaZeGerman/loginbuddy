package net.loginbuddy.service.config;

import net.loginbuddy.service.config.discovery.DiscoveryConfig;
import net.loginbuddy.service.config.discovery.DiscoveryUtil;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.File;
import java.io.FileReader;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

public class DefaultConfigLoader implements LoginbuddyConfigLoader {

    private final Logger LOGGER = Logger.getLogger(String.valueOf(DefaultConfigLoader.class));

    private ConfigUtil configUtil;
    private PropertiesUtil propertiesUtil;

    @Override
    public void loadConfig() {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            configUtil = (ConfigUtil) envCtx.lookup("bean/ConfigUtilFactory");
            envCtx.lookup("bean/DiscoveryUtilFactory");

            Properties props = new Properties();
            props.load(new FileReader(new File(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("loginbuddy.properties")).toURI())));
            propertiesUtil = new PropertiesUtil(props);
        } catch (Exception e) {
            LOGGER.severe("Loginbuddy configurations could not be loaded!");
            e.printStackTrace();
        }
    }

    @Override
    public void reloadConfig() {
        loadConfig();
    }

    @Override
    public ConfigUtil getConfigUtil() {
        return configUtil;
    }

    @Override
    public PropertiesUtil getPropertiesUtil() {
        return propertiesUtil;
    }

    @Override
    public boolean isConfigured() {
        return configUtil != null && DiscoveryConfig.CONFIG.isConfigured() && ((Bootstrap) configUtil).isConfigured() && ((Bootstrap) propertiesUtil).isConfigured();
    }
}
