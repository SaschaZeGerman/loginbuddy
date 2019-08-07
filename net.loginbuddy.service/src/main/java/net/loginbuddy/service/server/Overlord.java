package net.loginbuddy.service.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.MissingResourceException;
import java.util.Objects;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.service.config.ClientConfig;
import net.loginbuddy.service.config.LoginbuddyConfig;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Overlord extends HttpServlet {

  private static Logger LOGGER = Logger.getLogger(String.valueOf(Overlord.class));

  private JSONObject oidcConfig;
  ClientConfig clientConfig;
  String clientType;

  private String jsonArrayToString(JSONArray jsonArray) {
    StringBuilder sb = new StringBuilder();
    for(Object o : jsonArray) {
      sb.append((String)o).append(" ");
    }
    return sb.toString().substring(0,sb.toString().length()-1);
  }

  public String getIssuer() {
    String issuer = (String) oidcConfig.get(Constants.ISSUER.getKey());
    if(issuer == null || issuer.trim().length() == 0 ) {
      throw new IllegalArgumentException("issuer is required in discovery.json");
    }
    return (String) oidcConfig.get(Constants.ISSUER.getKey());
  }

  String getOpenIdConfigurationString() {
    return oidcConfig.toJSONString();
  }

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
      throw new IllegalArgumentException("discovery.json could not be found");
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

  String getResponseTypesSupported() {
    JSONArray supported = (JSONArray)oidcConfig.get(Discovery.RESPONSE_TYPES_SUPPORTED);
    if(supported == null || supported.size() == 0) {
      throw new IllegalArgumentException("supported response types are required in discovery.json");
    }
    return jsonArrayToString(supported);
  }

  String getGrantTypesSupported() {
    JSONArray supported = (JSONArray)oidcConfig.get(Discovery.GRANT_TYPES_SUPPORTED_OP);
    if(supported == null || supported.size() == 0) {
      return Constants.AUTHORIZATION_CODE.getKey();
    }
    return jsonArrayToString(supported);
  }

  String getTokenEndpointAuthMethodsSupported() {
    JSONArray supported = (JSONArray)oidcConfig.get(Discovery.TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED_OP);
    if(supported == null || supported.size() == 0) {
      return Constants.CLIENT_SECRET_BASIC.getKey() + "," + Constants.CLIENT_SECRET_POST.getKey();
    }
    return jsonArrayToString(supported);
  }

  String getScopesSupportedSupported() {
    JSONArray supported = (JSONArray)oidcConfig.get(Discovery.SCOPES_SUPPORTED_OP);
    if(supported == null || supported.size() == 0) {
      return Constants.OPENID_SCOPE.getKey();
    }
    return jsonArrayToString(supported);
  }
}