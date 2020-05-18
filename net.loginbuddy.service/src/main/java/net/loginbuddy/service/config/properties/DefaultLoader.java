package net.loginbuddy.service.config.properties;

import org.apache.http.MethodNotSupportedException;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Properties;
import java.util.logging.Logger;

public class DefaultLoader implements PropertyLoader {

    private Logger LOGGER = Logger.getLogger(String.valueOf(DefaultLoader.class));

    private Properties props;

    @Override
    public void load() throws Exception {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            props = (Properties) envCtx.lookup("bean/PropertiesFactory");
        } catch (Exception e) {
            LOGGER.severe("Properties could not be loaded!");
            throw e;
        }
    }

    @Override
    public void reload() throws Exception {
        load();
    }

    @Override
    public <T> T save(T configuration) throws MethodNotSupportedException {
        throw new MethodNotSupportedException("setting properties is not supported!");
    }

    @Override
    public <T> T update(T configuration) throws MethodNotSupportedException {
        throw new MethodNotSupportedException("property updates are not supported!");
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