package net.loginbuddy.service.config.discovery;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Management {

    @JsonProperty("configuration_endpoint")
    private String configurationEndpoint;

    public String getConfiguration() {
        return configurationEndpoint;
    }

    public void setConfiguration(String configurationResource) {
        this.configurationEndpoint = configurationResource;
    }
}
