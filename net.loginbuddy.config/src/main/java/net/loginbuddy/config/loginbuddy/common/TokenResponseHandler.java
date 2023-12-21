package net.loginbuddy.config.loginbuddy.common;

import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.config.loginbuddy.Providers;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

public interface TokenResponseHandler {

    JSONObject handleCodeTokenExchangeResponse(MsgResponse tokenResponse, boolean encryptToken, Providers providers, String clientId, String clientNonce, String jwksUri) throws ParseException;
    JSONObject handleRefreshTokenResponse(MsgResponse tokenResponse, Providers providers, String clientId) throws ParseException;

}
