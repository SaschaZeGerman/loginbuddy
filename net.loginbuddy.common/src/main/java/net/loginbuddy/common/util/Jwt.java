/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.common.util;

import net.loginbuddy.common.config.Constants;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.*;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.lang.JoseException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.crypto.spec.SecretKeySpec;
import java.util.*;
import java.util.logging.Logger;

/**
 * Implementation details taken from {@link "https://bitbucket.org/b_c/jose4j/wiki/Home"}
 */
public enum Jwt {

    DEFAULT;

    private final Logger LOGGER = Logger.getLogger(String.valueOf(Jwt.class));

    JsonWebKeySet jwks;

    Jwt() {
        // Generate an RSA key pair, which will be used for signing and verification of Loginbuddy JWTs, wrapped in a JWK
        jwks = new JsonWebKeySet();
        RsaJsonWebKey rsaJsonWebKey = null;
        try {
            rsaJsonWebKey = RsaJwkGenerator.generateJwk(2048);
        } catch (JoseException e) {
            LOGGER.warning(String.format("Should never happen! Error: '%s'", e.getMessage()));
        }
        rsaJsonWebKey.setKeyId(UUID.randomUUID().toString());
        rsaJsonWebKey.setUse("sig");
        jwks.addJsonWebKey(rsaJsonWebKey);
    }

