package net.loginbuddy.service.sidecar;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.loginbuddy.service.server.Overlord;

public class SidecarMaster extends Overlord {

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

    response.setStatus(400);
    response.setContentType("application/json");

    // port 444 should only be available via loginbuddy-sidecar
    if (! ("loginbuddy-sidecar".equals(request.getServerName()) && request.getLocalPort() == 444) ) {
      response.getWriter().write(getErrorAsJson("invalid_client", "connection not accepted").toJSONString());
    }
  }
}