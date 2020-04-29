/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.config;

import javax.naming.Context;
import javax.naming.InitialContext;
import java.io.File;
import java.io.FileReader;
import java.util.Objects;
import java.util.Properties;
import java.util.logging.Logger;

public enum LoginbuddyConfig implements Bootstrap {

    CONFIGS;

    private final Logger LOGGER = Logger.getLogger(String.valueOf(LoginbuddyConfig.class));

    private ConfigUtil configUtil;
    private DiscoveryUtil discoveryUtil;
    private PropertiesUtil propertiesUtil;

    LoginbuddyConfig() {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            configUtil = (ConfigUtil) envCtx.lookup("bean/ConfigUtilFactory");
            discoveryUtil = (DiscoveryUtil) envCtx.lookup("bean/DiscoveryUtilFactory");

            Properties props = new Properties();
            props.load(new FileReader(new File(Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("loginbuddy.properties")).toURI())));
            propertiesUtil = new PropertiesUtil(props);
        } catch (Exception e) {
            LOGGER.severe("Loginbuddy configurations could not be loaded!");
            e.printStackTrace();
        }
    }

    /**
     * Called on bootstrapping Loginbuddy in @see Overlord
     *
     * @return
     */
    @Override
    public boolean isConfigured() {
        return configUtil != null && discoveryUtil != null && ((Bootstrap)configUtil).isConfigured() && ((Bootstrap)discoveryUtil).isConfigured() && ((Bootstrap)propertiesUtil).isConfigured();
    }

    public ConfigUtil getConfigUtil() {
        return configUtil;
    }

    public DiscoveryUtil getDiscoveryUtil() {
        return discoveryUtil;
    }

    public PropertiesUtil getPropertiesUtil() {
        return propertiesUtil;
    }
}