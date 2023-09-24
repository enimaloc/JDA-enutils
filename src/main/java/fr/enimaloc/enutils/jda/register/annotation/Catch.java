package fr.enimaloc.enutils.jda.register.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface Catch {

    Class<? extends Throwable>[] value() default {Throwable.class};

}
