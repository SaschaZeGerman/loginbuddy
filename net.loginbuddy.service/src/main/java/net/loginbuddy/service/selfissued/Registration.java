package net.loginbuddy.service.selfissued;

import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import net.loginbuddy.service.config.LoginbuddyConfig;
import org.json.simple.JSONObject;

public class Registration extends HttpServlet {

  private static Logger LOGGER = Logger.getLogger(String.valueOf(Registration.class));

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

    ParameterValidatorResult issuerResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.ISSUER.getKey()));
    ParameterValidatorResult discoveryUrlResult = ParameterValidator
        .getSingleValue(request.getParameterValues(Constants.DISCOVERY_URL.getKey()));

    response.setStatus(400);
    response.setContentType("application/json");

    if (!issuerResult.getResult().equals(RESULT.VALID)) {
      LOGGER.warning("Missing or invalid or multiple issuer parameters given");
      response.getWriter()
          .write(HttpHelper.getErrorAsJson("invalid_request", "Missing or invalid or multiple issuer parameters given").toJSONString());
      return;
    }
    String discoveryUrl = null;
    if (!discoveryUrlResult.getResult().equals(RESULT.VALID)) {
      discoveryUrl = discoveryUrlResult.getValue();
    } else {
      LOGGER.info("Missing or invalid or multiple discovery_url parameters were given. Adding /.well-known/openid-configuration of the issuer to generate one");
      discoveryUrl = issuerResult.getValue() + "/.well-known/openid-configuration";
    }

    JSONObject registration = HttpHelper.retrieveAndRegister(issuerResult.getValue(), discoveryUrl, LoginbuddyConfig.getInstance().getDiscoveryUtil().getRedirectUri());
    if(registration.get("error") != null) {
      response.setStatus(400);
    } else {
      response.setStatus(200);
    }
    response.getWriter().write(registration.toJSONString());
  }
}