package net.loginbuddy.config.loginbuddy.common;

import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.config.loginbuddy.Providers;
import net.loginbuddy.config.loginbuddy.handler.LoginbuddyHandler;
import org.jose4j.lang.JoseException;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public interface TokenResponseHandler {

    JSONObject handleCodeTokenExchangeResponse(MsgResponse tokenResponse, boolean encryptToken, Providers providers, String clientId, String clientNonce, String jwksUri) throws ParseException;
    JSONObject handleRefreshTokenResponse(MsgResponse tokenResponse, Providers providers, String clientId) throws ParseException;
    OnBehalfOfResult getOnBehalfOfResult(String jwksUri, String idToken, String provider, String providerClientId, String clientNonce, String clientId) throws JoseException;

}
