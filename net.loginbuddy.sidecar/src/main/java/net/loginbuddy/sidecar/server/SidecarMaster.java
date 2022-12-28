package net.loginbuddy.sidecar.server;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import net.loginbuddy.common.api.HttpHelper;

public class SidecarMaster extends HttpServlet {

  static void checkClientConnection(HttpServletRequest request) throws IllegalAccessException {
    // port 444 (or 8044 for http) should only be available via loginbuddy-sidecar
    if (! ("loginbuddy-sidecar".equals(request.getServerName()) && (request.getLocalPort() == 444 || request.getLocalPort() == 8044 ))) {
      throw new IllegalAccessException(HttpHelper.getErrorAsJson("invalid_client", "connection not accepted").toJSONString());
    }
  }
}