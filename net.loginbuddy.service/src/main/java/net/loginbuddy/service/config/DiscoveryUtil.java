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
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;

import java.io.File;
import java.util.logging.Logger;

public class DiscoveryUtil {

    private Logger LOGGER = Logger.getLogger(String.valueOf(DiscoveryUtil.class));

    private com.fasterxml.jackson.databind.ObjectMapper MAPPER = new ObjectMapper();

    private String path;

    public DiscoveryUtil() {
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    private DiscoveryConfig getConfig() {
        try {
            DiscoveryConfig config = (DiscoveryConfig) LoginbuddyCache.getInstance().get("DiscoveryConfig");
            if (config == null) {
                JsonNode node = MAPPER.readValue(new File(this.path).getAbsoluteFile(), JsonNode.class);
                config = MAPPER.readValue(node.toString(), DiscoveryConfig.class);
                config.setJsonString(node.toString());
                LoginbuddyCache.getInstance().put("DiscoveryConfig", config);
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
        return getConfig().getResponseTypeSupported();
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
        return getConfig().toString();
    }

    public String getTokenEndpointAuthMethodsSupportedAsString() {
        return HttpHelper.stringArrayToString(getTokenEndpointAuthMethodsSupported());
    }

    public String getScopesSupportedAsString() {
        return HttpHelper.stringArrayToString(getScopesSupported());
    }

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