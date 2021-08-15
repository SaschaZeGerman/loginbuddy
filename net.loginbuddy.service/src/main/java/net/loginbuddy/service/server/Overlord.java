package net.loginbuddy.service.server;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.loginbuddy.LoginbuddyLoader;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.config.properties.PropertiesUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

public class Overlord extends HttpServlet {

  private static Logger LOGGER = Logger.getLogger(String.valueOf(Overlord.class));

  @Override
  public void init() throws ServletException {
    super.init();
    // initialize the configuration. If this fails, there is no reason to continue

    if (LoginbuddyUtil.UTIL.isConfigured() && PropertiesUtil.UTIL.isConfigured() && DiscoveryUtil.UTIL.isConfigured()) {
      LOGGER.info("Loginbuddy successfully started!");
      String customLoader = PropertiesUtil.UTIL.getStringProperty("config.loginbuddy.loader.default");
      if(customLoader != null) {
        try {
          Class cls = Class.forName(customLoader);
          LoginbuddyLoader myLoader = (LoginbuddyLoader) cls.getDeclaredConstructors()[0].newInstance();
          LoginbuddyUtil.UTIL.setLoader(myLoader);
          LOGGER.info(String.format("Custom LoginbuddyLoader was successfully initiated! Class: '%s'", customLoader));
        } catch (InvocationTargetException e) {
          if (e.getTargetException() != null) {
            LOGGER.warning(String.format("Custom LoginbuddyLoader created an error: '%s'", e.getTargetException().getMessage()));
          }
        } catch (Exception e) {
          LOGGER.warning(String.format("Custom LoginbuddyLoader could not be initiated! Error: '%s'", e.getMessage()));
        }
      } else {
        LOGGER.info("No custom LoginbuddyLoader was configured! Using the default loader");
      }
    } else {
      LOGGER.severe("Stopping Loginbuddy since its configuration could not be loaded! Fix that first!");
      System.exit(0);
    }
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