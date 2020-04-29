package net.loginbuddy.demoserver.provider;

import net.loginbuddy.common.util.Jwt;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class LoginbuddyProviderJwks extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.getWriter().println(Jwt.DEFAULT.getJwksForSigning().toJson());
    }
}
