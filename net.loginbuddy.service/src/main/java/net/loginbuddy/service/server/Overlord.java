package net.loginbuddy.service.server;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.service.config.LoginbuddyConfig;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class Overlord extends HttpServlet {

  private static Logger LOGGER = Logger.getLogger(String.valueOf(Overlord.class));

  @Override
  public void init() throws ServletException {
    super.init();
    // initialize the configuration. If this fails, there is no reason to continue
    if (LoginbuddyConfig.getInstance().isConfigured()) {
      LOGGER.info("Loginbuddy successfully started!");
    } else {
      LOGGER.severe("Stopping loginbuddy since its configuration could not be loaded! Fix that first!");
      System.exit(1);
    }
  }

  void notYetImplemented(HttpServletResponse response) throws IOException {
    response.setStatus(418);
    response.setContentType("application/json");
    response.getWriter().write("{\"sorry\":\"not yet implemented\"}");
  }

  public static String createJsonErrorResponse(String value) {
    return createJsonErrorResponse(value, "");
  }

  public static String createJsonErrorResponse(String value, String toLogger) {
    if (toLogger != null && toLogger.trim().length() > 0) {
      LOGGER.warning(value.concat(": ").concat(toLogger));
    } else {
      LOGGER.warning(value);
    }
    return HttpHelper.getErrorAsJson("invalid_request", value).toJSONString();
  }

}