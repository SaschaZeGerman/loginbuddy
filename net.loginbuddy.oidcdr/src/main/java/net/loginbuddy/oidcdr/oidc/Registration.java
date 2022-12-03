package net.loginbuddy.oidcdr.oidc;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.logging.Logger;

public class Registration extends HttpServlet {

    private static Logger LOGGER = Logger.getLogger(String.valueOf(Registration.class));

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            OIDCDRMaster.checkClientConnection(request);
        } catch (IllegalAccessException e) {
            LOGGER.warning(e.getMessage());
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write(e.getMessage());
            return;
        }

        ParameterValidatorResult issuerResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.ISSUER.getKey()));
        ParameterValidatorResult discoveryUrlResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.DISCOVERY_URL.getKey()));
        ParameterValidatorResult redirectUriResult = ParameterValidator
                .getSingleValue(request.getParameterValues(Constants.REDIRECT_URI.getKey()));

// ***************************************************************
// ** Whatever happens, we'll return JSON
// ***************************************************************

        response.setStatus(400);
        response.setContentType("application/json");

// ***************************************************************
// ** Check the given parameters
// ***************************************************************

        if (!issuerResult.getResult().equals(RESULT.VALID)) {
            LOGGER.warning("Missing or invalid or multiple issuer parameters given");
            response.getWriter()
                    .write(HttpHelper.getErrorAsJson("invalid_request", "Missing or invalid or multiple issuer parameters given")
                            .toJSONString());
            return;
        }

        if (!redirectUriResult.getResult().equals(RESULT.VALID)) {
            LOGGER.warning("Missing or invalid or multiple redirect_uri parameters given");
            response.getWriter()
                    .write(HttpHelper.getErrorAsJson("invalid_request", "Missing or invalid or multiple redirect_uri parameters given")
                            .toJSONString());
            return;
        }

// ***************************************************************
// ** If non was given we'll create the default: {issuer}/.well-known/openid-configuration
// ***************************************************************

        String discoveryUrl = null;
        if (discoveryUrlResult.getResult().equals(RESULT.VALID)) {
            discoveryUrl = discoveryUrlResult.getValue();
        } else {
            LOGGER.info("Missing or invalid or multiple discovery_url parameters were given. Adding /.well-known/openid-configuration of the issuer to generate one");
            if (Constants.ISSUER_SELFISSUED.getKey().equals(issuerResult.getValue())) {
                // for self-issued we host the API ourselves to keep things simple, before and after provider registration
                LOGGER.info("Simulating registration for self-issued provider");
                discoveryUrl = String.format("https://loginbuddy-oidcdr:445/selfissued/openid-configuration?client_id=%s", HttpHelper.urlEncode(redirectUriResult.getValue()));
            } else {
                discoveryUrl = issuerResult.getValue() + "/.well-known/openid-configuration";
            }
        }

// ***************************************************************
// ** Register at the given provider
// ***************************************************************

        JSONObject registration = HttpHelper.retrieveAndRegister(discoveryUrl, redirectUriResult.getValue(), true, true);

        if (registration.get("error") != null) {
            response.setStatus(400);
        } else {
            response.setStatus(200);
        }
        response.getWriter().write(registration.toJSONString());
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPost(request, response);
    }
}