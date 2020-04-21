package net.loginbuddy.service.config.internal.scope;

import com.fasterxml.jackson.annotation.JsonIgnore;

public abstract class ScopeChild implements ScopeVerifier {

    @JsonIgnore
    private ScopeVerifier parent;

    private String description;

    public ScopeVerifier getParent() {
        return parent;
    }

    public void setParent(ScopeVerifier parent) {
        this.parent = parent;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public boolean isValidScope(String scope) {
        return getScope().equals(scope) || parent.isValidScope(scope);
    }

    @Override
    public String getScope() {
        return String.format("%s.%s", parent.getScope(), getChildScope());
    }

    abstract String getChildScope();
}