package net.loginbuddy.service.config.discovery;

import hthurow.tomcatjndi.TomcatJNDI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestDiscoveryUtilMax {

    private TomcatJNDI tomcatJNDI;

    @Before
    public void setup() {
        tomcatJNDI = new TomcatJNDI();
        tomcatJNDI.processContextXml(new File("src/test/resources/testContextDiscoveryMax.xml"));
        tomcatJNDI.start();
        try {
            DiscoveryUtil.UTIL.getLoader().reload();
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @After
    public void after() {
        tomcatJNDI.tearDown();
    }

    @Test
    public void testSigningAlgSupported() {
        assertEquals("RS256", DiscoveryUtil.UTIL.getSigningAlgValuesSupported()[0]);
    }

    @Test
    public void testManagementConfigurationEndpoint() {
        assertEquals("https://{your-domain}/management/configuration", DiscoveryUtil.UTIL.getManagement().getConfigurationEndpoint());
    }
}