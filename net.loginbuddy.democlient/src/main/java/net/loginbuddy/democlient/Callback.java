package net.loginbuddy.democlient;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.MsgResponse;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

public class Callback extends HttpServlet {

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response)
      throws ServletException, IOException {

    // No validation whatsoever. This is just for demo!

    String clientState = request.getParameter(Constants.STATE.getKey());
    String code = request.getParameter(Constants.CODE.getKey());

    if (code != null) {

      List<NameValuePair> formParameters = new ArrayList<NameValuePair>();
      formParameters.add(new BasicNameValuePair(Constants.CODE.getKey(), code));
      try {
        HttpPost req = new HttpPost(String.format("https://%s/exchange", System.getenv("HOSTNAME_LOGINBUDDY")));

        HttpClient httpClient = HttpClientBuilder.create().build();
        req.setEntity(new UrlEncodedFormEntity(formParameters));

        HttpResponse tokenResponse = httpClient.execute(req);
        MsgResponse msgResp = new MsgResponse(tokenResponse.getHeaders("Content-Type")[0].getValue(),
            EntityUtils.toString(tokenResponse.getEntity()), tokenResponse.getStatusLine().getStatusCode());

        Map<String, Object> sessionValues = (Map<String, Object>) LoginbuddyCache.getInstance().remove(clientState);
        sessionValues.put("msgResponse", msgResp);

        LoginbuddyCache.getInstance().put(clientState, sessionValues);

        response.sendRedirect(String.format("democlientCallback.jsp?state=%s", clientState));

      } catch (Exception e) {
        e.printStackTrace();
      }
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setHeader("Allow", "GET");
    response.sendError(405, "Method not allowed");
  }
}