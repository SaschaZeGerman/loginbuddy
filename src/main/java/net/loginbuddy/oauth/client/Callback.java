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
import net.loginbuddy.oauth.util.ExchangeBean;
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
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException  {

        // handle provider response with 'code'
        try {

            String session = request.getParameter(Constants.STATE.getKey());
            if (session == null || session.trim().length() == 0 || request.getParameterValues(Constants.STATE.getKey()).length > 1) {
                response.sendError(400, "Missing or invalid state parameter");
                return;
            }

            Map<String, Object> sessionValues = (Map<String, Object>) LoginbuddyCache.getInstance().getCache().get(session);
            if (sessionValues == null || !session.equals(sessionValues.get(Constants.SESSION.getKey()))) {
                LOGGER.severe("The given state does not match the expected one!");
                response.sendError(400, "The current session is invalid or it has expired!");
                return;
            }

            String clientRedirectUri = (String)sessionValues.get(Constants.CLIENT_REDIRECT.getKey());
            String clientState = (String)sessionValues.get(Constants.CLIENT_STATE.getKey());

            String error = request.getParameter(Constants.ERROR.getKey());
            if(error != null) {
                String errorDescription = request.getParameter("error_description");
                if(errorDescription == null) {
                    errorDescription = "An error was returned by the provider without any description";
                }
                error = URLEncoder.encode(error, "UTF-8");
                errorDescription = URLEncoder.encode(errorDescription, "UTF-8");
                if(clientRedirectUri.contains("?")) {
                    clientRedirectUri = clientRedirectUri.concat("&");

                } else {
                    clientRedirectUri = clientRedirectUri.concat("?");
                }
                clientRedirectUri = clientRedirectUri.concat("error=").concat(error).concat("&error_description=").concat(errorDescription).concat("&state=").concat(clientState);
                response.sendRedirect(clientRedirectUri);
                return;
            }

            String authCode = request.getParameter(Constants.CODE.getKey());

            String provider = (String)sessionValues.get(Constants.CLIENT_PROVIDER.getKey());
            String code_verifier = (String)sessionValues.get(Constants.CODE_VERIFIER.getKey());

            ProviderConfig providerConfig = LoginbuddyConfig.getInstance().getConfigUtil().getProviderConfigByProvider(provider);

            String tokenEndpoint = null;
            String userInfoEndpoint = null;
            if(providerConfig.getOpenidConfigurationUri() != null) {
                tokenEndpoint = (String)sessionValues.get(Constants.TOKEN_ENDPOINT.getKey());
                userInfoEndpoint = (String)sessionValues.get(Constants.USERINFO_ENDPOINT.getKey());
            } else {
                tokenEndpoint = providerConfig.getTokenEndpoint();
                userInfoEndpoint = providerConfig.getUserinfoEndpoint();
            }

            JSONObject tokenResponse = codeTokenExchange(providerConfig.getClientId(), providerConfig.getClientSecret(), providerConfig.getRedirectUri(), authCode, tokenEndpoint, code_verifier);

            String id_token = tokenResponse.get("id_token").toString();

            String userinfo = "{}";
            if(!"".equals(userInfoEndpoint)) {
                String access_token = tokenResponse.get("access_token").toString();
                userinfo = getUserInfo(access_token, userInfoEndpoint).toJSONString();
            }

//            ExchangeBean eb = new ExchangeBean();
//            eb.setIss("https://".concat(System.getenv("HOSTNAME_LOGINBUDDY")));
//            eb.setIat(new Date().getTime());
//            eb.setAud("a_client_id");
//            eb.setNonce("a_nonce_value");
//            eb.setProvider(provider);
//            eb.setUserinfo(userinfo);
//            eb.setIdToken(id_token);
//            eb.setIdTokenPayload("an_id_token_payload");

            String responsePostForm = buildAutoForm(id_token, Base64.getEncoder().encodeToString(userinfo.getBytes()),clientRedirectUri,clientState);
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
            List<NameValuePair> formParameters = new ArrayList<NameValuePair>();
            formParameters.add(new BasicNameValuePair(Constants.CODE.getKey(), authCode));
            formParameters.add(new BasicNameValuePair(Constants.CLIENT_ID.getKey(), clientId));
            formParameters.add(new BasicNameValuePair(Constants.CLIENT_SECRET.getKey(), clientSecret));
            formParameters.add(new BasicNameValuePair(Constants.REDIRECT_URI.getKey(), redirectUri));
            formParameters.add(new BasicNameValuePair(Constants.GRANT_TYPE.getKey(), Constants.AUTHORIZATION_CODE.getKey()));
            formParameters.add(new BasicNameValuePair(Constants.CODE_VERIFIER.getKey(), codeVerifier));

            // Post request
            HttpEntity httpEntity = new UrlEncodedFormEntity(formParameters);
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
            String s = EntityUtils.toString(response.getEntity());
            return (JSONObject)new JSONParser().parse(s);

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