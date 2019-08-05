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
import java.util.logging.Logger;

public class LoginbuddyConfig {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(LoginbuddyConfig.class));

    private ConfigUtil configUtil;

    private static LoginbuddyConfig ourInstance;

    public static LoginbuddyConfig getInstance() {
        if(ourInstance == null) {
            ourInstance = new LoginbuddyConfig();
        }
        return ourInstance;
    }

    private LoginbuddyConfig() {
        try {
            Context initCtx = new InitialContext();
            Context envCtx = (Context) initCtx.lookup("java:comp/env");
            configUtil = (ConfigUtil) envCtx.lookup("bean/ConfigUtilFactory");
        } catch (Exception e) {
            LOGGER.severe("LoginbuddyConfiguration could not be loaded!");
            e.printStackTrace();
        }
    }

    public ConfigUtil getConfigUtil() {
        return configUtil;
    }

}