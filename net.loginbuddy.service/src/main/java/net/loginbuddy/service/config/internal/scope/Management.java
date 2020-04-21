package net.loginbuddy.service.config.internal.scope;

public class Management implements ScopeVerifier {

    private Configuration configuration;

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    @Override
    public boolean isValidScope(String scope) {
        return "management".equals(scope);
    }

    @Override
    public String getScope() {
        return "management";
    }
}