package net.loginbuddy.democlient;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
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
    String error = request.getParameter(Constants.ERROR.getKey());
    String error_description = request.getParameter(Constants.ERROR_DESCRIPTION.getKey());

    if (code != null) {

      List<NameValuePair> formParameters = new ArrayList<NameValuePair>();
      formParameters.add(new BasicNameValuePair(Constants.CODE.getKey(), code));
      try {
        HttpPost req = new HttpPost(String.format("https://%s/token", System.getenv("HOSTNAME_LOGINBUDDY")));

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
    } else if (error != null) {
      response.sendRedirect(String.format("democlientCallback.jsp?state=%s&error=%s&error_description=%s", clientState,
          URLEncoder.encode(error, "UTF-8"), URLEncoder.encode(error_description, "UTF-8")));
    }
  }

  @Override
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setHeader("Allow", "GET");
    response.sendError(405, "Method not allowed");
  }
}