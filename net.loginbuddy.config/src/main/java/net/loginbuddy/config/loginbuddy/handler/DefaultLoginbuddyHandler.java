package net.loginbuddy.config.loginbuddy.handler;

import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.config.loginbuddy.Providers;

import java.io.Serializable;

public class DefaultLoginbuddyHandler implements Serializable, LoginbuddyHandler {

    @Override
    public String getUserinfoApi(String targetApi, boolean includePrefix) {
        return targetApi;
    }

    @Override
    public String getRegistrationApi() {
        throw new UnsupportedOperationException("This is not needed for this handler");
    }

    @Override
    public String getPAuthorizeApi(String targetApi, boolean includePrefix) {
        return targetApi;
    }

    @Override
    public String getTokenApi(String targetApi, boolean includePrefix) {
        return targetApi;
    }

    @Override
    public String getJwksApi(String targetApi, boolean includePrefix) {
        return targetApi;
    }

    @Override
    public Providers getProviders(String provider) {
        return LoginbuddyUtil.UTIL.getProviderConfigByProvider(provider);
    }
}
