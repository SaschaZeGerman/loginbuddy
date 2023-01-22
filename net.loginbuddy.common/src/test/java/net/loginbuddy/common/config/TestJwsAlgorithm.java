package net.loginbuddy.common.config;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TestJwsAlgorithm {

    @Test
    public void testJwsAlgorthm() {
        assertEquals("RS256", JwsAlgorithm.RS256.toString());
        assertEquals("ES256", JwsAlgorithm.ES256.toString());
        assertEquals("ES256", JwsAlgorithm.findMatchingAlg("ES256").toString());
        assertEquals("RS256", JwsAlgorithm.findMatchingAlg("AS256").toString());
    }
}
