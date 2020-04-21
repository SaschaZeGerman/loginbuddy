package net.loginbuddy.service.config.internal.scope;

public class Clients extends ScopeChild {

    @Override
    String getChildScope() {
        return "clients";
    }
}
