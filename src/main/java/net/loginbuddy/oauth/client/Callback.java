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
import org.apache.http.HttpEntity;
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
import java.util.*;
import java.util.logging.Logger;


public class Callback extends HttpServlet {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(Callback.class));

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doGet(req, resp);
    }

    // handle provider response with 'code'
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException  {

        try {

            String state = request.getParameter(Constants.STATE.getKey());
            if (state == null || state.trim().length() == 0) {
                throw new IllegalStateException("Missing session, cannot initiate the authorization flow!");
            }

            Map<String, Object> sessionValues = (Map<String, Object>) LoginbuddyCache.getInstance().getCache().get(state);
            if (sessionValues == null) {
                throw new IllegalStateException("The current session is invalid or it has expired!");
            }

            if (!state.equals(sessionValues.get(Constants.STATE.getKey()))) {
                LOGGER.severe("authorization_code was received with invalid state!");
                throw new IllegalAccessError("state does not match!");
            }

            String authCode = request.getParameter(Constants.CODE.getKey());

            String provider = (String)sessionValues.get("clientProvider");
            String redirectUrl = (String)sessionValues.get("clientRedirectUri");
            String clientState = (String)sessionValues.get("clientState");
            String code_verifier = (String)sessionValues.get("code_verifier");

            ProviderConfig providerConfig = LoginbuddyConfig.getInstance().getConfigUtil().getProviderConfigByProvider(provider);

            String tokenEndpoint = null;
            String userInfoEndpoint = null;
            if(providerConfig.getOpenidConfigurationUri() != null) {
                tokenEndpoint = (String)sessionValues.get("token_endpoint");
                userInfoEndpoint = (String)sessionValues.get("userinfo_endpoint");
            } else {
                tokenEndpoint = providerConfig.getTokenEndpoint();
                userInfoEndpoint = providerConfig.getUserinfoEndpoint();
            }

            JSONObject tokenResponse = codeTokenExchange(providerConfig.getClientId(), providerConfig.getClientSecret(), providerConfig.getRedirectUri(), authCode, tokenEndpoint, code_verifier);
            String access_token = tokenResponse.get("access_token").toString();
            String id_token = tokenResponse.get("id_token").toString();

            JSONObject userinfo = getUserInfo(access_token, userInfoEndpoint);

            String responsePostForm = buildAutoForm(id_token, Base64.getEncoder().encodeToString(userinfo.toJSONString().getBytes()),redirectUrl,clientState);
            response.getWriter().println(responsePostForm);

        } catch (Exception e) {
            e.printStackTrace();
            response.getWriter().println("authorization request failed");
        }
    }

    private JSONObject codeTokenExchange(String clientId, String clientSecret, String redirectUri, String authCode, String tokenEndpoint, String codeVerifier) {

        HttpClient httpClient = HttpClientBuilder.create().build();

        try {

            HttpPost httpPost = new HttpPost(tokenEndpoint);

            // build POST request
            List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
            urlParameters.add(new BasicNameValuePair(Constants.CODE.getKey(), authCode));
            urlParameters.add(new BasicNameValuePair(Constants.CLIENT_ID.getKey(), clientId));
            urlParameters.add(new BasicNameValuePair(Constants.CLIENT_SECRET.getKey(), clientSecret));
            urlParameters.add(new BasicNameValuePair(Constants.REDIRECT_URI.getKey(), redirectUri));
            urlParameters.add(new BasicNameValuePair(Constants.GRANT_TYPE.getKey(), Constants.AUTHORIZATION_CODE.getKey()));
            urlParameters.add(new BasicNameValuePair(Constants.CODE_VERIFIER.getKey(), codeVerifier));

            // Post request
            HttpEntity httpEntity = new UrlEncodedFormEntity(urlParameters);
            httpPost.setEntity(httpEntity);
            HttpResponse response = httpClient.execute(httpPost); // this will fail if loginbuddy tries to connect to a server that has a self signed certificate!

            // assuming it is always JSON, not checking for content-type here
            return (JSONObject)new JSONParser().parse(EntityUtils.toString(response.getEntity()));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private JSONObject getUserInfo(String accessToken, String userInfoEndpoint) throws Exception {
        HttpClient httpClient = HttpClientBuilder.create().build();

        try {

            HttpGet httpGet = new HttpGet(userInfoEndpoint);
            httpGet.setHeader(Constants.AUTHORIZATION.getKey(), Constants.BEARER.getKey() + accessToken);

            // Execute get request
            HttpResponse response = httpClient.execute(httpGet);

            // assuming it is always JSON, not checking for content-type here
            return (JSONObject)new JSONParser().parse(EntityUtils.toString(response.getEntity()));

        } catch (IOException e) {
            LOGGER.severe("userinfo request failed");
            throw e;
        }
    }

    private String buildAutoForm(String idToken, String base64EncodedUserInfoResponse, String redirectUrl, String state) {
        return new StringBuilder("<html><HEAD><META HTTP-EQUIV='PRAGMA' CONTENT='NO-CACHE'><META HTTP-EQUIV='CACHE-CONTROL' CONTENT='NO-CACHE'><TITLE>Login Buddy Auto-Form POST</TITLE></HEAD>")
                .append("<body onLoad=\"document.forms[0].submit()\"><NOSCRIPT>Your browser does not support JavaScript.  Please click the 'Continue' button below to proceed. <br><br></NOSCRIPT>")
                .append("<form action=\"").append(redirectUrl).append("\" method=\"POST\">")
                .append("<input type=\"hidden\" name=\"userinforesponse\" value=\"").append(base64EncodedUserInfoResponse).append("\">")
                .append("<input type=\"hidden\" name=\"id_token\" value=\"").append(idToken).append("\">")
                .append("<input type=\"hidden\" name=\"state\" value=\"").append(state).append("\"><NOSCRIPT><INPUT TYPE=\"SUBMIT\" VALUE=\"Continue\"></NOSCRIPT></form>")
                .append("</body></html>").toString();
    }
}