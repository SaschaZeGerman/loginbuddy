package net.loginbuddy.common.api;

import net.loginbuddy.common.util.Jwt;
import org.apache.http.client.methods.HttpGet;

import java.util.HashMap;

public class GetRequest extends AnyRequest {

    private HttpGet req;
    private GetRequest(String targetApi) {
        req = new HttpGet(targetApi);
    }

    public static GetRequest create(String targetApi) {
        return new GetRequest(targetApi);
    }

    public GetRequest addHeader(String header, String value) {
        addHeader(req, header, value);
        return this;
    }

    public GetRequest setDpopHeader(String alg, String targetApi, String accessToken, String dpopNonce, String dpopNonceProvider) throws Exception {
        return setDpopHeader(alg, targetApi, accessToken, dpopNonce, dpopNonceProvider, new HashMap<>());
    }

    public GetRequest setDpopHeader(String alg, String targetApi, String accessToken, String dpopNonce, String dpopNonceProvider, HashMap<String, String> additionalClaims) throws Exception {
        addDpopHeader(req, Jwt.DEFAULT.createDpopProof(alg, "GET", targetApi, accessToken, dpopNonce, dpopNonceProvider, additionalClaims).getCompactSerialization());
        return this;
    }

    public GetRequest setBearerAccessToken(String accessToken) {
        setBearerAccessToken(req, accessToken);
        return this;
    }

    public GetRequest setAccessToken(String scheme, String accessToken) {
        setAccessToken(req, scheme, accessToken);
        return this;
    }

    public GetRequest setAcceptType(String acceptContentType) {
        addHeader(req, "Accept", acceptContentType);
        return this;
    }

    public HttpGet build() {
        return this.req;
    }

}
