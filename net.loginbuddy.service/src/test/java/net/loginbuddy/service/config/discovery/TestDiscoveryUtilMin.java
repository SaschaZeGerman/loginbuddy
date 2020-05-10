package net.loginbuddy.service.config.discovery;

import hthurow.tomcatjndi.TomcatJNDI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

public class TestDiscoveryUtilMin {

    private TomcatJNDI tomcatJNDI;

    @Before
    public void setup() {
        tomcatJNDI = new TomcatJNDI();
        tomcatJNDI.processContextXml(new File("src/test/resources/testContextDiscoveryMin.xml"));
        tomcatJNDI.start();
        DiscoveryUtil.UTIL.getLoader().reloadDiscovery();
    }

    @After
    public void after() {
        tomcatJNDI.tearDown();
    }

    @Test
    public void testDiscoveryIssuer() {
        assertEquals("RS256", DiscoveryUtil.UTIL.getIdTokenSigningAlgValuesSupported()[0]);
    }

}
