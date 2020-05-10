package net.loginbuddy.service.config;

public interface LoginbuddyConfigLoader extends Bootstrap {
    void loadConfig();
    void reloadConfig();
    ConfigUtil getConfigUtil();
}
