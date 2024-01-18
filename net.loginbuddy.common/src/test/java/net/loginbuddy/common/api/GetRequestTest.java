package net.loginbuddy.common.api;

import org.apache.http.client.methods.HttpGet;
import org.junit.Test;

import static org.junit.Assert.*;

public class GetRequestTest {

    @Test
    public void testGetRequestSimple() {
        HttpGet httpGet = null;
        try {
            httpGet = GetRequest.create("https://localhost").setDpopHeader("RS256", "https://localhost", null, null, null).build();
            assertEquals("GET", httpGet.getMethod());
            assertEquals("https://localhost",httpGet.getURI().toString());
            assertNotNull(httpGet.getFirstHeader("dpop").getValue());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
