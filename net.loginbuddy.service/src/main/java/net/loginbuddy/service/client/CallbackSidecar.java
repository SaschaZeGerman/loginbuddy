package net.loginbuddy.service.client;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.service.client.handler.CallbackHandler;
import net.loginbuddy.service.client.handler.CallbackHandlerSidecar;
import net.loginbuddy.service.server.Sidecar;
import net.loginbuddy.service.util.SessionContext;

import java.io.IOException;
import java.util.logging.Logger;

public class CallbackSidecar extends Callback {

    private static final Logger LOGGER = Logger.getLogger(CallbackSidecar.class.getName());

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            Sidecar.checkClientConnection(request);
        } catch (IllegalAccessException e) {
            LOGGER.warning(e.getMessage());
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write(e.getMessage());
            return;
        }

        super.doGet(request, response);
    }

    @Override
    protected boolean handleEarlyResponse(HttpServletResponse response, SessionContext sessionCtx) throws IOException {
        if (sessionCtx.getString(Constants.CLIENT_STATE.getKey()) != null) {
            response.setHeader("X-State", sessionCtx.getString(Constants.CLIENT_STATE.getKey()));
        }
        response.setContentType("application/json");

        if (sessionCtx.get("error") != null) {
            response.getWriter().write(
                    HttpHelper.getErrorAsJson(
                            sessionCtx.get(Constants.ERROR.getKey(), String.class),
                            sessionCtx.get(Constants.ERROR_DESCRIPTION.getKey(), String.class)
                    ).toJSONString());
            return true;
        }
        return false;
    }

    @Override
    protected SessionContext sendError(int httpStatus, String error, String errorMsg, HttpServletResponse response, SessionContext sessionCtx) throws IOException {
        response.getWriter().write(HttpHelper.getErrorAsJson(error, errorMsg).toJSONString());
        sessionCtx.put(Constants.ERROR.getKey(), error);
        sessionCtx.put(Constants.ERROR_DESCRIPTION.getKey(), errorMsg);
        return sessionCtx;
    }

    @Override
    protected SessionContext endFunHere(String error, String errorDescription, SessionContext sessionCtx, HttpServletResponse response) throws IOException {
        sessionCtx.put(Constants.ERROR.getKey(), error);
        sessionCtx.put(Constants.ERROR_DESCRIPTION.getKey(), errorDescription);
        return sessionCtx;
    }

    @Override
    protected CallbackHandler getCallbackHandler(SessionContext sessionCtx) {
        return new CallbackHandlerSidecar();
    }
}
