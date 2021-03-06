package net.loginbuddy.service.server;

import net.loginbuddy.config.discovery.DiscoveryUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Discovery extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setStatus(200);
    response.setContentType("application/json");
    response.getWriter().write(DiscoveryUtil.UTIL.getOpenIdConfigurationAsJsonString());
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }
}
