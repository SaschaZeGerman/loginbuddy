package net.loginbuddy.service.client.handler;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.config.JwsAlgorithm;
import net.loginbuddy.common.util.*;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.loginbuddy.Clients;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.config.loginbuddy.Providers;
import net.loginbuddy.config.loginbuddy.common.DefaultTokenResponseHandler;
import net.loginbuddy.config.loginbuddy.common.OnBehalfOf;
import net.loginbuddy.config.loginbuddy.common.OnBehalfOfResult;
import net.loginbuddy.config.loginbuddy.handler.LoginbuddyHandler;
import net.loginbuddy.service.util.SessionContext;
import org.jose4j.lang.JoseException;
import org.json.simple.JSONObject;

import java.util.logging.Logger;

public class CallbackHandlerImplicit extends CallbackHandlerDefault {

    private static final Logger LOGGER = Logger.getLogger(CallbackHandlerImplicit.class.getName());

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

        LoginbuddyHandler loginbuddyHandler = (LoginbuddyHandler)sessionCtx.get(Constants.ISSUER_HANDLER.getKey());
        
        Providers providers = loginbuddyHandler.getProviders(provider);
        
        try {

            OnBehalfOfResult resigningResult = new DefaultTokenResponseHandler().getOnBehalfOfResult(
                    loginbuddyHandler.getJwksApi((String)sessionCtx.get(Constants.JWKS_URI.getKey()), true),
                    idTokenResult.getValue(),
                    providers.getIssuer(),
                    providers.getClientId(),
                    sessionCtx.getString(Constants.CLIENT_NONCE.getKey()),
                    sessionCtx.getString(Constants.CLIENT_CLIENT_ID.getKey())
            );

// ***************************************************************
// ** In this flow there is no complete token response (access_token, refresh_token), we'll create it manually for id_token
// ***************************************************************

            JSONObject tokenResponseObject = new JSONObject();
            tokenResponseObject.put(Constants.ID_TOKEN.getKey(), resigningResult.getIdToken());
            eb.setTokenResponse(tokenResponseObject);
            eb.setIdTokenPayload(resigningResult.getIdTokenPayload());
            eb.setNormalized(Normalizer.normalizeDetails(providers.getMappings(), eb.getEbAsJson(), null));

            returnAuthorizationCode(response, sessionCtx, eb);
        } catch (Exception e) {
            LOGGER.warning(String.format("No id_token was issued or it was invalid! Details: %s", e.getMessage()));
            throw e;
        }
    }
}