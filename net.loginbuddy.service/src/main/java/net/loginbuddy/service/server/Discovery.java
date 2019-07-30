package net.loginbuddy.service.server;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class Discovery extends Overlord {

  // list all the key that we reference. Add _OP for optional ones so that it becomes more obvious when using them
  static final String TOKEN_ENDPOINT_AUTH_METHODS_SUPPORTED_OP = "token_endpoint_auth_methods_supported";
  static final String GRANT_TYPES_SUPPORTED_OP = "grant_types_supported";
  static final String RESPONSE_TYPES_SUPPORTED = "response_types_supported";

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setStatus(200);
    response.setContentType("application/json");
    response.getWriter().write(oidcConfig.toJSONString());
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    doGet(request, response);
  }
}
