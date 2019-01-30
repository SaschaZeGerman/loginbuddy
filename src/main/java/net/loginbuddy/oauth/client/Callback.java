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
import net.loginbuddy.util.Jwt;
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
import java.io.UnsupportedEncodingException;
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
                LOGGER.warning("Missing or invalid state parameter returned from provider!");
                response.sendError(400, "Missing or invalid state parameter");
                return;
            }

            Map<String, Object> sessionValues = (Map<String, Object>) LoginbuddyCache.getInstance().remove(session);
            if (sessionValues == null || !session.equals(sessionValues.get(Constants.SESSION.getKey()))) {
                LOGGER.warning("The current session is invalid or it has expired! Given: '" + session + "'");
                response.sendError(400, "The current session is invalid or it has expired!");
                return;
            }

            String clientRedirectUri = (String) sessionValues.get(Constants.CLIENT_REDIRECT.getKey());
            if (clientRedirectUri.contains("?")) {
                clientRedirectUri = clientRedirectUri.concat("&");

            } else {
                clientRedirectUri = clientRedirectUri.concat("?");
            }
            String clientState = (String) sessionValues.get(Constants.CLIENT_STATE.getKey());

            String error = request.getParameter(Constants.ERROR.getKey());
            String errorDescription = null;
            if (error != null) {
                errorDescription = request.getParameter("error_description");
                clientRedirectUri = getErrorForRedirect(clientRedirectUri, clientState, error, errorDescription);
                response.sendRedirect(clientRedirectUri);
                return;
            }

            String authCode = request.getParameter(Constants.CODE.getKey());
            if (authCode == null || authCode.trim().length() == 0 || request.getParameterValues(Constants.CODE.getKey()).length > 1) {
                clientRedirectUri = clientRedirectUri.concat("error=invalid_request&error_description=Missing+or+invalid+code+parameter");
                response.sendRedirect (clientRedirectUri);
                return;
            }

            String provider = (String) sessionValues.get(Constants.CLIENT_PROVIDER.getKey());
            String code_verifier = (String) sessionValues.get(Constants.CODE_VERIFIER.getKey());

            ProviderConfig providerConfig = LoginbuddyConfig.getInstance().getConfigUtil().getProviderConfigByProvider(provider);

            String tokenEndpoint = null;
            String userInfoEndpoint = null;
            String jwksUri = null;

            if (providerConfig.getOpenidConfigurationUri() != null) {
                tokenEndpoint = (String) sessionValues.get(Constants.TOKEN_ENDPOINT.getKey());
                userInfoEndpoint = (String) sessionValues.get(Constants.USERINFO_ENDPOINT.getKey());
                jwksUri = (String) sessionValues.get(Constants.JWKS_URI.getKey());
            } else {
                tokenEndpoint = providerConfig.getTokenEndpoint();
                userInfoEndpoint = providerConfig.getUserinfoEndpoint();
                jwksUri = providerConfig.getJwksUri();
            }

            ExchangeBean eb = new ExchangeBean();
            eb.setIss("https://".concat(System.getenv("HOSTNAME_LOGINBUDDY")));
            eb.setIat(new Date().getTime()/1000);
            eb.setAud((String)sessionValues.get(Constants.CLIENT_ID.getKey()));
            eb.setNonce(UUID.randomUUID().toString());
            eb.setProvider(provider);

            String access_token = null;
            String id_token = null;

            MsgResponse tokenResponse = postTokenExchange(providerConfig.getClientId(), providerConfig.getClientSecret(), providerConfig.getRedirectUri(), authCode, tokenEndpoint, code_verifier);
            if (tokenResponse != null) {
                if (tokenResponse.getStatus() == 200) {
                    if (tokenResponse.getContentType().startsWith("application/json")) {
                        JSONObject tokenResponseObject = ((JSONObject) new JSONParser().parse(tokenResponse.getMsg()));
                        access_token = tokenResponseObject.get("access_token").toString();
                        id_token = tokenResponseObject.get("id_token").toString();
                    } else {
                        // no sure what to do yet ... but it should also never happen!
                    }
                } else {
                    // need to handle error cases
                    if(tokenResponse.getContentType().startsWith("application/json")) {
                        JSONObject err = (JSONObject)new JSONParser().parse(tokenResponse.getMsg());
                        error = (String)err.get("error");
                        errorDescription = (String)err.get("error_description");
                        clientRedirectUri = getErrorForRedirect(clientRedirectUri, clientState, error, errorDescription);
                        response.sendRedirect(clientRedirectUri);
                        return;
                    }
                }
            } else {
                clientRedirectUri = clientRedirectUri.concat("error=invalid_request&error_description=The+code+exchange+failed.+An+access_token+could+not+be+retrieved");
                response.sendRedirect (clientRedirectUri);
                return;
            }

            try {
                MsgResponse jwks = getAPI(jwksUri);
                JSONObject idTokenPayload = new Jwt().validateJwt(id_token, jwks.getMsg(), providerConfig.getIssuer(), providerConfig.getClientId(), (String) sessionValues.get(Constants.NONCE.getKey()));
                eb.setIdTokenPayload(idTokenPayload);
                eb.setIdToken(id_token);
            } catch (Exception e) {
                JSONObject err = new JSONObject();
                error = "invalid_id_token";
                errorDescription = "The given id_token was invalid!";
                err.put("error", error);
                err.put("error_description", errorDescription);
                eb.setIdTokenPayload(err);
                eb.setIdToken("INVALID_" + id_token);
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
                        if (userinfoResp.getContentType().startsWith("application/json")) {
                            JSONObject err = (JSONObject) new JSONParser().parse(tokenResponse.getMsg());
                            error = (String) err.get("error");
                            errorDescription = (String) err.get("error_description");
                            if (errorDescription == null) {
                                errorDescription = "An error was returned by the provider without any description";
                            }
                            err.put("error", error);
                            err.put("error_description", errorDescription);
                            eb.setUserinfo(err);
                        }
                    } else {
                        eb.setUserinfo((JSONObject)new JSONParser().parse("{\"error\":\"unknown_error\"}"));
                    }
                }
            }

            String pickUpCode = UUID.randomUUID().toString();
            sessionValues.put("eb", eb.toString());
            LoginbuddyCache.getInstance().put(pickUpCode, sessionValues);

            clientRedirectUri = clientRedirectUri.concat("code=").concat(pickUpCode).concat("&state=").concat(clientState);
            response.sendRedirect(clientRedirectUri);

        } catch (Exception e) {
            LOGGER.warning("authorization request failed!");
            e.printStackTrace();
            response.getWriter().println("authorization request failed!");
        }
    }

    private String getErrorForRedirect(String clientRedirectUri, String clientState, String error, String errorDescription) throws UnsupportedEncodingException {
        if (errorDescription == null) {
            errorDescription = "An error was returned by the provider without any description";
        }
        error = URLEncoder.encode(error, "UTF-8");
        errorDescription = URLEncoder.encode(errorDescription, "UTF-8");
        return clientRedirectUri.concat("error=").concat(error).concat("&error_description=").concat(errorDescription).concat("&state=").concat(clientState);
    }

    private MsgResponse postTokenExchange(String clientId, String clientSecret, String redirectUri, String authCode, String tokenEndpoint, String codeVerifier) {

        // build POST request
        List<NameValuePair> formParameters = new ArrayList<>();
        formParameters.add(new BasicNameValuePair(Constants.CODE.getKey(), authCode));
        formParameters.add(new BasicNameValuePair(Constants.CLIENT_ID.getKey(), clientId));
        formParameters.add(new BasicNameValuePair(Constants.CLIENT_SECRET.getKey(), clientSecret));
        formParameters.add(new BasicNameValuePair(Constants.REDIRECT_URI.getKey(), redirectUri));
        formParameters.add(new BasicNameValuePair(Constants.GRANT_TYPE.getKey(), Constants.AUTHORIZATION_CODE.getKey()));
        formParameters.add(new BasicNameValuePair(Constants.CODE_VERIFIER.getKey(), codeVerifier));

        try {
            HttpPost req = new HttpPost(tokenEndpoint);

            HttpClient httpClient = HttpClientBuilder.create().build();
            req.setEntity(new UrlEncodedFormEntity(formParameters));

            HttpResponse response = httpClient.execute(req);
            return new MsgResponse(response.getHeaders("Content-Type")[0].getValue(), EntityUtils.toString(response.getEntity()), response.getStatusLine().getStatusCode());
        } catch (Exception e) {
            LOGGER.warning("Token exchange request failed!");
            e.printStackTrace();
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
            LOGGER.warning("Call to targetApi failed!");
            e.printStackTrace();
            return null;
        }
    }

    private MsgResponse getAPI(String targetApi) {
        try {
            HttpGet req = new HttpGet(targetApi);
            HttpClient httpClient = HttpClientBuilder.create().build();

            HttpResponse response = httpClient.execute(req);
            return new MsgResponse(response.getHeaders("Content-Type")[0].getValue(), EntityUtils.toString(response.getEntity()), response.getStatusLine().getStatusCode());
        } catch (Exception e) {
            LOGGER.warning("The API response could not be retrieved. Given URL: '" + targetApi + "'");
            e.printStackTrace();
            return null;
        }
    }

}