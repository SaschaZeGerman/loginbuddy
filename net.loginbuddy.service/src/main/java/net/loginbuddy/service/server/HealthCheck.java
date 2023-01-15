package net.loginbuddy.service.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.config.properties.PropertiesUtil;

import java.io.IOException;

public class HealthCheck extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        String path = request.getServletPath();
        response.setContentType("text/plain");
        if("/status/liveness".equalsIgnoreCase(path)) {
            response.setStatus(200);
            response.getWriter().println("OK");
        } else if("/status/health".equalsIgnoreCase(path)) {
            // TODO somehow include the message that Loginbuddy successfully started
            boolean result = LoginbuddyUtil.UTIL.isConfigured() && PropertiesUtil.UTIL.isConfigured() && DiscoveryUtil.UTIL.isConfigured();
            response.setStatus(result ? 200 : 500);
            response.getWriter().println(result ? "OK" : "Unhealthy");
        }
    }
}
