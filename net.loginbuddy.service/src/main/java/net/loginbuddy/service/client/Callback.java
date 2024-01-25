package net.loginbuddy.service.client;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.*;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.loginbuddy.common.OnBehalfOf;
import net.loginbuddy.config.loginbuddy.common.OnBehalfOfResult;
import net.loginbuddy.service.client.handler.CallbackHandler;
import net.loginbuddy.service.client.handler.CallbackHandlerCode;
import net.loginbuddy.service.client.handler.CallbackHandlerImplicit;
import net.loginbuddy.service.util.SessionContext;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

public class Callback extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(Callback.class.getName());

    protected SessionContext checkForSessionAndErrors(HttpServletRequest request, HttpServletResponse response) throws IOException {

        ParameterValidatorResult sessionIdResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.STATE.getKey()));
        ParameterValidatorResult errorResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.ERROR.getKey()));
        ParameterValidatorResult errorDescriptionResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.ERROR_DESCRIPTION.getKey()), "");

// ***************************************************************
// ** Check for the current session
// ***************************************************************

        if (!sessionIdResult.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
            LOGGER.warning("Missing or invalid state parameter returned from provider!");
            sendError(400, "invalid_response", "Missing or invalid state parameter returned from provider", response);
            return null;
        }

        SessionContext sessionCtx = (SessionContext) LoginbuddyCache.CACHE.remove(sessionIdResult.getValue());
        if (sessionCtx == null || !sessionIdResult.getValue().equals(sessionCtx.getId())) {
            LOGGER.warning("The current session is invalid or it has expired! Given: '" + sessionIdResult.getValue() + "'");
            return sendError(400, "invalid_session", "The current session is invalid or it has expired!", response, null);
        }

// ***************************************************************
// ** End the fun here if the provider send back an error
// ***************************************************************

        if (errorResult.getValue() != null) {
            return endFunHere(errorResult.getValue(), errorDescriptionResult.getValue(), sessionCtx, response);
        }

// ***************************************************************
// ** Check if we expected this call
// ***************************************************************

        if (!Constants.ACTION_CALLBACK.getKey().equals(sessionCtx.getString(Constants.ACTION_EXPECTED.getKey()))) {
            LOGGER.warning(
                    "The current action was not expected! Given: '" + sessionCtx.getString(Constants.ACTION_EXPECTED.getKey())
                            + "', expected: '" + Constants.ACTION_CALLBACK.getKey() + "'");
            return endFunHere("invalid_session", "the request was not expected", null, response);
        }

        return sessionCtx;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // either an invalid request ... or an implicit flow ...
        if (request.getParameterMap().get("state") == null && request.getParameterMap().get("handled") == null) {
            // TODO: prevent endless back and forth (handled enough?)
            StringBuilder sb = new StringBuilder();
            sb.append("<html><header><script>");
            sb.append("if (location.href.indexOf('#') >= 0) {\n" +
                    "if (location.search) {window.location = location.href.replace('#', '&handled=true&');\n" +
                    "} else {window.location = location.href.replace('#', '?handled=true&');}\n" +
                    "} else {if (location.search) {window.location = location.href + '&handled=true';\n" +
                    "} else {window.location = location.href + '?handled=true';}}");
            sb.append("</script></header><body></body></html>");
            response.setStatus(200);
            response.setContentType("text/html");
            response.getWriter().println(sb.toString());
        } else {

            try {

                SessionContext sessionCtx = checkForSessionAndErrors(request, response);
                if (sessionCtx == null) {
                    return;
                }

                // This is needed to merge the sidecar implementation into this container
                if (handleEarlyResponse(response, sessionCtx)) return;

// ***************************************************************
// ** Find the chosen provider of this session and get a token. Also start preparing the response for the client
// ***************************************************************

                String provider = sessionCtx.getString(Constants.CLIENT_PROVIDER.getKey());

                ExchangeBean eb = new ExchangeBean();
                eb.setIss(DiscoveryUtil.UTIL.getIssuer());
                eb.setIat(new Date().getTime() / 1000);
                eb.setAud(sessionCtx.getString(Constants.CLIENT_CLIENT_ID.getKey()));
                eb.setNonce(sessionCtx.getString(Constants.CLIENT_NONCE.getKey()));
                eb.setProvider(provider);

                CallbackHandler handler = getCallbackHandler(sessionCtx);
                handler.handleCallback(request, response, sessionCtx, eb, provider);

            } catch (Exception e) {
                LOGGER.warning(String.format("authorization request failed! %s", e.getMessage()));
                response.sendError(400, "authorization request failed!");
            }
        }
    }

    protected boolean handleEarlyResponse(HttpServletResponse response, SessionContext sessionCtx) throws IOException {
        return false;  // do not do anything here, overwritten in CallbackSidecar
    }

    protected CallbackHandler getCallbackHandler(SessionContext sessionCtx) {
        if (Constants.CODE.getKey().equals(sessionCtx.getString(Constants.ACTION_USED_RESPONSE_TYPE.getKey()))) {
            return new CallbackHandlerCode();
        } else if (Constants.ID_TOKEN.getKey().equals(sessionCtx.getString(Constants.ACTION_USED_RESPONSE_TYPE.getKey()))) {
            return new CallbackHandlerImplicit();
        }
        return null; // it can only be one of the two above for now. Still leaving the checks to not forget 'later'
    }

    protected SessionContext sendError(int httpStatus, String error, String errorMsg, HttpServletResponse response) throws IOException {
        response.sendError(httpStatus, errorMsg);
        return null;
    }
    protected SessionContext sendError(int httpStatus, String error, String errorMsg, HttpServletResponse response, SessionContext sessionCtx) throws IOException {
        response.sendError(httpStatus, errorMsg);
        return null;
    }

    protected SessionContext endFunHere(String error, String errorDescription, SessionContext sessionCtx, HttpServletResponse response) throws IOException {
        response.sendRedirect(
                HttpHelper.getErrorForRedirect(
                        sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()),
                        error,
                        errorDescription));
        return null;
    }
}