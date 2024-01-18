package net.loginbuddy.common.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestSanetizer {

    @Test
    public void testSanetizeUrlPathPattern() {
        String urlPath = "../html5shiv/3.7.3/html5shiv.min.js";
        assertEquals(urlPath, Sanetizer.checkForUrlPathPattern(urlPath, 256));
    }

    @Test
    public void testSanetizeUrlPathPatternShortened() {
        String urlPath = "../html5shiv/3.7.3/html5shiv.min.js";
        assertEquals("../html5shiv/3.7", Sanetizer.checkForUrlPathPattern(urlPath, 16));
    }

    @Test
    public void testSanetizeUrlPathPatternAbsolute() {
        String urlPath = "/html5shiv/3.7.3/html5shiv.min.js";
        assertEquals(urlPath, Sanetizer.checkForUrlPathPattern(urlPath, 256));
    }

    @Test
    public void testGetDomain() {
        String url = "http://mydomain.com";
        assertEquals("mydomain.com", Sanetizer.getDomain(url));

        url = "https://mydomain.com";
        assertEquals("mydomain.com", Sanetizer.getDomain(url));

        url = "https://mydomain.com:8080";
        assertEquals("mydomain.com:8080", Sanetizer.getDomain(url));

        url = "https://mydomain.com:8080?query";
        assertEquals("mydomain.com:8080", Sanetizer.getDomain(url));

        url = "https://mydomain.com:8080/path";
        assertEquals("mydomain.com:8080", Sanetizer.getDomain(url));

        url = "https://mydomain.com:8080/";
        assertEquals("mydomain.com:8080", Sanetizer.getDomain(url));

        url = "https://localhost:8443/";
        assertEquals("localhost:8443", Sanetizer.getDomain(url));

        url = "custom://mydomain.com:8080/";
        assertNull(Sanetizer.getDomain(url));
    }

}
