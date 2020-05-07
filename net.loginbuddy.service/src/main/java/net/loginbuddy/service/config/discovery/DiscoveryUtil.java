/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.service.config.discovery;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.service.config.Bootstrap;

import java.io.File;
import java.util.logging.Logger;

public enum DiscoveryUtil implements Bootstrap {

    CONFIG;

    private Logger LOGGER = Logger.getLogger(String.valueOf(DiscoveryUtil.class));

    private com.fasterxml.jackson.databind.ObjectMapper MAPPER = new ObjectMapper();

    private String path;

    private DiscoveryConfig config;

    DiscoveryUtil() {
        try {
            config = MAPPER.readValue(new File(this.path).getAbsoluteFile(), DiscoveryConfig.class);
        } catch (Exception e) {
            LOGGER.severe("discovery.json file could not be loaded or it is invalid JSON! Existing!");
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    private DiscoveryConfig getConfig() {
        try {
            config = (DiscoveryConfig) LoginbuddyCache.CACHE.get("DiscoveryConfig");
            if (config == null) {
                config = MAPPER.readValue(new File(this.path).getAbsoluteFile(), DiscoveryConfig.class);
                LoginbuddyCache.CACHE.put("DiscoveryConfig", config);
            }
            return config;
        } catch (Exception e) {
            LOGGER.severe("discovery.json file could not be loaded or it is invalid JSON! Existing!");
        }
        return null; // we should never get here
    }

    /**
     * A helper method for dynamic registrations
     */
    public String getRedirectUri() {
        return getRedirectUri("/callback");
    }

    /**
     * A helper method for dynamic registrations on dynamic providers
     */
    public String getRedirectUri(String path) {
        return getIssuer() + path;
    }

    public String getIssuer() {
        return getConfig().getIssuer();
    }

    public String[] getResponseTypesSupported() {
        return getConfig().getResponseTypesSupported();
    }

    public String[] getGrantTypesSupported() {
        return getConfig().getGrantTypesSupported();
    }

    public String[] getTokenEndpointAuthMethodsSupported() {
        return getConfig().getTokenEndpointAuthMethodsSupported();
    }

    public String[] getScopesSupported() {
        return getConfig().getScopesSupported();
    }

    public String getAuthorizationEndpoint() {
        return getConfig().getAuthorizationEndpoint();
    }

    public String getTokenEndpoint() {
        return getConfig().getTokenEndpoint();
    }

    public String getJwksUri() {
        return getConfig().getJwksUri();
    }

    public String[] getIdTokenSigningAlgValuesSupported() {
        return getConfig().getIdTokenSigningAlgValuesSupported();
    }

    public String getServiceDocumentation() {
        return getConfig().getServiceDocumentation();
    }

    public String[] getSubjectTypesSupported() {
        return getConfig().getSubjectTypesSupported();
    }

    public String[] getCodechallengeMethodsSupported() {
        return getConfig().getCodeChallengeMethodsSupported();
    }

    public String getUserinfoEndpoint() {
        return getConfig().getUserinfoEndpoint();
    }

    public String getPushedAuthorizationRequestEndpoint() {
        return getConfig().getPushedAuthorizationRequestEndpoint();
    }

    public DiscoveryConfig getOpenIdConfiguration() {
        return getConfig();
    }

    public String getOpenIdConfigurationAsJsonString() {
        try {
            return MAPPER.writeValueAsString(config);
        } catch (JsonProcessingException e) {
            LOGGER.warning("Discovery document could not be produced as string");
        }
        return "{\"error\":\"invalid_confiuration\", \"error_description\":\"the servcer configuration is faulty. Please contact teh administrator!\"}";
    }

    public String getTokenEndpointAuthMethodsSupportedAsString() {
        return HttpHelper.stringArrayToString(getTokenEndpointAuthMethodsSupported());
    }

    public String getScopesSupportedAsString() {
        return HttpHelper.stringArrayToString(getScopesSupported());
    }

    @Override
    public boolean isConfigured() {
        return getConfig() != null;
    }

    public String[] getSigningAlgValuesSupported() {
        return getConfig().getSigningAlgValuesSupported();
    }

    public String getSigningAlgValuesSupportedAsString() {
        return HttpHelper.stringArrayToString(getSigningAlgValuesSupported());
    }
}