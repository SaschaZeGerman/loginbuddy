package net.loginbuddy.service.server;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.Objects;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Overlord extends HttpServlet {

  private static Logger LOGGER = Logger.getLogger(String.valueOf(Overlord.class));

  protected JSONObject oidcConfig;

  @Override
  public void init() throws ServletException {
    super.init();
    try {
      BufferedReader f = new BufferedReader(new FileReader(new File(
          Objects.requireNonNull(Thread.currentThread().getContextClassLoader().getResource("discovery.json")).toURI())));
      StringBuilder fContent = new StringBuilder();
      String next = null;
      while( (next = f.readLine()) != null) {
        fContent.append(next);
      }
      oidcConfig = (JSONObject) new JSONParser().parse(fContent.toString());
    } catch (Exception e) {
      LOGGER.severe("No discovery document found!");
      e.printStackTrace();
    }
  }

}
