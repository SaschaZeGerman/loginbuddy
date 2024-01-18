package net.loginbuddy.common.api;

import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.CertificateManager;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class HttpHelper {

    private static final Logger LOGGER = Logger.getLogger(HttpHelper.class.getName());

    private static final Pattern urlPattern = Pattern.compile("^http[s]?://[a-zA-Z0-9.\\-:/]{1,92}");

    public HttpHelper() {
    }

    private static HttpResponse getHttpResponse(HttpRequestBase req) throws IOException {
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder.setConnectTimeout(5000);
        requestBuilder.setConnectionRequestTimeout(5000);
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultRequestConfig(requestBuilder.build());
        HttpClient httpClient = builder.build();
        return httpClient.execute(req);
    }

    // TODO check got 'single' header
    private static String getHeader(HttpResponse response, String headerName, String defaultValue) {
        Header[] headers = response.getHeaders(headerName);
        return headers == null ? defaultValue : headers.length != 1 ? defaultValue : headers[0].getValue();
    }

    public static boolean couldBeAUrl(String url) {
        if (url == null) {
            return false;
        }
        return urlPattern.matcher(url).matches();
    }

    public static JSONObject getErrorAsJson(String error, String errorDescription) {
        if ("".equals(error)) {
            error = "unknown";
        }
        if ("".equals(errorDescription)) {
            errorDescription = "An error without any description, sorry";
        }
        JSONObject result = new JSONObject();
        result.put("error", error);
        result.put("error_description", errorDescription);
        return result;
    }

    public static MsgResponse getAPI(String authScheme, String accessToken, String targetApi) throws IOException {
        return getAPI(GetRequest.create(targetApi).setAccessToken(authScheme, accessToken).build());
    }

    public static MsgResponse getAPI(String accessToken, String targetApi) throws IOException {
        return getAPI(GetRequest.create(targetApi).setBearerAccessToken(accessToken).build());
    }

    public static MsgResponse getAPI(String targetApi) throws IOException {
        return getAPI(GetRequest.create(targetApi).build());
    }

    public static MsgResponse getAPI(HttpGet req) throws IOException {
        HttpResponse response = getHttpResponse(req);
        Map<String, String> headers = new HashMap<>();
        for(Header h : response.getAllHeaders()) {
            headers.put(h.getName(), h.getValue());
        }
        return new MsgResponse(getHeader(response, "content-type", "application/json"),
                EntityUtils.toString(response.getEntity()), response.getStatusLine().getStatusCode(),
                headers);
    }

    public static MsgResponse postTokenExchange(String clientId, String clientSecret, String redirectUri, String authCode,
                                                String tokenEndpoint, String codeVerifier) throws IOException {

        // build POST request
        List<NameValuePair> formParameters = new ArrayList<>();
        formParameters.add(new BasicNameValuePair(Constants.CODE.getKey(), authCode));
        formParameters.add(new BasicNameValuePair(Constants.CLIENT_ID.getKey(), clientId));
        formParameters.add(new BasicNameValuePair(Constants.CLIENT_SECRET.getKey(), clientSecret));
        formParameters.add(new BasicNameValuePair(Constants.REDIRECT_URI.getKey(), redirectUri));
        formParameters.add(new BasicNameValuePair(Constants.GRANT_TYPE.getKey(), Constants.AUTHORIZATION_CODE.getKey()));
        if (codeVerifier != null) {
            formParameters.add(new BasicNameValuePair(Constants.CODE_VERIFIER.getKey(), codeVerifier));
        }

        return postMessage(formParameters, tokenEndpoint, "application/json");
    }

    /**
     *
     * @param queryString       a key=value[&key=....] where value is URLEncoded. This methods sets the request content-type to application/x-www-form-urlencoded using this string as body
     * @param targetUrl
     * @param acceptContentType
     * @return
     * @throws IOException
     */
    public static MsgResponse postMessage(String queryString, String targetUrl, String acceptContentType)
            throws IOException {
        return postMessage(
                PostRequest.create(targetUrl).setAcceptType(acceptContentType).setUrlEncodedParametersPayload(queryString).build(),
                acceptContentType);
    }

    public static MsgResponse postMessage(List<NameValuePair> formParameters, String targetUrl, String acceptContentType)
            throws IOException {
        return postMessage(
                PostRequest.create(targetUrl).setAcceptType(acceptContentType).setFormParametersPayload(formParameters).build(),
                acceptContentType);
    }

    public static MsgResponse postMessage(JSONObject input, String targetUrl, String acceptContentType) throws IOException {
        return postMessage(
                PostRequest.create(targetUrl).setAcceptType(acceptContentType).setJsonPayload(input.toJSONString()).build(),
                acceptContentType);
    }

    public static MsgResponse postMessage(HttpPost req, String acceptContentType) throws IOException {
        HttpResponse response = getHttpResponse(req);
        Map<String, String> headers = new HashMap<>();
        for(Header h : response.getAllHeaders()) {
            headers.put(h.getName(), h.getValue());
        }
        return new MsgResponse(getHeader(response, "content-type", acceptContentType),
                EntityUtils.toString(response.getEntity()), response.getStatusLine().getStatusCode(),
                headers);
    }

    public static JSONObject retrieveAndRegister(String discoveryUrl, String redirectUri) {
        return retrieveAndRegister(discoveryUrl, redirectUri, false, false);
    }

    public static JSONObject retrieveAndRegister(String discoveryUrl, String redirectUri, boolean updateProvider, boolean updateIssuer) {

        JSONObject errorResp = new JSONObject();
        try {
            if (discoveryUrl.startsWith("http:")) {
                throw new IllegalArgumentException("The discovery_url is invalid or not provided");
            }
            MsgResponse oidcConfig = getAPI(discoveryUrl);
            if (oidcConfig.getStatus() == 200) {
                if (oidcConfig.getContentType().startsWith("application/json")) {
                    JSONObject doc = (JSONObject) new JSONParser().parse(oidcConfig.getMsg());
                    // TODO check for 'code' and 'authorization_code' as supported
                    String registerUrl = (String) doc.get(Constants.REGISTRATION_ENDPOINT.getKey());
                    if (registerUrl == null || registerUrl.trim().length() == 0 || registerUrl.startsWith("http:")) {
                        throw new IllegalArgumentException("The registration_url is invalid or not provided");
                    } else {
                        JSONObject registrationMSg = new JSONObject();
                        JSONArray redirectUrisArray = new JSONArray();
                        redirectUrisArray.add(redirectUri);
                        registrationMSg.put(Constants.REDIRECT_URIS.getKey(), redirectUrisArray);
                        registrationMSg.put(Constants.TOKEN_ENDPOINT_AUTH_METHOD.getKey(), Constants.CLIENT_SECRET_POST.getKey());
                        MsgResponse registrationResponse = postMessage(registrationMSg, registerUrl, "application/json");
                        if (registrationResponse.getStatus() == 200) {
                            if (registrationResponse.getContentType().startsWith("application/json")) {
                                return providerTemplate(doc, (JSONObject) new JSONParser().parse(registrationResponse.getMsg()), redirectUri, updateProvider, updateIssuer);
                            } else {
                                // TODO handle the strange case of not getting JSON as content-type
                                return getErrorAsJson("invalid_configuration", "the registration response is not JSON");
                            }
                        } else {
                            // TODO handle the non 200 case for registration responses
                            LOGGER.warning(registrationResponse.getMsg());
                            return getErrorAsJson("invalid_request", "the registration failed");
                        }
                    }
                } else {
                    // TODO handle the strange case of not getting JSON as a content-type
                    return getErrorAsJson("invalid_configuration", "the openid-configuration response is not JSON");
                }
            } else {
                // TODO handle non 200 responses
                return getErrorAsJson("invalid_configuration", "the openid-configuration could not be retrieved");
            }
        } catch (Exception e) {
            // TODO need to handle errors
//      e.printStackTrace();
            if (e.getMessage().contains("refused")) {
                return getErrorAsJson("invalid_server", "connection to openid provider was refused");
            } else if (e.getMessage().contains("registration_url")) {
                return getErrorAsJson("invalid_server", e.getMessage());
            } else if (e.getMessage().contains("discovery_url")) {
                return getErrorAsJson("invalid_server", e.getMessage());
            } else if (e.getMessage().contains("PKIX path building failed")) {
                return getErrorAsJson("invalid_server", "OpenID Providers using a self-signed SSL certificate are not supported");
            } else if (e.getMessage().contains("failed to respond")) {
                return getErrorAsJson("invalid_server", "OpenID Provider did not respond. Need to use HTTPS?");
            } else if (e.getMessage().contains("Read timed out")) {
                return getErrorAsJson("invalid_server", "OpenID Provider connection timed out");
            } else {
                return getErrorAsJson("invalid_server", String.format("no idea what went wrong. Exception: %s", e.getMessage()));
            }
        }
    }

    public static String getErrorForRedirect(String redirectUri, String error, String errorDescription) {
        if ("".equals(errorDescription)) {
            errorDescription = "An error without any description, sorry";
        }
        error = urlEncode(error);
        errorDescription = urlEncode(errorDescription);

        return redirectUri.concat("error=").concat(error).concat("&error_description=").concat(errorDescription);
    }

    public static String stringArrayToString(String[] jsonArray) {
        return stringArrayToString(jsonArray, " ");
    }

    /**
     * Turn ["first","second"] to "first second"
     */
    public static String jsonArrayToString(JSONArray jsonArray) {
        return jsonArray.toJSONString().substring(1, jsonArray.toJSONString().length() - 1).replaceAll("[,\"]{1,5}", " ").trim();
    }

    /**
     * @param separator one of [,; ] as a separator between strings. Default: [ ]
     */
    public static String stringArrayToString(String[] jsonArray, String separator) {
        String str = Arrays.toString(jsonArray);
        return str.substring(1, str.length() - 1).replace(",", separator.matches("[,; ]") ? separator : " ");
    }

    public static String extractAccessToken(ParameterValidatorResult accessTokenParam, String authHeader) {
        String token = null;
        if (authHeader != null && authHeader.trim().length() > 0 && accessTokenParam.getResult().equals(RESULT.NONE)) {
            if (Stream.of(authHeader.split(" ")).anyMatch("bearer"::equalsIgnoreCase)) {
                token = authHeader.split(" ")[1];
            }
        }
        if (accessTokenParam.getResult().equals(RESULT.VALID) && authHeader == null) {
            token = accessTokenParam.getValue();
        }
        if (token == null) {
            LOGGER.warning("the access_token is missing or was provided multiple times");
            throw new IllegalArgumentException(getErrorAsJson("invalid_request", "Either none or multiple access_token were provided").toJSONString());
        }
        return token;
    }

    public static String urlEncode(String input) {
        return URLEncoder.encode(input == null ? "" : input, StandardCharsets.UTF_8).replaceAll("[+]", "%20");
    }

    /**
     * This template matches what is configured in config.json. At least for fields that should be provided through an OpenID Connect Discovery endpoint and the Registration
     *
     * @param oidcConfig
     * @param registration
     * @param redirectUri
     * @param updateProvider
     * @param updateIssuer
     * @return
     */
    private static JSONObject providerTemplate(JSONObject oidcConfig, JSONObject registration, String redirectUri, boolean updateProvider, boolean updateIssuer) {
        // TODO this should be removed, it is a mean redundant piece of code
        JSONObject config = new JSONObject();
        config.put("client_id", registration.get(Constants.CLIENT_ID.getKey()));
        config.put("client_secret", registration.get(Constants.CLIENT_SECRET.getKey()));
        config.put("redirect_uri", redirectUri);
        config.put("scope", HttpHelper.jsonArrayToString((JSONArray) oidcConfig.get(Constants.SCOPES_SUPPORTED.getKey())));
        config.put("authorization_endpoint", oidcConfig.get(Constants.AUTHORIZATION_ENDPOINT.getKey()));
        if (oidcConfig.get(Constants.PUSHED_AUTHORIZATION_REQUEST_ENDPOINT.getKey()) != null) {
            config.put("pushed_authorization_request_endpoint", oidcConfig.get(Constants.PUSHED_AUTHORIZATION_REQUEST_ENDPOINT.getKey()));
        }
        config.put("token_endpoint", oidcConfig.get(Constants.TOKEN_ENDPOINT.getKey()));
        config.put("userinfo_endpoint", oidcConfig.get(Constants.USERINFO_ENDPOINT.getKey()));
        config.put("jwks_uri", oidcConfig.get(Constants.JWKS_URI.getKey()));
        if (updateIssuer) {
            config.put("issuer", oidcConfig.get(Constants.ISSUER.getKey()));
        }
        if (updateProvider) {
            config.put("provider", oidcConfig.get(Constants.ISSUER.getKey()));
        }
        String responseType =
                ((JSONArray) oidcConfig.get(Constants.RESPONSE_TYPES_SUPPORTED.getKey())).contains("code") ? "code" :
                        ((JSONArray) oidcConfig.get(Constants.RESPONSE_TYPES_SUPPORTED.getKey())).contains("id_token") ? "id_token" : "unsupported";
        config.put("response_type", responseType);
        if (oidcConfig.get("dpop_bound_access_tokens") != null) {
            config.put("dpop_bound_access_tokens", Boolean.parseBoolean(oidcConfig.get("dpop_bound_access_tokens").toString()));
        } else {
            config.put("dpop_bound_access_tokens", false);
        }
        if (oidcConfig.get("dpop_signing_alg_values_supported") != null) {
            JSONArray algs = (JSONArray)oidcConfig.get("dpop_signing_alg_values_supported");
            String chosenAlg = null;
            for(Object alg : algs) {
                // we prefer ES256 over RS256
                if("ES256".equalsIgnoreCase((String)alg)) {
                    chosenAlg = "ES256";
                    break;
                } else if ("RS256".equalsIgnoreCase((String)alg) && chosenAlg == null) {
                    chosenAlg = "RS256";
                }
            }
            if(chosenAlg != null) {
                config.put("dpop_signing_alg", chosenAlg);
            } else {
                LOGGER.info("No suitable algorithm for DPoP is supported by this provider!");
            }
        }
        return config;
    }

    /**
     * Read message body from POST or PUT request
     */
    public static String readMessageBody(BufferedReader reader) throws IOException {
        StringBuilder sb = new StringBuilder();
        String nextLine = "";
        while ((nextLine = reader.readLine()) != null) {
            sb.append(nextLine);
        }
        return sb.toString().trim();
    }


    /**
     * Check if the user chose to dynamically set a provider
     */
    public static boolean checkForDynamicProvider(String provider, ParameterValidatorResult issuer,
                                                  ParameterValidatorResult discoveryUrlResult, boolean acceptDynamicProvider) {
        boolean result = false;
        if (acceptDynamicProvider) {
            result = Constants.DYNAMIC_PROVIDER.getKey().equalsIgnoreCase(provider);
            result = result && issuer.getResult().equals(RESULT.VALID);
            result = result && HttpHelper.couldBeAUrl(issuer.getValue());
            if (discoveryUrlResult.getResult().equals(RESULT.VALID)) {
                result = result && HttpHelper.couldBeAUrl(discoveryUrlResult.getValue());
            }
        }
        return result;
    }


    /**
     * Tries to connect to servers to import self-signed SSL certificates as trusted certificates. Intended for development purposes. The list of server is provided via env variable SSL_TRUSTED_SERVER
     */
    public static void loadTrustedServers() {
        String servers = null;
        try {
            servers = System.getenv("SSL_TRUSTED_SERVER");
        } catch (Exception e) {
            LOGGER.warning(String.format("Variable SSL_TRUSTED_SERVER is not accessible and therefore no self-signed certs are imported: %s", e.getMessage()));
        }
        servers = servers == null ? "" : servers;
        try {
            String oidcdr = System.getenv("SUPPORT_OIDCDR");
            if (Boolean.parseBoolean(oidcdr)) {
                servers = "loginbuddy-oidcdr:445,".concat(servers);
            } else {
                LOGGER.info("loginbuddy-oidcdr was not requested");
            }
        } catch (Exception e) {
            LOGGER.warning(String.format("Variable SUPPORT_OIDCDR is not accessible and therefore its certificate is not imported: %s", e.getMessage()));
        }
        try {
            if (servers.trim().length() > 0) {
                LOGGER.info("Connecting to trusted servers!");
                CertificateManager cm = new CertificateManager();
                for (String server : servers.split(",")) {
                    List<Certificate> certificates = cm.retrieveTrustedCert(server.split(":")[0], Integer.parseInt(server.split(":")[1]));
                    cm.addToTruststore(certificates);
                }
            }
        } catch (Exception e) {
            LOGGER.warning(String.format("Trusted server certificate could not be added! : %s", e.getMessage()));
        }
    }

    public static String createJsonErrorResponse(String value) {
        return createJsonErrorResponse(value, "");
    }

    public static String createJsonErrorResponse(String value, String toLogger) {
        if (toLogger != null && toLogger.trim().length() > 0) {
            LOGGER.warning(value.concat(": ").concat(toLogger));
        } else {
            LOGGER.warning(value);
        }
        return HttpHelper.getErrorAsJson("invalid_request", value).toJSONString();
    }

}