package net.loginbuddy.service.util;

import java.util.UUID;
import net.loginbuddy.common.cache.LoginbuddyContext;
import net.loginbuddy.common.config.Constants;

public class SessionContext extends LoginbuddyContext {

  public SessionContext() {
    super();
  }

  public void sessionInit(String clientId, String scope, String response_type, String code_challenge,
      String code_challenge_method, String redirectUri, String nonce, String state, String provider,
      String prompt, String loginHint, String idtokenHint, boolean checkRedirectUri) {

    put(Constants.CLIENT_ID.getKey(), clientId);
    put(Constants.CLIENT_SCOPE.getKey(), scope);
    put(Constants.CLIENT_RESPONSE_TYPE.getKey(), response_type);
    put(Constants.CLIENT_CODE_CHALLENGE.getKey(), code_challenge);
    put(Constants.CLIENT_CODE_CHALLENGE_METHOD.getKey(), code_challenge_method);
    put(Constants.CLIENT_REDIRECT.getKey(), redirectUri);
    put(Constants.CLIENT_STATE.getKey(), state);
    put(Constants.CLIENT_PROVIDER.getKey(), provider);
    put(Constants.CLIENT_PROMPT.getKey(), prompt == null ? "" : prompt);
    put(Constants.CLIENT_LOGIN_HINT.getKey(), loginHint == null ? "" : loginHint);
    put(Constants.CLIENT_ID_TOKEN_HINT.getKey(), idtokenHint == null ? "" : idtokenHint);
    put(Constants.CHECK_REDIRECT_URI.getKey(), checkRedirectUri);

    put(Constants.NONCE.getKey(), nonce == null ? UUID.randomUUID().toString() : nonce);
    put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_INITIALIZE.getKey());
  }

  public void sessionCallback() {
    put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_CALLBACK.getKey());
  }
}