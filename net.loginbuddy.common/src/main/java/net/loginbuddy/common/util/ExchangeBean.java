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

    private String iss, aud, nonce, provider;
    private long iat;

    private JSONObject userinfo, idTokenPayload, tokenResponse, normalized;

    public ExchangeBean() {
        userinfo = new JSONObject();
        tokenResponse = new JSONObject();
    }

    public void setTokenResponse(JSONObject tokenResponse) {
        this.tokenResponse = tokenResponse;
    }

    public void setIss(String iss) {
        this.iss = iss;
    }

    public void setAud(String aud) {
        this.aud = aud;
    }

    public void setNormalized(JSONObject normalized) {
        this.normalized = normalized;
    }

    public void setNonce(Object nonce) {
        this.nonce = nonce == null ? null : nonce.toString();
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public void setUserinfo(JSONObject userinfo) {
        this.userinfo = userinfo;
    }

    public void setIdTokenPayload(JSONObject idTokenPayload) {
        this.idTokenPayload = idTokenPayload;
    }

    public void setIat(long iat) {
        this.iat = iat;
    }

    public JSONObject getEbAsJson() {
        JSONObject details_provider = new JSONObject();
        JSONObject details_loginbuddy = new JSONObject();

        details_provider.put("provider", provider);

        details_provider.put("userinfo", userinfo);

        details_provider.put("id_token_payload", idTokenPayload);

        details_loginbuddy.put("iss", iss);

        if (nonce != null) {
            details_loginbuddy.put("nonce", nonce);
        }

        details_loginbuddy.put("iat", iat);
        details_loginbuddy.put("aud", aud);

        tokenResponse.put("details_provider", details_provider);
        tokenResponse.put("details_loginbuddy", details_loginbuddy);

        if (normalized != null)
            tokenResponse.put("details_normalized", normalized);

        return tokenResponse;
    }

    @Override
    public String toString() {
        return getEbAsJson().toJSONString();
    }
}