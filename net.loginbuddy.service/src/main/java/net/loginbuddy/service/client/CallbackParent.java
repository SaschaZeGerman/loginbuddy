package net.loginbuddy.service.client;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.service.util.SessionContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

public class CallbackParent extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(CallbackParent.class));

    protected String getMessageForRedirect(String redirectUri, String urlSafeKey, String value) {
        return redirectUri.concat(urlSafeKey).concat("=").concat(HttpHelper.urlEncode(value));
    }

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
            response.sendError(400, "Missing or invalid state parameter");
            return null;
        }

        SessionContext sessionCtx = (SessionContext) LoginbuddyCache.getInstance().remove(sessionIdResult.getValue());
        if (sessionCtx == null || !sessionIdResult.getValue().equals(sessionCtx.getId())) {
            LOGGER.warning("The current session is invalid or it has expired! Given: '" + sessionIdResult.getValue() + "'");
            response.sendError(400, "The current session is invalid or it has expired!");
            return null;
        }

// ***************************************************************
// ** End the fun here if the provider send back an error
// ***************************************************************

        if (errorResult.getValue() != null) {
            response.sendRedirect(HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), errorResult.getValue(), errorDescriptionResult.getValue()));
            return null;
        }

// ***************************************************************
// ** Check if we expected this call
// ***************************************************************

        if (!Constants.ACTION_CALLBACK.getKey().equals(sessionCtx.getString(Constants.ACTION_EXPECTED.getKey()))) {
            LOGGER.warning(
                    "The current action was not expected! Given: '" + sessionCtx.getString(Constants.ACTION_EXPECTED.getKey())
                            + "', expected: '" + Constants.ACTION_CALLBACK.getKey() + "'");
            response.sendRedirect(HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "invalid_session", "the request was not expected"));
            return null;
        }

        return sessionCtx;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

}
