package net.loginbuddy.config.loginbuddy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.loginbuddy.config.Bootstrap;

import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class Loginbuddy implements Bootstrap, Cloneable {

    @JsonProperty("clients")
    @JsonIgnore(false)
    private List<Clients> clients;

    @JsonProperty("providers")
    @JsonIgnore(false)
    private List<Providers> providers;

    public Loginbuddy() {
        clients = new ArrayList<>();
        providers = new ArrayList<>();
    }

    public List<Clients> getClients() {
        return clients;
    }

    public void setClients(List<Clients> clients) {
        this.clients = clients;
    }

    public List<Providers> getProviders() {
        return providers;
    }

    public void setProviders(List<Providers> providers) {
        this.providers = providers;
    }

    @Override
    @JsonIgnore
    public boolean isConfigured() {
        return clients != null && providers != null;
    }
}