package net.loginbuddy.service.config;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.logging.Logger;

public class DefaultConfigLoader implements LoginbuddyConfigLoader {

    private final Logger LOGGER = Logger.getLogger(String.valueOf(DefaultConfigLoader.class));

    private ConfigUtil configUtil;

    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public void loadConfig() {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            configUtil = (ConfigUtil) envCtx.lookup("bean/ConfigUtilFactory");
            LOGGER.info("Default Loginbuddy Configuration loaded!");
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
    public boolean isConfigured() {
        return configUtil != null && ((Bootstrap) configUtil).isConfigured();
    }
}
