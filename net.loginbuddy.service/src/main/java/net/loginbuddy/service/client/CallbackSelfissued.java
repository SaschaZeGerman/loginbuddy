package net.loginbuddy.service.client;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.cache.LoginbuddyCache;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ExchangeBean;
import net.loginbuddy.common.util.Jwt;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import net.loginbuddy.service.config.LoginbuddyConfig;
import net.loginbuddy.service.config.ProviderConfig;
import net.loginbuddy.service.util.SessionContext;
import org.json.simple.JSONObject;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.UUID;
import java.util.logging.Logger;

public class CallbackSelfissued extends CallbackParent {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(CallbackSelfissued.class));

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

        if (request.getParameterMap().get("state") == null) {
            // TODO: prevent endless back and forth
            StringBuilder sb = new StringBuilder();
            sb.append("<html><header><script>");
            sb.append("if(window.location.search) { window.location = window.location.replace('#', '&');\n" +
                    "} else {window.location = window.location.replace('#', '&');}");
            sb.append("</script></header><body></body></html>");
            response.setStatus(200);
            response.getWriter().println(sb.toString());
        } else {

            try {

                SessionContext sessionCtx = checkForSessionAndErrors(request, response);
                if (sessionCtx == null) {
                    return;
                }

// ***************************************************************
// ** If we did not get a valid id_token parameter we are done
// ***************************************************************

                ParameterValidatorResult idTokenResult = ParameterValidator
                        .getSingleValue(request.getParameterValues(Constants.ID_TOKEN.getKey()));
                if (!idTokenResult.getResult().equals(RESULT.VALID)) {
                    LOGGER.warning("Missing id_token parameter returned from provider!");
                    response.sendRedirect(HttpHelper.getErrorForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "invalid_session", "missing or invalid id_token parameter"));
                    return;
                }

// ***************************************************************
// ** Find the chosen provider of this session and get a token. Also start preparing the response for the client
// ***************************************************************

                String provider = sessionCtx.getString(Constants.CLIENT_PROVIDER.getKey());

                ExchangeBean eb = new ExchangeBean();
                eb.setIss(LoginbuddyConfig.getInstance().getDiscoveryUtil().getIssuer());
                eb.setIat(new Date().getTime() / 1000);
                eb.setAud(sessionCtx.getString(Constants.CLIENT_CLIENT_ID.getKey()));
                eb.setNonce(sessionCtx.getString(Constants.CLIENT_NONCE.getKey()));
                eb.setProvider(provider);

                ProviderConfig providerConfig = LoginbuddyConfig.getInstance().getConfigUtil().getProviderConfigByProvider(provider);

                JSONObject idTokenPayload = null;
                try {
                    idTokenPayload = new Jwt().validateJwt(idTokenResult.getValue(), null, providerConfig.getIssuer(), providerConfig.getClientId(), sessionCtx.getString(Constants.CLIENT_NONCE.getKey()));
                    eb.setIdTokenPayload(idTokenPayload);
                } catch (Exception e) {
                    LOGGER.warning(String.format("No id_token is invalid! Details: %s", e.getMessage()));
                    throw e;
                }

// ***************************************************************
// ** In this flow there is no token response, we'll create ia manually
// ***************************************************************

                JSONObject tokenResponseObject = new JSONObject();
                tokenResponseObject.put("id_token", idTokenResult.getValue());
                eb.setTokenResponse(tokenResponseObject);

// ***************************************************************
// ** Issue our own authorization_code and add details for the final client response
// ***************************************************************

                String authorizationCode = UUID.randomUUID().toString();
                sessionCtx.put("eb", eb.toString());
                sessionCtx.put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_TOKEN_EXCHANGE.getKey());
                LoginbuddyCache.getInstance().put(authorizationCode, sessionCtx, LoginbuddyConfig.getInstance().getPropertiesUtil().getLongProperty("lifetime.oauth.authcode"));

                response.sendRedirect(getMessageForRedirect(sessionCtx.getString(Constants.CLIENT_REDIRECT_VALID.getKey()), "code", authorizationCode));

            } catch (Exception e) {
                LOGGER.warning(String.format("authorization request failed! %s", e.getMessage()));
                e.printStackTrace();
                response.sendError(400, "authorization request failed!");
            }
        }
    }
}