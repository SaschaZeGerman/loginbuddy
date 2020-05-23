package net.loginbuddy.config.loginbuddy;

import net.loginbuddy.config.Bootstrap;
import net.loginbuddy.config.Loader;

public interface LoginbuddyLoader extends Bootstrap, Loader {
    Loginbuddy getLoginbuddy();
}
