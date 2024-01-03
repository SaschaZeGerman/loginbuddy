package net.loginbuddy.service.server;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.loginbuddy.common.api.HttpHelper;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.config.loginbuddy.Clients;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;

public interface Sidecar {

    static ClientAuthenticator.ClientCredentialsResult getClientForAuthorize(ParameterValidatorResult clientIdResult, String signedResponseAlg, boolean acceptDynamicProvider) {
        Clients c = LoginbuddyUtil.UTIL.getClientConfigByClientId(clientIdResult.getValue());
        if (c == null) {
            return new ClientAuthenticator.ClientCredentialsResult("An invalid client_id was provided!", false, null);
        }
        c.setSignedResponseAlg(signedResponseAlg);
        c.setAcceptDynamicProvider(acceptDynamicProvider);
        return new ClientAuthenticator.ClientCredentialsResult(null, true, c);
    }

    static ClientAuthenticator.ClientCredentialsResult getClientForToken(ParameterValidatorResult clientIdResult) {
        Clients c = LoginbuddyUtil.UTIL.getClientConfigByClientId(clientIdResult.getValue());
        if (c == null) {
            return new ClientAuthenticator.ClientCredentialsResult("An invalid client_id was provided!", false, null);
        }
        return new ClientAuthenticator.ClientCredentialsResult(null, true, c);
    }

    static void checkClientConnection(HttpServletRequest request) throws IllegalAccessException {
        // port 444 (or 8044 for http) should only be available via loginbuddy-sidecar
        if (!("loginbuddy-sidecar".equals(request.getServerName()) && (request.getLocalPort() == 444 || request.getLocalPort() == 8044))) {
            throw new IllegalAccessException(HttpHelper.getErrorAsJson("invalid_client", "connection not accepted").toJSONString());
        }
    }
}
