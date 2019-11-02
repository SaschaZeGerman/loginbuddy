package net.loginbuddy.common.util;

import java.util.logging.Logger;
import java.util.regex.Pattern;

public class Sanetizer {

    private static final Logger LOGGER = Logger.getLogger(String.valueOf(Sanetizer.class));

    private static Pattern urlPattern = Pattern.compile("^http[s]?://[a-zA-Z0-9.\\-:_/]{1,256}"); // will be extended over time as required
    private static Pattern urlPathPattern = Pattern.compile("^[.]{0,2}/[a-zA-Z0-9.\\-:_/]{1,256}"); // will be extended over time as required

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

    public static String checkForUrlPattern(String input, int maxLength) {
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
            LOGGER.warning(String.format("The given URL is invalid! URL: '%s', max length: '%d'", input, maxLength));
            return ("The given URL is invalid! Please correct it!");
        }
    }

    public static String checkForUrlPathPattern(String input, int maxLength) {
        if(input == null || input.trim().length() == 0) {
            return "";
        }
        if(urlPathPattern.matcher(input).matches()){
            if(maxLength == 0) {
                return input;
            } else {
                return input.length() <= maxLength ? input : input.substring(0, maxLength);
            }
        } else {
            LOGGER.warning(String.format("The given URL path is invalid! URL path: '%s', max length: '%d'", input, maxLength));
            return ("The given URL path is invalid! Please correct it!");
        }
    }
}
