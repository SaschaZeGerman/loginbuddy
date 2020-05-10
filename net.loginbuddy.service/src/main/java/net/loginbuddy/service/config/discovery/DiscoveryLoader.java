package net.loginbuddy.service.config.discovery;

import net.loginbuddy.service.config.Bootstrap;

public interface DiscoveryLoader extends Bootstrap {
    void loadDiscovery();
    void reloadDiscovery();
    DiscoveryConfig getDiscoveryConfig();
}
