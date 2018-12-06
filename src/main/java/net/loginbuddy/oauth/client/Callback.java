/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.oauth.client;

import net.loginbuddy.cache.LoginbuddyCache;
import net.loginbuddy.config.Constants;
import net.loginbuddy.config.LoginbuddyConfig;
import net.loginbuddy.config.ProviderConfig;
import net.loginbuddy.oauth.util.MsgResponse;
import net.loginbuddy.oauth.util.ExchangeBean;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Logger;


public class Callback extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(Callback.class));

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        try {

            String session = request.getParameter(Constants.STATE.getKey());
            if (session == null || session.trim().length() == 0 || request.getParameterValues(Constants.STATE.getKey()).length > 1) {
                response.sendError(400, "Missing or invalid state parameter");
                return;
            }

            Map<String, Object> sessionValues = (Map<String, Object>) LoginbuddyCache.getInstance().getCache().get(session);
            if (sessionValues == null || !session.equals(sessionValues.get(Constants.SESSION.getKey()))) {
                LOGGER.severe("The current session is invalid or it has expired! Given: '" + session + "'");
                response.sendError(400, "The current session is invalid or it has expired!");
                return;
            }

            String clientRedirectUri = (String) sessionValues.get(Constants.CLIENT_REDIRECT.getKey());
            String clientState = (String) sessionValues.get(Constants.CLIENT_STATE.getKey());

            String error = request.getParameter(Constants.ERROR.getKey());
            String errorDescription = null;
            if (error != null) {
                errorDescription = request.getParameter("error_description");
                if (errorDescription == null) {
                    errorDescription = "An error was returned by the provider without any description";
                }
                error = URLEncoder.encode(error, "UTF-8");
                errorDescription = URLEncoder.encode(errorDescription, "UTF-8");
                if (clientRedirectUri.contains("?")) {
                    clientRedirectUri = clientRedirectUri.concat("&");

                } else {
                    clientRedirectUri = clientRedirectUri.concat("?");
                }
                clientRedirectUri = clientRedirectUri.concat("error=").concat(error).concat("&error_description=").concat(errorDescription).concat("&state=").concat(clientState);
                response.sendRedirect(clientRedirectUri);
                return;
            }

            String authCode = request.getParameter(Constants.CODE.getKey());
            if (authCode == null || authCode.trim().length() == 0 || request.getParameterValues(Constants.CODE.getKey()).length > 1) {
                if (clientRedirectUri.contains("?")) {
                    clientRedirectUri = clientRedirectUri.concat("&");

                } else {
                    clientRedirectUri = clientRedirectUri.concat("?");
                }
                clientRedirectUri = clientRedirectUri.concat("error=invalid_request&error_description=Missing+or+invalid+code+parameter");
                response.sendRedirect (clientRedirectUri);
                return;
            }

            String provider = (String) sessionValues.get(Constants.CLIENT_PROVIDER.getKey());
            String code_verifier = (String) sessionValues.get(Constants.CODE_VERIFIER.getKey());

            ProviderConfig providerConfig = LoginbuddyConfig.getInstance().getConfigUtil().getProviderConfigByProvider(provider);

            String tokenEndpoint = null;
            String userInfoEndpoint = null;
            if (providerConfig.getOpenidConfigurationUri() != null) {
                tokenEndpoint = (String) sessionValues.get(Constants.TOKEN_ENDPOINT.getKey());
                userInfoEndpoint = (String) sessionValues.get(Constants.USERINFO_ENDPOINT.getKey());
            } else {
                tokenEndpoint = providerConfig.getTokenEndpoint();
                userInfoEndpoint = providerConfig.getUserinfoEndpoint();
            }

            ExchangeBean eb = new ExchangeBean();
            eb.setIss("https://".concat(System.getenv("HOSTNAME_LOGINBUDDY")));
            eb.setIat(new Date().getTime());
            eb.setAud("a_client_id");
            eb.setNonce("a_nonce_value");
            eb.setProvider(provider);

            String access_token = null;

            MsgResponse tokenResponse = postTokenExchange(providerConfig.getClientId(), providerConfig.getClientSecret(), providerConfig.getRedirectUri(), authCode, tokenEndpoint, code_verifier);
            if (tokenResponse != null) {
                if (tokenResponse.getStatus() == 200) {
                    if (tokenResponse.getContentType().startsWith("application/json")) {
                        JSONObject tokenResponseObject = ((JSONObject) new JSONParser().parse(tokenResponse.getMsg()));
                        access_token = tokenResponseObject.get("access_token").toString();
                        eb.setIdToken(tokenResponseObject.get("id_token").toString());
                    } else {
                        // no sure what to do yet ... but it should also never happen!
                    }
                } else {
                    // need to handle error cases
//                    response.sendError(...);
                }
            }

            // Call /userinfo only if it was configured in config.json
            if (userInfoEndpoint != null && !"".equals(userInfoEndpoint)) {
                MsgResponse userinfoResp = getProtectedAPI(access_token, userInfoEndpoint);
                if (userinfoResp != null) {
                    if (userinfoResp.getStatus() == 200) {
                        if (userinfoResp.getContentType().startsWith("application/json")) {
                            JSONObject userinfoRespObject = (JSONObject) new JSONParser().parse(userinfoResp.getMsg());
                            eb.setUserinfo(userinfoRespObject);
                        }
                    } else if (userinfoResp.getStatus() == 401) {
                        // return error to client
                    }
                }
            }

            String pickUpCode = UUID.randomUUID().toString();
            LoginbuddyCache.getInstance().getCache().put(pickUpCode, eb.toString());

            if (clientRedirectUri.contains("?")) {
                clientRedirectUri = clientRedirectUri.concat("&");

            } else {
                clientRedirectUri = clientRedirectUri.concat("?");
            }

            clientRedirectUri = clientRedirectUri.concat("code=").concat(pickUpCode).concat("&state=").concat(clientState);
            response.sendRedirect(clientRedirectUri);

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println("authorization request failed");
        }
    }

    private MsgResponse postTokenExchange(String clientId, String clientSecret, String redirectUri, String authCode, String tokenEndpoint, String codeVerifier) {

        // build POST request
        List<NameValuePair> formParameters = new ArrayList<NameValuePair>();
        formParameters.add(new BasicNameValuePair(Constants.CODE.getKey(), authCode));
        formParameters.add(new BasicNameValuePair(Constants.CLIENT_ID.getKey(), clientId));
        formParameters.add(new BasicNameValuePair(Constants.CLIENT_SECRET.getKey(), clientSecret));
        formParameters.add(new BasicNameValuePair(Constants.REDIRECT_URI.getKey(), redirectUri));
        formParameters.add(new BasicNameValuePair(Constants.GRANT_TYPE.getKey(), Constants.AUTHORIZATION_CODE.getKey()));
        formParameters.add(new BasicNameValuePair(Constants.CODE_VERIFIER.getKey(), codeVerifier));

        try {
            HttpPost req = new HttpPost(tokenEndpoint);

            HttpClient httpClient = HttpClientBuilder.create().build();
            req.setHeader(Constants.AUTHORIZATION.getKey(), Constants.BASIC.getKey() + Arrays.toString(Base64.getEncoder().encode((clientId.concat(":").concat(clientSecret)).getBytes())));
            req.setEntity(new UrlEncodedFormEntity(formParameters));

            HttpResponse response = httpClient.execute(req);
            return new MsgResponse(response.getHeaders("Content-Type")[0].getValue(), EntityUtils.toString(response.getEntity()), response.getStatusLine().getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.severe("token exchange request failed!");
            return null;
        }
    }

    private MsgResponse getProtectedAPI(String accessToken, String targetApi) {
        try {
            HttpGet req = new HttpGet(targetApi);
            HttpClient httpClient = HttpClientBuilder.create().build();
            req.setHeader(Constants.AUTHORIZATION.getKey(), Constants.BEARER.getKey() + accessToken);

            HttpResponse response = httpClient.execute(req);
            return new MsgResponse(response.getHeaders("Content-Type")[0].getValue(), EntityUtils.toString(response.getEntity()), response.getStatusLine().getStatusCode());
        } catch (Exception e) {
            e.printStackTrace();
            LOGGER.severe("Call to targetApi failed!");
            return null;
        }
    }
}