/*
 * Copyright (c) 2018. . All rights reserved.
 *
 * This software may be modified and distributed under the terms of the Apache License 2.0 license.
 * See http://www.apache.org/licenses/LICENSE-2.0 for details.
 *
 */

package net.loginbuddy.common.util;

import org.jose4j.jws.JsonWebSignature;
import org.jose4j.lang.JoseException;
import org.junit.Before;
import org.junit.Test;

import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class TestJwt {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(Jwt.class));

    /**
     * Example JWT, created at {@link https://jwt.io}
     * <p>
     * JWT-Header: {
     * "alg": "RS256",
     * "typ": "JWT",
     * "kid": "kid01"
     * }
     * <p>
     * JWT-Payload: {
     * 	"sub": "1234567890",
     * 	"name": "John Doe",
     * 	"admin": true,
     * 	"iat": 1516239022,
     * 	"exp": 1516239022,
     * 	"iss": "https://example.com/server",
     * 	"aud": "example_audience",
     * 	"nonce": "nonceInIdToken"
     * }
     */
    private String expiredJwt = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImtpZDAxIn0.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiYWRtaW4iOnRydWUsImlhdCI6MTUxNjIzOTAyMiwiZXhwIjoxNTE2MjM5MDIyLCJpc3MiOiJodHRwczovL2V4YW1wbGUuY29tL3NlcnZlciIsImF1ZCI6ImV4YW1wbGVfYXVkaWVuY2UiLCJub25jZSI6Im5vbmNlSW5JZFRva2VuIn0.lJyGNhII9hun0bxlCJs7jrK7-9rENKHFSf-O9uV59UPlngY-DMiIx0IrFMySrZaSnUFSN5MijjzYjRsNm5wag_Rrk2ygscl9CYlbGVJUZMgteQBtElN2G0nNw69leyHTUSInsyXv8jpqPxwrZRTv3iPHsafDVThDJQJve3qnTeg";

    private String iss;
    private String aud;
    private String nonce;
    private LogRecord testRecord;

    @Before
    public void setup() {
        this.iss = "https://example.com/server";
        this.aud = "example_audience";
        this.nonce = "nonceInIdToken";
        helperLogging();
    }

    @Test
    public void testNull() {
        try {
            new Jwt().validateJwt(null, null, null, null, null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("All parameters are required!", e.getMessage());
        }
        try {
            new Jwt().validateJwt("", null, "", "", "");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("All parameters are required!", e.getMessage());
        }
        try {
            new Jwt().validateJwt("", "", null, "", "");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("All parameters are required!", e.getMessage());
        }
        try {
            new Jwt().validateJwt("", "", "", null, "");
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("All parameters are required!", e.getMessage());
        }
        try {
            new Jwt().validateJwt("", "", "", "", null);
            fail();
        } catch (IllegalArgumentException e) {
            assertEquals("All parameters are required!", e.getMessage());
        }
    }

    @Test
    public void testIss() {
        try {
            new Jwt().validateJwt(expiredJwt, "", "unknown_iss", aud, nonce);
        } catch (Exception e) {
            assertEquals("The given JWT could not be parsed or decoded!", e.getMessage());
        }
        assertEquals("Unexpected iss. Expected: 'unknown_iss', actual: '" + iss + "'", testRecord.getMessage());

    }

    @Test
    public void testWrongAud() {
        try {
            new Jwt().validateJwt(expiredJwt, "", iss, "unknown_aud", nonce);
        } catch (Exception e) {
            assertEquals("The given JWT could not be parsed or decoded!", e.getMessage());
        }
        assertEquals("Unexpected aud. Expected: 'unknown_aud', actual: '" + aud + "'", testRecord.getMessage());
    }

    @Test
    public void testExpired() {
        try {
            new Jwt().validateJwt(expiredJwt, "", iss, aud, nonce);
        } catch (Exception e) {
            assertEquals("The given JWT could not be parsed or decoded!", e.getMessage());
        }
        assertEquals("The JWT has expired!", testRecord.getMessage());
    }

    @Test
    public void testNonce() {
        try {
            new Jwt().validateJwt(expiredJwt, "", iss, aud, "expectedNonce");
        } catch (Exception e) {
            assertEquals("The given JWT could not be parsed or decoded!", e.getMessage());
        }
        assertEquals("Unexpected nonce. Expected: 'expectedNonce', actual: '" + nonce + "'", testRecord.getMessage());
    }

    @Test
    public void testCreateJwtHS256() {
        String content = "{\"iss\":\"https://example.com/server\",\"aud\":\"example_audience\",\"exp\":1916239022}";

        JsonWebSignature jws = new Jwt().createSignedJwtHs256("passwordwithmorethanjustafewcharacters".getBytes(), content);
        try {
            assertEquals("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJodHRwczovL2V4YW1wbGUuY29tL3NlcnZlciIsImF1ZCI6ImV4YW1wbGVfYXVkaWVuY2UiLCJleHAiOjE5MTYyMzkwMjJ9.4pXNojVorXiNSlfw9qm1q5q0M2uJJiiTGeeYcky7FWU",
                    jws.getCompactSerialization()
            );
        } catch (JoseException e) {
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