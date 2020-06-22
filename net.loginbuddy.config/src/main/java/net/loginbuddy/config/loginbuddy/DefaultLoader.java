package net.loginbuddy.config.loginbuddy;

import org.apache.http.MethodNotSupportedException;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.logging.Logger;

public class DefaultLoader implements LoginbuddyLoader {

    private final Logger LOGGER = Logger.getLogger(String.valueOf(DefaultLoader.class));

    private Loginbuddy config, configTemplates;

    @Override
    public void load() throws Exception {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            config = (Loginbuddy) envCtx.lookup("bean/LoginbuddyFactory");
        } catch (Exception e) {
            LOGGER.severe("configuration could not be loaded!");
            throw e;
        }
    }

    @Override
    public void reload() throws Exception {
        load();
    }

    @Override
    public <T> T save(T configuration) throws MethodNotSupportedException {
        throw new MethodNotSupportedException("loginbuddy configuration updates are not supported!");
    }

    @Override
    public <T> T update(T configuration) throws MethodNotSupportedException {
        throw new MethodNotSupportedException("loginbuddy configuration updates are not supported!");
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