package net.loginbuddy.service.server;

import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Discovery extends Overlord {

  private static final Logger LOGGER = Logger.getLogger(String.valueOf(Discovery.class));

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setStatus(200);
    response.setContentType("application/json");
    response.getWriter().write(oidcConfig.toJSONString());
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }
}
