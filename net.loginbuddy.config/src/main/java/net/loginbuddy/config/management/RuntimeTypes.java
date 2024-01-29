package net.loginbuddy.config.management;

import net.loginbuddy.config.management.scope.LoginbuddyScope;

public enum RuntimeTypes {

    CACHE("cache", LoginbuddyScope.Runtime, LoginbuddyScope.RuntimeRead, LoginbuddyScope.RuntimeWrite);

    private String type;
    private LoginbuddyScope[] scope;

    RuntimeTypes(String type, LoginbuddyScope... scope) {
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