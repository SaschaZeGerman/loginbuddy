package net.loginbuddy.provider;

import net.loginbuddy.cache.LoginbuddyCache;
import net.loginbuddy.config.Constants;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.logging.Logger;

public class LoginbuddyProvider extends HttpServlet {
    private static final Logger LOGGER = Logger.getLogger(String.valueOf(LoginbuddyProvider.class));

    // (fake) handle authorization request
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) {

        try {
            String clientId = request.getParameter(Constants.CLIENT_ID.getKey());

            String code = String.valueOf(new Date().getTime());
            LoginbuddyCache.getInstance().getCache().put(code, clientId);

            String state = request.getParameter(Constants.STATE.getKey());
            state = URLEncoder.encode(state, "UTF-8");

            String redirectUri = request.getParameter(Constants.REDIRECT_URI.getKey());
            if (redirectUri.contains("?")) {
                redirectUri += "&state=" + state;
            } else {
                redirectUri += "?state=" + state;
            }
            redirectUri += "&code=" + URLEncoder.encode(code, "UTF-8");

            response.sendRedirect(redirectUri);
        } catch (Exception e) {
            LOGGER.severe("Fake provider failed to return auth code");
        }
    }

    // handle code exchange request
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String clientId = request.getParameter(Constants.CLIENT_ID.getKey());
        String code = request.getParameter(Constants.CODE.getKey());

        if (clientId != null && clientId.equals(LoginbuddyCache.getInstance().getCache().get(code)) ) {

            org.json.simple.JSONObject fakeProviderResponse = new JSONObject();
            fakeProviderResponse.put("access_token", "FAKEya29.GlyoBd7plzZ02kYia");
            fakeProviderResponse.put("token_type", "Bearer");
            fakeProviderResponse.put("expires_in", "3600");
            fakeProviderResponse.put("id_token", "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJoZWxsbyI6ImxvZ2luYnVkZHkifQ.z9r5WoqNycrF7YLOZTZpMarwUeTopU1UZzRAU7beFTc");
            fakeProviderResponse.put("id_token_secret", "loginbuddy");
            response.setStatus(200);
            response.setContentType("application/json");
            response.getWriter().println(fakeProviderResponse);
        } else {
            response.setStatus(400);
            response.setContentType("text/plain");
            response.getWriter().println("Shitty request!");
        }
    }
}