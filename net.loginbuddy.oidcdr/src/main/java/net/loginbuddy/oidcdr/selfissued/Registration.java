package net.loginbuddy.oidcdr.selfissued;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class Registration extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        ParameterValidatorResult clientIdResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.CLIENT_ID.getKey()));

        response.setContentType("application/json");
        if(clientIdResult.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
            response.setStatus(200);
            response.getWriter().println(String.format("{\"client_id\": \"%s\",\"client_secret_expires_at\": 0, \"client_secret\":\"\"}", clientIdResult.getValue()));
        } else {
            response.setStatus(400);
            response.getWriter().println(HttpHelper.getErrorAsJson("invalid_request", "invalid client_id for self-issued registration"));
        }
    }
}