package net.loginbuddy.service.management;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.management.AccessToken;
import net.loginbuddy.config.management.AccessTokenLocation;
import net.loginbuddy.config.management.ConfigurationTypes;
import net.loginbuddy.config.management.RuntimeTypes;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class RuntimeMaster extends Management {

    public static boolean isRuntimeManagementEnabled() {
        return isManagementEnabled() && DiscoveryUtil.UTIL.getManagement().getRuntimeEndpoint() != null;
    }

    // TODO update group(2) when a JSON schema gets introduced that validates values such as permitted provider name length
    private static Pattern pEntities = Pattern.compile(String.format("^/(%s)(?:/([a-zA-Z0-9-_]{1,64}))?$", RuntimeTypes.CACHE));

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");

        if (!isRuntimeManagementEnabled()) {
            response.setStatus(400);
            response.getWriter().println(HttpHelper.getErrorAsJson("invalid_request", "runtime management is not enabled").toJSONString());
            return;
        }

        ParameterValidatorResult actionResult = ParameterValidator.getSingleValue(request.getParameterValues("action"));
        if (!actionResult.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
            response.setStatus(400);
            response.getWriter().println(HttpHelper.getErrorAsJson("invalid_request", "the action for this request is missing or was provided multiple times").toJSONString());
            return;
        }

        Matcher matcher = pEntities.matcher(request.getPathInfo() == null ? "unknown" : request.getPathInfo());
        if (matcher.find()) {
            try {
                handleGet(request, response, RuntimeTypes.valueOf(matcher.group(1).toUpperCase()), matcher.group(2), new AccessToken(request, AccessTokenLocation.HEADER), actionResult.getValue());
            } catch (IllegalArgumentException e) {
                response.setStatus(400);
                response.getWriter().println(HttpHelper.getErrorAsJson("invalid_request", e.getMessage()).toJSONString());
            }
        } else {
            response.setStatus(400);
            response.getWriter().println(HttpHelper.getErrorAsJson("invalid_request", "runtime type was not given or is not supported").toJSONString());
        }
    }


    /**
     * This method retrieves or updated runtime data.
     *
     * @param request     the request
     * @param response    the response
     * @param runtimeType the requested runtime type. Values must match a valid of RuntimeTypes
     * @param selector    depending on the type this value must identify a specific runtime entity
     * @param token       the access_token for this request/ It must have been granted for required scope and resource
     * @param action      the action to take on the given type and selector
     * @throws ServletException
     * @throws IOException
     */
    protected abstract void handleGet(HttpServletRequest request, HttpServletResponse response, RuntimeTypes runtimeType, String selector, AccessToken token, String action) throws ServletException, IOException;

}
