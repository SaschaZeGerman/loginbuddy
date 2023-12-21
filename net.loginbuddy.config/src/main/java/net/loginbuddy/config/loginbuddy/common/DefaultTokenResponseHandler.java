package net.loginbuddy.config.loginbuddy.common;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.Jwt;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.config.loginbuddy.Providers;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.logging.Logger;

public class DefaultTokenResponseHandler implements TokenResponseHandler {

    private static final Logger LOGGER = Logger.getLogger(DefaultTokenResponseHandler.class.getName());

    @Override
    public JSONObject handleCodeTokenExchangeResponse(MsgResponse tokenResponse, boolean encryptToken, Providers providers, String clientId, String clientNonce, String jwksUri) throws ParseException {
        JSONObject tokenResponseObject = ((JSONObject) new JSONParser().parse(tokenResponse.getMsg()));
        LOGGER.fine(tokenResponseObject.toJSONString());
        String access_token = tokenResponseObject.get(Constants.ACCESS_TOKEN.getKey()).toString();
        tokenResponseObject.put("provider_access_token", access_token);
        if (encryptToken) {
            tokenResponseObject.put(Constants.ACCESS_TOKEN.getKey(), LoginbuddyUtil.UTIL.encrypt(String.format("%s:%s", providers.getProvider(), access_token)));
            if (tokenResponseObject.get(Constants.REFRESH_TOKEN.getKey()) != null) {
                tokenResponseObject.put(Constants.REFRESH_TOKEN.getKey(), LoginbuddyUtil.UTIL.encrypt(String.format("%s:%s:%s", providers.getProvider(), clientId, tokenResponseObject.get(Constants.REFRESH_TOKEN.getKey()))));
            }
        }
        String id_token = tokenResponseObject.get(Constants.ID_TOKEN.getKey()) == null ? null : (String) tokenResponseObject.get(Constants.ID_TOKEN.getKey());
        if (id_token != null) {
            try {
                MsgResponse jwks = HttpHelper.getAPI(jwksUri);
                JSONObject idTokenPayload = Jwt.DEFAULT.validateIdToken(id_token, jwks.getMsg(), providers.getIssuer(), providers.getClientId(), clientNonce);
                // check if the client is configured to get an id_token re-signed by Loginbuddy, on behalf of the original issuer
                OnBehalfOfResult resigningResult = OnBehalfOf.signOnBehalfOf(
                        clientId,
                        clientNonce,
                        "id_token",
                        idTokenPayload,
                        id_token
                );
                tokenResponseObject.put("id_token", resigningResult.getIdToken());
                tokenResponseObject.put("id_token_payload", resigningResult.getIdTokenPayload());
            } catch (Exception e) {
                LOGGER.warning(String.format("No id_token was issued or it was invalid! Details: %s", e.getMessage()));
            }
        } else {
            LOGGER.warning("No id_token was issued");
        }
        return tokenResponseObject;
    }

    @Override
    public JSONObject handleRefreshTokenResponse(MsgResponse tokenResponse, Providers providers, String clientId) throws ParseException {
        JSONObject tokenResponseObject = ((JSONObject) new JSONParser().parse(tokenResponse.getMsg()));
        LOGGER.fine(tokenResponseObject.toJSONString());
        String access_token = tokenResponseObject.get(Constants.ACCESS_TOKEN.getKey()).toString();
        tokenResponseObject.put(Constants.ACCESS_TOKEN.getKey(), LoginbuddyUtil.UTIL.encrypt(String.format("%s:%s", providers.getProvider(), access_token)));
        if (tokenResponseObject.get(Constants.REFRESH_TOKEN.getKey()) != null) {
            tokenResponseObject.put(Constants.REFRESH_TOKEN.getKey(), LoginbuddyUtil.UTIL.encrypt(String.format("%s:%s:%s", providers.getProvider(), clientId, tokenResponseObject.get(Constants.REFRESH_TOKEN.getKey()))));
        }
        // TODO handle id_token if included in response
//        String id_token = tokenResponseObject.get(Constants.ID_TOKEN.getKey()) == null ? null : (String)tokenResponseObject.get(Constants.ID_TOKEN.getKey());
//        if (id_token != null) {
//            try {
//                JSONObject idTokenPayload = Jwt.DEFAULT.validateIdToken(id_token, jwks.getMsg(), providers.getIssuer(), providers.getClientId(), clientNonce);
//                // check if the client is configured to get an id_token re-signed by Loginbuddy, on behalf of the original issuer
//                OnBehalfOfResult resigningResult = OnBehalfOf.signOnBehalfOf(
//                        clientId,
//                        clientNonce,
//                        "id_token",
//                        idTokenPayload,
//                        id_token
//                );
//                tokenResponseObject.put("id_token", resigningResult.getIdToken());
//                tokenResponseObject.put("id_token_payload", resigningResult.getIdTokenPayload());
//            } catch (Exception e) {
//                LOGGER.warning(String.format("No id_token was issued or it was invalid! Details: %s", e.getMessage()));
//            }
//        } else {
//            LOGGER.warning("No id_token was issued");
//        }
        return tokenResponseObject;
    }
}
