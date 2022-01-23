package net.loginbuddy.oidcdr;

import net.loginbuddy.common.api.HttpHelper;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;

public class OIDCDRMaster extends HttpServlet {

  void checkClientConnection(HttpServletRequest request) throws IllegalAccessException {
    // port 445 should only be available via loginbuddy-oidcdr
    if (! ("loginbuddy-oidcdr".equals(request.getServerName()) && request.getLocalPort() == 445) ) {
      throw new IllegalAccessException(HttpHelper.getErrorAsJson("invalid_client", "connection not accepted").toJSONString());
    }
  }

}
