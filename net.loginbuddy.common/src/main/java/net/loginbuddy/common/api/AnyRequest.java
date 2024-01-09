package net.loginbuddy.common.api;

import net.loginbuddy.common.config.Constants;
import org.apache.http.client.methods.HttpRequestBase;

import java.util.logging.Logger;

public abstract class AnyRequest {

    private static final Logger LOGGER = Logger.getLogger(AnyRequest.class.getName());

    protected void addHeader(HttpRequestBase rb, String header, String value) {
        rb.setHeader(header, value);
    }

    protected void setBearerAccessToken(HttpRequestBase rb, String accessToken) {
        if (accessToken != null) {
            rb.setHeader(Constants.AUTHORIZATION.getKey(), String.format("%s %s", Constants.BEARER.getKey(), accessToken));
        } else {
            LOGGER.warning("The given access_token is null, the request may fail!");
        }
    }

}