    /**
     * Loginbuddys keys
     * @return
     */
    public JsonWebKeySet getJwksForSigning() {
        return jwks;
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
     * Generate a JWT based on a private key.
     *
     * @return JWT compact URL-safe serialization
     * @see <a href="https://bitbucket.org/b_c/jose4j/wiki/JWT%20Examples#markdown-header-producing-and-consuming-a-signed-jwt"></a>
     */
    public JsonWebSignature createSignedJwtRs256(String issuer, String audience, int lifetimeInMinutes, String subject, String nonce, boolean includePublicKey) throws Exception {
        Map<String, String> claims = new HashMap<>();
        claims.put("nonce", nonce);
        return createSignedJwtRs256(issuer, audience, lifetimeInMinutes, subject, includePublicKey, claims);
    }

    public JsonWebSignature createSignedJwtRs256(String issuer, String audience, int lifetimeInMinutes, String subject, boolean includePublicKey, Map<String, String> additionalClaims) throws Exception {

        // Generate an RSA key pair, which will be used for signing and verification of the JWT, wrapped in a JWK
        RsaJsonWebKey rsaJsonWebKey = (RsaJsonWebKey) DEFAULT.getJwksForSigning().getJsonWebKeys().get(0);

        // Create the Claims, which will be the content of the JWT
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(issuer);  // who creates the token and signs it
        claims.setSubject(subject); // the subject/principal is whom the token is about
        claims.setAudience(audience); // to whom the token is intended to be sent
        claims.setExpirationTimeMinutesInTheFuture(lifetimeInMinutes); // time when the token will expire
        claims.setIssuedAtToNow();  // when the token was issued/created (now)
        claims.setGeneratedJwtId(); // a unique identifier for the token
        claims.setNotBeforeMinutesInThePast(2); // time before which the token is not yet valid (2 minutes ago)
        if (includePublicKey) {
            claims.setClaim("sub_jwk", new JSONParser().parse(rsaJsonWebKey.toJson()));
        }
        for (String claim : additionalClaims.keySet()) {
            claims.setClaim(claim, additionalClaims.get(claim));
        }
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(claims.toJson());
        jws.setKey(rsaJsonWebKey.getPrivateKey());
        jws.setKeyIdHeaderValue(rsaJsonWebKey.getKeyId());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        return jws;

    }

    /**
     * Generate a JWT based on a private key. Nothing fancy, using the example implementation of jose4j
     *
     * @return JWT compact URL-safe serialization
     * @see <a href="https://bitbucket.org/b_c/jose4j/wiki/JWT%20Examples#markdown-header-producing-and-consuming-a-signed-jwt"></a>
     */
    public JsonWebSignature createSignedJwt(String payload, String alg) {
        // TODO algorithm is currently ignored
        // Generate an RSA key pair, which will be used for signing and verification of the JWT, wrapped in a JWK
        RsaJsonWebKey rsaJsonWebKey = (RsaJsonWebKey) DEFAULT.getJwksForSigning().getJsonWebKeys().get(0);

        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(payload);
        jws.setKey(rsaJsonWebKey.getPrivateKey());
        jws.setKeyIdHeaderValue(rsaJsonWebKey.getKeyId());
        jws.setAlgorithmHeaderValue(AlgorithmIdentifiers.RSA_USING_SHA256);
        return jws;
    }

    /**
     * Validates an id_token with standard claims and optional others
     * @param jwt the compactsecrialized jwt (aaaa.bbbb.ccccc)
     * @param jsonWebKeySetJson the jwks that includes the key to be used for signature validation
     * @param expectedIss expected issuer
     * @param expectedAud expected audience
     * @param expectedNonce expected nonce
     * @return the json payload
     */
    public JSONObject validateIdToken(String jwt, String jsonWebKeySetJson, String expectedIss, String expectedAud, String expectedNonce) {

        if (jwt == null || expectedIss == null || expectedAud == null || expectedNonce == null) {
            LOGGER.warning("All parameters are required! Verify that neither empty nor null values have been used!");
            throw new IllegalArgumentException("All parameters are required!");
        }

        try {

            JsonWebSignature jws = new JsonWebSignature();
            jws.setCompactSerialization(jwt);
            JSONObject payload = (JSONObject) new org.json.simple.parser.JSONParser().parse(jws.getUnverifiedPayload());

            // 'sub_jwk' in Loginbuddy is only used if issuer == self-issued
            // more details see: https://openid.net/specs/openid-connect-core-1_0.html#SelfIssuedResponse

            VerificationKeyResolver resolver;
            if (payload.get("sub_jwk") != null && Constants.ISSUER_SELFISSUED.getKey().equalsIgnoreCase(expectedIss)) {
                List<JsonWebKey> keys = new ArrayList<>();
                keys.add(JsonWebKey.Factory.newJwk(((JSONObject) payload.get("sub_jwk")).toJSONString()));
                resolver = new JwksVerificationKeyResolver(keys);
            } else {
                if (jsonWebKeySetJson != null) {
                    resolver = new JwksVerificationKeyResolver(new JsonWebKeySet(jsonWebKeySetJson).getJsonWebKeys());
                } else {
                    LOGGER.warning("id_token validation failed due to missing jwk(s)!");
                    throw new IllegalArgumentException("Missing JWKS!");
                }
            }
            JwtConsumer jwtConsumer = new JwtConsumerBuilder()
                    .setRequireExpirationTime()
                    .setAllowedClockSkewInSeconds(0)
                    .setRequireSubject()
                    .setExpectedAudience(expectedAud)
                    .setExpectedIssuer(expectedIss)
                    .setVerificationKeyResolver(resolver)
                    .setJwsAlgorithmConstraints(
                            AlgorithmConstraints.ConstraintType.WHITELIST,
                            AlgorithmIdentifiers.RSA_USING_SHA256
                    )
                    .build();
            payload = (JSONObject) new JSONParser().parse(jwtConsumer.processToClaims(jwt).toJson());

            // do additional validations

            if (expectedNonce.equals(payload.get("nonce"))) {
                return payload;
            } else {
                LOGGER.warning("Unexpected nonce. Expected: '" + expectedNonce + "', actual: '" + payload.get("nonce") + "'");
            }
        } catch (JoseException e) {
            LOGGER.warning(e.getMessage());
        } catch (Exception e) {
            LOGGER.warning(String.format("The given id_token could not be validated! Error: '%s'", e.getMessage()));
        }
        throw new IllegalArgumentException("The given id_token could not be validated!");
    }

    /**
     * Validates the signature, aud, iss, exp and nonce of a given JWT. Other values have to be verified on demand
     *
     * @param jwt               The JWT to be validated
     * @param jsonWebKeySetJson The JSON Web Key Set as a JSON string. This may be null if the JWT includes the 'sub_jwk' claim
     * @param expectedIss       Expected issuer of the JWT
     * @param expectedAud       Expected audience
     * @param expectedNonce     Expected nonce
     */
    @Deprecated
    public JSONObject validateJwt(String jwt, String jsonWebKeySetJson, String expectedIss, String expectedAud, String expectedNonce) {
        if (jwt == null || expectedIss == null || expectedAud == null || expectedNonce == null) {
            LOGGER.warning("All parameters are required! Verify that neither empty nor null values have been used!");
            throw new IllegalArgumentException("All parameters are required!");
        }
        try {

            JsonWebSignature jws = new JsonWebSignature();
            jws.setCompactSerialization(jwt);
            JSONObject jo = (JSONObject) new org.json.simple.parser.JSONParser().parse(jws.getUnverifiedPayload());

            // simple string comparisons first
            if (expectedIss.equals(jo.get("iss"))) {
                if (validateAud(expectedAud, jo.get("aud"))) {
                    if (expectedNonce.equals(jo.get("nonce"))) {
                        if ((new Date().getTime() / 1000) < Long.parseLong(String.valueOf(jo.get("exp")))) {
                            JsonWebKey jwk = null;
                            if (jo.get("sub_jwk") != null && jsonWebKeySetJson == null) {
                                jwk = JsonWebKey.Factory.newJwk(((JSONObject) jo.get("sub_jwk")).toJSONString());
                            } else {
                                JsonWebKeySet jsonWebKeySet = new JsonWebKeySet(jsonWebKeySetJson);
                                VerificationJwkSelector jwkSelector = new VerificationJwkSelector();
                                jwk = jwkSelector.select(jws, jsonWebKeySet.getJsonWebKeys());
                            }
                            jws.setKey(jwk.getKey());
                            String jwkAlg = jwk.getAlgorithm() == null ? "RS256" : jwk.getAlgorithm(); // since 'alg' in JWKS is optional, we use RS256 as the default value
                            if (jwkAlg.equalsIgnoreCase(jws.getAlgorithmHeaderValue())) {
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
            LOGGER.warning("No sure what went wrong ... check the stacktrace below ... ");
            e.printStackTrace();
            throw new IllegalArgumentException("The given JWT could not be parsed or decoded!");
        }
        throw new IllegalArgumentException("The given JWT could not be parsed or decoded!");
    }

    @Deprecated
    private boolean validateAud(String expectedAud, Object actualAud) {
        if (expectedAud == null || actualAud == null) {
            return false;
        }
        if (actualAud instanceof JSONArray) {
            JSONArray actualAudiences = (JSONArray) actualAud;
            for (Object actualAudience : actualAudiences) {
                if (expectedAud.equals(actualAudience)) {
                    return true;
                }
            }
            return false;
        }
        return expectedAud.equals(actualAud);
    }
}