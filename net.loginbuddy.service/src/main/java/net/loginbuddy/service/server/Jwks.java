package net.loginbuddy.service.server;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.util.Jwt;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Jwks extends Overlord {

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.setContentType("application/json");
        try {
            resp.setStatus(200);
            resp.getWriter().println(Jwt.getJwksForSigning().toJson());
        } catch (Exception e) {
            resp.setStatus(500);
            resp.getWriter().print(HttpHelper.getErrorAsJson("server_error", "jwks could not be created"));
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }
}