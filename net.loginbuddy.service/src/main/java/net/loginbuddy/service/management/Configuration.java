package net.loginbuddy.service.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.loginbuddy.Clients;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.config.loginbuddy.Providers;
import net.loginbuddy.config.management.AccessToken;
import net.loginbuddy.config.management.ActualScope;
import net.loginbuddy.config.management.ConfigurationTypes;
import net.loginbuddy.config.properties.PropertiesUtil;
import net.loginbuddy.config.management.scope.LoginbuddyScope;
import net.loginbuddy.config.management.scope.RequireScope;

import java.io.IOException;

public class Configuration extends ConfigurationMaster {

    protected void doGetProtected(HttpServletRequest request, HttpServletResponse response, ConfigurationTypes configType, String selector, AccessToken token) throws ServletException, IOException {

        int status = 400;
        String output = "{}";
        if (DiscoveryUtil.UTIL.getManagement().getConfigurationEndpoint().equals(token.getResource())) {
            if (ConfigurationTypes.CLIENTS.equals(configType)) {
                if (selector != null) {
                    status = 200;
                    output = doGetClients(selector, token.getScope());
                } else {
                    output = HttpHelper.getErrorAsJson("invalid_request", "required client_id was not given or was provided multiple times").toJSONString();
                }
            } else if (ConfigurationTypes.PROVIDERS.equals(configType)) {
                if (selector != null) {
                    status = 200;
                    output = doGetProviders(selector, token.getScope());
                } else {
                    output = HttpHelper.getErrorAsJson("invalid_request", "required provider was not given or was provided multiple times").toJSONString();
                }
            } else if (ConfigurationTypes.PROPERTIES.equals(configType)) {
                status = 200;
                output = doGetProperties(token.getScope());
            } else if (ConfigurationTypes.DISCOVERY.equals(configType)) {
                status = 200;
                output = doGetDiscovery(token.getScope());
            }
        } else {
            output = HttpHelper.getErrorAsJson("invalid_request", "access_token not valid for this resource").toJSONString();
        }
        response.setStatus(status);
        response.getWriter().println(output);
    }

    @Override
    protected String doPostProtected(String httpBody, ConfigurationTypes configType, String selector, AccessToken token) throws Exception {
        if (DiscoveryUtil.UTIL.getManagement().getConfigurationEndpoint().equals(token.getResource())) {
            if (ConfigurationTypes.CLIENTS.equals(configType)) {
                return doPostClients(httpBody, token.getClientId(), selector, token.getScope());
            } else if (ConfigurationTypes.PROVIDERS.equals(configType)) {
                return doPostProviders(httpBody, selector, token.getScope());
            } else {
                throw new IllegalArgumentException("requested configuration is not supported");
            }
        } else {
            throw new IllegalArgumentException("access_token not valid for this resource");
        }
    }

    @Override
    protected String doPutProtected(String httpBody, ConfigurationTypes configType, String selector, AccessToken token) throws Exception {
        if (DiscoveryUtil.UTIL.getManagement().getConfigurationEndpoint().equals(token.getResource())) {
            if (ConfigurationTypes.CLIENTS.equals(configType)) {
                return doPutClients(httpBody, token.getClientId(), selector, token.getScope());
            } else if (ConfigurationTypes.PROVIDERS.equals(configType)) {
                return doPutProviders(httpBody, selector, token.getScope());
            } else {
                throw new IllegalArgumentException("requested configuration is not supported");
            }
        } else {
            throw new IllegalArgumentException("access_token not valid for this resource");
        }
    }

    @RequireScope(expected = LoginbuddyScope.ReadClients)
    private String doGetClients(String selector, @ActualScope String givenScope) throws JsonProcessingException {
        if (LoginbuddyScope.ReadClients.isScopeValid(givenScope)) {
            if ("all".equalsIgnoreCase(selector)) {
                return LoginbuddyUtil.UTIL.getClientsAsJsonString();
            }
            Clients clients = LoginbuddyUtil.UTIL.getClientConfigByClientId(selector);
            if (clients != null) {
                return LoginbuddyUtil.UTIL.getClientAsJsonString(clients);
            }
        } else {
            return LoginbuddyScope.getInvalidScopeError(givenScope);
        }
        return "{}";
    }

