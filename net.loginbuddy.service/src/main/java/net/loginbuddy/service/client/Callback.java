package net.loginbuddy.service.client;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ExchangeBean;
import net.loginbuddy.common.util.Jwt;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.service.config.LoginbuddyConfig;
import net.loginbuddy.service.config.discovery.DiscoveryUtil;
import net.loginbuddy.service.config.properties.PropertiesUtil;
import net.loginbuddy.service.util.SessionContext;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

public class Callback extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(Callback.class));

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

        SessionContext sessionCtx = (SessionContext) LoginbuddyCache.CACHE.remove(sessionIdResult.getValue());
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

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        // either a invalid request ... or an implicit flow ...
        if (request.getParameterMap().get("state") == null && request.getParameterMap().get("handled") == null ) {
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

                CallbackHandler handler = null;
                if (Constants.CODE.getKey().equals(sessionCtx.getString(Constants.ACTION_USED_RESPONSE_TYPE.getKey()))) {
                    handler = new CallbackHandlerCode();
                } else if (Constants.ID_TOKEN.getKey().equals(sessionCtx.getString(Constants.ACTION_USED_RESPONSE_TYPE.getKey()))) {
                    handler = new CallbackHandlerImplicit();
                }
                handler.handleCallback(request, response, sessionCtx, eb, provider);

            } catch (Exception e) {
                LOGGER.warning(String.format("authorization request failed! %s", e.getMessage()));
                e.printStackTrace();
                response.sendError(400, "authorization request failed!");
            }
        }
    }

    void returnAuthorizationCode(HttpServletResponse response, SessionContext sessionCtx, ExchangeBean eb) throws Exception {

// ***************************************************************
// ** Issue our own authorization_code and add details for the final client response
// ***************************************************************

        String authorizationCode = UUID.randomUUID().toString();
        if( !("".equals(sessionCtx.getString(Constants.CLIENT_SIGNED_RESPONSE_ALG.getKey()))) ){
            sessionCtx.put("eb", Jwt.DEFAULT.createSignedJwt(eb.toString(), sessionCtx.getString(Constants.CLIENT_SIGNED_RESPONSE_ALG.getKey())).getCompactSerialization());
        } else {
            sessionCtx.put("eb", eb.toString());
        }
        sessionCtx.put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_TOKEN_EXCHANGE.getKey());
        LoginbuddyCache.CACHE.put(authorizationCode, sessionCtx, PropertiesUtil.UTIL.getLongProperty("lifetime.oauth.authcode"));

        response.sendRedirect(getMessageForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), Constants.CODE.getKey(), authorizationCode));
    }
}