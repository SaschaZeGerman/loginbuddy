package net.loginbuddy.service.client;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.config.JwsAlgorithm;
import net.loginbuddy.common.util.Jwt;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.service.server.SidecarMaster;
import net.loginbuddy.service.util.SessionContext;
import org.jose4j.lang.JoseException;

import java.io.IOException;
import java.util.logging.Logger;

public class CallbackSidecarParent extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(CallbackSidecarParent.class));

    protected SessionContext checkForSessionAndErrors(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            SidecarMaster.checkClientConnection(request);
        } catch (IllegalAccessException e) {
            LOGGER.warning(e.getMessage());
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write(e.getMessage());
            return null;
        }

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
            response.getWriter().write(HttpHelper.getErrorAsJson("invalid_response", "Missing or invalid state parameter returned from provider").toJSONString());
            return null;
        }

        SessionContext sessionCtx = (SessionContext) LoginbuddyCache.CACHE.remove(sessionIdResult.getValue());
        if (sessionCtx == null || !sessionIdResult.getValue().equals(sessionCtx.getId())) {
            LOGGER.warning("The current session is invalid or it has expired! Given: '" + sessionIdResult.getValue() + "'");
            response.getWriter().write(HttpHelper.getErrorAsJson("invalid_session", "the current session is invalid or it has expired").toJSONString());

            sessionCtx = new SessionContext();
            sessionCtx.put(Constants.ERROR.getKey(), "invalid_session");
            sessionCtx.put(Constants.ERROR_DESCRIPTION.getKey(), "the current session is invalid or it has expired");
            return sessionCtx;
        }

// ***************************************************************
// ** End the fun here if the provider send back an error
// ***************************************************************

        if (errorResult.getValue() != null) {
            sessionCtx.put(Constants.ERROR.getKey(), errorResult.getValue());
            sessionCtx.put(Constants.ERROR_DESCRIPTION.getKey(), errorDescriptionResult.getValue());
            return sessionCtx;
        }

// ***************************************************************
// ** Check if we expected this call
// ***************************************************************

        if (!Constants.ACTION_CALLBACK.getKey().equals(sessionCtx.getString(Constants.ACTION_EXPECTED.getKey()))) {
            LOGGER.warning(
                    "The current action was not expected! Given: '" + sessionCtx.getString(Constants.ACTION_EXPECTED.getKey())
                            + "', expected: '" + Constants.ACTION_CALLBACK.getKey() + "'");
            sessionCtx.put(Constants.ERROR.getKey(), errorResult.getValue());
            sessionCtx.put(Constants.ERROR_DESCRIPTION.getKey(), errorDescriptionResult.getValue());
            return sessionCtx;
        }

        return sessionCtx;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        doGet(request, response);
    }

    protected String getSignedResponse(String payload, String alg) throws JoseException {
        return Jwt.DEFAULT.createSignedJwt(payload, JwsAlgorithm.findMatchingAlg(alg)).getCompactSerialization();
    }

}
