/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.util;

import java.util.Date;
import java.util.logging.Logger;

import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jwk.VerificationJwkSelector;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.json.simple.JSONObject;

import javax.crypto.spec.SecretKeySpec;

/**
 * Implementation details taken from {@link "https://bitbucket.org/b_c/jose4j/wiki/Home"}
 *
 */
public class Jwt {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(Jwt.class));

    public Jwt() {
    }

    /**
     * Create a JWS. The secret will be used with HMAC-SHA256. The content may represent a JSON structure but maybe not.
     *
     * @param secret  The secret used to create the signature. MUST be at least 256bit long!
     * @param content The content that becomes the payload
     * @return The JWS object or null if something went wrong
     */
    public JsonWebSignature createSignedJwtHs256(byte[] secret, String content) {
        try {
            JsonWebSignature jws = new JsonWebSignature();
            jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.HMAC_SHA256);
            jws.setHeader("typ", "JWT");
            jws.setPayload(content);
            SecretKeySpec key = new SecretKeySpec(secret, AlgorithmIdentifiers.HMAC_SHA256);
            jws.setKey(key);
            return jws;
        } catch (Exception e) {
            LOGGER.warning("The JWT could not be created!");
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Validates the signature, aud, iss, exp and nonce of a given JWT. Other values have to be verified on demand
     *
     * @param jwt The JWT to be validated
     * @param jsonWebKeySetJson The JSON Web Key Set as a JSON string
     * @param expectedIss Expected issuer of the JWT
     * @param expectedAud Expected audience
     * @param expectedNonce Expected nonce
     */
    public JSONObject validateJwt(String jwt, String jsonWebKeySetJson, String expectedIss, String expectedAud, String expectedNonce) {
        if(jwt==null || jsonWebKeySetJson == null || expectedIss == null || expectedAud == null || expectedNonce == null) {
            LOGGER.warning("All parameters are required! Verify that neither empty nor null values have been used!");
            throw new IllegalArgumentException("All parameters are required!");
        }
        try {

            JsonWebSignature jws = new JsonWebSignature();
            jws.setCompactSerialization(jwt);
            JSONObject jo = (JSONObject)new org.json.simple.parser.JSONParser().parse(jws.getUnverifiedPayload());

            // simple string comparisons first
            if(expectedIss.equals(jo.get("iss"))) {
                if(expectedAud.equals(jo.get("aud"))) {
                    if(expectedNonce.equals(jo.get("nonce"))) {
                        if ((new Date().getTime() / 1000) < Long.parseLong(String.valueOf(jo.get("exp")))) {
                            JsonWebKeySet jsonWebKeySet = new JsonWebKeySet(jsonWebKeySetJson);
                            VerificationJwkSelector jwkSelector = new VerificationJwkSelector();
                            JsonWebKey jwk = jwkSelector.select(jws, jsonWebKeySet.getJsonWebKeys());
                            jws.setKey(jwk.getKey());
                            if(jwk.getAlgorithm().equals(jws.getAlgorithmHeaderValue())) {
                                if (jws.verifySignature()) {
                                    return jo;
                                } else {
                                    LOGGER.warning("The JWT signature is invalid!");
                                }
                            } else {
                                LOGGER.warning("The JWT has been signed with an unexpected algorithm!");
                            }
                        } else {
                            LOGGER.warning("The JWT has expired!");
                        }
                    } else {
                        LOGGER.warning("Unexpected nonce. Expected: '" + expectedNonce + "', actual: '" + jo.get("nonce") + "'");
                    }
                } else {
                    LOGGER.warning("Unexpected aud. Expected: '" + expectedAud + "', actual: '" + jo.get("aud") + "'");
                }
            } else {
                LOGGER.warning("Unexpected iss. Expected: '" + expectedIss + "', actual: '" + jo.get("iss") + "'");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        throw new IllegalArgumentException("The given JWT could not be parsed or decoded!");
    }
}