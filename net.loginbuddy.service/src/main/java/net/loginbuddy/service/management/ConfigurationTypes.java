package net.loginbuddy.service.management;

public enum ConfigurationTypes {

    CLIENTS("clients"),
    PROVIDERS("providers"),
    DISCOVERY("discovery"),
    PROPERTIES("properties");

    private String type;

    ConfigurationTypes(String type) {
        this.type = type;
    }

    @Override
    public String toString() {
        return this.type;
    }

}