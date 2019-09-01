package net.loginbuddy.selfissued;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import net.loginbuddy.common.api.HttpHelper;

public class SelfIssuedMaster extends HttpServlet {

  void checkClientConnection(HttpServletRequest request) throws IllegalAccessException {
    // port 444 should only be available via loginbuddy-selfissued
    if (! ("loginbuddy-selfissued".equals(request.getServerName()) && request.getLocalPort() == 445) ) {
      throw new IllegalAccessException(HttpHelper.getErrorAsJson("invalid_client", "connection not accepted").toJSONString());
    }
  }

}
