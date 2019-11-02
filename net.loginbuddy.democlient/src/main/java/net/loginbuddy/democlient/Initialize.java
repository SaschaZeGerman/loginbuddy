package net.loginbuddy.democlient;

import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.Sanetizer;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

public class Initialize extends HttpServlet {

  private static final Logger LOGGER = Logger.getLogger(String.valueOf(Initialize.class));

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doPost(request, response);
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    // No validation whatsoever. This is just for demo!

    String providerAddition = request.getParameter(Constants.PROVIDER_ADDITION.getKey()); // optional
    if (providerAddition.equals(ParameterValidatorResult.RESULT.VALID)) {
      LOGGER.warning(String.format("Invalid request! Unused field had values: '%s'", providerAddition));
      response.sendError(400, "Invalid request, please try again!");
      return;
    }

    String clientId = request.getParameter(Constants.CLIENT_ID.getKey());
    String clientResponseType = request.getParameter(Constants.RESPONSE_TYPE.getKey());
    String clientRedirectUri = Sanetizer.checkForUrlPattern(request.getParameter(Constants.REDIRECT_URI.getKey()), 256);
    String clientNonce = request.getParameter(Constants.NONCE.getKey());
    String clientState = request.getParameter(Constants.STATE.getKey());
    String clientScope = request.getParameter(Constants.SCOPE.getKey());
    String clientProvider = request.getParameter(Constants.PROVIDER.getKey()); // optional

    Map<String, Object> sessionValues = new HashMap<>();
    sessionValues.put(Constants.CLIENT_ID.getKey(), clientId);
    sessionValues.put(Constants.CLIENT_RESPONSE_TYPE.getKey(), clientResponseType);
    sessionValues.put(Constants.CLIENT_REDIRECT.getKey(), clientRedirectUri);
    sessionValues.put(Constants.NONCE.getKey(), clientNonce);
    sessionValues.put(Constants.CLIENT_STATE.getKey(), clientState);
    sessionValues.put(Constants.CLIENT_SCOPE.getKey(), clientScope);
    sessionValues.put(Constants.CLIENT_PROVIDER.getKey(), clientProvider);

    LoginbuddyCache.getInstance().put(clientState, sessionValues);

    response.sendRedirect(String.format("https://%s/authorize?client_id=%s&response_type=%s&redirect_uri=%s&nonce=%s&state=%s&scope=%s&provider=%s",
        System.getenv("HOSTNAME_LOGINBUDDY"),
        URLEncoder.encode(clientId, "UTF-8"),
        URLEncoder.encode(clientResponseType, "UTF-8"),
        URLEncoder.encode(clientRedirectUri, "UTF-8"),
        URLEncoder.encode(clientNonce, "UTF-8"),
        URLEncoder.encode(clientState, "UTF-8"),
        URLEncoder.encode(clientScope, "UTF-8"),
        URLEncoder.encode(clientProvider == null ? "" : clientProvider, "UTF-8")));
  }
}