package net.loginbuddy.oidcdr.server;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

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
            // TODO figure out 'what is healthy' for this container
            response.setStatus(200);
            response.getWriter().println("OK");
        }
    }
}
