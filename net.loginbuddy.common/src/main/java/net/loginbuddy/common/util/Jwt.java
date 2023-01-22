/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.common.util;

import net.loginbuddy.common.config.Constants;
import net.loginbuddy.common.config.JwsAlgorithm;
import net.loginbuddy.common.storage.LoginbuddyStorage;
import org.jose4j.jwa.AlgorithmConstraints;
import org.jose4j.jwk.*;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.EllipticCurves;
import org.jose4j.keys.resolvers.JwksVerificationKeyResolver;
import org.jose4j.keys.resolvers.VerificationKeyResolver;
import org.jose4j.lang.JoseException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.util.*;
import java.util.logging.Logger;

/**
 * Implementation details taken from {@link "<a href="https://bitbucket.org/b_c/jose4j/wiki/Home">https://bitbucket.org/b_c/jose4j/wiki/Home</a>"}
 */
public enum Jwt {

    DEFAULT;

    private final Logger LOGGER = Logger.getLogger(Jwt.class.getName());

    Jwt() {
    }

    /**
     * Loginbuddys keys
     *
     * @return
     */
    public JsonWebKeySet getJwksForSigning() {
        JsonWebKeySet jwks = new JsonWebKeySet();
        jwks.addJsonWebKey(getKeyForAlgorithm(JwsAlgorithm.RS256));
        jwks.addJsonWebKey(getKeyForAlgorithm(JwsAlgorithm.ES256));
        return jwks;

    }

    /**
     * Generate a JWT based on a private key.
     *
     * @return JWT compact URL-safe serialization
     * @see <a href="https://bitbucket.org/b_c/jose4j/wiki/JWT%20Examples#markdown-header-producing-and-consuming-a-signed-jwt"></a>
     */
    public JsonWebSignature createSignedJwtRs256(String issuer, String audience, int lifetimeInMinutes, String subject, String nonce, boolean includePublicKey) throws Exception {
        Map<String, String> claims = new HashMap<>();
        claims.put(Constants.NONCE.getKey(), nonce);
        return createSignedJwtRs256(issuer, audience, lifetimeInMinutes, subject, includePublicKey, claims);
    }

    public JsonWebSignature createSignedJwtRs256(String issuer, String audience, int lifetimeInMinutes, String subject, boolean includePublicKey, Map<String, String> additionalClaims) throws Exception {
        JwtClaims claims = createJwtPayload(issuer, audience, lifetimeInMinutes, subject, additionalClaims);
        if (includePublicKey) {
            claims.setClaim("sub_jwk", new JSONParser().parse(getKeyForAlgorithm(JwsAlgorithm.RS256).toJson()));
        }
        return createSignedJwt(claims.toJson(), JwsAlgorithm.RS256);
    }

    public JsonWebSignature createSignedJwtEs256(String issuer, String audience, int lifetimeInMinutes, String subject, String nonce, boolean includePublicKey) throws Exception {
        Map<String, String> claims = new HashMap<>();
        claims.put(Constants.NONCE.getKey(), nonce);
        return createSignedJwtEs256(issuer, audience, lifetimeInMinutes, subject, includePublicKey, claims);
    }

    public JsonWebSignature createSignedJwtEs256(String issuer, String audience, int lifetimeInMinutes, String subject, boolean includePublicKey, Map<String, String> additionalClaims) throws Exception {
        JwtClaims claims = createJwtPayload(issuer, audience, lifetimeInMinutes, subject, additionalClaims);
        if (includePublicKey) {
            claims.setClaim("sub_jwk", new JSONParser().parse(getKeyForAlgorithm(JwsAlgorithm.ES256).toJson()));
        }
        return createSignedJwt(claims.toJson(), JwsAlgorithm.ES256);
    }

