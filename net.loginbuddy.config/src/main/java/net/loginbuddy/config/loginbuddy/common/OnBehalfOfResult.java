package net.loginbuddy.config.loginbuddy.common;

import org.json.simple.JSONObject;

public class OnBehalfOfResult {

    private String idToken;
    private JSONObject idTokenPayload;

    public OnBehalfOfResult(String idToken, JSONObject idTokenPayload) {
        this.idToken = idToken;
        this.idTokenPayload = idTokenPayload;
    }

    public String getIdToken() {
        return idToken;
    }

    public JSONObject getIdTokenPayload() {
        return idTokenPayload;
    }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public void setIdTokenPayload(JSONObject idTokenPayload) {
        this.idTokenPayload = idTokenPayload;
    }
}
