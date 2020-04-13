package net.loginbuddy.demoserver.provider;

import net.loginbuddy.common.config.Constants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LoginbuddyProviderConfiguration extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(200);
        response.setContentType("application/json");

        String hostname_loginbuddy_demoserver = System.getenv("HOSTNAME_LOGINBUDDY_DEMOSERVER");

        JSONObject obj = new JSONObject();
        obj.put(Constants.ISSUER.getKey(), String.format("https://%s", hostname_loginbuddy_demoserver));
        obj.put(Constants.AUTHORIZATION_ENDPOINT.getKey(), String.format("https://%s/authorize", hostname_loginbuddy_demoserver));
        obj.put(Constants.TOKEN_ENDPOINT.getKey(), String.format("https://%s/token", hostname_loginbuddy_demoserver));
        obj.put(Constants.USERINFO_ENDPOINT.getKey(), String.format("https://%s/userinfo", hostname_loginbuddy_demoserver));
        obj.put(Constants.JWKS_URI.getKey(), String.format("https://%s/jwks", hostname_loginbuddy_demoserver));
        obj.put(Constants.REGISTRATION_ENDPOINT.getKey(), String.format("https://%s/register", hostname_loginbuddy_demoserver));

        JSONArray values = new JSONArray();
        values.add("openid");
        values.add("email");
        values.add("profile");
        obj.put(Constants.SCOPES_SUPPORTED.getKey(), values);

        values = new JSONArray();
        values.add("code");
        obj.put(Constants.RESPONSE_TYPES_SUPPORTED.getKey(), values);

        values = new JSONArray();
        values.add("authorization_code");
        obj.put(Constants.GRANT_TYPES_SUPPORTED.getKey(), values);

        values = new JSONArray();
        values.add("pairwise");
        values.add("public");
        obj.put(Constants.SUBJECT_TYPES_SUPPORTED.getKey(), values);

        values = new JSONArray();
        values.add("RS256");
        obj.put(Constants.ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED.getKey(), values);

        response.getWriter().print(obj.toJSONString());
    }
}
