package fr.enimaloc.enutils.jda.utils;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

public class AnnotationUtils {
    public static final int INT_DEFAULT = Integer.MIN_VALUE;
    public static final double DOUBLE_DEFAULT = Double.MIN_VALUE;
    public static final float FLOAT_DEFAULT = Float.MIN_VALUE;
    public static final long LONG_DEFAULT = Long.MIN_VALUE;
    public static final short SHORT_DEFAULT = Short.MIN_VALUE;
    public static final byte BYTE_DEFAULT = Byte.MIN_VALUE;
    public static final char CHAR_DEFAULT = '\0';
    public static final String STR_DEFAULT = "\0";

    public static boolean isDefault(int i) {
        return i == INT_DEFAULT;
    }

    public static boolean isDefault(double d) {
        return d == DOUBLE_DEFAULT;
    }

    public static boolean isDefault(float f) {
        return f == FLOAT_DEFAULT;
    }

    public static boolean isDefault(long l) {
        return l == LONG_DEFAULT;
    }

    public static boolean isDefault(short s) {
        return s == SHORT_DEFAULT;
    }

    public static boolean isDefault(byte b) {
        return b == BYTE_DEFAULT;
    }

    public static boolean isDefault(char c) {
        return c == CHAR_DEFAULT;
    }

    public static boolean isDefault(String s) {
        return s.equals(STR_DEFAULT);
    }

    public static boolean isDefault(Object o) {
        return o == null;
    }

    public static OptionalInt get(int i) {
        return isDefault(i) ? OptionalInt.empty() : OptionalInt.of(i);
    }

    public static OptionalDouble get(double d) {
        return isDefault(d) ? OptionalDouble.empty() : OptionalDouble.of(d);
    }

    public static Optional<Float> get(float f) {
        return isDefault(f) ? Optional.empty() : Optional.of(f);
    }

    public static OptionalLong get(long l) {
        return isDefault(l) ? OptionalLong.empty() : OptionalLong.of(l);
    }

    public static Optional<Short> get(short s) {
        return isDefault(s) ? Optional.empty() : Optional.of(s);
    }

    public static Optional<Byte> get(byte b) {
        return isDefault(b) ? Optional.empty() : Optional.of(b);
    }

    public static Optional<Character> get(char c) {
        return isDefault(c) ? Optional.empty() : Optional.of(c);
    }

    public static Optional<String> get(String s) {
        return isDefault(s) ? Optional.empty() : Optional.of(s);
    }

    private AnnotationUtils() {}
}
