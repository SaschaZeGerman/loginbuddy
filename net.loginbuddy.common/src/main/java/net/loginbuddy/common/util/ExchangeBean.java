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

    private JSONObject userinfo, idTokenPayload, tokenResponse;

    public ExchangeBean() {
        userinfo = new JSONObject();
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

    @Override
    public String toString() {

        JSONObject details_provider = new JSONObject();
        JSONObject details_loginbuddy = new JSONObject();

        details_provider.put("provider", provider);

        if(userinfo != null)
            details_provider.put("userinfo", userinfo);
        else
            details_provider.put("userinfo", new JSONObject());

        if(idTokenPayload != null)
            details_provider.put("id_token_payload", idTokenPayload);
        else
            details_provider.put("id_token_payload", new JSONObject());

        tokenResponse.put("details_provider", details_provider);

        details_loginbuddy.put("iss", iss);

        if(nonce != null)
            details_loginbuddy.put("nonce", nonce);

        details_loginbuddy.put("iat", iat);
        details_loginbuddy.put("aud", aud);

        tokenResponse.put("details_loginbuddy", details_loginbuddy);

        return tokenResponse.toJSONString();
    }
}