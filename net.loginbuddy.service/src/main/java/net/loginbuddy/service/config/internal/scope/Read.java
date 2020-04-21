package net.loginbuddy.service.config.internal.scope;

public class Read extends Configuration {

    private Clients clients;
    private Providers providers;
    private Discovery discovery;
    private Properties properties;

    public Clients getClients() {
        return clients;
    }

    public void setClients(Clients clients) {
        this.clients = clients;
    }

    public Providers getProviders() {
        return providers;
    }

    public void setProviders(Providers providers) {
        this.providers = providers;
    }

    public Discovery getDiscovery() {
        return discovery;
    }

    public void setDiscovery(Discovery discovery) {
        this.discovery = discovery;
    }

    public Properties getProperties() {
        return properties;
    }

    public void setProperties(Properties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isValidScope(String scope) {
        return "management.configuration.read".equals(scope) || super.isValidScope(scope);
    }

    @Override
    public String getScope() {
        return "management.configuration.read";
    }
}
