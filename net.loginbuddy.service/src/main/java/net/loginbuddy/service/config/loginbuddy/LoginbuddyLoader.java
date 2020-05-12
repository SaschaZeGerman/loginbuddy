package net.loginbuddy.service.config.loginbuddy;

import net.loginbuddy.service.config.Bootstrap;
import net.loginbuddy.service.config.Loader;

public interface LoginbuddyLoader extends Bootstrap, Loader {
    Loginbuddy getLoginbuddy();
}
