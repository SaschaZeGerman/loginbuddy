package net.loginbuddy.service.config.loginbuddy;

import hthurow.tomcatjndi.TomcatJNDI;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

public class TestLoginbuddyConfig {

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
    public void testLoadClient() {
        assertEquals("public", LoginbuddyUtil.UTIL.getClientConfigByClientId("clientIdForTestingPurposes").getClientType());
    }

    @Test
    public void testLoadProvider() {
        assertEquals("loginbuddy_demoId", LoginbuddyUtil.UTIL.getProviderConfigByProvider("server_loginbuddy").getClientId());
    }

    @Test
    public void testReloadConfig() {
        LoginbuddyLoader loader = new LoginbuddyLoader() {
            @Override
            public void load() {
            }

            @Override
            public void reload() {
            }

            @Override
            public Loginbuddy getLoginbuddy() {
                return new TestLoginbuddy();
            }

            @Override
            public boolean isConfigured() {
                return true;
            }
        };

        LoginbuddyUtil.UTIL.setLoader(loader);
        assertEquals("confidential", LoginbuddyUtil.UTIL.getClientConfigByClientId("reloadClientId").getClientType());

        // check if the reloaded config is still active
        assertEquals("confidential", LoginbuddyUtil.UTIL.getClientConfigByClientId("reloadClientId").getClientType());
    }

    class TestLoginbuddy extends Loginbuddy {

        @Override
        public List<Clients> getClients() {
            Clients cc = new Clients();
            cc.setClientId("reloadClientId");
            cc.setClientType("confidential");
            List<Clients> clients = new ArrayList<>();
            clients.add(cc);
            return clients;
        }

        @Override
        public void setClients(List<Clients> clients) {
            super.setClients(clients);
        }

        @Override
        public List<Providers> getProviders() {
            return super.getProviders();
        }

        @Override
        public void setProviders(List<Providers> providers) {
            super.setProviders(providers);
        }
    }
}