package net.loginbuddy.config.scope;

import java.lang.annotation.Documented;

@Documented
public @interface RequireScope {
    LoginbuddyScope expected();
}
