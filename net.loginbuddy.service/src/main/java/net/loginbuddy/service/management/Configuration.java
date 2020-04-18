package net.loginbuddy.service.management;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.service.config.ClientConfig;
import net.loginbuddy.service.config.LoginbuddyConfig;
import net.loginbuddy.service.config.ProviderConfig;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Configuration extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");

        ParameterValidatorResult configType = ParameterValidator
                .getSingleValue(request.getParameterValues("type"));

        if (configType.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
            if ("client".equalsIgnoreCase(configType.getValue())) {
                ParameterValidatorResult clientId = ParameterValidator
                        .getSingleValue(request.getParameterValues(Constants.CLIENT_ID.getKey()));
                if (clientId.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
                    response.setStatus(200);
                    ClientConfig clientConfig = LoginbuddyConfig.getInstance().getConfigUtil().getClientConfigByClientId(clientId.getValue());
                    response.getWriter().println(clientConfig == null ? "{}" : new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(clientConfig));
                } else {
                    String error = HttpHelper.getErrorAsJson("invalid_request", "required client_id was not given or was provided multiple times").toJSONString();
                    response.setStatus(400);
                    response.getWriter().println(error);
                }
            } else if ("provider".equalsIgnoreCase(configType.getValue())) {
                ParameterValidatorResult provider = ParameterValidator
                        .getSingleValue(request.getParameterValues(Constants.PROVIDER.getKey()));
                if (provider.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
                    response.setStatus(200);
                    ProviderConfig providerConfig = LoginbuddyConfig.getInstance().getConfigUtil().getProviderConfigByProvider(provider.getValue());
                    response.getWriter().println(providerConfig == null ? "{}" : new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(providerConfig));
                } else {
                    String error = HttpHelper.getErrorAsJson("invalid_request", "required provider was not given or was provided multiple times").toJSONString();
                    response.setStatus(400);
                    response.getWriter().println(error);
                }
            } else if ("properties".equalsIgnoreCase(configType.getValue())) {
                response.setStatus(200);
                response.getWriter().println(LoginbuddyConfig.getInstance().getPropertiesUtil().getPropertiesAsJsonString());
            } else if ("discovery".equalsIgnoreCase(configType.getValue())) {
                response.setStatus(200);
                response.getWriter().println(LoginbuddyConfig.getInstance().getDiscoveryUtil().getOpenIdConfigurationAsJsonString());
            } else {
                String error = HttpHelper.getErrorAsJson("invalid_request", String.format("the given configuration type is not supported. Given: '%s'", configType.getValue())).toJSONString();
                response.setStatus(400);
                response.getWriter().println(error);
            }
        } else {
            String error = HttpHelper.getErrorAsJson("invalid_request", "configuration type was not given or was provided multiple times").toJSONString();
            response.setStatus(400);
            response.getWriter().println(error);
        }
    }
}
