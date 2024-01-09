package net.loginbuddy.common.api;

import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.util.Jwt;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.logging.Logger;

public class PostRequest extends AnyRequest {

    private static final Logger LOGGER = Logger.getLogger(PostRequest.class.getName());

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

    public PostRequest setDpopHeaderRS256(String method, String targetApi, String accessToken, String nonce) throws Exception {
        addHeader(req, "dpop", Jwt.DEFAULT.createDpopProofRS256(method, targetApi, accessToken, nonce).getCompactSerialization());
        return this;
    }

    public PostRequest setBearerAccessToken(String accessToken) {
        setBearerAccessToken(req, accessToken);
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
