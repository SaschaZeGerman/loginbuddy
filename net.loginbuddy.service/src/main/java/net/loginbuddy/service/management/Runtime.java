package net.loginbuddy.service.management;

import com.fasterxml.jackson.core.JsonProcessingException;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.management.AccessToken;
import net.loginbuddy.config.management.ActualScope;
import net.loginbuddy.config.management.RuntimeTypes;
import net.loginbuddy.config.scope.LoginbuddyScope;
import net.loginbuddy.config.scope.RequireScope;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class Runtime extends RuntimeMaster {

    @Override
    protected void handleGet(HttpServletRequest request, HttpServletResponse response, RuntimeTypes runtimeType, String selector, AccessToken token, String action) throws ServletException, IOException {

        int status = 400;
        String output = "{}";
        if (DiscoveryUtil.UTIL.getManagement().getRuntimeEndpoint().equals(token.getResource())) {
            if (RuntimeTypes.CACHE.equals(runtimeType)) {
                if (selector != null) {
                    status = 200;
                    output = doCache(selector, token.getScope(), action);
                } else {
                    output = HttpHelper.getErrorAsJson("invalid_request", "required selector was not given or was provided multiple times").toJSONString();
                }
            }
        } else {
            output = HttpHelper.getErrorAsJson("invalid_request", "access_token not valid for this resource").toJSONString();
        }
        response.setStatus(status);
        response.getWriter().println(output);

    }

    @RequireScope(expected = LoginbuddyScope.RuntimeWrite)
    private String doCache(String selector, @ActualScope String givenScope, String action) throws JsonProcessingException {
        if (LoginbuddyScope.RuntimeWrite.isScopeValid(givenScope)) {
            if ("providers".equals(selector)) {
                if ("flush".equals(action)) {
                    Object o = LoginbuddyCache.CACHE.remove("providers");
                    if (o instanceof List) {
                        return String.format("{\"cache\": {\"providers_flushed\":%d}}", ((List) o).size());
                    } else {
                        return "{\"cache\": {\"providers_flushed\":-1}}";
                    }
                } else {
                    throw new IllegalArgumentException("given action is not supported");
                }
            } else {
                throw new IllegalArgumentException("given selector is not supported");
            }
        } else {
            return LoginbuddyScope.getInvalidScopeError(givenScope);
        }
    }
}
