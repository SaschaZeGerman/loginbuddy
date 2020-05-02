package net.loginbuddy.service.management;

import net.loginbuddy.common.api.HttpHelper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ConfigurationMaster extends HttpServlet {

    // TODO update group(2) when a JSON schema gets introduced that validates values such as permitted provider name length
    private static Pattern pEntities = Pattern.compile(
            String.format("^/(%s|%s|%s|%s)(?:/([a-zA-Z0-9-_]{1,64}))?$",
                    ConfigurationTypes.CLIENTS.toString(),
                    ConfigurationTypes.PROVIDERS.toString(),
                    ConfigurationTypes.DISCOVERY.toString(),
                    ConfigurationTypes.PROPERTIES.toString()));

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");

        Matcher matcher = pEntities.matcher(request.getPathInfo() == null ? "unknown" : request.getPathInfo());
        if (matcher.find()) {
            try {
                doGetProtected(request, response, ConfigurationTypes.valueOf(matcher.group(1).toUpperCase()), matcher.group(2), new AccessToken(request, AccessToken.TokenLocation.HEADER));
            } catch (IllegalArgumentException e) {
                response.setStatus(400);
                response.getWriter().println(HttpHelper.getErrorAsJson("invalid_request", e.getMessage()).toJSONString());
            }
        } else {
            response.setStatus(400);
            response.getWriter().println(HttpHelper.getErrorAsJson("invalid_request", "configuration type was not given or is not supported").toJSONString());
        }
    }

    /**
     * This method retrieves requested configuration.
     *
     * @param request the request
     * @param response the response
     * @param configType the requested configuration type. Values must match a valid of ConfigurationTypes
     * @param selector depending on the type this value must identify a specific configuration. For example, client configurations this would require a valid client_id
     * @param token the access_token for this request/ It must have been granted for required scope and resource
     * @throws ServletException
     * @throws IOException
     */
    protected abstract void doGetProtected(HttpServletRequest request, HttpServletResponse response, ConfigurationTypes configType, String selector, AccessToken token) throws ServletException, IOException;
}
