package net.loginbuddy.config.discovery;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.naming.ResourceRef;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import java.io.File;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * This factory produces a DiscoveryUtil as enumeration.
 * The default bean factory wold not work with enumerations.
 * See: org.apache.naming.factory.BeanFactory
 * <p>
 * The file server.xml contains the <Context>...</Context> object which includes a reference to this class.
 */
public class Factory implements ObjectFactory {

    private static final Logger LOGGER = Logger.getLogger(Factory.class.getName());

    private final com.fasterxml.jackson.databind.ObjectMapper MAPPER = new ObjectMapper();

    private String path;

    @Override
    public Object getObjectInstance(Object o, Name name, Context context, Hashtable<?, ?> hashtable) throws Exception {
        path = (String) ((ResourceRef) o).get("path").getContent();
        LOGGER.info(String.format("Loading default discovery configuration: '%s'", path));
        return MAPPER.readValue(new File(this.path).getAbsoluteFile(), Discovery.class);
    }
}
