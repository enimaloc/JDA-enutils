package fr.enimaloc.enutils.jda.utils;

import fr.enimaloc.enutils.jda.utils.function.Catcher;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.Contract;

import java.lang.annotation.Annotation;
import java.lang.reflect.AccessibleObject;
import java.lang.reflect.AnnotatedElement;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;
import java.util.regex.Pattern;

public class Checks {

    private Checks() {}

    public static void min(int value, int min) {
        min(value, min, "Value must be greater than or equal to " + min);
    }

    public static void min(int value, int min, String message) {
        check(value >= min, message);
    }

    public static void max(int value, int max) {
        max(value, max, "Value must be less than or equal to " + max);
    }

    public static void max(int value, int max, String message) {
        check(value <= max, message);
    }

    public static void equals(int value, int equals) {
        equals(value, equals, "Value must be equals to " + equals);
    }

    public static void equals(int value, int equals, String message) {
        check(value == equals, message);
    }


    @Contract("null, _ -> fail; _, _ -> _")
    public static void notNull(Object object) {
        notNull(object, "Object cannot be null");
    }

    @Contract("null, _ -> fail; _, _ -> _")
    public static void notNull(Object object, String message) {
        check(object != null, message);
    }

    public static void notEmpty(String string) {
        notEmpty(string, "String cannot be empty");
    }

    public static void notEmpty(String string, String message) {
        check(!string.isEmpty(), message);
    }

    public static void notBlank(String string) {
        notBlank(string, "String cannot be blank");
    }

    public static void notBlank(String string, String message) {
        check(!string.isBlank(), message);
    }

    public static void notEmpty(Object[] array) {
        notEmpty(array, "Array cannot be empty");
    }

    public static void notEmpty(Object[] array, String message) {
        check(array.length > 0, message);
    }

    public static void notEmpty(Collection<?> collection) {
        notEmpty(collection, "Collection cannot be empty");
    }

    public static void notEmpty(Collection<?> collection, String message) {
        check(!collection.isEmpty(), message);
    }

    public static void inRange(int value, int min, int max) {
        inRange(value, min, max, "Value must be in range [" + min + ", " + max + "]");
    }

    public static void inRange(int value, int min, int max, String message) {
        check(value >= min && value <= max, message);
    }

    public static void equals(Object object1, Object object2) {
        equals(object1, object2, "Objects are not equals");
    }

    public static void equals(Object object1, Object object2, String message) {
        check(object1.equals(object2), message);
    }

    public static void equals(Object[] array1, Object[] array2) {
        equals(array1, array2, "Arrays are not equals");
    }

    public static void equals(Object[] array1, Object[] array2, String message) {
        check(Arrays.equals(array1, array2), message);
    }

    public static void notEquals(Object object1, Object object2) {
        notEquals(object1, object2, "Objects are equals");
    }

    public static void notEquals(Object object1, Object object2, String message) {
        check(!object1.equals(object2), message);
    }

    public static <T> void allEquals(T[] array, T value) {
        allEquals(array, value, "All elements must be equals to " + value);
    }

    public static <T> void allEquals(T[] array, T value, String message) {
        check(Arrays.stream(array).allMatch(value::equals), message);
    }

    public static <T> void allSame(T[] array) {
        allSame(array, "All elements must be the same");
    }

    public static <T> void allSame(T[] array, String message) {
        check(Arrays.stream(array).allMatch(array[0]::equals), message);
    }

    public static <T> void allMatch(T[] array, Predicate<T> predicate) {
        allMatch(array, predicate, "All elements of the array must match the predicate");
    }

    public static <T> void allMatch(T[] array, Predicate<T> predicate, String message) {
        check(Arrays.stream(array).allMatch(predicate), message);
    }

    public static <T> void contains(T object, T[] array) {
        contains(object, array, "Array does not contain " + object);
    }

    public static <T> void contains(T object, T[] array, String message) {
        check(Arrays.stream(array).anyMatch(object::equals), message);
    }

    public static <T> void contains(T object, Collection<T> collection) {
        contains(object, collection, "Collection does not contain " + object);
    }

    public static <T> void contains(T object, Collection<T> collection, String message) {
        check(collection.contains(object), message);
    }

    public static <T> void notContains(T object, T[] array) {
        notContains(object, array, "Array contains " + object);
    }

    public static <T> void notContains(T object, T[] array, String message) {
        check(Arrays.stream(array).noneMatch(object::equals), message);
    }

    public static <T> void notContains(T object, Collection<T> collection) {
        notContains(object, collection, "Collection contains " + object);
    }

    public static <T> void notContains(T object, Collection<T> collection, String message) {
        check(!collection.contains(object), message);
    }

    @Contract("false, _ -> fail")
    public static void check(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    public static void or(Catcher<IllegalArgumentException>... checks) {
        int thrown = 0;
        IllegalArgumentException last = new IllegalArgumentException("No checks passed.");
        for (Catcher<IllegalArgumentException> check : checks) {
            try {
                check.run();
            } catch (IllegalArgumentException e) {
                last   = e;
                thrown++;
            }
        }
        if (thrown == checks.length) {
            throw last;
        }
    }

    public static void matchRegex(String name, @Language("regexp") String regex) {
        matchRegex(name, regex, "The name " + name + " does not match the regex " + regex);
    }

    public static void matchRegex(String name, @Language("regexp") String regex, String message) {
        check(name.matches(regex), message);
    }

    public static void matchRegex(String name, @Language("regexp") String regex, int flags) {
        matchRegex(name, regex, flags, "The name " + name + " does not match the regex " + regex);
    }

    public static void matchRegex(String name, @Language("regexp") String regex, int flags, String message) {
        check(Pattern.compile(regex, flags).matcher(name).matches(), message);
    }

    public static class Reflection {

        Reflection() {}

        public static void assignableFrom(Class<?> clazz, Class<?> assignable) {
            assignableFrom(clazz, assignable, "The class " + clazz.getName() + " is not assignable from " + assignable.getName());
        }

        public static void assignableFrom(Class<?> clazz, Class<?> assignable, String message) {
            check(assignable.isAssignableFrom(clazz), message);
        }

        public static void annotationPresent(AnnotatedElement element, Class<? extends Annotation> annotation) {
            annotationPresent(element, annotation, "Annotation " + annotation.getName() + " is not present on " + element);
        }

        public static void annotationPresent(
                AnnotatedElement element, Class<? extends Annotation> annotation, String message
        ) {
            check(element.isAnnotationPresent(annotation), message);
        }

        public static void trySetAccessible(AccessibleObject object) {
            trySetAccessible(object, "Cannot set object accessible.");
        }

        public static void trySetAccessible(AccessibleObject object, String message) {
            check(object.trySetAccessible(), message);
        }
    }
}
