package net.loginbuddy.config.properties;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.naming.ResourceRef;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import java.io.File;
import java.io.FileReader;
import java.util.Hashtable;
import java.util.Properties;
import java.util.logging.Logger;

public class Factory implements ObjectFactory {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(Factory.class));

    private com.fasterxml.jackson.databind.ObjectMapper MAPPER = new ObjectMapper();

    private String path;

    @Override
    public Object getObjectInstance(Object o, Name name, Context context, Hashtable<?, ?> hashtable) throws Exception {
        path = (String) ((ResourceRef) o).get("path").getContent();
        LOGGER.info(String.format("Loading default loginbuddy properties: '%s'", path));
        Properties props = new Properties();
        props.load(new FileReader(new File(path)));
        return props;
    }
}