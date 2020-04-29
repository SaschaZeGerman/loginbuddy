package net.loginbuddy.service.management;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

@Documented
@Target(ElementType.PARAMETER)
public @interface ActualScope {
}
