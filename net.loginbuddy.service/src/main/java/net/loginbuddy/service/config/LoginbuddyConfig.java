/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.config;

public enum LoginbuddyConfig implements Bootstrap {

    CONFIGS;

    private LoginbuddyConfigLoader configLoader;

    LoginbuddyConfig() {
        setDefaultConfigLoader();
    }

    public void setConfigLoader(LoginbuddyConfigLoader configLoader) {
        this.configLoader = configLoader;
        this.configLoader.reloadConfig();
    }

    public void setDefaultConfigLoader() {
        configLoader = new DefaultConfigLoader();
        configLoader.loadConfig();
    }

    public ConfigUtil getConfigUtil() {
        return configLoader.getConfigUtil();
    }

    @Override
    public boolean isConfigured() {
        return configLoader != null && configLoader.isConfigured();
    }
}