package net.loginbuddy.config.management.scope;

import java.lang.annotation.Documented;

@Documented
public @interface RequireScope {
    LoginbuddyScope expected();
}
