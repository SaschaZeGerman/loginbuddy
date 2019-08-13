package net.loginbuddy.service.server;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletResponse;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.service.config.LoginbuddyConfig;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

public class Overlord extends HttpServlet {

  private static Logger LOGGER = Logger.getLogger(String.valueOf(Overlord.class));

  @Override
  public void init() throws ServletException {
    super.init();
    // initialize the configuration. If this fails, there is no reason to continue
    if(LoginbuddyConfig.getInstance().isConfigured()) {
      LOGGER.info("Loginbuddy successfully started!");
    } else {
      LOGGER.severe("Stopping loginbuddy since its configuration could not be loaded! Fix that first!");
      System.exit(1);
    }
  }

  protected MsgResponse getAPI(String accessToken, String targetApi) {
    try {
      HttpGet req = new HttpGet(targetApi);
      HttpClient httpClient = HttpClientBuilder.create().build();
      req.setHeader(Constants.AUTHORIZATION.getKey(), Constants.BEARER.getKey() + accessToken);

      HttpResponse response = httpClient.execute(req);
      return new MsgResponse(response.getHeaders("Content-Type")[0].getValue(),
          EntityUtils.toString(response.getEntity()), response.getStatusLine().getStatusCode());
    } catch (Exception e) {
      LOGGER.warning("Call to targetApi failed!"); // TODO return error
      e.printStackTrace();
      return null;
    }
  }

  protected MsgResponse getAPI(String targetApi) {
    try {
      HttpGet req = new HttpGet(targetApi);
      HttpClient httpClient = HttpClientBuilder.create().build();

      HttpResponse response = httpClient.execute(req);
      return new MsgResponse(response.getHeaders("Content-Type")[0].getValue(),
          EntityUtils.toString(response.getEntity()), response.getStatusLine().getStatusCode());
    } catch (Exception e) {
      LOGGER.warning("The API response could not be retrieved. Given URL: '" + targetApi + "'"); // TODO return error
      e.printStackTrace();
      return null;
    }
  }

  protected String getErrorForRedirect(String redirectUri, String error, String errorDescription)
      throws UnsupportedEncodingException {
    if ("".equals(errorDescription)) {
      errorDescription = "An error without any description, sorry";
    }
    error = URLEncoder.encode(error, "UTF-8");
    errorDescription = URLEncoder.encode(errorDescription, "UTF-8");

    return redirectUri.concat("error=").concat(error).concat("&error_description=").concat(errorDescription);
  }

  protected String getMessageForRedirect(String redirectUri, String urlSafeKey, String value)
      throws UnsupportedEncodingException {
    return redirectUri.concat(urlSafeKey).concat("=").concat(URLEncoder.encode(value, "UTF-8"));
  }

  void notYetImplemented(HttpServletResponse response) throws IOException {
    response.setStatus(418);
    response.setContentType("application/json");
    response.getWriter().write("{\"sorry\":\"not yet implemented\"}");
  }
}