package net.loginbuddy.service.util;

import net.loginbuddy.common.cache.LoginbuddyContext;
import net.loginbuddy.common.config.Constants;

import java.util.UUID;

public class SessionContext extends LoginbuddyContext {

  public SessionContext() {
    super();
  }

  /**
   *
   * @param clientId
   * @param scope
   * @param response_type
   * @param code_challenge
   * @param code_challenge_method
   * @param redirectUri
   * @param nonce
   * @param state
   * @param provider
   * @param prompt
   * @param loginHint
   * @param idtokenHint
   * @param checkRedirectUri
   * @param redirectUriValid
   * @param acceptDynamicProvider
   * @param signedResponseAlg
   * @param obfuscateToken
   */
  public void setSessionInit(String clientId, String scope, String response_type, String code_challenge,
      String code_challenge_method, String redirectUri, String nonce, String state, String provider,
      String prompt, String loginHint, String idtokenHint, boolean checkRedirectUri, String redirectUriValid, boolean acceptDynamicProvider,
                             String signedResponseAlg, boolean obfuscateToken) {

    put(Constants.CLIENT_CLIENT_ID.getKey(), clientId);
    put(Constants.CLIENT_SCOPE.getKey(), scope);
    put(Constants.CLIENT_RESPONSE_TYPE.getKey(), response_type);
    put(Constants.CLIENT_CODE_CHALLENGE.getKey(), code_challenge);
    put(Constants.CLIENT_CODE_CHALLENGE_METHOD.getKey(), code_challenge_method);
    put(Constants.CLIENT_REDIRECT.getKey(), redirectUri);
    put(Constants.CLIENT_REDIRECT_VALID.getKey(), redirectUriValid);
    put(Constants.CLIENT_STATE.getKey(), state);
    put(Constants.CLIENT_PROVIDER.getKey(), provider);
    put(Constants.CLIENT_PROMPT.getKey(), prompt == null ? "" : prompt);
    put(Constants.CLIENT_LOGIN_HINT.getKey(), loginHint == null ? "" : loginHint);
    put(Constants.CLIENT_ID_TOKEN_HINT.getKey(), idtokenHint == null ? "" : idtokenHint);
    put(Constants.CLIENT_ACCEPT_DYNAMIC_PROVIDER.getKey(), acceptDynamicProvider);
    put(Constants.CLIENT_SIGNED_RESPONSE_ALG.getKey(), signedResponseAlg);
    put(Constants.CHECK_REDIRECT_URI.getKey(), checkRedirectUri);
    put(Constants.ISSUER_HANDLER.getKey(), Constants.ISSUER_HANDLER_LOGINBUDDY.getKey()); // default: assuming a registered provider is used

    put(Constants.CLIENT_NONCE.getKey(), nonce == null ? UUID.randomUUID().toString() : nonce);
    put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_INITIALIZE.getKey());

    put(Constants.OBFUSCATE_TOKEN.getKey(), obfuscateToken);
  }

  /**
   *
   * @param responseType the responseType that was used. With that, we know what to expect at the callback
   */
  public void setSessionCallback(Constants responseType) {
        put(Constants.ACTION_EXPECTED.getKey(), Constants.ACTION_CALLBACK.getKey());
        put(Constants.ACTION_USED_RESPONSE_TYPE.getKey(), responseType.getKey());
  }
}