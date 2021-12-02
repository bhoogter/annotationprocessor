package com.benjaminhoogterp;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
public @interface WrapMethod {
    boolean recordParams() default true;

    boolean recordResult() default true;
}
