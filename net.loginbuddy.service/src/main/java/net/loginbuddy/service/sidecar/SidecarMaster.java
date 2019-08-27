package net.loginbuddy.service.sidecar;

import javax.servlet.http.HttpServletRequest;
import net.loginbuddy.service.server.Overlord;

public class SidecarMaster extends Overlord {

  void checkClientConnection(HttpServletRequest request) throws IllegalAccessException {
    // port 444 should only be available via loginbuddy-sidecar
    if (! ("loginbuddy-sidecar".equals(request.getServerName()) && request.getLocalPort() == 444) ) {
      throw new IllegalAccessException(getErrorAsJson("invalid_client", "connection not accepted").toJSONString());
    }
  }
}