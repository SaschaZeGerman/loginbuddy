package net.loginbuddy.config.discovery;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Management {

    @JsonProperty("configuration_endpoint")
    private String configurationEndpoint;

    @JsonProperty("runtime_endpoint")
    private String runtimeEndpoint;

    public String getConfigurationEndpoint() {
        return configurationEndpoint;
    }

    public void setConfigurationEndpoint(String configurationResource) {
        this.configurationEndpoint = configurationResource;
    }

    public String getRuntimeEndpoint() {
        return runtimeEndpoint;
    }

    public void setRuntimeEndpoint(String runtimeEndpoint) {
        this.runtimeEndpoint = runtimeEndpoint;
    }
}