    /**
     * Generate a JWT based on a private key. Nothing fancy, using the example implementation of jose4j
     *
     * @return JWT compact URL-safe serialization
     * @see <a href="https://bitbucket.org/b_c/jose4j/wiki/JWT%20Examples#markdown-header-producing-and-consuming-a-signed-jwt"></a>
     */
    public JsonWebSignature createSignedJwt(String payload, JwsAlgorithm alg) {
        JwsAlgorithm algToUse = alg == null || getKeyForAlgorithm(alg) == null ? JwsAlgorithm.RS256 : alg;
        PublicJsonWebKey jwk = getKeyForAlgorithm(algToUse);
        JsonWebSignature jws = new JsonWebSignature();
        jws.setPayload(payload);
        jws.setKey(jwk.getPrivateKey());
        jws.setKeyIdHeaderValue(jwk.getKeyId());
        jws.setAlgorithmHeaderValue(algToUse.toString());
        return jws;
    }

    /**
     * Validates an id_token with standard claims and optional others
     *
     * @param jwt               the compact serialized jwt (aaaa.bbbb.ccccc)
     * @param jsonWebKeySetJson the jwks that includes the key to be used for signature validation
     * @param expectedIss       expected issuer
     * @param expectedAud       expected audience
     * @param expectedNonce     expected nonce
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
                            AlgorithmConstraints.ConstraintType.PERMIT,
                            AlgorithmIdentifiers.RSA_USING_SHA256,
                            AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256
                    )
                    .build();
            payload = (JSONObject) new JSONParser().parse(jwtConsumer.processToClaims(jwt).toJson());

            // do additional validations

            if (expectedNonce.equals(payload.get(Constants.NONCE.getKey()))) {
                return payload;
            } else {
                LOGGER.warning(String.format("Unexpected nonce. Expected: '%s', actual: '%s'", expectedNonce, payload.get(Constants.NONCE.getKey())));
            }
        } catch (JoseException e) {
            LOGGER.warning(e.getMessage());
        } catch (Exception e) {
            LOGGER.warning(String.format("The given id_token could not be validated! Error: '%s'", e.getMessage()));
        }
        throw new IllegalArgumentException("The given id_token could not be validated!");
    }

    private JwtClaims createJwtPayload(String issuer, String audience, int lifetimeInMinutes, String subject, Map<String, String> additionalClaims) {
        // Create the Claims, which will be the content of the JWT
        JwtClaims claims = new JwtClaims();
        claims.setIssuer(issuer);  // who creates the token and signs it
        claims.setSubject(subject); // the subject/principal is whom the token is about
        claims.setAudience(audience); // to whom the token is intended to be sent
        claims.setExpirationTimeMinutesInTheFuture(lifetimeInMinutes); // time when the token will expire
        claims.setIssuedAtToNow();  // when the token was issued/created (now)
        claims.setGeneratedJwtId(); // a unique identifier for the token
        claims.setNotBeforeMinutesInThePast(2); // time before which the token is not yet valid (2 minutes ago)
        for (String claim : additionalClaims.keySet()) {
            claims.setClaim(claim, additionalClaims.get(claim));
        }
        return claims;
    }

    private PublicJsonWebKey getKeyForAlgorithm(JwsAlgorithm algorithm) {
        PublicJsonWebKey jwk = (PublicJsonWebKey) LoginbuddyStorage.STORAGE.get(String.format("jwks_%s", algorithm));
        if(jwk == null) {
            if (AlgorithmIdentifiers.RSA_USING_SHA256.equalsIgnoreCase(algorithm.toString())) {
                try {
                    jwk = RsaJwkGenerator.generateJwk(2048);
                    jwk.setAlgorithm(AlgorithmIdentifiers.RSA_USING_SHA256);
                } catch (JoseException e) {
                    // should never happen
                    LOGGER.severe(String.format("Should never happen: %s", e.getMessage()));
                    return null;
                }
            } else {
                try {
                    jwk = EcJwkGenerator.generateJwk(EllipticCurves.P256);
                    jwk.setAlgorithm(AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256);
                } catch (JoseException e) {
                    // should never happen
                    LOGGER.severe(String.format("Should never happen: %s", e.getMessage()));
                    return null;
                }
            }
            jwk.setKeyId(UUID.randomUUID().toString());
            jwk.setUse("sig");
            LoginbuddyStorage.STORAGE.putIfNull(String.format("jwks_%s", algorithm), jwk);
        }
        return jwk;
    }
}