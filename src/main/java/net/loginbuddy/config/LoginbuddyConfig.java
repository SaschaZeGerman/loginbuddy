package net.loginbuddy.config;

import javax.naming.Context;
import javax.naming.InitialContext;

public class LoginbuddyConfig {

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
            e.printStackTrace();
        }
    }

    public ConfigUtil getConfigUtil() {
        return configUtil;
    }

}
