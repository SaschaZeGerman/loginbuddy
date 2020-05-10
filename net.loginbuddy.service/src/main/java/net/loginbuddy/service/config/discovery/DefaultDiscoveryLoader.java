package net.loginbuddy.service.config.discovery;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.logging.Logger;

public class DefaultDiscoveryLoader implements DiscoveryLoader {

    private Logger LOGGER = Logger.getLogger(String.valueOf(DefaultDiscoveryLoader.class));

    private DiscoveryConfig config;

    @Override
    public boolean isConfigured() {
        return config != null;
    }

    @Override
    public void loadDiscovery() {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            config = (DiscoveryConfig)envCtx.lookup("bean/DiscoveryUtilFactory");
            LOGGER.info("Default Discovery Configuration loaded!");
        } catch (Exception e) {
            LOGGER.severe("discovery.json file could not be loaded or it is invalid JSON!");
        }
    }

    @Override
    public void reloadDiscovery() {
        loadDiscovery();
    }

    @Override
    public DiscoveryConfig getDiscoveryConfig() {
        return config;
    }
}