/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.config.loginbuddy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.service.config.Bootstrap;
import net.loginbuddy.service.config.discovery.DiscoveryUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public enum LoginbuddyUtil implements Bootstrap {

    UTIL;

    private Logger LOGGER = Logger.getLogger(String.valueOf(LoginbuddyUtil.class));

    private LoginbuddyLoader loader;
    private com.fasterxml.jackson.databind.ObjectMapper MAPPER = new ObjectMapper();

    LoginbuddyUtil() {
        try {
            setDefaultLoader();
        } catch (Exception e) {
            LOGGER.severe(String.format("Loginbuddy configuration could not be loaded! Error: '%s'", e.getMessage()));

        }
    }

    public void setDefaultLoader() throws Exception {
        setLoader(new DefaultLoader());
    }

    public void setLoader(LoginbuddyLoader loader) throws Exception {
        this.loader = loader;
        this.loader.load();
    }

    @Override
    public boolean isConfigured() {
        return loader != null && loader.isConfigured();
    }

    public List<Clients> getClients() {
        return loader.getLoginbuddy().getClients();
    }

    public List<Providers> getProviders() {
        // need it from cache for provider configurations that used dynamic registrations. Otherwise we register again and again
        List<Providers> providers = (List<Providers>) LoginbuddyCache.CACHE.get("providers");
        try {
            if (providers == null) {
                providers = loader.getLoginbuddy().getProviders();
                for (Providers next : providers) {
                    if (getProviderType(next).equals(ProviderConfigType.MINIMAL)) {
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

    public Clients getClientConfigByClientId(String clientId) {
        return getClientConfigByClientId(getClients(), clientId);
    }

    public Clients getClientConfigByClientId(List<Clients> clients, String clientId) {
        return clients.stream()
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
            return null;
        }
    }

    public String getClientsAsJsonString() throws JsonProcessingException {
        return getClientsAsJsonString(getClients());
    }

    public String getClientsAsJsonString(List<Clients> clients) throws JsonProcessingException {
        return MAPPER.writeValueAsString(clients);
    }

    public String getClientAsJsonString(Clients client) throws JsonProcessingException {
        return MAPPER.writeValueAsString(client);
    }

    public String getProvidersAsJsonString() throws JsonProcessingException {
        return MAPPER.writeValueAsString(getProviders());
    }

    public String getProviderAsJsonString(Providers provider) throws JsonProcessingException {
        return MAPPER.writeValueAsString(provider);
    }

    public List<Clients> replaceClients(String clientId, String clientsAsJsonString) throws IllegalArgumentException {
        try {
            List<Clients> newClients = new ArrayList<>();
            if(clientsAsJsonString.startsWith("[")) {
                newClients.addAll(Arrays.asList(MAPPER.readValue(clientsAsJsonString, Clients[].class)));
            } else {
                newClients.add(MAPPER.readValue(clientsAsJsonString, Clients.class));
            }
            if(clientId != null) {
                if (getClientConfigByClientId(newClients, clientId) == null) {
                    Clients tempClient = getClientConfigByClientId(clientId);
                    if(tempClient != null) {
                        newClients.add(tempClient);
                    } else {
                        throw new IllegalArgumentException("The given client_id is unknown!");
                    }
                }
            }
            return loader.save(newClients);
        } catch(Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private ProviderConfigType getProviderType(Providers p) {
        return p.getClientId() == null ? ProviderConfigType.MINIMAL
                : p.getOpenidConfigurationUri() == null ? ProviderConfigType.FULL : ProviderConfigType.DEFAULT;
    }
}