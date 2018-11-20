/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
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
            LOGGER.severe("Failed to read config file ::" + e.getMessage());
            throw e;
        }
    }

    private List<ClientConfig> getClients() throws Exception {
        JsonNode clientsNode = getConfig().get(clients);

        if (clientsNode == null || !clientsNode.isArray()) {
            throw new Exception("Fail to read provider from config file");
        }

        try {
            return Arrays.asList(MAPPER.readValue(clientsNode.toString(), ClientConfig[].class));
        } catch (Exception e) {
            throw new Exception("Fail to map clients [" + clientsNode.toString() + "]");
        }
    }

    public List<ProviderConfig> getProviders() throws Exception {
        JsonNode providerNode = getConfig().get(providers);

        if (providerNode == null || !providerNode.isArray()) {
            throw new Exception("Fail to read provider from config file");
        }

        try {
            return Arrays.asList(MAPPER.readValue(providerNode.toString(), ProviderConfig[].class));
        } catch (Exception e) {
            throw new Exception("Fail to map providers [" + providerNode.toString() + "]");
        }
    }

    public ClientConfig getClientConfigByRedirectUri(String redirectUri) throws Exception {
        return getClients().stream()
                .filter(clientConfig -> clientConfig.getRedirectUri().equals(redirectUri))
                .findFirst()
                .orElse(null);
    }

    public ProviderConfig getProviderConfigByProvider(String loginHint) throws Exception {
        return getProviders().stream()
                .filter(provider -> provider.getProvider().equals(loginHint))
                .findFirst()
                .orElse(null);
    }
}
