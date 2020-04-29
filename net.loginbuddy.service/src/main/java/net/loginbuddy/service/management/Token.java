package net.loginbuddy.service.management;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.Jwt;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.service.config.LoginbuddyConfig;
import net.loginbuddy.service.server.ClientAuthenticator;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Token extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        response.addHeader("Cache-Control", "no-store");
        response.addHeader("Pragma", "no-cache");
        response.setStatus(400);

        ParameterValidatorResult clientIdResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CLIENT_ID.getKey()));
        ParameterValidatorResult clientSecretResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CLIENT_SECRET.getKey()));
        ParameterValidatorResult grantTypeResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.GRANT_TYPE.getKey()));
        ParameterValidatorResult resourceResult = ParameterValidator
                .getSingleValue(request.getParameterValues("resource"));
        ParameterValidatorResult nonceResult = ParameterValidator
                .getSingleValue(request.getParameterValues("nonce"));
        ParameterValidatorResult scopeResult = ParameterValidator
                .getSingleValue(request.getParameterValues("scope"));

        ClientAuthenticator.ClientCredentialsResult clientCredentialsResult = ClientAuthenticator.validateClientCredentials(clientIdResult, clientSecretResult, request.getHeader(Constants.AUTHORIZATION.getKey()));
        if(clientCredentialsResult.getErrorMsg() != null) {
            response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", clientCredentialsResult.getErrorMsg()).toJSONString());
            return;
        }

        if(clientCredentialsResult.getClientConfig().getClientProviders().length == 1 &&
                clientCredentialsResult.getClientConfig().getClientProviders()[0].equalsIgnoreCase("loginbuddy") &&
                clientCredentialsResult.getClientConfig().getClientType().equalsIgnoreCase("confidential") &&
                "client_credentials".equalsIgnoreCase(grantTypeResult.getValue())) {

            Map<String, String> claims = new HashMap<>();
            claims.put("nonce", nonceResult.getValue());
            // TODO validate scope and resource
            claims.put("resource", resourceResult.getValue());
            claims.put("scope", scopeResult.getValue());
            try {
                String token = Jwt.DEFAULT.createSignedJwtRs256(
                        LoginbuddyConfig.CONFIGS.getDiscoveryUtil().getIssuer(),
                        LoginbuddyConfig.CONFIGS.getDiscoveryUtil().getIssuer(),
                        5,
                        clientCredentialsResult.getClientConfig().getClientId(),
                        false,
                        claims)
                        .getCompactSerialization();
                JSONObject result = new JSONObject();
                result.put(Constants.ACCESS_TOKEN.getKey(), token);
                result.put(Constants.TOKEN_TYPE.getKey(), Constants.BEARER.getKey());
                result.put(Constants.EXPIRES_IN.getKey(), 300);
                result.put(Constants.SCOPE.getKey(), scopeResult.getValue());
                response.setStatus(200);
                response.getWriter().println(result.toJSONString());
            } catch (Exception e) {
                e.printStackTrace();
                response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "something went awfully wrong").toJSONString());
            }
        } else {
            response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "the client is not authorized for this API").toJSONString());
        }
    }
}
