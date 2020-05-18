package net.loginbuddy.service.config.loginbuddy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

public class TestCustomLoginbuddyConfigLoader {

    @Test
    public void TestSave() {
        try {
            LoginbuddyLoader ll = new CustomLoginbuddyConfigLoader("src/test/resources/config.json");
            List<Clients> clients = Arrays.asList(new ObjectMapper().readValue(new File("src/test/resources/clients.json"), Clients[].class));
            ll.save(clients);
            assertEquals(1, ll.getLoginbuddy().getClients().size());
            assertEquals(4, ll.getLoginbuddy().getProviders().size());
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }
}