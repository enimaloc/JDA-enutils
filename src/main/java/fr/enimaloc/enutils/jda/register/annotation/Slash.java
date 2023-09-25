package fr.enimaloc.enutils.jda.register.annotation;

import java.lang.annotation.*;

import static fr.enimaloc.enutils.jda.utils.AnnotationUtils.*;


@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface Slash {
    String name() default STR_DEFAULT;
    String description() default STR_DEFAULT;
    I18n i18nName() default @I18n(key = "commands.{{command_name}}.name");
    I18n i18nDescription() default @I18n(key = "commands.{{command_name}}.description");


    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.PARAMETER)
    @interface Option {
        String name() default STR_DEFAULT;
        String description() default STR_DEFAULT;


        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.PARAMETER)
        @interface Range {
            double min() default Double.MIN_NORMAL;
            double max() default Double.MAX_VALUE;
        }


        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.PARAMETER)
        @interface Length {
            int min() default 0;
            int max() default 6000;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.PARAMETER)
        @Repeatable(Choices.class)
        @interface Choice {
            String value();
            String devValue() default STR_DEFAULT;
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.PARAMETER)
        @interface Choices {
            Choice[] value() default {};
        }

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.PARAMETER)
        @interface AutoCompletion {
            MethodTarget target() default @MethodTarget();
            String[] array() default {};
        }

    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface Sub {
        String name() default STR_DEFAULT;
        String description() default STR_DEFAULT;
        I18n i18nName() default @I18n(key = "commands.{{command_name}}.name");
        I18n i18nDescription() default @I18n(key = "commands.{{command_name}}.description");

        @Retention(RetentionPolicy.RUNTIME)
        @Target(ElementType.FIELD)
        @interface GroupProvider {
            String name() default STR_DEFAULT;
            String description() default STR_DEFAULT;
            I18n i18nName() default @I18n(key = "commands.{{command_name}}.name");
            I18n i18nDescription() default @I18n(key = "commands.{{command_name}}.description");
        }
    }
}