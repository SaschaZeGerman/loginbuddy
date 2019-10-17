package net.loginbuddy.service.sidecar;

import net.loginbuddy.common.api.HttpHelper;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;

public class SidecarMaster extends HttpServlet {

  static void checkClientConnection(HttpServletRequest request) throws IllegalAccessException {
    // port 444 should only be available via loginbuddy-sidecar
    if (! ("loginbuddy-sidecar".equals(request.getServerName()) && request.getLocalPort() == 444) ) {
      throw new IllegalAccessException(HttpHelper.getErrorAsJson("invalid_client", "connection not accepted").toJSONString());
    }
  }
}