package net.loginbuddy.service.server;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface GrantTypeHandler {

    void handleGrantType(HttpServletRequest request, HttpServletResponse response) throws IOException;

}
