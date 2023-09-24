package fr.enimaloc.enutils.jda.register.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import static fr.enimaloc.enutils.jda.utils.AnnotationUtils.STR_DEFAULT;

@Retention(RetentionPolicy.RUNTIME)
public @interface Context {

    String name() default STR_DEFAULT;
    I18n i18n() default @I18n(key = "context.{{context_name}}.name");
}
