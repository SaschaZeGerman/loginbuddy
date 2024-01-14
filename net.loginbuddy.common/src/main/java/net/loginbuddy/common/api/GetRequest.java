package net.loginbuddy.common.api;

import net.loginbuddy.common.util.Jwt;
import org.apache.http.client.methods.HttpGet;

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

    public GetRequest setDpopHeader(String alg, String targetApi, String accessToken, String nonce) throws Exception {
        addHeader(req, "dpop", Jwt.DEFAULT.createDpopProof(alg, "GET", targetApi, accessToken, nonce).getCompactSerialization());
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

    public HttpGet build() {
        return this.req;
    }

}
