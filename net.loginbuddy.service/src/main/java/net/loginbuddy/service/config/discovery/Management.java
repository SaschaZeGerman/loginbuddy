package net.loginbuddy.service.config.discovery;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Management {

    @JsonProperty("configuration_endpoint")
    private String configurationEndpoint;

    public String getConfigurationEndpoint() {
        return configurationEndpoint;
    }

    public void setConfigurationEndpoint(String configurationResource) {
        this.configurationEndpoint = configurationResource;
    }
}
