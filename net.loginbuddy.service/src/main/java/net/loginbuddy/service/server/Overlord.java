package net.loginbuddy.service.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Objects;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.service.config.ClientConfig;
import net.loginbuddy.service.config.LoginbuddyConfig;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Overlord extends HttpServlet {

  private static Logger LOGGER = Logger.getLogger(String.valueOf(Overlord.class));

  protected JSONObject oidcConfig;
  protected ClientConfig clientConfig;
  protected String clientType;

  @Override
  public void init() throws ServletException {
    super.init();
    try {
      BufferedReader f = new BufferedReader(new FileReader(new File(
          Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("discovery.json"))
              .toURI())));
      StringBuilder fContent = new StringBuilder();
      String next = null;
      while ((next = f.readLine()) != null) {
        fContent.append(next);
      }
      oidcConfig = (JSONObject) new JSONParser().parse(fContent.toString());
    } catch (Exception e) {
      LOGGER.severe("No discovery document found!");
      e.printStackTrace();
    }
  }

  /**
   * If this method is not called we'll have NullPointerExceptions in derived classes
   * @param clientId
   * @return
   */
  boolean loadClientConfig(String clientId) {
    try {
      clientConfig = LoginbuddyConfig.getInstance().getConfigUtil().getClientConfigByClientId(clientId);
      clientType = clientConfig.getClientType();
      if (clientConfig == null) {
        LOGGER.warning("The given client_id is unknown or invalid");
        return false;
      }
    } catch (Exception e) {
      // should never occur
      LOGGER.severe("The system has not been configured yet!");
      return false;
    }
    return true;
  }

  String getGrantTypesSupported() {
    String supported = (String)oidcConfig.get(Discovery.GRANT_TYPES_SUPPORTED_OP);
    if(supported == null || supported.trim().length() == 0) {
      return Constants.AUTHORIZATION_CODE.getKey();
    }
    return supported;
  }

  String getTokenEndpointAuthMethodsSupported() {
    String supported = (String)oidcConfig.get(Discovery.TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED_OP);
    if(supported == null || supported.trim().length() == 0) {
      return Constants.CLIENT_SECRET_BASIC.getKey() + "," + Constants.CLIENT_SECRET_POST.getKey();
    }
    return supported;
  }

}
