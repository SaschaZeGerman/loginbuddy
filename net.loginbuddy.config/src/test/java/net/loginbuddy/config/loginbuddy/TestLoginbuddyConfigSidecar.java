package net.loginbuddy.config.loginbuddy;

import hthurow.tomcatjndi.TomcatJNDI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestLoginbuddyConfigSidecar {

    private TomcatJNDI tomcatJNDI;

    @Before
    public void setup() {
        tomcatJNDI = new TomcatJNDI();
        tomcatJNDI.processContextXml(new File("src/test/resources/testContextSidecar.xml"));
        tomcatJNDI.start();
        try {
            LoginbuddyUtil.UTIL.setDefaultLoader();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @After
    public void after() {
        tomcatJNDI.tearDown();
    }

    @Test
    public void testLoadClient() {
        assertEquals("confidential", LoginbuddyUtil.UTIL.getClientConfigByClientId("loginbuddy-sidecar").getClientType());
    }
}