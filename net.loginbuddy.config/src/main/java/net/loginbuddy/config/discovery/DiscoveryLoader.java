package net.loginbuddy.config.discovery;

import net.loginbuddy.config.Bootstrap;
import net.loginbuddy.config.Loader;

public interface DiscoveryLoader extends Bootstrap, Loader {
    Discovery getDiscoveryConfig();
}
