/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.config;

import net.loginbuddy.service.config.discovery.DiscoveryConfig;
import net.loginbuddy.service.config.discovery.DiscoveryUtil;

public enum LoginbuddyConfig {

    CONFIGS;

    private LoginbuddyConfigLoader configLoader;

    LoginbuddyConfig() {
        configLoader = new DefaultConfigLoader();
        configLoader.loadConfig();
    }

    public void setConfigLoader(LoginbuddyConfigLoader configLoader) {
        this.configLoader = configLoader;
        this.configLoader.reloadConfig();
    }

    /**
     * Called on bootstrapping Loginbuddy in @see Overlord
     *
     * @return
     */
    public boolean isConfigured() {
        return configLoader.isConfigured();
    }

    public ConfigUtil getConfigUtil() {
        return configLoader.getConfigUtil();
    }

    public DiscoveryConfig getDiscoveryConfig() {
        return DiscoveryConfig.CONFIG;
    }

    public PropertiesUtil getPropertiesUtil() {
        return configLoader.getPropertiesUtil();
    }
}