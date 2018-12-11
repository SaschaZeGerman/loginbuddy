/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.oauth.util;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.Serializable;
import java.util.logging.Logger;

public class ExchangeBean implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(ExchangeBean.class));

    private String iss, aud, nonce, provider, idToken;
    private JSONObject userinfo;
    private long iat;

    public ExchangeBean() {
        userinfo = new JSONObject();
    }

    public String getIss() {
        return iss;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    public String getAud() {
        return aud;
    }

    public void setAud(String aud) {
        this.aud = aud;
    }

    public String getNonce() {
        return nonce;
    }

    public void setNonce(String nonce) {
        this.nonce = nonce;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public JSONObject getUserinfo() {
        return userinfo;
    }

    public void setUserinfo(JSONObject userinfo) {
        this.userinfo = userinfo;
    }

    public String getIdToken() {
        return idToken;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public long getIat() {
        return iat;
    }

    public void setIat(long iat) {
        this.iat = iat;
    }

    @Override
    public String toString() {

        JSONObject output = new JSONObject();
        JSONObject data = new JSONObject();

        data.put("provider", provider);
        data.put("userinfo", userinfo);
        data.put("id_token", idToken);
        data.put("id_token_payload", getIdTokenPayload(idToken));

        output.put("iss", iss);
        output.put("iat", iat);
        output.put("aud", aud);
        output.put("nonce", nonce);
        output.put("data", data);

        return output.toJSONString();
    }

    // TODO: implement
    private JSONObject getIdTokenPayload(String idToken) {
        String idTokenPayload = "{\"id_token\":\"payload\"}";
        try {
            return (JSONObject)new JSONParser().parse(idTokenPayload);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }
}