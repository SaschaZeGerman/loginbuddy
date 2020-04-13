package net.loginbuddy.demoserver.provider;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * This endpoint simulates a registration endpoint
 * It is only used to demonstrate how Loginbuddy can leverage the registration feature at any OpenID Provider.
 * Therefore, we do not care about 'real' validations, we'll just return a valid response
 */
public class LoginbuddyProviderRegister extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");

        String givenContentType = request.getHeader("content-type");
        if (givenContentType == null || givenContentType.trim().length() == 0 || !givenContentType.startsWith("application/json")) {
            response.setStatus(400);
            response.getWriter().println(HttpHelper.getErrorAsJson("invalid_request", "unsupported content-type").toJSONString());
            return;
        }

        // check if a message was provided
        String messageBody = HttpHelper.readMessageBody(request.getReader());
        if (messageBody.length() == 0) {
            response.setStatus(400);
            response.getWriter().println(HttpHelper.getErrorAsJson("invalid_request", "missing registration message").toJSONString());
            return;
        }

        // check if a message was JSON
        try {
            new JSONParser().parse(messageBody);
        } catch (ParseException e) {
            response.setStatus(400);
            response.getWriter().println(HttpHelper.getErrorAsJson("invalid_request", "registration message must be valid JSON").toJSONString());
            return;
        }

        JSONObject obj = new JSONObject();
        obj.put(Constants.CLIENT_ID.getKey(), UUID.randomUUID().toString());
        obj.put(Constants.CLIENT_SECRET.getKey(), UUID.randomUUID().toString());
        obj.put(Constants.CLIENT_SECRET_EXPIRES_AT.getKey(), 0);

        response.setStatus(200);
        response.getWriter().println(obj.toJSONString());
    }
}
