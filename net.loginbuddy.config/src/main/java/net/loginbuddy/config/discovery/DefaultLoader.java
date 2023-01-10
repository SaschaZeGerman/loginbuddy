package net.loginbuddy.config.discovery;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.logging.Logger;

public class DefaultLoader implements DiscoveryLoader {

    private static final Logger LOGGER = Logger.getLogger(DefaultLoader.class.getName());

    private Discovery config;

    @Override
    public void load() throws Exception {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            config = (Discovery)envCtx.lookup("bean/DiscoveryUtilFactory");
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
    public <T> T save(T configuration) throws Exception {
        throw new IllegalArgumentException("setting discovery configuration is not supported!");
    }

    @Override
    public <T> T update(T configuration) throws Exception {
        throw new IllegalArgumentException("discovery configuration updates are not supported!");
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