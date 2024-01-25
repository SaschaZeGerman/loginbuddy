package net.loginbuddy.config.loginbuddy.handler;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.storage.LoginbuddyStorage;
import net.loginbuddy.config.loginbuddy.Providers;

import java.io.Serializable;
import java.util.Map;

public class OidcdrLoginbuddyHandler implements Serializable, LoginbuddyHandler {

    @Override
    public String getUserinfoApi(String targetApi, boolean includePrefix) {
        return includePrefix ?
                String.format("https://loginbuddy-oidcdr:445/oidcdr/userinfo?%s=%s",
                        Constants.TARGET_PROVIDER.getKey(),
                        HttpHelper.urlEncode(targetApi)) :
                targetApi;
    }

    @Override
    public String getRegistrationApi() {
        return "https://loginbuddy-oidcdr:445/oidcdr/register";
    }

    @Override
    public String getPAuthorizeApi(String targetApi, boolean includePrefix) {
        return includePrefix ?
                String.format("https://loginbuddy-oidcdr:445/oidcdr/pauthorize?%s=%s",
                        Constants.TARGET_PROVIDER.getKey(),
                        HttpHelper.urlEncode(targetApi)) :
                targetApi;
    }

    @Override
    public String getTokenApi(String targetApi, boolean includePrefix) {
        return includePrefix ?
                String.format("https://loginbuddy-oidcdr:445/oidcdr/token?%s=%s",
                        Constants.TARGET_PROVIDER.getKey(),
                        HttpHelper.urlEncode(targetApi)) :
                targetApi;
    }

    @Override
    public String getJwksApi(String targetApi, boolean includePrefix) {
        return includePrefix ?
                String.format("https://loginbuddy-oidcdr:445/oidcdr/jwks?%s=%s",
                        Constants.TARGET_PROVIDER.getKey(),
                        HttpHelper.urlEncode(targetApi)) :
                targetApi;
    }

    @Override
    public Providers getProviders(String provider) {
        // TODO check if this provider is accessible to clients that do not accept dynamically registered providers!
        Map<String, Providers> dynamicProviders = (Map<String, Providers>) LoginbuddyStorage.STORAGE.get(Constants.PROVIDER_DYNAMIC_REGISTRATION.getKey());
        if(dynamicProviders != null) {
            return dynamicProviders.get(provider);
        }
        return null;
    }
}
