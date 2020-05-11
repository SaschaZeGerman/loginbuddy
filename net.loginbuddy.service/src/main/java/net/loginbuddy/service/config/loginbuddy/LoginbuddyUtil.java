/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.config.loginbuddy;

import com.fasterxml.jackson.databind.ObjectMapper;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.service.config.Bootstrap;
import net.loginbuddy.service.config.discovery.DiscoveryUtil;
import net.loginbuddy.service.server.Overlord;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class LoginbuddyUtil extends Overlord implements Bootstrap {

    private Logger LOGGER = Logger.getLogger(String.valueOf(LoginbuddyUtil.class));

    private com.fasterxml.jackson.databind.ObjectMapper MAPPER = new ObjectMapper();

    private String path;

    private Loginbuddy loginbuddy;

    public LoginbuddyUtil() {
    }

    private Loginbuddy getLoginbuddy() {
        try {
            loginbuddy = (Loginbuddy) LoginbuddyCache.CACHE.get("ConfigUtilGetConfig");
            if (loginbuddy == null) {
                loginbuddy = MAPPER.readValue(new File(this.path).getAbsoluteFile(), Loginbuddy.class);
                LoginbuddyCache.CACHE.put("ConfigUtilGetConfig", loginbuddy);
            }
            return loginbuddy;
        } catch (IOException e) {
            LOGGER.severe("LoginBuddyConfiguration file could not be loaded!");
            return null; // this should not occur ... TODO make this fail safe
        }
    }

    private List<Clients> getClients() {
        return getLoginbuddy().getClients();
    }

    private List<Providers> getProviders() {
        // need it from cache for provider configurations that used dynamic registrations. Otherwise we register again and again
        List<Providers> providers = (List<Providers>) LoginbuddyCache.CACHE.get("providers");
        try {
            if (providers == null) {
                providers = getLoginbuddy().getProviders();
                for (Providers next : providers) {
                    if (next.getProviderType().equals(ProviderConfigType.MINIMAL)) {
                        next.enhanceToFull(MAPPER.readValue(HttpHelper.retrieveAndRegister(next.getOpenidConfigurationUri(),
                                DiscoveryUtil.UTIL.getRedirectUri()).toJSONString(),
                                Providers.class));
                    }
                }
                LoginbuddyCache.CACHE.put("providers", providers);
            }
        } catch (Exception e) {
            LOGGER.severe(String.format("Fail to map provider! Error: '%s'", e.getMessage()));
        }
        return providers;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Clients getClientConfigByClientId(String clientId) {
        return getClients().stream()
                .filter(clientConfig -> clientConfig.getClientId().equals(clientId))
                .findFirst()
                .orElse(null);
    }

    // TODO make this thing more efficient
    public List<Providers> getProviders(String clientId) throws Exception {
        Clients cc = getClientConfigByClientId(clientId);
        if (cc.getClientProviders() != null && cc.getClientProviders().length > 0) {
            List<Providers> result = new ArrayList<>();
            for (Providers pc : getProviders()) {
                if (Arrays.asList(cc.getClientProviders()).contains(pc.getProvider())) {
                    result.add(pc);
                }
            }
            return result;
        } else {
            return getProviders();
        }
    }

    public Providers getProviderConfigByProvider(String providerHint) {
        return getProviders().stream()
                .filter(provider -> provider.getProvider().equalsIgnoreCase(providerHint))
                .findFirst()
                .orElse(null);
    }

    public Providers getProviderConfigByIssuer(String issuerHint) {
        return getProviders().stream()
                .filter(provider -> provider.getIssuer().equalsIgnoreCase(issuerHint))
                .findFirst()
                .orElse(null);
    }

    public Providers getProviderConfigFromJsonString(String providerHint) {
        try {
            return MAPPER.readValue(providerHint, Providers.class);
        } catch (IOException e) {
            LOGGER.warning("The provider configuration could no be mapped to ProviderConfig");
            e.printStackTrace();
            ;
            return null;
        }
    }

    @Override
    public boolean isConfigured() {
        return getLoginbuddy() != null;
    }
}