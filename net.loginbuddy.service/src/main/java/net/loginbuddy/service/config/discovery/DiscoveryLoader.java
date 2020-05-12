package net.loginbuddy.service.config.discovery;

import net.loginbuddy.service.config.Bootstrap;
import net.loginbuddy.service.config.Loader;

public interface DiscoveryLoader extends Bootstrap, Loader {
    Discovery getDiscoveryConfig();
}
