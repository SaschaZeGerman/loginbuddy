package net.loginbuddy.oauth.server;

import net.loginbuddy.cache.LoginbuddyCache;
import net.loginbuddy.config.Constants;
import net.loginbuddy.config.ProviderConfig;
import net.loginbuddy.config.LoginbuddyConfig;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;

public class Authorize extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(Authorize.class));

    // initiate authorization flow with provider
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        String state = request.getParameter(Constants.STATE.getKey());
        if (state == null || state.trim().length() == 0) {
            throw new IllegalStateException("Missing session, cannot initiate the authorization flow!");
        }

        Map<String, Object> sessionValues = (Map<String, Object>) LoginbuddyCache.getInstance().getCache().get(state);
        if (sessionValues == null) {
            throw new IllegalStateException("The current session is invalid or it has expired!");
        }

        String provider = (String)sessionValues.get("clientProvider");
        if (provider == null) {
            provider = request.getParameter(Constants.PROVIDER.getKey());
        }

        ProviderConfig providerConfig = null;
        try {
            providerConfig = LoginbuddyConfig.getInstance().getConfigUtil().getProviderConfigByProvider(provider);
        } catch(Exception e) {
            throw new IllegalArgumentException("error=invalid_configuration&error_description=invalid provider configuration");
        }
        if (providerConfig == null) {
            throw new IllegalStateException("error=invalid_request&error_description=invalid_provider");
        }


        sessionValues.put("clientProvider", provider);

        StringBuilder authorizeUrl = new StringBuilder();

        // check for 'openid configuration' first
        if (providerConfig.getOpenidConfigurationUri() != null) {
            JSONObject openIdConfig = getOpenIDConfiguration(providerConfig.getOpenidConfigurationUri());
            if (openIdConfig != null) {
                providerConfig.setAuthorizationEndpoint(openIdConfig.get("authorization_endpoint").toString());
                sessionValues.put(Constants.TOKEN_ENDPOINT.getKey(), openIdConfig.get("token_endpoint").toString());
                sessionValues.put(Constants.USERINFO_ENDPOINT.getKey(), openIdConfig.get("userinfo_endpoint").toString());
            }
        }

        // RFC 7636, generate PKCE values
        String code_verifier = UUID.randomUUID().toString().replace("-", "");
        code_verifier = new String(Base64.getEncoder().encode(code_verifier.getBytes())).replace("=", "");
        sessionValues.put("code_verifier", code_verifier);

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            // this should never ever happen!
            throw new IOException(e.getCause());
        }

        byte[] encodedhash = digest.digest(code_verifier.getBytes(StandardCharsets.UTF_8));
        String code_challenge = new String(Base64.getUrlEncoder().encode(encodedhash)).replace("=", "");

        authorizeUrl.append(providerConfig.getAuthorizationEndpoint())
            .append("?").append(Constants.CLIENT_ID.getKey())
            .append("=").append(providerConfig.getClientId())
            .append("&").append(Constants.RESPONSE_TYPE.getKey())
            .append("=").append(providerConfig.getResponseType())
            .append("&").append(Constants.SCOPE.getKey())
            .append("=").append(URLEncoder.encode(Constants.OPENID_SCOPE.getKey(), "UTF-8"))
            .append("&").append(Constants.NONCE.getKey())
            .append("=").append(sessionValues.get(Constants.NONCE.getKey()))
            .append("&").append(Constants.REDIRECT_URI.getKey())
            .append("=").append(URLEncoder.encode(providerConfig.getRedirectUri(), "utf-8"))
            .append("&").append("code_challenge=").append(code_challenge)
            .append("&").append("code_challenge_method=S256")
            .append("&").append(Constants.STATE.getKey())
            .append("=").append(sessionValues.get(Constants.STATE.getKey()));

        LoginbuddyCache.getInstance().getCache().put((String)sessionValues.get(Constants.STATE.getKey()), sessionValues);

        response.sendRedirect(authorizeUrl.toString());

    }

    /**
     * Retrieve the openid-configuration of the given provider
     *
     * @param openidConfigUrl
     */
    private JSONObject getOpenIDConfiguration(String openidConfigUrl) {

        HttpClient httpClient = HttpClientBuilder.create().build();

        try {
            HttpGet httpGet = new HttpGet(openidConfigUrl);
            HttpResponse response = httpClient.execute(httpGet);

            if (response.getStatusLine().getStatusCode() == 200) {
                // assuming it is always JSON, not checking for content-type here
                return (JSONObject) new JSONParser().parse(EntityUtils.toString(response.getEntity()));
            } else throw new Exception(EntityUtils.toString(response.getEntity()));

        } catch (Exception e) {
            e.printStackTrace();
        }

        // should never get here
        return null;
    }
}