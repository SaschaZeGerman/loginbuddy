package net.loginbuddy.service.server;

import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.Sanetizer;
import net.loginbuddy.service.config.discovery.DiscoveryUtil;
import net.loginbuddy.service.config.loginbuddy.Clients;
import net.loginbuddy.service.config.loginbuddy.LoginbuddyUtil;

import java.util.Base64;
import java.util.stream.Stream;

public class ClientAuthenticator {

    public static ClientCredentialsResult validateClientCredentials(ParameterValidatorResult clientIdResult, ParameterValidatorResult clientSecretResult, String authHeader) {

// ***************************************************************
// ** Find the clientId. Either as POST parameter or in the Authorization header but not at both locations
// ***************************************************************

        String clientId = clientIdResult.getValue();
        String clientCreds = null;
        String usedAuthMethod = Constants.CLIENT_SECRET_POST.getKey(); // our assumed default

        if ((clientId == null && authHeader == null) || (clientId != null && authHeader != null)) {
            return new ClientCredentialsResult("missing or duplicate client credentials");
        } else if (clientId == null) {
            if (authHeader.split(" ").length == 2 && authHeader.split(" ")[0].equalsIgnoreCase("basic")) {
                clientCreds = new String(Base64.getDecoder().decode(authHeader.split(" ")[1]));
                clientId = clientCreds.split(":")[0];
                usedAuthMethod = Constants.CLIENT_SECRET_BASIC.getKey();
            } else {
                return new ClientCredentialsResult("missing client credentials");
            }
        }

// ***************************************************************
// ** Lookup the client registration details to verify given credentials
// ***************************************************************

        Clients cc = LoginbuddyUtil.UTIL.getClientConfigByClientId(clientId);
        if (cc != null) {
            // let's check supported methods (if any were configured. Otherwise we'll accept the one that was used)
            String supportedMethods = DiscoveryUtil.UTIL.getTokenEndpointAuthMethodsSupportedAsString();
            if (supportedMethods == null) {
                supportedMethods = usedAuthMethod;
            }

            if (Stream.of((supportedMethods).split("[,; ]")).anyMatch(usedAuthMethod::equalsIgnoreCase)) {
                // Public clients cannot use a Basic authorization header. They miss the 'secret' portion of the string 'client_id:'
                //
                if (Constants.CLIENT_TYPE_PUBLIC.getKey().equals(cc.getClientType()) && Constants.CLIENT_SECRET_BASIC.getKey().equals(usedAuthMethod)) {
                    return new ClientCredentialsResult("unsupported authentication method for public clients was used");
                } else if (Constants.CLIENT_TYPE_CONFIDENTIAL.getKey().equalsIgnoreCase(cc.getClientType())) {
                    String clientSecret = Constants.CLIENT_SECRET_POST.getKey().equals(usedAuthMethod) ? clientSecretResult.getValue() : clientCreds.split(":")[1];
                    if (clientSecret == null || clientSecret.trim().length() == 0) {
                        return new ClientCredentialsResult("missing client_secret");
                    } else if (!cc.getClientSecret().equals(clientSecret)) {
                        return new ClientCredentialsResult("invalid client credentials given");
                    }
                }
            } else {
                return new ClientCredentialsResult("unsupported authentication method was used");
            }
        } else {
            return new ClientCredentialsResult("An invalid client_id was provided!");
        }
        return new ClientCredentialsResult(null, true, cc);
    }

    public static ClientRedirecUriResult validateClddddientRedirectUri(ParameterValidatorResult clientRedirectUriResult, String registeredRedirectUris, String clientType) {

        boolean checkRedirectUri = true;

        if (clientRedirectUriResult.getResult().equals(ParameterValidatorResult.RESULT.MULTIPLE)) {
            return new ClientRedirecUriResult("Too many redirect_uri parameters given!", checkRedirectUri, null, null);
        }

        String clientRedirectUri = clientRedirectUriResult.getValue();
        if (clientRedirectUri == null) {
            if (Constants.CLIENT_TYPE_PUBLIC.getKey().equals(clientType)) {
                return new ClientRedirecUriResult("Missing redirect_uri parameter!", checkRedirectUri, null, clientRedirectUri);
            } else if (registeredRedirectUris.split("[,; ]").length != 1) {
                return new ClientRedirecUriResult("Missing redirect_uri parameter!", checkRedirectUri, null, clientRedirectUri);

            } else {
                // confidential clients only need a registered redirectUri and not need to request it UNLESS multiple ones were registered
                clientRedirectUri = registeredRedirectUris;
                checkRedirectUri = false; // it was not given, so no need to check for it at the token endpoint
            }
        }
        if (Stream.of(registeredRedirectUris.split("[,; ]")).noneMatch(clientRedirectUri::equals)) {
            return new ClientRedirecUriResult(String.format("Invalid redirect_uri: %s", Sanetizer.checkForUrlPattern(clientRedirectUri, 256)), checkRedirectUri, null, clientRedirectUri);
        }

        String clientRedirectUriValid;
        if (clientRedirectUri.contains("?")) {
            clientRedirectUriValid = clientRedirectUri.concat("&");
        } else {
            clientRedirectUriValid = clientRedirectUri.concat("?");
        }

        return new ClientRedirecUriResult(null, checkRedirectUri, clientRedirectUriValid, clientRedirectUri);
    }

    public static class ClientCredentialsResult {

        private String errorMsg;
        private boolean isValid;
        private Clients clients;

        public ClientCredentialsResult(String errorMsg) {
            this(errorMsg, false, null);
        }

        public ClientCredentialsResult(String errorMsg, boolean isValid, Clients clients) {
            this.errorMsg = errorMsg;
            this.isValid = isValid;
            this.clients = clients;
        }

        public String getErrorMsg() {
            return errorMsg;
        }

        public boolean isValid() {
            return isValid;
        }

        public Clients getClients() {
            return clients;
        }
    }

    public static class ClientRedirecUriResult {

        private String givenRedirectUri;
        private String validatedRedirectUri;
        private String errorMsg;
        private boolean checkRedirectUri;

        public ClientRedirecUriResult(String errorMsg, boolean checkRedirectUri, String validatedRedirectUri, String givenRedirectUri) {
            this.errorMsg = errorMsg;
            this.checkRedirectUri = checkRedirectUri;
            this.validatedRedirectUri = validatedRedirectUri;
            this.givenRedirectUri = givenRedirectUri;
        }

        public String getErrorMsg() {
            return errorMsg;
        }

        public boolean checkRedirectUri() {
            return checkRedirectUri;
        }

        public String getValidatedRedirectUri() {
            return validatedRedirectUri;
        }

        public String getGivenRedirectUri() {
            return givenRedirectUri;
        }
    }
}
