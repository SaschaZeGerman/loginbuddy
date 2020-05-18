package net.loginbuddy.service.config.properties;

import hthurow.tomcatjndi.TomcatJNDI;
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
        try {
            PropertiesUtil.UTIL.setDefaultLoader();
        } catch (Exception e) {
            fail(e.getMessage());
        }
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
    public void testGetStringProperty() {
        assertEquals(
                "net.loginbuddy.service.config.loginbuddy.CustomLoginbuddyConfigLoader",
                PropertiesUtil.UTIL.getStringProperty("config.loginbuddy.loader.default"));
    }

    @Test
    public void testSave() {
        Properties props = new Properties();
        try {
            PropertiesUtil.UTIL.setProperties(props);
            fail();
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void testOverrideLoader() {
        assertEquals(60l, PropertiesUtil.UTIL.getLongProperty("lifetime.proxy.userinfo"));

        PropertyLoader l = new TestPropertyLoader();
        try {
            PropertiesUtil.UTIL.setLoader(l);
        } catch (Exception e) {
            fail(e.getMessage());
        }

        assertEquals(10l, PropertiesUtil.UTIL.getLongProperty("lifetime.proxy.userinfo"));
    }

    class TestPropertyLoader implements PropertyLoader {

        Properties testProps;

        public TestPropertyLoader() {
            testProps = new Properties();
            testProps.put("lifetime.proxy.userinfo", "10");
            testProps.put("lifetime.oauth.authcode.loginbuddy.flow", "20");
            testProps.put("lifetime.oauth.authcode", "30");
            testProps.put("lifetime.oauth.authcode.provider.flow", "40");
        }

        @Override
        public Properties getProperties() {
            return testProps;
        }

        @Override
        public boolean isConfigured() {
            return testProps != null;
        }

        @Override
        public void load() {
            // nothing to do here
            // if we would get properties from a different location we would pull them in here
        }

        @Override
        public void reload() {
            // nothing to do here
            // if we would get properties from a different location we would pull them in here
        }

        @Override
        public <T> T save(T configuration) throws MethodNotSupportedException {
            testProps = (Properties)configuration;
            return (T)testProps;
        }

        @Override
        public <T> T update(T configuration) throws MethodNotSupportedException {
            testProps.putAll((Properties)configuration);
            return (T)testProps;
        }
    }
}