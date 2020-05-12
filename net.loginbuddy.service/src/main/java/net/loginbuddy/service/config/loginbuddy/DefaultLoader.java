package net.loginbuddy.service.config.loginbuddy;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.logging.Logger;

public class DefaultLoader implements LoginbuddyLoader {

    private final Logger LOGGER = Logger.getLogger(String.valueOf(DefaultLoader.class));

    private Loginbuddy config;

    @Override
    public void load() {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            config = (Loginbuddy) envCtx.lookup("bean/LoginbuddyFactory");
        } catch (Exception e) {
            LOGGER.severe("configuration could not be loaded!");
        }
    }

    @Override
    public void reload() {
        load();
    }

    @Override
    public Loginbuddy getLoginbuddy() {
        return config;
    }

    @Override
    public boolean isConfigured() {
        return config != null && config.isConfigured();
    }
}