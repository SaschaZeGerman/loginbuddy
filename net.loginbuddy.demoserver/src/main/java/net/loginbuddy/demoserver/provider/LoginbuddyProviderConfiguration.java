package net.loginbuddy.demoserver.provider;

import net.loginbuddy.common.config.Constants;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LoginbuddyProviderConfiguration extends LoginbuddyProviderCommon {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        response.setStatus(200);
        response.setContentType("application/json");

        JSONObject obj = new JSONObject();
        obj.put(Constants.ISSUER.getKey(), String.format("%s%s%s", scheme, hostname, port));
        obj.put(Constants.AUTHORIZATION_ENDPOINT.getKey(), String.format("%s%s%s/authorize", scheme, hostname, port));
        obj.put(Constants.TOKEN_ENDPOINT.getKey(), String.format("%s%s%s/token", scheme, hostname, port));
        obj.put(Constants.USERINFO_ENDPOINT.getKey(), String.format("%s%s%s/userinfo", scheme, hostname, port));
        obj.put(Constants.JWKS_URI.getKey(), String.format("%s%s%s/jwks", scheme, hostname, port));
        obj.put(Constants.REGISTRATION_ENDPOINT.getKey(), String.format("%s%s%s/register", scheme, hostname, port));

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
        values.add("ES256");
        obj.put(Constants.ID_TOKEN_SIGNING_ALG_VALUES_SUPPORTED.getKey(), values);

        response.getWriter().print(obj.toJSONString());
    }
}
