package net.loginbuddy.config.loginbuddy;

import net.loginbuddy.common.util.CertificateManager;
import org.apache.naming.ResourceRef;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.spi.ObjectFactory;
import java.io.File;
import java.io.FileReader;
import java.util.Hashtable;
import java.util.logging.Logger;

public class Factory implements ObjectFactory {

    private static Logger LOGGER = Logger.getLogger(String.valueOf(Factory.class));

    @Override
    public Object getObjectInstance(Object o, Name name, Context context, Hashtable<?, ?> hashtable) throws Exception {

        CertificateManager.loadTrustedServers();

        String path = (String) ((ResourceRef) o).get("path").getContent();
        LOGGER.info(String.format("Loading default Loginbuddy configuration: '%s'", path));
        JSONObject configJson = (JSONObject) new JSONParser().parse(new FileReader(new File(path)));
        JSONObject configTemplateJson = null;

        RefAddr addr = ((ResourceRef) o).get("pathTemplate");
        if(addr != null) {
            String pathTemplate = (String)addr.getContent();
            if (pathTemplate != null) {
                LOGGER.info(String.format("Loading template configuration: '%s'", pathTemplate));
                configTemplateJson = (JSONObject) new JSONParser().parse(new FileReader(new File(pathTemplate)));
            }
        }

        LOGGER.info(String.format("Loaded Loginbuddy configuration file: '%s'", path));
        return new LoginbuddyObjectMapper().readLoginbuddy(configJson, configTemplateJson);
    }
}