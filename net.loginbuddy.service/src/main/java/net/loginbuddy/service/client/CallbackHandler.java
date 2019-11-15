package net.loginbuddy.service.client;

import net.loginbuddy.common.util.ExchangeBean;
import net.loginbuddy.service.util.SessionContext;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface CallbackHandler {
    void handleCallback(HttpServletRequest request, HttpServletResponse response, SessionContext sessionCtx, ExchangeBean eb, String provider) throws Exception;
}
