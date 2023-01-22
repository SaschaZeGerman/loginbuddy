package net.loginbuddy.config.loginbuddy.common;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import net.loginbuddy.common.config.JwsAlgorithm;
import net.loginbuddy.common.util.Jwt;
import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.loginbuddy.Clients;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.lang.JoseException;
import org.json.simple.JSONObject;

import java.io.Serializable;

public class OnBehalfOf implements Serializable {

    @JsonProperty("token_type")
    @JsonIgnore(false)
    public String tokenType;

    @JsonProperty("alg")
    public String alg;

    public OnBehalfOf() {
        alg = AlgorithmIdentifiers.RSA_USING_SHA256;
    }

    public OnBehalfOf(String tokenType, String alg) {
        this();
        if(alg != null) {
            this.alg = alg;
        }
        this.tokenType = tokenType;
    }

    public String getTokenType() {
        return tokenType;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getAlg() {
        return alg;
    }

    public void setAlg(String alg) {
        this.alg = alg;
    }

    /**
     *
     * @param clientId
     * @param clientNonce
     * @param tokenType
     * @param idTokenPayload
     * @param idToken
     * @return an id_token signed by Loginbuddy
     */
    public static OnBehalfOfResult signOnBehalfOf(String clientId, String clientNonce, String tokenType, JSONObject idTokenPayload, String idToken) throws JoseException {

        OnBehalfOfResult result = new OnBehalfOfResult(idToken, idTokenPayload);

        Clients currentClient = LoginbuddyUtil.UTIL.getClientConfigByClientId(clientId);
        if (currentClient != null) {
            for (OnBehalfOf obo : currentClient.getOnBehalfOf()) {
                if (obo.getTokenType().equalsIgnoreCase(tokenType)) {
                    JSONObject onBehalfOf = new JSONObject();
                    onBehalfOf.put("iss", idTokenPayload.get("iss"));
                    onBehalfOf.put("aud", idTokenPayload.get("aud"));
                    onBehalfOf.put("nonce", idTokenPayload.get("nonce"));
                    idTokenPayload.put("on_behalf_of", onBehalfOf);
                    idTokenPayload.put("iss", DiscoveryUtil.UTIL.getIssuer());
                    idTokenPayload.put("aud", currentClient.getClientId());
                    idTokenPayload.put("nonce", clientNonce);
                    // obo.getAlg() return the value that was configured in config.json for this client in on_behalf_of. Falling back to RS256 if the given value is not supported
                    result.setIdToken(Jwt.DEFAULT.createSignedJwt(idTokenPayload.toJSONString(), JwsAlgorithm.valueOf(obo.getAlg())).getCompactSerialization());
                    result.setIdTokenPayload(idTokenPayload);
                    break;
                }
            }
        }
        return result;
    }
}
