package net.loginbuddy.service.config.loginbuddy;

import net.loginbuddy.service.config.Bootstrap;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.logging.Logger;

public class DefaultConfigLoader implements LoginbuddyConfigLoader {

    private final Logger LOGGER = Logger.getLogger(String.valueOf(DefaultConfigLoader.class));

    private LoginbuddyUtil loginbuddyUtil;

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
            loginbuddyUtil = (LoginbuddyUtil) envCtx.lookup("bean/ConfigUtilFactory");
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
    public LoginbuddyUtil getLoginbuddyUtil() {
        return loginbuddyUtil;
    }

    @Override
    public boolean isConfigured() {
        return loginbuddyUtil != null && ((Bootstrap) loginbuddyUtil).isConfigured();
    }
}
