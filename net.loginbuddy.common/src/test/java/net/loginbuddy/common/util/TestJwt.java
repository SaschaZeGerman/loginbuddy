/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.common.util;

import net.loginbuddy.common.config.JwsAlgorithm;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class TestJwt {

    private static final Logger LOGGER_JWT = Logger.getLogger(Jwt.class.getName());

    private static String TestJwt, TestJwtExpired, TestJwtEs256, TestJwtExpiredEs256;
    private static JsonWebKeySet TestJwks;

    private String iss;
    private String aud;
    private String nonce;
    private LogRecord testRecord;

    @BeforeClass
    public static void setupClass() {
        TestJwks = Jwt.DEFAULT.getJwksForSigning();
        JSONObject jo = new JSONObject();
        jo.put("iss", "https://example.com/server");
        jo.put("sub", "subject");
        jo.put("aud", "audience");
        jo.put("nonce", "nonceInIdToken");
        jo.put("iat", Long.valueOf(String.valueOf(new Date().getTime()).substring(0, 10)));
        jo.put("exp", Long.valueOf(String.valueOf(new Date().getTime()+20000).substring(0, 10)));
        try {
            TestJwt = Jwt.DEFAULT.createSignedJwt(jo.toJSONString(), JwsAlgorithm.RS256).getCompactSerialization();
            TestJwtEs256 = Jwt.DEFAULT.createSignedJwt(jo.toJSONString(), JwsAlgorithm.ES256).getCompactSerialization();

            jo.put("iat", Long.valueOf(String.valueOf(new Date().getTime()-60000).substring(0, 10)));
            jo.put("exp", Long.valueOf(String.valueOf(new Date().getTime()-10000).substring(0, 10)));

            TestJwtExpired = Jwt.DEFAULT.createSignedJwt(jo.toJSONString(), JwsAlgorithm.RS256).getCompactSerialization();
            TestJwtExpiredEs256 = Jwt.DEFAULT.createSignedJwt(jo.toJSONString(), JwsAlgorithm.ES256).getCompactSerialization();
        } catch (JoseException e) {
            fail(e.getMessage());
        }
    }

    @Before
    public void setup() {
        this.iss = "https://example.com/server";
        this.aud = "audience";
        this.nonce = "nonceInIdToken";
        helperLogging();
    }

    @Test
    public void testNull() {
        try {
            Jwt.DEFAULT.validateIdToken(null, null, null, null, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("All parameters are required!", e.getMessage());
        }
        try {
            Jwt.DEFAULT.validateIdToken("", null, "", "", "");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("The given id_token could not be validated!", e.getMessage());
        }
        try {
            Jwt.DEFAULT.validateIdToken("", "", null, "", "");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("All parameters are required!", e.getMessage());
        }
        try {
            Jwt.DEFAULT.validateIdToken("", "", "", null, "");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("All parameters are required!", e.getMessage());
        }
        try {
            Jwt.DEFAULT.validateIdToken("", "", "", "", null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("All parameters are required!", e.getMessage());
        }
    }

    @Test
    public void testIss() {
        try {
            Jwt.DEFAULT.validateIdToken(TestJwtExpired, TestJwks.toJson(), "unknown_iss", aud, nonce);
        } catch (Exception e) {
            assertEquals("The given id_token could not be validated!", e.getMessage());
        }
        assertTrue(testRecord.getMessage().contains("Issuer (iss) claim value (https://example.com/server) doesn't match expected value of unknown_iss"));
    }

    @Test
    public void testIssEs256() {
        try {
            Jwt.DEFAULT.validateIdToken(TestJwtExpiredEs256, TestJwks.toJson(), "unknown_iss", aud, nonce);
        } catch (Exception e) {
            assertEquals("The given id_token could not be validated!", e.getMessage());
        }
        assertTrue(testRecord.getMessage().contains("Issuer (iss) claim value (https://example.com/server) doesn't match expected value of unknown_iss"));
    }

    @Test
    public void testWrongAud() {
        try {
            Jwt.DEFAULT.validateIdToken(TestJwt, TestJwks.toJson(), iss, "unknown_aud", nonce);
        } catch (Exception e) {
            assertEquals("The given id_token could not be validated!", e.getMessage());
        }
        assertTrue(testRecord.getMessage().contains("Audience (aud) claim [audience] doesn't contain an acceptable identifier. Expected unknown_aud as an aud value."));
    }

    @Test
    public void testExpired() {
        try {
            Jwt.DEFAULT.validateIdToken(TestJwtExpired, TestJwks.toJson(), iss, aud, nonce);
        } catch (Exception e) {
            assertEquals("The given id_token could not be validated!", e.getMessage());
        }
        assertTrue(testRecord.getMessage().contains("The JWT is no longer valid"));
    }

    @Test
    public void testNonce() {
        try {
            Jwt.DEFAULT.validateIdToken(TestJwt, TestJwks.toJson(), iss, aud, "expectedNonce");
        } catch (Exception e) {
            assertEquals("The given id_token could not be validated!", e.getMessage());
        }
        assertEquals("Unexpected nonce. Expected: 'expectedNonce', actual: '" + nonce + "'", testRecord.getMessage());
    }

    @Test
    public void testCreateJwtRs256() {
        try {
            JsonWebSignature jws = Jwt.DEFAULT.createSignedJwtRs256("https://self-issued.me", "https://local.loginbuddy.net/callback", 5, "01234567890", "randomnonce", true);
            assertEquals(JwsAlgorithm.RS256.toString(), jws.getAlgorithm().getAlgorithmIdentifier());
            assertNotNull(((JSONObject) new JSONParser().parse(jws.getUnverifiedPayload())).get("sub_jwk"));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testCreateJwtEc256() {
        try {
            JsonWebSignature jws = Jwt.DEFAULT.createSignedJwtEs256("https://self-issued.me", "https://local.loginbuddy.net/callback", 5, "01234567890", "randomnonce", true);
            assertEquals(JwsAlgorithm.ES256.toString(), jws.getAlgorithm().getAlgorithmIdentifier());
            assertNotNull(((JSONObject) new JSONParser().parse(jws.getUnverifiedPayload())).get("sub_jwk"));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testJwks() {
        assertEquals(2, Jwt.DEFAULT.getJwksForSigning().getJsonWebKeys().size());
        assertEquals(JwsAlgorithm.RS256.toString(), Jwt.DEFAULT.getJwksForSigning().getJsonWebKeys().get(0).getAlgorithm());
        assertEquals(JwsAlgorithm.ES256.toString(), Jwt.DEFAULT.getJwksForSigning().getJsonWebKeys().get(1).getAlgorithm());
        assertNotNull(Jwt.DEFAULT.getJwksForSigning().getJsonWebKeys().get(0).getKeyId());
        assertNotNull(Jwt.DEFAULT.getJwksForSigning().getJsonWebKeys().get(1).getKeyId());
    }

    /**
     * Retrieving the created logger error message. Used when there is no other way of verifying what went wrong
     */
    private void helperLogging() {

        LOGGER_JWT.addHandler(new Handler() {

            @Override
            public void publish(LogRecord record) {
                testRecord = record;
            }

            @Override
            public void flush() {
                // do nothing
            }

            @Override
            public void close() throws SecurityException {
                // do nothing
            }

        });
    }
}