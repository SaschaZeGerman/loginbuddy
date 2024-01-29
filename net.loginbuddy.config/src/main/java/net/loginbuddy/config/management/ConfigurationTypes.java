package net.loginbuddy.config.management;

import net.loginbuddy.config.management.scope.LoginbuddyScope;

public enum ConfigurationTypes {

    CLIENTS("clients", LoginbuddyScope.ReadClients, LoginbuddyScope.WriteClients),
    PROVIDERS("providers", LoginbuddyScope.ReadProviders, LoginbuddyScope.WriteProviders),
    DISCOVERY("discovery", LoginbuddyScope.ReadDiscovery),
    PROPERTIES("properties", LoginbuddyScope.ReadProperties);

    private String type;
    private LoginbuddyScope[] scope;

    ConfigurationTypes(String type, LoginbuddyScope... scope) {
        this.type = type;
        this.scope = scope;
    }

    public LoginbuddyScope[] getScope() {
        return scope;
    }

    @Override
    public String toString() {
        return this.type;
    }

}