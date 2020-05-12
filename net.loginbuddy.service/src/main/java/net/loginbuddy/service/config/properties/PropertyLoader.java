package net.loginbuddy.service.config.properties;

import net.loginbuddy.service.config.Bootstrap;
import net.loginbuddy.service.config.Loader;

import java.util.Properties;

public interface PropertyLoader extends Bootstrap, Loader {
    Properties getProperties();
}
