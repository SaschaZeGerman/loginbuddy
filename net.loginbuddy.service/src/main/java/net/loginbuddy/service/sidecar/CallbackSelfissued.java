package net.loginbuddy.service.sidecar;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ExchangeBean;
import net.loginbuddy.common.util.Jwt;
import net.loginbuddy.common.util.ParameterValidator;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.service.config.loginbuddy.LoginbuddyConfig;
import net.loginbuddy.service.config.discovery.DiscoveryUtil;
import net.loginbuddy.service.config.loginbuddy.Providers;
import net.loginbuddy.service.util.SessionContext;
import org.json.simple.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Date;
import java.util.logging.Logger;

public class CallbackSelfissued extends CallbackParent {

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

            Providers providers = LoginbuddyConfig.CONFIG.getLoginbuddyUtil().getProviderConfigByProvider(provider);

            JSONObject idTokenPayload = null;
            try {
                idTokenPayload = Jwt.DEFAULT.validateIdToken(idTokenResult.getValue(), null, providers.getIssuer(), providers.getClientId(), sessionCtx.getString(Constants.CLIENT_NONCE.getKey()));
                eb.setIdTokenPayload(idTokenPayload);
            } catch (Exception e) {
                LOGGER.warning(String.format("No id_token was issued or it was invalid! Details: %s", e.getMessage()));
                throw e;
            }

// ***************************************************************
// ** In this flow there is no token response, we'll create it manually
// ***************************************************************

            JSONObject tokenResponseObject = new JSONObject();
            tokenResponseObject.put(Constants.ID_TOKEN.getKey(), idTokenResult.getValue());
            eb.setTokenResponse(tokenResponseObject);

// ***************************************************************
// ** Return the details to the client
// ***************************************************************

            response.setStatus(200);
            if( !("".equals(sessionCtx.getString(Constants.CLIENT_SIGNED_RESPONSE_ALG.getKey()))) ){
                response.setContentType("application/jwt");
                response.getWriter().write(getSignedResponse(eb.toString(), sessionCtx.getString(Constants.CLIENT_SIGNED_RESPONSE_ALG.getKey())));
            } else {
                response.getWriter().write(eb.toString());
            }

        } catch (Exception e) {
            LOGGER.warning("authorization request failed!");
            e.printStackTrace();
            response.getWriter().write(HttpHelper.getErrorAsJson("invalid_request", "authorization request failed").toJSONString());
        }
    }
}