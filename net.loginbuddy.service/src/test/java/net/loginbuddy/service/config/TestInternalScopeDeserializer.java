package net.loginbuddy.service.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import net.loginbuddy.service.config.internal.scope.*;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

public class TestInternalScopeDeserializer {

    private static com.fasterxml.jackson.databind.ObjectMapper MAPPER = new ObjectMapper();
    private static InternalScopeConfig scopeConfig;

    @BeforeClass
    public static void setUpClass() {
        try {
            SimpleModule module = new SimpleModule();
            module.addDeserializer(Read.class, new ScopeReadDeserializer());
            module.addDeserializer(Write.class, new ScopeWriteDeserializer());

            MAPPER.registerModule(module);

            JsonNode node = MAPPER.readValue(getDefaultScope(), JsonNode.class);
            scopeConfig = MAPPER.readValue(node.toString(), InternalScopeConfig.class);
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void testIsManagementScopeValid() {
        assertTrue(scopeConfig.getManagement().isValidScope("management"));
        assertTrue(scopeConfig.getManagement().getConfiguration().isValidScope("management"));

        assertTrue(scopeConfig.getManagement().getConfiguration().getRead().isValidScope("management"));
        assertTrue(scopeConfig.getManagement().getConfiguration().getRead().getClients().isValidScope("management"));

        assertTrue(scopeConfig.getManagement().getConfiguration().getWrite().isValidScope("management"));
        assertTrue(scopeConfig.getManagement().getConfiguration().getWrite().getClients().isValidScope("management"));
    }

    @Test
    public void testIsConfigurationScopeValid() {
        assertFalse(scopeConfig.getManagement().isValidScope("management.configuration"));
        assertTrue(scopeConfig.getManagement().getConfiguration().isValidScope("management.configuration"));

        assertTrue(scopeConfig.getManagement().getConfiguration().getRead().isValidScope("management.configuration"));
        assertTrue(scopeConfig.getManagement().getConfiguration().getRead().getClients().isValidScope("management.configuration"));

        assertTrue(scopeConfig.getManagement().getConfiguration().getWrite().isValidScope("management.configuration"));
        assertTrue(scopeConfig.getManagement().getConfiguration().getWrite().getProviders().isValidScope("management.configuration"));
    }

    @Test
    public void testIsReadScopeValid() {
        assertFalse(scopeConfig.getManagement().isValidScope("management.configuration.read"));
        assertFalse(scopeConfig.getManagement().getConfiguration().isValidScope("management.configuration.read"));

        assertTrue(scopeConfig.getManagement().getConfiguration().getRead().isValidScope("management.configuration.read"));
        assertTrue(scopeConfig.getManagement().getConfiguration().getRead().getDiscovery().isValidScope("management.configuration.read"));

        assertFalse(scopeConfig.getManagement().getConfiguration().getWrite().isValidScope("management.configuration.read"));
    }

    @Test
    public void testIsWriteScopeValid() {
        assertFalse(scopeConfig.getManagement().isValidScope("management.configuration.write"));
        assertFalse(scopeConfig.getManagement().getConfiguration().isValidScope("management.configuration.write"));

        assertFalse(scopeConfig.getManagement().getConfiguration().getRead().isValidScope("management.configuration.write"));

        assertTrue(scopeConfig.getManagement().getConfiguration().getWrite().isValidScope("management.configuration.write"));
        assertTrue(scopeConfig.getManagement().getConfiguration().getWrite().getProviders().isValidScope("management.configuration.write"));
    }

    @Test
    public void testIsScopeChildScopeValid() {
        assertFalse(scopeConfig.getManagement().isValidScope("management.configuration.read.clients"));
        assertFalse(scopeConfig.getManagement().isValidScope("management.configuration.write.clients"));
        assertFalse(scopeConfig.getManagement().getConfiguration().isValidScope("management.configuration.read.clients"));

        assertFalse(scopeConfig.getManagement().getConfiguration().getRead().isValidScope("management.configuration.read.clients"));
        assertTrue(scopeConfig.getManagement().getConfiguration().getRead().getClients().isValidScope("management.configuration.read.clients"));
        assertFalse(scopeConfig.getManagement().getConfiguration().getRead().getProperties().isValidScope("management.configuration.read.clients"));

        assertFalse(scopeConfig.getManagement().getConfiguration().getWrite().isValidScope("management.configuration.write.clients"));
        assertTrue(scopeConfig.getManagement().getConfiguration().getWrite().getProviders().isValidScope("management.configuration.write.providers"));
        assertFalse(scopeConfig.getManagement().getConfiguration().getWrite().getProviders().isValidScope("management.configuration.write.clients"));
    }

    private static String getDefaultScope() {
        return "{\n" +
                "  \"management\": {\n" +
                "    \"configuration\": {\n" +
                "      \"read\": {\n" +
                "        \"clients\": {\n" +
                "          \"description\": \"Read client configurations\"\n" +
                "        },\n" +
                "        \"providers\": {\n" +
                "          \"description\": \"Read OpenID Provider configurations\"\n" +
                "        },\n" +
                "        \"discovery\": {\n" +
                "          \"description\": \"Read the OpenID Discovery configuration\"\n" +
                "        },\n" +
                "        \"properties\": {\n" +
                "          \"description\": \"Read system properties\"\n" +
                "        }\n" +
                "      },\n" +
                "      \"write\": {\n" +
                "        \"clients\": {\n" +
                "          \"description\": \"Write client configurations\"\n" +
                "        },\n" +
                "        \"providers\": {\n" +
                "          \"description\": \"Write OpenID Provider configurations\"\n" +
                "        },\n" +
                "        \"discovery\": {\n" +
                "          \"description\": \"Write the OpenID Discovery configuration\"\n" +
                "        },\n" +
                "        \"properties\": {\n" +
                "          \"description\": \"Write system properties\"\n" +
                "        }\n" +
                "      }\n" +
                "    }\n" +
                "  }\n" +
                "}";
    }
}