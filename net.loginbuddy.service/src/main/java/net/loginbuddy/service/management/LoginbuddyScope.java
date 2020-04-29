package net.loginbuddy.service.management;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.util.Sanetizer;

public enum LoginbuddyScope {

    Management("management", "access to any management APIs"),
    Configuration("management.configuration", "access to configuration management APIs" ),
    Read("management.configuration.read", "access to any read-only configuration APIs"),
    ReadClients("management.configuration.read.clients", "access to read-only client configuration APIs"),
    ReadProviders("management.configuration.read.providers", "access to read-only providers configuration APIs"),
    ReadProperties("management.configuration.read.properties", "access to read-only properties configuration APIs"),
    ReadDiscovery("management.configuration.read.discovery", "access to read-only discovery configuration APIs"),
    Write("management.configuration.write", "access to any write-only configuration APIs"),
    WriteClients("management.configuration.write.clients", "access to write-only client configuration APIs"),
    WriteProviders("management.configuration.write.providers", "access to write-only providers configuration APIs"),
    WriteProperties("management.configuration.write.properties", "access to write-only properties configuration APIs"),
    WriteDiscovery("management.configuration.write.discovery", "access to write-only discovery configuration APIs");

    String scope, description;

    LoginbuddyScope(String scope, String description) {
        this.scope = scope;
        this.description = description;
    }

    public boolean isScopeValid(String actualScope) {
        if(actualScope != null) {
            for(String next : actualScope.split("[;, ]")){
                if(scope.startsWith(next)) {
                    return true;
                }
            }
        }
        return false;
    }

    public String getScope() {
        return scope;
    }

    public String getDescription() {
        return description;
    }

    public static String getInvalidScopeError(String givenScope) {
        return HttpHelper.getErrorAsJson("invalid_request", String.format("missing required scope. Given: '%s'", Sanetizer.sanetize(givenScope))).toJSONString();
    }
}