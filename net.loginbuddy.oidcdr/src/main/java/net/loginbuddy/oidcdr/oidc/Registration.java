package net.loginbuddy.oidcdr.oidc;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.logging.Logger;

public class Registration extends HttpServlet {

    private static Logger LOGGER = Logger.getLogger(Registration.class.getName());

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

        MsgResponse msg = HttpHelper.getAPI(discoveryUrl);
        try {
            JSONObject oidcConfig = (JSONObject) new JSONParser().parse(msg.getMsg());
            if(msg.getStatus() > 200) {
                LOGGER.warning(String.format("Retrieving OIDC discovery failed. error: %s, error_description: %s", oidcConfig.get("error"), oidcConfig.get("error_description")));
                response.setStatus(400);
                response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", (String)oidcConfig.get("error_description")).toJSONString());
                return;
            }
            String registerUrl = (String) oidcConfig.get(Constants.REGISTRATION_ENDPOINT.getKey());
            if (registerUrl == null || registerUrl.trim().length() == 0 || registerUrl.startsWith("http:")) {
                LOGGER.warning(String.format("The OP does not support dynamic registration. OP: %s", oidcConfig.get(Constants.ISSUER.getKey())));
                response.setStatus(400);
                response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "The OP does not support dynamic registration").toJSONString());
            } else {
                MsgResponse registration = HttpHelper.register(registerUrl, redirectUriResult.getValue());
                if (registration.getStatus() == 200) {
                    JSONObject providerConfig = (JSONObject) new JSONParser().parse(registration.getMsg());
                    oidcConfig.put(Constants.CLIENT_ID.getKey(), providerConfig.get(Constants.CLIENT_ID.getKey()));
                    oidcConfig.put(Constants.CLIENT_SECRET.getKey(), providerConfig.get(Constants.CLIENT_SECRET.getKey()));
                    oidcConfig.put(Constants.PROVIDER.getKey(), oidcConfig.get(Constants.ISSUER.getKey()));
                    oidcConfig.put(Constants.REDIRECT_URI.getKey(), redirectUriResult.getValue());
                    oidcConfig.remove("openid_discovery_uri");

                    LOGGER.info(String.format("Successfully registered at: %s", registerUrl));

                    response.setStatus(200);
                    response.getWriter().write(oidcConfig.toJSONString());
                } else {
                    LOGGER.warning(String.format("The registration at this URL failed: %s, error: %s", registerUrl, registration.getMsg()));
                    response.setStatus(400);
                    response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "The registration at this OP failed").toJSONString());
                }
            }
        } catch (ParseException e) {
            LOGGER.warning(e.getMessage());
            response.setStatus(400);
            LOGGER.warning(String.format("The OIDC document could not be parsed as JSON: %s\n", msg.getMsg()));
            response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "The OIDC document could not be parsed as JSON").toJSONString());
        }

    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        doPost(request, response);
    }
}