package net.loginbuddy.service.config.loginbuddy;

import net.loginbuddy.service.config.Bootstrap;
import net.loginbuddy.service.config.loginbuddy.LoginbuddyUtil;

public interface LoginbuddyConfigLoader extends Bootstrap {
    void loadConfig();
    void reloadConfig();
    LoginbuddyUtil getLoginbuddyUtil();
}
