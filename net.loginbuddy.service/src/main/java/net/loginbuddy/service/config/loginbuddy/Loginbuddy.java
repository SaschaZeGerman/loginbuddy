package net.loginbuddy.service.config.loginbuddy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class Loginbuddy {

    @JsonProperty("clients")
    @JsonIgnore(false)
    private List<Clients> clients;

    @JsonProperty("providers")
    @JsonIgnore(false)
    private List<Providers> providers;

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
}
