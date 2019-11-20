package net.loginbuddy.common.util;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

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

}
