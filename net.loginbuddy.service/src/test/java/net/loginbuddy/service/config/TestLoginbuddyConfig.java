package net.loginbuddy.service.config;

import hthurow.tomcatjndi.TomcatJNDI;
import net.loginbuddy.service.config.discovery.DiscoveryConfig;
import net.loginbuddy.service.config.discovery.DiscoveryUtil;
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
    }

    @After
    public void after() {
        tomcatJNDI.tearDown();
    }

    @Test
    public void testLoadClient() {
        assertEquals("public", LoginbuddyConfig.CONFIGS.getConfigUtil().getClientConfigByClientId("clientIdForTestingPurposes").getClientType());
    }

    @Test
    public void testLoadProvider() {
        assertEquals("loginbuddy_demoId", LoginbuddyConfig.CONFIGS.getConfigUtil().getProviderConfigByProvider("server_loginbuddy").getClientId());
    }

    @Test
    public void testLoadDiscovery() {
        assertEquals("https://{your-domain}", DiscoveryConfig.CONFIG.getIssuer());
    }

    @Test
    public void testLoadProperties() {
        assertEquals(60, LoginbuddyConfig.CONFIGS.getPropertiesUtil().getLongProperty("lifetime.proxy.userinfo"));
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
            public ConfigUtil getConfigUtil() {
                return new TestConfigUtil();
            }

            @Override
            public PropertiesUtil getPropertiesUtil() {
                return null;
            }

            @Override
            public boolean isConfigured() {
                return false;
            }
        };
        LoginbuddyConfig.CONFIGS.setConfigLoader(loader);
        assertEquals("confidential", LoginbuddyConfig.CONFIGS.getConfigUtil().getClientConfigByClientId("reloadClientId").getClientType());

        // check if the reloaded config is still active
        assertEquals("confidential", LoginbuddyConfig.CONFIGS.getConfigUtil().getClientConfigByClientId("reloadClientId").getClientType());
    }

    class TestConfigUtil extends ConfigUtil {

        @Override
        public ClientConfig getClientConfigByClientId(String clientId) {
            ClientConfig cc = new ClientConfig();
            cc.setClientId("reloadClientId");
            cc.setClientType("confidential");
            return cc;
        }

        @Override
        public List<ProviderConfig> getProviders(String clientId) throws Exception {
            return super.getProviders(clientId);
        }

        @Override
        public ProviderConfig getProviderConfigByProvider(String providerHint) {
            return super.getProviderConfigByProvider(providerHint);
        }

        @Override
        public ProviderConfig getProviderConfigByIssuer(String issuerHint) {
            return super.getProviderConfigByIssuer(issuerHint);
        }

        @Override
        public ProviderConfig getProviderConfigFromJsonString(String providerHint) {
            return super.getProviderConfigFromJsonString(providerHint);
        }

        @Override
        public boolean isConfigured() {
            return super.isConfigured();
        }
    }
}