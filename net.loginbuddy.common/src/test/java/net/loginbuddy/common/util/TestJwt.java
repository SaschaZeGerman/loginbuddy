/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.common.util;

import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class TestJwt {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(Jwt.class));

    private static String TestJwt, TestJwtExpired;
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
            TestJwt = Jwt.DEFAULT.createSignedJwt(jo.toJSONString(), "RS256").getCompactSerialization();

            jo.put("iat", Long.valueOf(String.valueOf(new Date().getTime()-60000).substring(0, 10)));
            jo.put("exp", Long.valueOf(String.valueOf(new Date().getTime()-10000).substring(0, 10)));
            TestJwtExpired = Jwt.DEFAULT.createSignedJwt(jo.toJSONString(), "RS256").getCompactSerialization();
        } catch (JoseException e) {
            e.printStackTrace();
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
    public void testCreateJwtHS256() {
        String content = "{\"iss\":\"https://example.com/server\",\"aud\":\"example_audience\",\"exp\":1916239022}";

        JsonWebSignature jws = Jwt.DEFAULT.createSignedJwtHs256("passwordwithmorethanjustafewcharacters".getBytes(), content);
        try {
            assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2V4YW1wbGUuY29tL3NlcnZlciIsImF1ZCI6ImV4YW1wbGVfYXVkaWVuY2UiLCJleHAiOjE5MTYyMzkwMjJ9.4pXNojVorXiNSlfw9qm1q5q0M2uJJiiTGeeYcky7FWU",
                    jws.getCompactSerialization()
            );
        } catch (JoseException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testCreateJwtRs256() {
        try {
            JsonWebSignature jws = Jwt.DEFAULT.createSignedJwtRs256("https://self-issued.me", "https://local.loginbuddy.net/callback", 5, "01234567890", "randomnonce", true);
            assertEquals("RS256", jws.getAlgorithm().getAlgorithmIdentifier());
            assertNotNull(((JSONObject) new JSONParser().parse(jws.getUnverifiedPayload())).get("sub_jwk"));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Retrieving the created logger error message. Used when there is no other way of verifying what went wrong
     */
    private void helperLogging() {

        LOGGER.addHandler(new Handler() {

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