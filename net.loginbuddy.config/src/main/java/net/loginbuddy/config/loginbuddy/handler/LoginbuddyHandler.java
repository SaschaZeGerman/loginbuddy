package net.loginbuddy.config.loginbuddy.handler;

import net.loginbuddy.config.loginbuddy.Providers;

public interface LoginbuddyHandler {

    String getPAuthorizeApi(String targetApi, boolean includePrefix);
    String getTokenApi(String targetApi, boolean includePrefix);
    String getJwksApi(String targetApi, boolean includePrefix);
    String getUserinfoApi(String targetApi, boolean includePrefix);
    String getRegistrationApi();
    Providers getProviders(String provider);

}
