package net.loginbuddy.democlient;

import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.Sanetizer;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class Initialize extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(String.valueOf(Initialize.class));

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doPost(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    // just checking if this unused hidden field had a value which would be suspicious

    ParameterValidatorResult providerAddition = ParameterValidator
            .getSingleValue(request.getParameterValues(Constants.PROVIDER_ADDITION.getKey()));
    if (providerAddition.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
      LOGGER.warning(String.format("Invalid request! Unused field had values: '%s'", providerAddition));
      response.sendError(400, "Invalid request, please try again!");
      return;
    }

    // Simple validation. This is just for demo!

    ParameterValidatorResult clientIdResult = ParameterValidator
            .getSingleValue(request.getParameterValues(Constants.CLIENT_ID.getKey()));
    String clientId = Sanetizer.sanetize(clientIdResult.getValue(), 64);

    ParameterValidatorResult clientProviderResult = ParameterValidator
            .getSingleValue(request.getParameterValues(Constants.PROVIDER.getKey()));
    String clientProvider = Sanetizer.sanetize(clientProviderResult.getValue(), 64);

    ParameterValidatorResult clientObfuscateTokenResult = ParameterValidator
            .getSingleValue(request.getParameterValues(Constants.OBFUSCATE_TOKEN.getKey()));
    boolean clientObfuscateToken = Boolean.parseBoolean(Sanetizer.sanetize(clientObfuscateTokenResult.getValue(), 5));

    // Create a session

    Map<String, Object> sessionValues = new HashMap<>();
    sessionValues.put(Constants.CLIENT_ID.getKey(), clientIdResult.getValue());

    String clientResponseType = "code";
    sessionValues.put(Constants.CLIENT_RESPONSE_TYPE.getKey(), clientResponseType);

    String clientRedirectUri = String.format("https://%s/callback", System.getenv("HOSTNAME_LOGINBUDDY_DEMOCLIENT"));
    sessionValues.put(Constants.CLIENT_REDIRECT.getKey(), clientRedirectUri);

    String clientNonce = UUID.randomUUID().toString();
    sessionValues.put(Constants.NONCE.getKey(), clientNonce);

    String clientState = UUID.randomUUID().toString();
    sessionValues.put(Constants.CLIENT_STATE.getKey(), clientState);

    String clientScope = "openid email profile";
    sessionValues.put(Constants.CLIENT_SCOPE.getKey(), clientScope);

    sessionValues.put(Constants.CLIENT_PROVIDER.getKey(), clientProvider);

    LoginbuddyCache.CACHE.put(clientState, sessionValues);

    // Create authorization URL

    response.sendRedirect(String.format("https://%s/authorize?client_id=%s&response_type=%s&redirect_uri=%s&nonce=%s&state=%s&scope=%s&provider=%s&obfuscate_token=%b",
        System.getenv("HOSTNAME_LOGINBUDDY"),
        URLEncoder.encode(clientId, "UTF-8"),
        URLEncoder.encode(clientResponseType, "UTF-8"),
        URLEncoder.encode(clientRedirectUri, "UTF-8"),
        URLEncoder.encode(clientNonce, "UTF-8"),
        URLEncoder.encode(clientState, "UTF-8"),
        URLEncoder.encode(clientScope, "UTF-8"),
        URLEncoder.encode(clientProvider, "UTF-8"),
        clientObfuscateToken));
  }
}