package net.loginbuddy.selfissued.oidc;

import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import net.loginbuddy.selfissued.SelfIssuedMaster;
import org.json.simple.JSONObject;

public class Registration extends SelfIssuedMaster {

  private static Logger LOGGER = Logger.getLogger(String.valueOf(Registration.class));

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

    ParameterValidatorResult issuerResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.ISSUER.getKey()));
    ParameterValidatorResult discoveryUrlResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.DISCOVERY_URL.getKey()));
    ParameterValidatorResult redirectUriResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.REDIRECT_URI.getKey()));

    response.setStatus(400);
    response.setContentType("application/json");

    if (!issuerResult.getResult().equals(RESULT.VALID)) {
      LOGGER.warning("Missing or invalid or multiple issuer parameters given");
      response.getWriter()
          .write(HttpHelper.getErrorAsJson("invalid_request", "Missing or invalid or multiple issuer parameters given")
              .toJSONString());
      return;
    }
    if (!redirectUriResult.getResult().equals(RESULT.VALID)) {
      LOGGER.warning("Missing or invalid or multiple redirect_uri parameters given");
      response.getWriter()
          .write(HttpHelper.getErrorAsJson("invalid_request", "Missing or invalid or multiple redirect_uri parameters given")
              .toJSONString());
      return;
    }
    String discoveryUrl = null;
    if (discoveryUrlResult.getResult().equals(RESULT.VALID)) {
      discoveryUrl = discoveryUrlResult.getValue();
    } else {
      LOGGER.info(
          "Missing or invalid or multiple discovery_url parameters were given. Adding /.well-known/openid-configuration of the issuer to generate one");
      discoveryUrl = issuerResult.getValue() + "/.well-known/openid-configuration";
    }

    JSONObject registration = HttpHelper
        .retrieveAndRegister(discoveryUrl, redirectUriResult.getValue(), true, true);

    if (registration.get("error") != null) {
      response.setStatus(400);
    } else {
      response.setStatus(200);
    }
    response.getWriter().write(registration.toJSONString());
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
    doPost(request, response);
  }
}