package net.loginbuddy.service.config.discovery;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
