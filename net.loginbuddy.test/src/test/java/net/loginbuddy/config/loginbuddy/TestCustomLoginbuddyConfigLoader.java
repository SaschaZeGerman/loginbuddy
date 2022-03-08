package net.loginbuddy.config.loginbuddy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class TestCustomLoginbuddyConfigLoader {

    @Before
    public void setup() {
        try {
            Files.copy(
                    new File("src/test/resources/config.json.bak").toPath(),
                    new File("src/test/resources/config.json").toPath(),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            e.getMessage();
            fail(e.getMessage());
        }
    }

    @Test
    public void TestSaveClients() {
        try {
            LoginbuddyLoader ll = new CustomLoginbuddyConfigLoader(
                    "src/test/resources/config.json",
                    "src/test/resources/configTemplates.json");
            List<Clients> clients = new LoginbuddyObjectMapper().readClients(new File("src/test/resources/clients.json"));
            ll.save(clients);
            assertEquals(1, ll.getLoginbuddy().getClients().size());
            assertTrue(ll.getLoginbuddy().getClients().get(0).isRegisteredRedirectUri("https://localhost/custom/loader"));
            assertEquals(4, ll.getLoginbuddy().getProviders().size());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void TestUpdateClients() {
        try {
            LoginbuddyLoader ll = new CustomLoginbuddyConfigLoader(
                    "src/test/resources/config.json",
                    "src/test/resources/configTemplates.json");
            List<Clients> clients = new LoginbuddyObjectMapper().readClients(new File("src/test/resources/clientsUpdate.json"));
            ll.update(clients);
            assertEquals(4, ll.getLoginbuddy().getClients().size());
            boolean gotUpdated = false;
            for(Clients next : ll.getLoginbuddy().getClients()) {
                if("https://democlient.loginbuddy.net/updated".equals(next.getClientUri()) && "clientIdForTestingPurposes".equals(next.getClientId())) {
                    gotUpdated = true;
                }
            }
            assertTrue(gotUpdated);
            assertEquals(4, ll.getLoginbuddy().getProviders().size());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void TestProviderTemplatesProviders() {
        try {
            LoginbuddyLoader ll = new CustomLoginbuddyConfigLoader(
                    "src/test/resources/config.json",
                    "src/test/resources/configTemplates.json");
            List<Providers> providers = ll.getLoginbuddy().getProviders();
            assertEquals("replacedClientId", providers.get(3).getClientId());
            assertEquals("https://template.loginbuddy.net/authorize", providers.get(3).getAuthorizationEndpoint());
            assertEquals(4, ll.getLoginbuddy().getProviders().size());
            assertEquals("server_scope", ll.getLoginbuddy().getProviders().get(3).getScope());
            assertFalse(ll.getLoginbuddy().getProviders().get(3).getPkce());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void TestMissingClientSecret() {
        try {
            new LoginbuddyObjectMapper().readClients(new File("src/test/resources/clientsUpdateMissingSecret.json"));
            fail("confidential client misses a required client_secret");
        } catch (Exception e) {
            assertTrue(true);
        }
    }

    @Test
    public void TestMissingTemplates() {
        try {
            LoginbuddyLoader ll = new CustomLoginbuddyConfigLoader("src/test/resources/config.json");
            fail("a template is referenced but not provided");
        } catch (Exception e) {
            assertEquals("At least one provider references a template but no templates were given! Provider: 'server_templated'", e.getMessage());
        }
    }
}