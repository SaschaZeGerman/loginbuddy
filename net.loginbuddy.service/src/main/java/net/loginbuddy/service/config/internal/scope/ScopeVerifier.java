package net.loginbuddy.service.config.internal.scope;

public interface ScopeVerifier {

    boolean isValidScope(String scope);
    String getScope();
}
