package net.loginbuddy.sidecar.server;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ExchangeBean;
import net.loginbuddy.common.util.Jwt;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.config.loginbuddy.Providers;
import net.loginbuddy.config.loginbuddy.common.OnBehalfOf;
import net.loginbuddy.config.loginbuddy.common.OnBehalfOfResult;
import net.loginbuddy.sidecar.util.SessionContext;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

public class CallbackSelfissued extends CallbackParent {

    // TODO remove since it is not in use

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(CallbackSelfissued.class));

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {
            SidecarMaster.checkClientConnection(request);
        } catch (IllegalAccessException e) {
            LOGGER.warning(e.getMessage());
            response.setStatus(400);
            response.setContentType("application/json");
            response.getWriter().write(e.getMessage());
            return;
        }

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
            if (!idTokenResult.getResult().equals(ParameterValidatorResult.RESULT.VALID)) {
                LOGGER.warning("Missing id_token parameter returned from provider!");
                response.getWriter().write(HttpHelper.getErrorAsJson("invalid_session", "missing or invalid id_token parameter").toJSONString());
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

            Providers providers = LoginbuddyUtil.UTIL.getProviderConfigByProvider(provider);

            // ***************************************************************
            // ** In this flow there is no token response, we'll create it manually
            // ***************************************************************
            JSONObject tokenResponseObject = new JSONObject();
            String id_token = idTokenResult.getValue();
            try {
                JSONObject idTokenPayload = Jwt.DEFAULT.validateIdToken(id_token, null, providers.getIssuer(), providers.getClientId(), sessionCtx.getString(Constants.CLIENT_NONCE.getKey()));
                // check if the client is configured to get an id_token re-signed by Loginbuddy, on behalf of the original issuer
                // check if the client is configured to get an id_token re-signed by Loginbuddy, on behalf of the original issuer
                OnBehalfOfResult resigningResult = OnBehalfOf.signOnBehalfOf(
                        sessionCtx.getString(Constants.CLIENT_CLIENT_ID.getKey()),
                        sessionCtx.getString(Constants.CLIENT_NONCE.getKey()),
                        Constants.ID_TOKEN.getKey(),
                        idTokenPayload,
                        id_token
                );
                tokenResponseObject.put(Constants.ID_TOKEN.getKey(), resigningResult.getIdToken());
                eb.setIdTokenPayload(resigningResult.getIdTokenPayload());
            } catch (Exception e) {
                LOGGER.warning(String.format("No id_token was issued or it was invalid! Details: %s", e.getMessage()));
                throw e;
            }
            eb.setTokenResponse(tokenResponseObject);
// ***************************************************************
// ** Return the details to the client
// ***************************************************************

            response.setStatus(200);
            if (!("".equals(sessionCtx.getString(Constants.CLIENT_SIGNED_RESPONSE_ALG.getKey())))) {
                response.setContentType("application/jwt");
                response.getWriter().write(getSignedResponse(eb.toString(), sessionCtx.getString(Constants.CLIENT_SIGNED_RESPONSE_ALG.getKey())));
            } else {
                response.getWriter().write(eb.toString());
            }

        } catch (Exception e) {
            LOGGER.warning(String.format("authorization request failed! %s", e.getMessage()));
            response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "authorization request failed").toJSONString());
        }
    }
}