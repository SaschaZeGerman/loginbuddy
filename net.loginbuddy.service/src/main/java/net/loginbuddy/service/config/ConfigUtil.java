/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class ConfigUtil {

    private Logger LOGGER = Logger.getLogger(String.valueOf(ConfigUtil.class));

    private com.fasterxml.jackson.databind.ObjectMapper MAPPER = new ObjectMapper();
    private String loginbuddy = "loginbuddy";
    private String clients = "clients";
    private String providers = "providers";

    private String path;

    public ConfigUtil() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    private JsonNode getConfig() throws IOException {
        try {
            JsonNode node = MAPPER.readValue(new File(this.path).getAbsoluteFile(), JsonNode.class);
            return node.get(loginbuddy);

        } catch (IOException e) {
            LOGGER.severe("LoginBuddyConfiguration file could not be loaded!");
            throw e;
        }
    }

    private List<ClientConfig> getClients() throws Exception {
        JsonNode clientsNode = getConfig().get(clients);

        if (clientsNode == null || !clientsNode.isArray()) {
            LOGGER.severe("Fail to read provider from config file!");
            throw new Exception("Fail to read provider from config file!");
        }

        try {
            return Arrays.asList(MAPPER.readValue(clientsNode.toString(), ClientConfig[].class));
        } catch (Exception e) {
            LOGGER.severe("Fail to map clients [" + clientsNode.toString() + "]");
            throw new Exception("Fail to map clients [" + clientsNode.toString() + "]");
        }
    }

    public ClientConfig getClientConfigByClientId(String clientId) throws Exception {
        return getClients().stream()
                .filter(clientConfig -> clientConfig.getClientId().equals(clientId))
                .findFirst()
                .orElse(null);
    }

    // TODO make this thing more efficient
    public List<ProviderConfig> getProviders(String clientId) throws Exception {
        ClientConfig cc = getClientConfigByClientId(clientId);
        if(cc.getClientProviders() != null && cc.getClientProviders().length > 0) {
            List<ProviderConfig> result = new ArrayList<>();
            for(ProviderConfig pc : getProviders()) {
                if(Arrays.asList(cc.getClientProviders()).contains(pc.getProvider())) {
                    result.add(pc);
                }
            }
            return result;
        } else {
            return getProviders();
        }
    }

    private List<ProviderConfig> getProviders() throws Exception {
        JsonNode providerNode = getConfig().get(providers);

        if (providerNode == null || !providerNode.isArray()) {
            LOGGER.severe("Fail to read provider from config file!");
            throw new Exception("Fail to read provider from config file!");
        }

        try {
            return Arrays.asList(MAPPER.readValue(providerNode.toString(), ProviderConfig[].class));
        } catch (Exception e) {
            LOGGER.severe("Fail to map providers [" + providerNode.toString() + "]");
            throw new Exception("Fail to map providers [" + providerNode.toString() + "]");
        }
    }

    public ProviderConfig getProviderConfigByProvider(String providerHint) throws Exception {
        return getProviders().stream()
                .filter(provider -> provider.getProvider().equalsIgnoreCase(providerHint))
                .findFirst()
                .orElse(null);
    }
}
