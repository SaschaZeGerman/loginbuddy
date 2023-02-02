package net.loginbuddy.config.loginbuddy;

import hthurow.tomcatjndi.TomcatJNDI;
import net.loginbuddy.common.api.HttpHelper;
import org.apache.http.MethodNotSupportedException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.junit.Assert.*;

public class TestLoginbuddyConfigSidecar {

    private TomcatJNDI tomcatJNDI;

    @Before
    public void setup() {
        tomcatJNDI = new TomcatJNDI();
        tomcatJNDI.processContextXml(new File("src/test/resources/testContextSidecar.xml"));
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
        assertEquals("confidential", LoginbuddyUtil.UTIL.getClientConfigByClientId("loginbuddy-sidecar").getClientType());
    }
}