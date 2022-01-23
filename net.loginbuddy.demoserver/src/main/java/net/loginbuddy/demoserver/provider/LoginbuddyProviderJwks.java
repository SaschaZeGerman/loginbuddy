package net.loginbuddy.demoserver.provider;

import net.loginbuddy.common.util.Jwt;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LoginbuddyProviderJwks extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().println(Jwt.DEFAULT.getJwksForSigning().toJson());
    }
}
