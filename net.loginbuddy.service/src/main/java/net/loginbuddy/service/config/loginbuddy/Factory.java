package net.loginbuddy.service.config.loginbuddy;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.naming.ResourceRef;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import java.io.File;
import java.util.Hashtable;
import java.util.logging.Logger;

public class Factory implements ObjectFactory {

    private Logger LOGGER = Logger.getLogger(String.valueOf(Factory.class));

    private com.fasterxml.jackson.databind.ObjectMapper MAPPER = new ObjectMapper();

    private String path;

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    @Override
    public Object getObjectInstance(Object o, Name name, Context context, Hashtable<?, ?> hashtable) throws Exception {
        path = (String) ((ResourceRef) o).get("path").getContent();
        LOGGER.info(String.format("Loading default loginbuddy configuration: '%s'", path));
        return MAPPER.readValue(new File(this.path).getAbsoluteFile(), Loginbuddy.class);
    }
}
