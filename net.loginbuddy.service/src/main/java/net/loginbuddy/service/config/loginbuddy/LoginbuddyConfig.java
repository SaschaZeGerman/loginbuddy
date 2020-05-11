/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.config.loginbuddy;

import net.loginbuddy.service.config.Bootstrap;

public enum LoginbuddyConfig implements Bootstrap {

    CONFIG;

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

    public LoginbuddyUtil getLoginbuddyUtil() {
        return configLoader.getLoginbuddyUtil();
    }

    @Override
    public boolean isConfigured() {
        return configLoader != null && configLoader.isConfigured();
    }
}