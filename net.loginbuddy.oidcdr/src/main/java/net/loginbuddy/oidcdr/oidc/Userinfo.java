package net.loginbuddy.oidcdr.oidc;

import jakarta.servlet.http.HttpServlet;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.logging.Logger;

public class Userinfo extends HttpServlet {

    private static Logger LOGGER = Logger.getLogger(String.valueOf(Userinfo.class));

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            OIDCDRMaster.checkClientConnection(request);
        } catch (IllegalAccessException e) {
            LOGGER.warning(e.getMessage());
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write(e.getMessage());
            return;
        }

        ParameterValidatorResult targetEndpointResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.TARGET_PROVIDER.getKey()));

        // TODO: better validation!
        String accessToken = request.getHeader(Constants.AUTHORIZATION.getKey());
        accessToken = accessToken != null ? accessToken.split(" ")[1] : "";

        MsgResponse msg = HttpHelper.getAPI(accessToken, targetEndpointResult.getValue());

        // TODO: validate msg as good as possible
        response.setStatus(msg.getStatus());
        response.setContentType(msg.getContentType());
        response.getWriter().write(msg.getMsg());

    }
}