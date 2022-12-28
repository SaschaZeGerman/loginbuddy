package net.loginbuddy.service.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.config.discovery.DiscoveryUtil;

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
