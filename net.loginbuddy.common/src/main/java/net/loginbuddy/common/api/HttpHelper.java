package net.loginbuddy.common.api;

import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.MsgResponse;
import net.loginbuddy.common.util.ParameterValidatorResult;
import net.loginbuddy.common.util.ParameterValidatorResult.RESULT;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public class HttpHelper {

    private static final Logger LOGGER = Logger.getLogger(HttpHelper.class.getName());

    private static final Pattern urlPattern = Pattern.compile("^http[s]?://[a-zA-Z0-9.\\-:/]{1,92}");

    public HttpHelper() {
    }

    private static MsgResponse getHttpResponse(HttpRequestBase req) {
        return getHttpResponse(req, "application/json");
    }

    private static MsgResponse getHttpResponse(HttpRequestBase req, String defaultContentType) {
        RequestConfig.Builder requestBuilder = RequestConfig.custom();
        requestBuilder.setConnectTimeout(5000);
        requestBuilder.setConnectionRequestTimeout(5000);
        HttpClientBuilder builder = HttpClientBuilder.create();
        builder.setDefaultRequestConfig(requestBuilder.build());
        HttpClient httpClient = builder.build();
        try {
            HttpResponse response = httpClient.execute(req);
            Map<String, String> headers = new HashMap<>();
            for (Header h : response.getAllHeaders()) {
                headers.put(h.getName(), h.getValue());
            }
            return new MsgResponse(getHeader(response, "content-type", defaultContentType),
                    EntityUtils.toString(response.getEntity()), response.getStatusLine().getStatusCode(),
                    headers);
        } catch (Exception e) {
            MsgResponse msg = new MsgResponse();
            msg.setContentType("application/json");
            msg.setStatus(400);
            if (e.getMessage().contains("refused")) {
                msg.setMsg(getErrorAsJson("invalid_server", "connection to openid provider was refused").toJSONString());
            } else if (e.getMessage().contains("registration_url")) {
                msg.setMsg(getErrorAsJson("invalid_server", e.getMessage()).toJSONString());
            } else if (e.getMessage().contains("discovery_url")) {
                msg.setMsg(getErrorAsJson("invalid_server", e.getMessage()).toJSONString());
            } else if (e.getMessage().contains("PKIX path building failed")) {
                msg.setMsg(getErrorAsJson("invalid_server", "OpenID Providers using a self-signed SSL certificate are not supported").toJSONString());
            } else if (e.getMessage().contains("subject alternative names")) {
                msg.setMsg(getErrorAsJson("invalid_server", "The OpenID Provider uses an invalid certificate with unsupported subject alternative names").toJSONString());
            } else if (e.getMessage().contains("failed to respond")) {
                msg.setMsg(getErrorAsJson("invalid_server", "OpenID Provider did not respond. Need to use HTTPS?").toJSONString());
            } else if (e.getMessage().contains("Read timed out")) {
                msg.setMsg(getErrorAsJson("invalid_server", "OpenID Provider connection timed out").toJSONString());
            } else {
                msg.setMsg(getErrorAsJson("invalid_server", String.format("no idea what went wrong. Exception: %s", e.getMessage())).toJSONString());
            }
            return msg;
        }
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

    public static MsgResponse getAPI(String accessToken, String targetApi) {
        return getAPI(GetRequest.create(targetApi).setBearerAccessToken(accessToken).build());
    }

    public static MsgResponse getAPI(String targetApi)  {
        return getAPI(GetRequest.create(targetApi).build());
    }

    public static MsgResponse getAPI(HttpGet req) {
        return getHttpResponse(req);
    }

    public static MsgResponse postTokenExchange(String clientId, String clientSecret, String redirectUri, String authCode,
                                                String tokenEndpoint, String codeVerifier) throws IOException {

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
     * @param queryString       a key=value[&key=....] where value is URLEncoded. This methods sets the request content-type to application/x-www-form-urlencoded using this string as body
     * @param targetUrl
     * @param defaultContentType
     * @return
     * @throws IOException
     */
    public static MsgResponse postMessage(String queryString, String targetUrl, String defaultContentType)
            throws IOException {
        return postMessage(
                PostRequest.create(targetUrl).setAcceptType(defaultContentType).setUrlEncodedParametersPayload(queryString).build(),
                defaultContentType);
    }

    public static MsgResponse postMessage(List<NameValuePair> formParameters, String targetUrl, String defaultContentType)
            throws IOException {
        return postMessage(
                PostRequest.create(targetUrl).setAcceptType(defaultContentType).setFormParametersPayload(formParameters).build(),
                defaultContentType);
    }

    public static MsgResponse postMessage(JSONObject input, String targetUrl, String defaultContentType) {
        return postMessage(
                PostRequest.create(targetUrl).setAcceptType(defaultContentType).setJsonPayload(input.toJSONString()).build(),
                defaultContentType);
    }

    public static MsgResponse postMessage(HttpPost req, String defaultContentType) {
        return getHttpResponse(req, defaultContentType);
    }

    public static MsgResponse register(String registerUrl, String redirectUri) throws ParseException {
        JSONObject registrationMSg = new JSONObject();
        JSONArray redirectUrisArray = new JSONArray();
        redirectUrisArray.add(redirectUri);
        registrationMSg.put(Constants.REDIRECT_URIS.getKey(), redirectUrisArray);
        registrationMSg.put(Constants.TOKEN_ENDPOINT_AUTH_METHOD.getKey(), Constants.CLIENT_SECRET_POST.getKey());
        return postMessage(registrationMSg, registerUrl, "application/json");
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