package net.loginbuddy.config.loginbuddy.common;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.json.simple.parser.ParseException;

import java.io.IOException;

public interface GrantTypeHandler {

    void handleGrantType(HttpServletRequest request, HttpServletResponse response, String... extras) throws IOException;

}
