package net.loginbuddy.service.client;

import net.loginbuddy.common.api.HttpHelper;
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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.logging.Logger;

public class CallbackHandlerImplicit extends Callback implements CallbackHandler {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(CallbackHandlerImplicit.class));

    @Override
    public void handleCallback(HttpServletRequest request, HttpServletResponse response, SessionContext sessionCtx, ExchangeBean eb, String provider) throws Exception {

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

        ProviderConfig providerConfig = LoginbuddyConfig.getInstance().getConfigUtil().getProviderConfigByProvider(provider);

        JSONObject idTokenPayload = null;
        try {
            idTokenPayload = new Jwt().validateJwt(idTokenResult.getValue(), null, providerConfig.getIssuer(), providerConfig.getClientId(), sessionCtx.getString(Constants.CLIENT_NONCE.getKey()));
            eb.setIdTokenPayload(idTokenPayload);
        } catch (Exception e) {
            LOGGER.warning(String.format("No id_token was issued or it was invalid! Details: %s", e.getMessage()));
            throw e;
        }

// ***************************************************************
// ** In this flow there is no token response, we'll create ia manually
// ***************************************************************

        JSONObject tokenResponseObject = new JSONObject();
        tokenResponseObject.put(Constants.ID_TOKEN.getKey(), idTokenResult.getValue());
        eb.setTokenResponse(tokenResponseObject);

        returnAuthorizationCode(response, sessionCtx, eb);
    }
}