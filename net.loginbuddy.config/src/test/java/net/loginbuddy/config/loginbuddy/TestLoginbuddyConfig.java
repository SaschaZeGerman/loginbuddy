package net.loginbuddy.config.loginbuddy;

import com.fasterxml.jackson.databind.ObjectMapper;
import hthurow.tomcatjndi.TomcatJNDI;
import net.loginbuddy.common.api.HttpHelper;
import org.apache.http.MethodNotSupportedException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class TestLoginbuddyConfig {

    private TomcatJNDI tomcatJNDI;

    @Before
    public void setup() {
        tomcatJNDI = new TomcatJNDI();
        tomcatJNDI.processContextXml(new File("src/test/resources/testContext.xml"));
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
        assertEquals("public", LoginbuddyUtil.UTIL.getClientConfigByClientId("clientIdForTestingPurposes").getClientType());
    }

    @Test
    public void testLoadClientOnBehalfOf() {
        try {
            String newConfig = HttpHelper.readMessageBody(
                    new BufferedReader(
                            new FileReader(
                                    new File("src/test/resources/testConfigOnBehalfOf.json"))));
            Loginbuddy config = new LoginbuddyObjectMapper().readLoginbuddy((JSONObject)new JSONParser().parse(newConfig));
            assertEquals("RS256", config.getClients().get(0).getOnBehalfOf().get(0).getAlg());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testLoadProvider() {
        assertEquals("loginbuddy_demoId", LoginbuddyUtil.UTIL.getProviderConfigByProvider("server_loginbuddy").getClientId());
        try {
            JSONObject mappings = LoginbuddyUtil.UTIL.getProviderConfigByProvider("server_loginbuddy").getMappings();
            assertEquals("value1", mappings.get("key1"));
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void testTemplates() {
         Providers c = LoginbuddyUtil.UTIL.getProviderConfigByProvider("server_loginbuddy_01");
         assertEquals("loginbuddy_demoId_temp01", c.getClientId());

         c = LoginbuddyUtil.UTIL.getProviderConfigByProvider("server_loginbuddy_02");
         assertEquals("loginbuddy_demoId_temp02", c.getClientId());

         c = LoginbuddyUtil.UTIL.getProviderConfigByProvider("google");
         assertEquals("https://local.loginbuddy.net/callback", c.getRedirectUri());
         assertEquals("loginbuddy_google_client_secret_01", c.getClientSecret());

         // this references the client_id of the duplicate provider configuration
         // only the first appearance of a provider configuration should be used
         c = LoginbuddyUtil.UTIL.getProviderConfigByProvider("google");
         assertEquals("https://local.loginbuddy.net/callback", c.getRedirectUri());
         assertNotEquals("loginbuddy_google_client_secret_02", c.getClientSecret());

         assertNull(LoginbuddyUtil.UTIL.getProviderConfigByProvider("googleUnknown"));
    }

    @Test
    public void testReplaceClientsWithCustomLoader() {
        try {

            // check the defaults
            assertEquals(1, LoginbuddyUtil.UTIL.getClients().size());
            assertEquals("public", LoginbuddyUtil.UTIL.getClientConfigByClientId("clientIdForTestingPurposes").getClientType());
            assertEquals(6, LoginbuddyUtil.UTIL.getProviders().size());

            // set the new loader that has three clients and contains the client we do not want to lose on 'replaceClients'
            LoginbuddyLoader l = new CustomLoginbuddyLoader();
            LoginbuddyUtil.UTIL.setLoader(l);
            assertEquals(3, LoginbuddyUtil.UTIL.getClients().size());

            // this net.loginbuddy.service.config does not include 'clientIdNotToBeLost' but contains only one other client
            String newConfig = HttpHelper.readMessageBody(
                    new BufferedReader(
                            new FileReader(
                                    new File("src/test/resources/testReplaceClients.json"))));
            List<Clients> replaced = LoginbuddyUtil.UTIL.replaceClients("clientIdNotToBeLost", newConfig);
            boolean containsClientId = false;
            for(Clients next : replaced) {
                if("clientIdNotToBeLost".equals(next.getClientId())) {
                    containsClientId = true;
                    break;
                }
            }
            assertTrue(containsClientId);
            assertEquals(2, LoginbuddyUtil.UTIL.getClients().size());

            // this time we do not care about losing the client 'clientIdNotToBeLost'
            replaced = LoginbuddyUtil.UTIL.replaceClients(null, newConfig);
            assertEquals(1, LoginbuddyUtil.UTIL.getClients().size());
            assertEquals("customClientIdForCustomLoader", LoginbuddyUtil.UTIL.getClients().get(0).getClientId());

        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
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
            public <T> T save(T configuration) throws MethodNotSupportedException {
                return null;
            }

            @Override
            public <T> T update(T configuration) throws MethodNotSupportedException {
                return null;
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

        try {
            LoginbuddyUtil.UTIL.setLoader(loader);
        } catch (Exception e) {
            fail(e.getMessage());
        }
        assertEquals("confidential", LoginbuddyUtil.UTIL.getClientConfigByClientId("reloadClientId").getClientType());

        // check if the reloaded net.loginbuddy.service.config is still active
        assertEquals("confidential", LoginbuddyUtil.UTIL.getClientConfigByClientId("reloadClientId").getClientType());
    }

    class TestLoginbuddy extends Loginbuddy {

        @Override
        public List<Clients> getClients() {
            Clients cc = new Clients("reloadClientId", "confidential");
            List<Clients> clients = new ArrayList<>();
            clients.add(cc);
            return clients;
        }

        @Override
        public List<Providers> getProviders() {
            return super.getProviders();
        }
    }

    class CustomLoginbuddyLoader implements LoginbuddyLoader {

        private Logger LOGGER = Logger.getLogger(String.valueOf(CustomLoginbuddyLoader.class));

        private List<Clients> myClients;
        private List<Providers> myProviders;

        private Loginbuddy lb;
        private LoginbuddyObjectMapper mapper;

        private String dbLocation;

        public CustomLoginbuddyLoader() {
            this.myClients = new ArrayList<>();
            this.myProviders = new ArrayList<>();
            mapper = new LoginbuddyObjectMapper();
            dbLocation = "src/test/resources/testCustomLoginbuddyConfig.json";
            load();
        }

        @Override
        public void load() {
            try {
                lb = mapper.readLoginbuddy(new File(dbLocation));
                LOGGER.info(String.format("Loading custom test Loginbuddy configuration: '%s'", dbLocation));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        @Override
        public void reload() {
            load();
        }

        @Override
        public <T> T save(T configuration) throws MethodNotSupportedException {
            try {
                JSONObject obj = new JSONObject();
                if (configuration instanceof List && ((List) configuration).size() > 0) {
                    if (((List) configuration).get(0) instanceof Clients) {
                        obj.put("clients", new JSONParser().parse(mapper.writeValueAsString(configuration)));
                        obj.put("providers", new JSONParser().parse(mapper.writeValueAsString(myProviders)));
                    } else if (((List) configuration).get(0) instanceof Providers) {
                        obj.put("clients", new JSONParser().parse(mapper.writeValueAsString(myClients)));
                        obj.put("providers", new JSONParser().parse(mapper.writeValueAsString(configuration)));
                    }
                }
                dbLocation = "src/test/resources/testCustomLoginbuddyConfigUpdated.json";
                FileWriter fw = new FileWriter(new File(dbLocation));
                obj.writeJSONString(fw);
                fw.flush();
                fw.close();
                load();
                return configuration;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return configuration;
        }

        @Override
        public <T> T update(T configuration) throws MethodNotSupportedException {
            return null;
        }

        @Override
        public Loginbuddy getLoginbuddy() {
            return lb;
        }

        @Override
        public boolean isConfigured() {
            return true;
        }
    }
}