    @RequireScope(expected = LoginbuddyScope.ReadProviders)
    private String doGetProviders(String selector, @ActualScope String givenScope) throws JsonProcessingException {
        if (LoginbuddyScope.ReadProviders.isScopeValid(givenScope)) {
            if ("all".equalsIgnoreCase(selector)) {
                return LoginbuddyUtil.UTIL.getProvidersAsJsonString();
            }
            Providers provider = LoginbuddyUtil.UTIL.getProviderConfigByProvider(selector);
            if (provider != null) {
                return LoginbuddyUtil.UTIL.getProviderAsJsonString(provider);
            }
        } else {
            return LoginbuddyScope.getInvalidScopeError(givenScope);
        }
        return "{}";
    }

    @RequireScope(expected = LoginbuddyScope.ReadProperties)
    private String doGetProperties(@ActualScope String givenScope) {
        if (LoginbuddyScope.ReadProperties.isScopeValid(givenScope)) {
            return PropertiesUtil.UTIL.getPropertiesAsJsonString();
        } else {
            return LoginbuddyScope.getInvalidScopeError(givenScope);
        }
    }

    @RequireScope(expected = LoginbuddyScope.ReadDiscovery)
    private String doGetDiscovery(@ActualScope String givenScope) {
        if (LoginbuddyScope.ReadDiscovery.isScopeValid(givenScope)) {
            return DiscoveryUtil.UTIL.getOpenIdConfigurationAsJsonString();
        } else {
            return LoginbuddyScope.getInvalidScopeError(givenScope);
        }
    }

    @RequireScope(expected = LoginbuddyScope.WriteClients)
    private String doPostClients(String httpBody, String requestingClientId, String selector, @ActualScope String givenScope) throws
            Exception {
        if (LoginbuddyScope.WriteClients.isScopeValid(givenScope)) {
            if (selector == null && httpBody.startsWith("[")) {
                return LoginbuddyUtil.UTIL.getClientsAsJsonString(
                        LoginbuddyUtil.UTIL.replaceClients(requestingClientId, httpBody));
            } else {
                throw new IllegalArgumentException("provide a list of client configurations");
            }
        } else {
            return LoginbuddyScope.getInvalidScopeError(givenScope);
        }
    }

    @RequireScope(expected = LoginbuddyScope.WriteProviders)
    private String doPostProviders(String httpBody, String selector, @ActualScope String givenScope) throws
            Exception {
        if (LoginbuddyScope.WriteProviders.isScopeValid(givenScope)) {
            if (selector == null && httpBody.startsWith("[")) {
                return LoginbuddyUtil.UTIL.getProvidersAsJsonString(
                        LoginbuddyUtil.UTIL.replaceProviders(httpBody));
            } else {
                throw new IllegalArgumentException("provide a list of provider configurations");
            }
        } else {
            return LoginbuddyScope.getInvalidScopeError(givenScope);
        }
    }

    @RequireScope(expected = LoginbuddyScope.WriteClients)
    private String doPutClients(String httpBody, String requestingClientId, String selector, @ActualScope String givenScope) throws
            Exception {
        if (LoginbuddyScope.WriteClients.isScopeValid(givenScope)) {
            if (selector == null && httpBody.startsWith("[")) {
                return LoginbuddyUtil.UTIL.getClientsAsJsonString(
                        LoginbuddyUtil.UTIL.updateClients(requestingClientId, httpBody));
            } else {
                throw new IllegalArgumentException("provide a list of client configurations");
            }
        } else {
            return LoginbuddyScope.getInvalidScopeError(givenScope);
        }
    }

    @RequireScope(expected = LoginbuddyScope.WriteProviders)
    private String doPutProviders(String httpBody, String selector, @ActualScope String givenScope) throws
            Exception {
        if (LoginbuddyScope.WriteProviders.isScopeValid(givenScope)) {
            if (selector == null && httpBody.startsWith("[")) {
                return LoginbuddyUtil.UTIL.getProvidersAsJsonString(
                        LoginbuddyUtil.UTIL.updateProviders(httpBody));
            } else {
                throw new IllegalArgumentException("provide a list of provider configurations");
            }
        } else {
            return LoginbuddyScope.getInvalidScopeError(givenScope);
        }
    }
}