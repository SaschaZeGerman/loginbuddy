package net.loginbuddy.sidecar.server;

import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.loginbuddy.LoginbuddyLoader;
import net.loginbuddy.config.loginbuddy.LoginbuddyUtil;
import net.loginbuddy.config.properties.PropertiesUtil;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.util.logging.Logger;

public class Overlord extends HttpServlet {

    private static Logger LOGGER = Logger.getLogger(String.valueOf(Overlord.class));

    @Override
    public void init() throws ServletException {
        super.init();
        // initialize the configuration. If this fails, there is no reason to continue

        if (LoginbuddyUtil.UTIL.isConfigured() && PropertiesUtil.UTIL.isConfigured() && DiscoveryUtil.UTIL.isConfigured()) {
            LOGGER.info("Loginbuddy successfully started!");
            String customLoader = PropertiesUtil.UTIL.getStringProperty("config.loginbuddy.loader.default");
            if (customLoader != null) {
                try {
                    Class cls = Class.forName(customLoader);
                    LoginbuddyLoader myLoader = (LoginbuddyLoader) cls.getDeclaredConstructors()[0].newInstance();
                    LoginbuddyUtil.UTIL.setLoader(myLoader);
                    LOGGER.info(String.format("Custom LoginbuddyLoader was successfully initiated! Class: '%s'", customLoader));
                } catch (Exception e) {
                    LOGGER.warning(String.format("Custom LoginbuddyLoader could not be initiated! Error: '%s'", e.getMessage()));
                }
            }
        } else {
            LOGGER.severe("Stopping Loginbuddy since its configuration could not be loaded! Fix that first!");
            System.exit(0);
        }
    }
}