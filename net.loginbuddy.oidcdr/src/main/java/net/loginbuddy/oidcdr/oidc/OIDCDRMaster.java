package net.loginbuddy.oidcdr.oidc;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import net.loginbuddy.common.api.HttpHelper;

public class OIDCDRMaster extends HttpServlet {

  static void checkClientConnection(HttpServletRequest request) throws IllegalAccessException {

    // port 445 should only be available via loginbuddy-oidcdr
    if (! ("loginbuddy-oidcdr".equals(request.getServerName()) && request.getLocalPort() == 445) ) {
      throw new IllegalAccessException(HttpHelper.getErrorAsJson("invalid_client", "connection not accepted").toJSONString());
    }

  }
}