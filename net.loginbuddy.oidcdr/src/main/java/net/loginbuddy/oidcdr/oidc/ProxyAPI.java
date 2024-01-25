package net.loginbuddy.oidcdr.oidc;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.GetRequest;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.api.PostRequest;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class ProxyAPI extends HttpServlet {

    private static Logger LOGGER = Logger.getLogger(ProxyAPI.class.getName());

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

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

        List<NameValuePair> params = new ArrayList<>();
        for(String param : request.getParameterMap().keySet()) {
            if(!"target".equalsIgnoreCase(param)) {
                params.add(new BasicNameValuePair(param, request.getParameterMap().get(param)[0]));
            }
        }

        String dpopHeader = request.getHeader("dpop");
        String authorizationHeader = request.getHeader(Constants.AUTHORIZATION.getKey());

        PostRequest req = PostRequest.create(targetEndpointResult.getValue()).setFormParametersPayload(params).setAcceptType("application/json");
        if(dpopHeader != null && dpopHeader.trim().length() >0 ) {
            req.addHeader("dpop", dpopHeader);
        }
        if(authorizationHeader != null && authorizationHeader.trim().length() >0 ) {
            req.addHeader(Constants.AUTHORIZATION.getKey(), authorizationHeader);
        }

        LOGGER.info(String.format("Connecting to: %s", targetEndpointResult.getValue()));

        MsgResponse msg = HttpHelper.postMessage(req.build(), "application/json");

        // TODO: validate msg as good as possible
        response.setStatus(msg.getStatus());
        response.setContentType(msg.getContentType());
        response.getWriter().write(msg.getMsg());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

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

        String dpopHeader = request.getHeader("dpop");
        String authorizationHeader = request.getHeader(Constants.AUTHORIZATION.getKey());

        GetRequest req = GetRequest.create(targetEndpointResult.getValue()).setAcceptType("application/json");
        if(dpopHeader != null && dpopHeader.trim().length() >0 ) {
            req.addHeader("dpop", dpopHeader);
        }
        if(authorizationHeader != null && authorizationHeader.trim().length() >0 ) {
            req.addHeader(Constants.AUTHORIZATION.getKey(), authorizationHeader);
        }

        LOGGER.info(String.format("Connecting to: %s", targetEndpointResult.getValue()));

        MsgResponse msg = HttpHelper.getAPI(req.build());

        // TODO: validate msg as good as possible
        response.setStatus(msg.getStatus());
        response.setContentType(msg.getContentType());
        response.getWriter().write(msg.getMsg());
    }
}