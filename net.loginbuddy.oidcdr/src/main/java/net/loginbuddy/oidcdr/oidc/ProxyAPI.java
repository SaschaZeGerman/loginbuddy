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
import java.util.Iterator;
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
        for (String param : request.getParameterMap().keySet()) {
            if (!"target".equalsIgnoreCase(param)) {
                params.add(new BasicNameValuePair(param, request.getParameterMap().get(param)[0]));
            }
        }

        PostRequest postRequest = PostRequest.create(targetEndpointResult.getValue()).setFormParametersPayload(params).setAcceptType("application/json");
        copyHeaderToRequest(request, postRequest, "dpop", Constants.AUTHORIZATION.getKey());

        LOGGER.info(String.format("Connecting to: %s", targetEndpointResult.getValue()));

        MsgResponse msg = HttpHelper.postMessage(postRequest.build(), "application/json");
        copyHeaderToResponse(msg, response, "dpop-nonce","www-authenticate", "status", "content-type");

        // TODO: validate msg as good as possible
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

        GetRequest getRequest = GetRequest.create(targetEndpointResult.getValue()).setAcceptType("application/json");
        copyHeaderToRequest(request, getRequest, "dpop", Constants.AUTHORIZATION.getKey());

        LOGGER.info(String.format("Connecting to: %s", targetEndpointResult.getValue()));

        MsgResponse msg = HttpHelper.getAPI(getRequest.build());
        copyHeaderToResponse(msg, response, "dpop-nonce","www-authenticate", "status", "content-type");

        // TODO: validate msg as good as possible
        response.getWriter().write(msg.getMsg());
    }

    /**
     * Copy headers to the response that were received by a previous request
     *
     * @param from Contains headers to copy from
     * @param to Response to which headers will be added
     * @param headerNames List of header names to copy from msg to response
     */
    private void copyHeaderToResponse(MsgResponse from, HttpServletResponse to, String... headerNames) {
        for (String header : from.getHeaders().keySet()) {
            for(String headerName : headerNames) {
                if (headerName.equalsIgnoreCase(header)) {
                    to.addHeader(header, from.getHeader(header));
                }
            }
        }
    }

    private static void copyHeaderToRequest(HttpServletRequest from, GetRequest to, String... headerNames) {
        Iterator<String> iter = from.getHeaderNames().asIterator();
        String nextHeader;
        while(iter.hasNext()) {
            nextHeader = iter.next();
            for(String headerName : headerNames) {
                if(headerName.equalsIgnoreCase(nextHeader)) {
                    to.addHeader(headerName, from.getHeader(nextHeader));
                }
            }
        }
    }

    private static void copyHeaderToRequest(HttpServletRequest from, PostRequest to, String... headerNames) {
        Iterator<String> iter = from.getHeaderNames().asIterator();
        String nextHeader;
        while(iter.hasNext()) {
            nextHeader = iter.next();
            for(String headerName : headerNames) {
                if(headerName.equalsIgnoreCase(nextHeader)) {
                    to.addHeader(headerName, from.getHeader(nextHeader));
                }
            }
        }
    }
}