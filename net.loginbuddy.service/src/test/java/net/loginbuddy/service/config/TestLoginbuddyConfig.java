package net.loginbuddy.service.config;

import hthurow.tomcatjndi.TomcatJNDI;
import net.loginbuddy.service.config.loginbuddy.*;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestLoginbuddyConfig {

    private TomcatJNDI tomcatJNDI;

    @Before
    public void setup() {
        tomcatJNDI = new TomcatJNDI();
        tomcatJNDI.processContextXml(new File("src/test/resources/testContext.xml"));
        tomcatJNDI.start();
        LoginbuddyConfig.CONFIG.setDefaultConfigLoader();
    }

    @After
    public void after() {
        tomcatJNDI.tearDown();
    }

    @Test
    public void testLoadClient() {
        assertEquals("public", LoginbuddyConfig.CONFIG.getLoginbuddyUtil().getClientConfigByClientId("clientIdForTestingPurposes").getClientType());
    }

    @Test
    public void testLoadProvider() {
        assertEquals("loginbuddy_demoId", LoginbuddyConfig.CONFIG.getLoginbuddyUtil().getProviderConfigByProvider("server_loginbuddy").getClientId());
    }

    @Test
    public void testReloadConfig() {
        LoginbuddyConfigLoader loader = new LoginbuddyConfigLoader() {
            @Override
            public void loadConfig() {

            }

            @Override
            public void reloadConfig() {

            }

            @Override
            public LoginbuddyUtil getLoginbuddyUtil() {
                return new TestLoginbuddyUtil();
            }

            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        LoginbuddyConfig.CONFIG.setConfigLoader(loader);
        assertEquals("confidential", LoginbuddyConfig.CONFIG.getLoginbuddyUtil().getClientConfigByClientId("reloadClientId").getClientType());

        // check if the reloaded config is still active
        assertEquals("confidential", LoginbuddyConfig.CONFIG.getLoginbuddyUtil().getClientConfigByClientId("reloadClientId").getClientType());
    }

    class TestLoginbuddyUtil extends LoginbuddyUtil {

        @Override
        public Clients getClientConfigByClientId(String clientId) {
            Clients cc = new Clients();
            cc.setClientId("reloadClientId");
            cc.setClientType("confidential");
            return cc;
        }

        @Override
        public List<Providers> getProviders(String clientId) throws Exception {
            return super.getProviders(clientId);
        }

        @Override
        public Providers getProviderConfigByProvider(String providerHint) {
            return super.getProviderConfigByProvider(providerHint);
        }

        @Override
        public Providers getProviderConfigByIssuer(String issuerHint) {
            return super.getProviderConfigByIssuer(issuerHint);
        }

        @Override
        public Providers getProviderConfigFromJsonString(String providerHint) {
            return super.getProviderConfigFromJsonString(providerHint);
        }

        @Override
        public boolean isConfigured() {
            return super.isConfigured();
        }
    }
}