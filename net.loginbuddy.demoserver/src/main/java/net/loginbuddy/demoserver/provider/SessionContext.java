package net.loginbuddy.demoserver.provider;

import java.util.Date;
import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.cache.LoginbuddyContext;

public class SessionContext extends LoginbuddyContext {

  public SessionContext() {
    super();
  }

  public void sessionInit(String clientId, String scope, String response_type, String code_challenge,
      String code_challenge_method, String redirectUri, String nonce, String state) {
    put(Constants.CLIENT_ID.getKey(), clientId);
    put(Constants.SCOPE.getKey(), scope);
    put(Constants.RESPONSE_TYPE.getKey(), response_type);
    put(Constants.CODE_CHALLENGE.getKey(), code_challenge);
    put(Constants.CODE_CHALLENGE_METHOD.getKey(), code_challenge_method);
    put(Constants.REDIRECT_URI.getKey(), redirectUri);
    put(Constants.NONCE.getKey(), nonce);
    put(Constants.STATE.getKey(), state);
    put(Constants.SESSION.getKey(), getId());
    put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_LOGIN.getKey());
  }

  public void sessionLoginProvided(String usernameLabel, String username) {
    put(usernameLabel, username);
    put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_AUTHENTICATE.getKey());
  }

  public void sessionAuthenticated() {
    put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_GRANT.getKey());
  }

  public void sessionGranted() {
    put("grant", String.valueOf(new Date()
        .getTime())); // TODO: remember when this grant was given! If we had a 'grant' table, it would go in there
  }

  public long sessionToken(String access_token, String refresh_token, String id_token) {
    put("access_token", access_token);
    put("refresh_token", refresh_token);
    put("id_token", id_token);
    put("access_token_expiration", new Date().getTime() + 3600000); // getTime should be 10-digits (seconds) but it is millis (13-digits)
    put("refresh_token_expiration", new Date().getTime() + 7200000);
    return 3600; // accessToken lifetime
  }
}
