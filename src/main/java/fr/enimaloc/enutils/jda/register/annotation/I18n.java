package fr.enimaloc.enutils.jda.register.annotation;

import net.dv8tion.jda.api.interactions.DiscordLocale;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public @interface I18n {
    String basePath() default "i18n";
    String key() default "%s";
    MethodTarget target() default @MethodTarget();

    Locale[] locales() default {};

    @interface Locale {
        DiscordLocale language();
        String value();
    }


    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface LocaleSetter {}
}
