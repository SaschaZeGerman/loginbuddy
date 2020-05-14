package net.loginbuddy.service.config.properties;

import hthurow.tomcatjndi.TomcatJNDI;
import net.loginbuddy.service.config.loginbuddy.LoginbuddyUtil;
import org.apache.http.MethodNotSupportedException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Properties;

import static org.junit.Assert.*;

public class TestPropertiesUtil {

    private TomcatJNDI tomcatJNDI;

    @Before
    public void setup() {
        tomcatJNDI = new TomcatJNDI();
        tomcatJNDI.processContextXml(new File("src/test/resources/testContext.xml"));
        tomcatJNDI.start();
        LoginbuddyUtil.UTIL.setDefaultLoader();
    }

    @After
    public void after() {
        tomcatJNDI.tearDown();
    }

    @Test
    public void testLifetimeProxyUserinfo() {
        assertEquals(60, PropertiesUtil.UTIL.getLongProperty("lifetime.proxy.userinfo"));
    }

    @Test
    public void testSave() {
        Properties props = new Properties();
        try {
            PropertiesUtil.UTIL.setProperties(props);
            fail();
        } catch (MethodNotSupportedException e) {
            assertTrue(true);
        }
    }
}