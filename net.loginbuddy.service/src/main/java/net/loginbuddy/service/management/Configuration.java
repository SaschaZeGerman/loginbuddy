package net.loginbuddy.service.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.service.config.discovery.DiscoveryUtil;
import net.loginbuddy.service.config.loginbuddy.Clients;
import net.loginbuddy.service.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.service.config.loginbuddy.Providers;
import net.loginbuddy.service.config.properties.PropertiesUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Configuration extends ConfigurationMaster {

    protected void doGetProtected(HttpServletRequest request, HttpServletResponse response, ConfigurationTypes configType, String selector, AccessToken token) throws ServletException, IOException {

        int status = 400;
        String output = "{}";
        // TODO tie api to resource in access_token. endsWith() is not good enough
        if (token.getResource().endsWith("/management/configuration")) {
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
            output = HttpHelper.getErrorAsJson("invalid_request", "access_token not valid for this API").toJSONString();
        }
        response.setStatus(status);
        response.getWriter().println(output);
    }

    @Override
    protected String doPostProtected(String httpBody, ConfigurationTypes configType, String selector, AccessToken token) throws Exception {
        if (token.getResource().endsWith("/management/configuration")) {
            if (ConfigurationTypes.CLIENTS.equals(configType)) {
                return doPostClients(httpBody, token.getClientId(), selector, token.getScope());
            }
            else {
                return "";
//                return doPostProviders(selector, token.getScope());
            }
        } else {
            throw new IllegalArgumentException("requested resource is not available!");
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
    private String doPostClients(String httpBody, String clientId, String selector, @ActualScope String givenScope) throws
            JsonProcessingException {
        if (LoginbuddyScope.WriteClients.isScopeValid(givenScope)) {
            if (selector == null) {
                return LoginbuddyUtil.UTIL.getClientsAsJsonString(
                        LoginbuddyUtil.UTIL.replaceClients(clientId, httpBody));
            }
//            Clients clients = LoginbuddyUtil.UTIL.getClientConfigByClientId(selector);
//            if(clients != null) {
//                return LoginbuddyUtil.UTIL.getClientAsJsonString(clients);
//            }
        } else {
            return LoginbuddyScope.getInvalidScopeError(givenScope);
        }
        return "[]";
    }
}