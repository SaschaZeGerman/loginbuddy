package net.loginbuddy.config.loginbuddy;

import net.loginbuddy.config.discovery.DiscoveryUtil;
import net.loginbuddy.config.properties.PropertiesUtil;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Logger;

public class OverlordMaster {

    private static Logger LOGGER = Logger.getLogger(String.valueOf(OverlordMaster.class));

    public void initializeConfiguration() {
        if (LoginbuddyUtil.UTIL.isConfigured() && PropertiesUtil.UTIL.isConfigured() && DiscoveryUtil.UTIL.isConfigured()) {
            LOGGER.info("Loginbuddy successfully started!");
            String customLoader = PropertiesUtil.UTIL.getStringProperty("config.loginbuddy.loader.default");
            if (!(customLoader == null || "null".equalsIgnoreCase(customLoader))) {
                try {
                    Class cls = Class.forName(customLoader);
                    LoginbuddyLoader myLoader = null;
                    if (cls.getDeclaredConstructors().length > 1) {
                        for (Constructor constructor : cls.getDeclaredConstructors()) {
                            if (constructor.getParameterCount() == 0) {
                                myLoader = (LoginbuddyLoader) constructor.newInstance();
                                break;
                            }
                        }
                    } else {
                        myLoader = (LoginbuddyLoader) cls.getDeclaredConstructors()[0].newInstance();
                    }
                    LoginbuddyUtil.UTIL.setLoader(myLoader);
                    LOGGER.info(String.format("Custom LoginbuddyLoader was successfully initiated! Class: '%s'", customLoader));
                } catch (InvocationTargetException e) {
                    if (e.getTargetException() != null) {
                        LOGGER.warning(String.format("Custom LoginbuddyLoader created an error: '%s'", e.getTargetException().getMessage()));
                    }
                } catch (Exception e) {
                    LOGGER.warning(String.format("Custom LoginbuddyLoader could not be initiated! Error: '%s'", e.getMessage()));
                }
            } else {
                LOGGER.info("No custom LoginbuddyLoader was configured! Using the default loader");
            }
        } else {
            LOGGER.severe("Stopping Loginbuddy since its configuration could not be loaded! Fix that first!");
            System.exit(0);
        }
    }

}
