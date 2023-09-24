package fr.enimaloc.enutils.jda.register.annotation;

import static fr.enimaloc.enutils.jda.utils.AnnotationUtils.STR_DEFAULT;

public @interface MethodTarget {
    String method() default STR_DEFAULT;
    Class<?> clazz() default Void.class;
}
