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
import net.loginbuddy.service.config.Bootstrap;

import java.util.logging.Logger;

public enum DiscoveryUtil implements Bootstrap {

    UTIL;

    private Logger LOGGER = Logger.getLogger(String.valueOf(DiscoveryUtil.class));

    private DiscoveryLoader loader;
    private com.fasterxml.jackson.databind.ObjectMapper MAPPER = new ObjectMapper();

    DiscoveryUtil() {
        try {
            setDefaultLoader();
        } catch (Exception e) {
            LOGGER.severe(String.format("Discovery document could not be loaded! Error: '%s'", e.getMessage()));

        }
    }

    public void setDefaultLoader() throws Exception {
        setLoader(new DefaultLoader());
    }

    public void setLoader(DiscoveryLoader loader) throws Exception{
        this.loader = loader;
        this.loader.load();
    }

    @Override
    public boolean isConfigured() {
        return loader != null && loader.isConfigured();
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
        return loader.getDiscoveryConfig().getIssuer();
    }

    public String[] getResponseTypesSupported() {
        return loader.getDiscoveryConfig().getResponseTypesSupported();
    }

    public String[] getGrantTypesSupported() {
        return loader.getDiscoveryConfig().getGrantTypesSupported();
    }

    public String[] getTokenEndpointAuthMethodsSupported() {
        return loader.getDiscoveryConfig().getTokenEndpointAuthMethodsSupported();
    }

    public String[] getScopesSupported() {
        return loader.getDiscoveryConfig().getScopesSupported();
    }

    public String getAuthorizationEndpoint() {
        return loader.getDiscoveryConfig().getAuthorizationEndpoint();
    }

    public String getTokenEndpoint() {
        return loader.getDiscoveryConfig().getTokenEndpoint();
    }

    public String getJwksUri() {
        return loader.getDiscoveryConfig().getJwksUri();
    }

    public String[] getIdTokenSigningAlgValuesSupported() {
        return loader.getDiscoveryConfig().getIdTokenSigningAlgValuesSupported();
    }

    public String getServiceDocumentation() {
        return loader.getDiscoveryConfig().getServiceDocumentation();
    }

    public String[] getSubjectTypesSupported() {
        return loader.getDiscoveryConfig().getSubjectTypesSupported();
    }

    public String[] getCodeChallengeMethodsSupported() {
        return loader.getDiscoveryConfig().getCodeChallengeMethodsSupported();
    }

    public String getUserinfoEndpoint() {
        return loader.getDiscoveryConfig().getUserinfoEndpoint();
    }

    public String getPushedAuthorizationRequestEndpoint() {
        return loader.getDiscoveryConfig().getPushedAuthorizationRequestEndpoint();
    }

    public String getOpenIdConfigurationAsJsonString() {
        try {
            return MAPPER.writeValueAsString(loader.getDiscoveryConfig());
        } catch (JsonProcessingException e) {
            LOGGER.warning("Discovery document could not be produced as string");
        }
        return "{\"error\":\"invalid_configuration\", \"error_description\":\"the server configuration is faulty. Please contact the administrator!\"}";
    }

    public String getTokenEndpointAuthMethodsSupportedAsString() {
        return HttpHelper.stringArrayToString(getTokenEndpointAuthMethodsSupported());
    }

    public String getScopesSupportedAsString() {
        return HttpHelper.stringArrayToString(getScopesSupported());
    }

    public String[] getSigningAlgValuesSupported() {
        return loader.getDiscoveryConfig().getSigningAlgValuesSupported();
    }

    public String getSigningAlgValuesSupportedAsString() {
        return HttpHelper.stringArrayToString(getSigningAlgValuesSupported());
    }

    public Management getManagement() {
        return loader.getDiscoveryConfig().getManagement();
    }

    public DiscoveryLoader getLoader() {
        return loader;
    }
}