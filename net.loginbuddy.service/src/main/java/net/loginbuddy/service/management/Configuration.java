package net.loginbuddy.service.management;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.service.config.ClientConfig;
import net.loginbuddy.service.config.LoginbuddyConfig;
import net.loginbuddy.service.config.ProviderConfig;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Configuration extends ConfigurationMaster {

    protected void doGet(HttpServletRequest request, HttpServletResponse response, ConfigurationTypes configType, String selector) throws ServletException, IOException {

        int status = 400;
        String output = "{}";
        if (ConfigurationTypes.CLIENTS.equals(configType)) {
            if (selector != null) {
                status = 200;
                ClientConfig clientConfig = LoginbuddyConfig.getInstance().getConfigUtil().getClientConfigByClientId(selector);
                output = clientConfig == null ? "{}" : new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientConfig);
            } else {
                output = HttpHelper.getErrorAsJson("invalid_request", "required client_id was not given or was provided multiple times").toJSONString();
            }
        } else if (ConfigurationTypes.PROVIDERS.equals(configType)) {
            if (selector != null) {
                status = 200;
                ProviderConfig providerConfig = LoginbuddyConfig.getInstance().getConfigUtil().getProviderConfigByProvider(selector);
                output = providerConfig == null ? "{}" : new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(providerConfig);
            } else {
                output = HttpHelper.getErrorAsJson("invalid_request", "required provider was not given or was provided multiple times").toJSONString();
            }
        } else if (ConfigurationTypes.PROPERTIES.equals(configType)) {
            status = 200;
            output = LoginbuddyConfig.getInstance().getPropertiesUtil().getPropertiesAsJsonString();
        } else if (ConfigurationTypes.DISCOVERY.equals(configType)) {
            status = 200;
            output = LoginbuddyConfig.getInstance().getDiscoveryUtil().getOpenIdConfigurationAsJsonString();
        }
        response.setStatus(status);
        response.getWriter().println(output);
    }
}