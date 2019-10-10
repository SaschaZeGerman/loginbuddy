package net.loginbuddy.common.util;

import java.util.regex.Pattern;

public class Sanetizer {

    private static Pattern urlPattern = Pattern.compile("^http[s]?://[a-zA-Z0-9.\\-:/]{1,92}");

    public static String sanetize(String input) {
        return sanetize(input, 0);
    }

    public static String sanetize(String input, int maxLength) {
        if(input == null || input.trim().length() == 0) {
            return "";
        }
        String result = input
                .replaceAll("<","&lt;")
                .replaceAll(">","&gt;")
                .replaceAll("&", "&amp;")
                .replaceAll("\"","&quot;")
                .replaceAll("'", "&#x27;")
                .replaceAll("/", "&#x2F;");
        if(maxLength == 0) {
            return result;
        } else {
            return result.length() <= maxLength ? result : result.substring(0, maxLength);
        }
    }

    public static String sanetizeUrl(String input, int maxLength) {
        if(input == null || input.trim().length() == 0) {
            return "";
        }
        if(urlPattern.matcher(input).matches()){
            if(maxLength == 0) {
                return input;
            } else {
                return input.length() <= maxLength ? input : input.substring(0, maxLength);
            }
        } else {
            return ("The given URL is invalid! Please correct it!");
        }
    }
}
