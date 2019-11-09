package net.loginbuddy.test;

import net.loginbuddy.common.util.Jwt;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SOAPUIHelper extends HttpServlet {

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if ("/test/generate/jwt".equals(request.getServletPath())) {

            String issuer = request.getParameter("issuer");
            String audience = request.getParameter("audience");
            String subject = request.getParameter("subject");
            String nonce = request.getParameter("nonce");

            if (issuer == null || audience == null || subject == null || nonce == null) {
                response.setStatus(400);
                response.addHeader("loginbuddy-test-err", "missing test values - issuer,audience,subject,nonce");
                response.getWriter().println("{\"error\":\"invalid_request\", \"error_description\":\"missing test values - issuer,audience,subject,nonce\"}");
            } else {
                response.setContentType("application/json");
                try {
                    response.setStatus(200);
                    response.getWriter().println(String.format("{\"jwt\":\"%s\"}", getJwt(issuer, audience, subject, nonce)));
                } catch (Exception e) {
                    e.printStackTrace();
                    response.setStatus(400);
                    response.getWriter().println(String.format("{\"error\":\"invalid_request\", \"error_description\":\"%s\"}", e.getMessage()));
                }
            }
        }
    }

    private String getJwt(String issuer, String audience, String subject, String nonce) throws Exception {
        return new Jwt().createSignedJwtRs256(issuer, audience, 1, subject, nonce, true).getCompactSerialization();
    }
}