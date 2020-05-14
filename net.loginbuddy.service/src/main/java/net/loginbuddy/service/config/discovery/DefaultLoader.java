package net.loginbuddy.service.config.discovery;

import org.apache.http.MethodNotSupportedException;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.logging.Logger;

public class DefaultLoader implements DiscoveryLoader {

    private Logger LOGGER = Logger.getLogger(String.valueOf(DefaultLoader.class));

    private Discovery config;

    @Override
    public void load() {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            config = (Discovery)envCtx.lookup("bean/DiscoveryUtilFactory");
        } catch (Exception e) {
            LOGGER.severe("configuration could not be loaded!");
        }
    }

    @Override
    public void reload() {
        load();
    }

    @Override
    public <T> T save(T configuration) throws MethodNotSupportedException {
        throw new MethodNotSupportedException("setting discovery configuration is not supported!");
    }

    @Override
    public <T> T update(T configuration) throws MethodNotSupportedException {
        throw new MethodNotSupportedException("discovery configuration updates are not supported!");
    }

    @Override
    public Discovery getDiscoveryConfig() {
        return config;
    }

    @Override
    public boolean isConfigured() {
        return config != null && config.isConfigured();
    }
}