package net.loginbuddy.config.loginbuddy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class TestLoginbuddyTemplates {

    private Loginbuddy config;
    private ObjectMapper mapper;
    private Loginbuddy configTemplates;
    private JSONObject configJson, configTemplateJson;

    @Before
    public void setup() throws IOException, ParseException {
        configJson = (JSONObject)new JSONParser().parse(new FileReader(new File("src/test/resources/testConfigUsingTemplate.json")));
        configTemplateJson = (JSONObject)new JSONParser().parse(new FileReader(new File("src/test/resources/testConfigTemplates.json")));

        mapper = new ObjectMapper();
        config = mapper.readValue(configJson.toJSONString(), Loginbuddy.class);
        configTemplates = mapper.readValue(configTemplateJson.toJSONString(), Loginbuddy.class);
    }

    @Test
    public void testConfigAssimilate() {

        config.assimilateProviders(configTemplates);

        assertEquals(3, config.getProviders().size()); // no provider was added or removed
        assertEquals("MyLinkedInClientId", config.getProviders().get(0).getClientId());
        assertEquals("ignore", config.getProviders().get(2).getClientId());

    }

    @Test
    public void testConfigTemplateNull() {

        config.assimilateProviders(null);

        assertEquals(3, config.getProviders().size());
        assertNull(config.getProviders().get(0).getAuthorizationEndpoint());

    }

}
