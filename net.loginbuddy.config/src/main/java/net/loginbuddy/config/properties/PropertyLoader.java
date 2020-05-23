package net.loginbuddy.config.properties;

import net.loginbuddy.config.Bootstrap;
import net.loginbuddy.config.Loader;

import java.util.Properties;

public interface PropertyLoader extends Bootstrap, Loader {
    Properties getProperties();
}
