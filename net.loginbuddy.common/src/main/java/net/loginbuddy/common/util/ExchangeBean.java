/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.common.util;

import org.json.simple.JSONObject;

import java.io.Serializable;
import java.util.logging.Logger;

/**
 * The data model that is returned to the client (application)
 */
public class ExchangeBean implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(ExchangeBean.class));

    private String iss, aud, nonce, provider, idToken;
    private JSONObject userinfo, idTokenPayload;
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

    public JSONObject getIdTokenPayload() {
        return idTokenPayload;
    }

    public void setIdTokenPayload(JSONObject idTokenPayload) {
        this.idTokenPayload = idTokenPayload;
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
        data.put("id_token_payload", idTokenPayload);

        output.put("iss", iss);
        output.put("iat", iat);
        output.put("aud", aud);
        output.put("nonce", nonce);
        output.put("data", data);

        return output.toJSONString();
    }
}