package fr.enimaloc.enutils.jda.utils;

import fr.enimaloc.enutils.jda.register.annotation.MethodTarget;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Optional;

public class Utils {
    public static Optional<Method> getMethod(Object sourceInstance, MethodTarget target) {
        return getMethod(sourceInstance.getClass(), target);
    }

    public static Optional<Method> getMethod(Class<?> sourceClazz, MethodTarget target) {
        return getMethod(sourceClazz, target.clazz(), target.method());
    }

    public static Optional<Method> getMethod(Object sourceInstance, Class<?> clazz, String name) {
        return getMethod(sourceInstance.getClass(), clazz, name);
    }

    public static Optional<Method> getMethod(Class<?> sourceClazz, Class<?> clazz, String name) {
        if (clazz == null || clazz == Void.class) {
            clazz = sourceClazz;
        }
        return Arrays.stream(clazz.getDeclaredMethods()).filter(m -> m.getName().equals(name)).findFirst();
    }
}
