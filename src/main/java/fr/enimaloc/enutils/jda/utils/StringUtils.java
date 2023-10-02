package fr.enimaloc.enutils.jda.utils;

import org.jetbrains.annotations.Nullable;

public class StringUtils {
    private StringUtils() {}

    public static String truncate(String s, int maxLength) {
        return truncate(s, maxLength, "...");
    }

    public static String truncate(String s, int maxLength, @Nullable String end) {
        if (s == null) {
            return null;
        }
        if (end == null) {
            end = "";
        }
        return s.length() > maxLength - end.length() ? s.substring(0, maxLength - end.length()) + end : s;
    }
}
