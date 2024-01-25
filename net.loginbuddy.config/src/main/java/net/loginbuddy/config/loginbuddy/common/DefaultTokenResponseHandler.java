package net.loginbuddy.config.loginbuddy.common;

import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.Jwt;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.config.loginbuddy.Providers;
import org.jose4j.lang.JoseException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.logging.Logger;

public class DefaultTokenResponseHandler implements TokenResponseHandler {

    private static final Logger LOGGER = Logger.getLogger(DefaultTokenResponseHandler.class.getName());

    @Override
    public JSONObject handleCodeTokenExchangeResponse(MsgResponse tokenResponse, boolean encryptToken, Providers providers, String clientId, String clientNonce, String jwksUri) throws ParseException {
        JSONObject tokenResponseObject = ((JSONObject) new JSONParser().parse(tokenResponse.getMsg()));
        String access_token = tokenResponseObject.get(Constants.ACCESS_TOKEN.getKey()).toString();
        tokenResponseObject.put("provider_access_token", access_token);
        if (encryptToken) {
            encryptAndPackageToken(providers, clientId, tokenResponseObject, access_token);
        }
        String id_token = tokenResponseObject.get(Constants.ID_TOKEN.getKey()) == null ? null : (String) tokenResponseObject.get(Constants.ID_TOKEN.getKey());
        if (id_token != null) {
            try {
                OnBehalfOfResult resigningResult = getOnBehalfOfResult(
                        jwksUri,
                        id_token,
                        providers.getIssuer(),
                        providers.getClientId(),
                        clientNonce,
                        clientId
                );
                tokenResponseObject.put(Constants.ID_TOKEN.getKey(), resigningResult.getIdToken());
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
        String access_token = tokenResponseObject.get(Constants.ACCESS_TOKEN.getKey()).toString();
        encryptAndPackageToken(providers, clientId, tokenResponseObject, access_token);
        return tokenResponseObject;
    }

    @Override
    public OnBehalfOfResult getOnBehalfOfResult(String jwksUri, String idToken, String provider, String providerClientId, String clientNonce, String clientId) throws JoseException {
        MsgResponse jwks = null;
        JSONObject idTokenPayload = null;
        if(Constants.ISSUER_SELFISSUED.getKey().equalsIgnoreCase(provider)) {
            idTokenPayload = Jwt.DEFAULT.validateIdToken(idToken, null, provider, providerClientId, clientNonce);
        } else {
            jwks = HttpHelper.getAPI(jwksUri);
            idTokenPayload = Jwt.DEFAULT.validateIdToken(idToken, jwks.getMsg(), provider, providerClientId, clientNonce);
        }
        // check if the client is configured to get an id_token re-signed by Loginbuddy, on behalf of the original issuer
        return OnBehalfOf.signOnBehalfOf(clientId,clientNonce,"id_token",idTokenPayload,idToken);
    }

    private void encryptAndPackageToken(Providers providers, String clientId, JSONObject tokenResponseObject, String access_token) {
        tokenResponseObject.put(Constants.ACCESS_TOKEN.getKey(), LoginbuddyUtil.UTIL.encrypt(String.format("%s:%s", providers.getProvider(), access_token)));
        if (tokenResponseObject.get(Constants.REFRESH_TOKEN.getKey()) != null) {
            tokenResponseObject.put(Constants.REFRESH_TOKEN.getKey(), LoginbuddyUtil.UTIL.encrypt(String.format("%s:%s:%s", providers.getProvider(), clientId, tokenResponseObject.get(Constants.REFRESH_TOKEN.getKey()))));
        }
    }
}
