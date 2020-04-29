package net.loginbuddy.service.management;

import java.lang.annotation.Documented;

@Documented
public @interface RequireScope {
    LoginbuddyScope expected();
}
