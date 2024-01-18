package net.loginbuddy.common.api;

import net.loginbuddy.common.util.Jwt;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;

public class PostRequest extends AnyRequest {

    private HttpPost req;
    private PostRequest(String targetApi) {
        req = new HttpPost(targetApi);
    }

    public static PostRequest create(String targetApi) {
        return new PostRequest(targetApi);
    }

    public PostRequest addHeader(String header, String value) {
        addHeader(req, header, value);
        return this;
    }

    public PostRequest setDpopHeader(String alg, String targetApi, String accessToken, String dpopNonce, String dpopNonceProvider) throws Exception {
        return setDpopHeader(alg, targetApi, accessToken, dpopNonce, dpopNonceProvider, new HashMap<>());
    }

    public PostRequest setDpopHeader(String alg, String targetApi, String accessToken, String dpopNonce, String dpopNonceProvider, HashMap<String, String> additionalClaims) throws Exception {
        addDpopHeader(req, Jwt.DEFAULT.createDpopProof(alg, "POST", targetApi, accessToken, dpopNonce, dpopNonceProvider, additionalClaims).getCompactSerialization());
        return this;
    }

    public PostRequest setBearerAccessToken(String accessToken) {
        setBearerAccessToken(req, accessToken);
        return this;
    }

    public PostRequest setAccessToken(String scheme, String accessToken) {
        setAccessToken(req, scheme, accessToken);
        return this;
    }

    public PostRequest setContentType(String contentType) {
        addHeader(req, "Content-Type", contentType);
        return this;
    }

    public PostRequest setAcceptType(String acceptContentType) {
        addHeader(req, "Accept", acceptContentType);
        return this;
    }

    /**
     * The method also sets content-type application/json. To overwrite call setContentType(...)
     * @param jsonString The request body as JSON string
     * @return
     */
    public PostRequest setJsonPayload(String jsonString) {
        StringEntity requestEntity = new StringEntity(jsonString, "UTF-8");
        requestEntity.setContentType("application/json");
        req.setEntity(requestEntity);
        setContentType("application/json");
        return this;
    }

    /**
     * The method also sets content-type application/x-www-form-urlencoded. To overwrite call setContentType(...)
     * @param formParameters Basic parameters that ARE NOT URLEncoded!
     * @return
     * @throws UnsupportedEncodingException
     */
    public PostRequest setFormParametersPayload(List<NameValuePair> formParameters) throws UnsupportedEncodingException {
        req.setEntity(new UrlEncodedFormEntity(formParameters));
        return this;
    }

    /**
     * The method also sets content-type application/x-www-form-urlencoded. To overwrite call setContentType(...)
     * @param queryString Keys and values MUST BE URLEncoded!
     * @return
     * @throws UnsupportedEncodingException
     */
    public PostRequest setUrlEncodedParametersPayload(String queryString) throws UnsupportedEncodingException {
        req.setEntity(new StringEntity(queryString));
        setContentType("application/x-www-form-urlencoded");
        return this;
    }

    public HttpPost build() {
        return this.req;
    }
}
