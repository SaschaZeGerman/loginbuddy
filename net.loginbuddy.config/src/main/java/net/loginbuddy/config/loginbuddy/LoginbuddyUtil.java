/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.config.loginbuddy;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.config.Bootstrap;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.loginbuddy.common.Meta;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public enum LoginbuddyUtil implements Bootstrap {

    UTIL;

    private Logger LOGGER = Logger.getLogger(String.valueOf(LoginbuddyUtil.class));

    private LoginbuddyLoader loader;
    private LoginbuddyObjectMapper MAPPER = new LoginbuddyObjectMapper();

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
        // need it from cache for provider configurations that used dynamic registrations. Otherwise, we register again and again
        List<Providers> providers = (List<Providers>) LoginbuddyCache.CACHE.get("providers");
        try {
            if (providers == null) {
                providers = loader.getLoginbuddy().getProviders();
                for (Providers next : providers) {
                    // dynamic registration is done at boot time. However, if that fails this section will try again
                    if (getProviderType(next).equals(ProviderConfigType.MINIMAL)) {
                        JSONObject retrieveAndRegister = HttpHelper.retrieveAndRegister(next.getOpenidConfigurationUri(),DiscoveryUtil.UTIL.getRedirectUri());
                        if(retrieveAndRegister.get("error") == null) {
                            Providers readValue = MAPPER.readValue(retrieveAndRegister.toJSONString(), Providers.class);
                            next.enhanceToFull(readValue);
                            next.setMeta(new Meta());
                        } else {
                            LOGGER.warning(String.format("Could not register: '%s'", retrieveAndRegister.get("error_description")));
                            next.getMeta().addStatus(Meta.STATUS_REGISTRATION_ERROR, (String)retrieveAndRegister.get("error_description"));
                        }
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
        if (cc.getClientProviders().size() > 0) {
            List<Providers> result = new ArrayList<>();
            for (Providers pc : getProviders()) {
                if (cc.getClientProviders().contains(pc.getProvider())) {
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
        return getProvidersAsJsonString(getProviders());
    }

    public String getProvidersAsJsonString(List<Providers> providers) throws JsonProcessingException {
        return MAPPER.writeValueAsString(providers);
    }

    public String getProviderAsJsonString(Providers provider) throws JsonProcessingException {
        return MAPPER.writeValueAsString(provider);
    }

    /**
     * Replaces the current list of clients with the given one. However, the requesting client will not be replaced/ removed!
     * @param requestingClientId the client_id of the client that initiated the method.
     * @param clientsAsJsonString the JSON list of clients to replace the existing ones.
     * @return
     * @throws IllegalArgumentException
     */
    public List<Clients> replaceClients(String requestingClientId, String clientsAsJsonString) throws IllegalArgumentException {
        try {
            List<Clients> newClients = new ArrayList<>();
            if(clientsAsJsonString.startsWith("[")) {
                newClients.addAll(MAPPER.readClients(clientsAsJsonString));
            } else {
                newClients.add(MAPPER.readClient(clientsAsJsonString));
            }
            if(requestingClientId != null) {
                if (getClientConfigByClientId(newClients, requestingClientId) == null) {
                    Clients tempClient = getClientConfigByClientId(requestingClientId);
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

    /**
     * Updates the current list of clients with the given bew values!
     * @param requestingClientId the client_id of the client that initiated the method.
     * @param clientsAsJsonString the JSON list of clients to replace the existing ones.
     * @return
     * @throws IllegalArgumentException
     */
    public List<Clients> updateClients(String requestingClientId, String clientsAsJsonString) throws IllegalArgumentException {
        try {
            List<Clients> newClients = new ArrayList<>();
            if(clientsAsJsonString.startsWith("[")) {
                newClients.addAll(MAPPER.readClients(clientsAsJsonString));
            } else {
                newClients.add(MAPPER.readClient(clientsAsJsonString));
            }
            return loader.update(newClients);
        } catch(Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Replaces the current list of providers with the given one.
     * @param providersAsJsonString the JSON list of providers to replace the existing ones.
     * @return
     * @throws IllegalArgumentException
     */
    public List<Providers> replaceProviders(String providersAsJsonString) throws IllegalArgumentException {
        try {
            List<Providers> newProviders = new ArrayList<>();
            if(providersAsJsonString.startsWith("[")) {
                newProviders.addAll(Arrays.asList(MAPPER.readValue(providersAsJsonString, Providers[].class)));
            } else {
                newProviders.add(MAPPER.readValue(providersAsJsonString, Providers.class));
            }
            return loader.save(newProviders);
        } catch(Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    /**
     * Updates the current list of providers with the given updated values.
     * @param providersAsJsonString the JSON list of providers to replace the existing ones.
     * @return
     * @throws IllegalArgumentException
     */
    public List<Providers> updateProviders(String providersAsJsonString) throws IllegalArgumentException {
        try {
            List<Providers> newProviders = new ArrayList<>();
            if(providersAsJsonString.startsWith("[")) {
                newProviders.addAll(Arrays.asList(MAPPER.readValue(providersAsJsonString, Providers[].class)));
            } else {
                newProviders.add(MAPPER.readValue(providersAsJsonString, Providers.class));
            }
            return loader.update(newProviders);
        } catch(Exception e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private ProviderConfigType getProviderType(Providers p) {
        return p.getClientId() == null ? ProviderConfigType.MINIMAL : p.getOpenidConfigurationUri() == null ? ProviderConfigType.FULL : ProviderConfigType.DEFAULT;
    }
}