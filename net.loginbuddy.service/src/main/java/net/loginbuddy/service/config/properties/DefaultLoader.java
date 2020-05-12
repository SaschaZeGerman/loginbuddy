package net.loginbuddy.service.config.properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;
import java.util.logging.Logger;

class DefaultLoader implements PropertyLoader {

    private Logger LOGGER = Logger.getLogger(String.valueOf(DefaultLoader.class));

    private Properties props;

    @Override
    public void load() {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            props = (Properties) envCtx.lookup("bean/PropertiesFactory");
        } catch (Exception e) {
            LOGGER.severe("Properties could not be loaded!");
        }
    }

    @Override
    public void reload() {
        load();
    }

    @Override
    public Properties getProperties() {
        return props;
    }

    @Override
    public boolean isConfigured() {
        return props != null;
    }
}