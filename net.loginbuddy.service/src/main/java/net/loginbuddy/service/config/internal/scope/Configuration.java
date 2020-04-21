package net.loginbuddy.service.config.internal.scope;

public class Configuration extends Management implements ScopeVerifier {

    private Read read;
    private Write write;

    public Read getRead() {
        return read;
    }

    public void setRead(Read read) {
        this.read = read;
    }

    public Write getWrite() {
        return write;
    }

    public void setWrite(Write write) {
        this.write = write;
    }

    @Override
    public boolean isValidScope(String scope) {
        return "management.configuration".equals(scope) || super.isValidScope(scope);
    }

    @Override
    public String getScope() {
        return "management.configuration";
    }
}
