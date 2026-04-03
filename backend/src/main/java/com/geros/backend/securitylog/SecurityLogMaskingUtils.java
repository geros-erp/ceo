package com.geros.backend.securitylog;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SecurityLogMaskingUtils {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("([A-Za-z0-9._%+-]+)@([A-Za-z0-9.-]+\\.[A-Za-z]{2,})");
    private static final Pattern IPV4_PATTERN = Pattern.compile("\\b(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\b");
    private static final Pattern IPV6_PATTERN = Pattern.compile("(?i)\\b([0-9a-f]{1,4}:){2,}[0-9a-f]{1,4}\\b");

    private SecurityLogMaskingUtils() {
    }

    public static String maskEmail(String value) {
        if (value == null || value.isBlank() || !value.contains("@")) {
            return value;
        }

        String[] parts = value.split("@", 2);
        String local = parts[0];
        String domain = parts[1];

        String maskedLocal = local.length() <= 2
                ? local.charAt(0) + "*"
                : local.substring(0, 2) + repeat('*', Math.max(local.length() - 2, 3));

        int dotIndex = domain.indexOf('.');
        String domainName = dotIndex >= 0 ? domain.substring(0, dotIndex) : domain;
        String suffix = dotIndex >= 0 ? domain.substring(dotIndex) : "";
        String maskedDomain = domainName.isEmpty()
                ? "***"
                : domainName.substring(0, 1) + repeat('*', Math.max(domainName.length() - 1, 3));

        return maskedLocal + "@" + maskedDomain + suffix;
    }

    public static String maskIp(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        Matcher ipv4Matcher = IPV4_PATTERN.matcher(value);
        if (ipv4Matcher.matches()) {
            return ipv4Matcher.group(1) + "." + ipv4Matcher.group(2) + ".*.*";
        }

        Matcher ipv6Matcher = IPV6_PATTERN.matcher(value);
        if (ipv6Matcher.matches()) {
            String[] segments = value.split(":");
            if (segments.length >= 2) {
                return segments[0] + ":" + segments[1] + ":****:****";
            }
        }

        return value;
    }

    public static String maskHost(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        if (value.contains("@")) {
            return maskEmail(value);
        }
        if (value.length() <= 4) {
            return value.charAt(0) + "***";
        }
        return value.substring(0, 2) + repeat('*', Math.max(value.length() - 2, 3));
    }

    public static String maskFreeText(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }

        String masked = maskEmbeddedEmails(value);
        masked = maskEmbeddedIps(masked);
        masked = masked.replaceAll("(?i)(password|contrasena|clave)\\s*=\\s*[^,\\s]+", "$1=***");
        masked = masked.replaceAll("(?i)(token)\\s*=\\s*[^,\\s]+", "$1=***");
        return masked;
    }

    private static String maskEmbeddedEmails(String value) {
        Matcher matcher = EMAIL_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(maskEmail(matcher.group())));
        }
        matcher.appendTail(buffer);
        return buffer.toString();
    }

    private static String maskEmbeddedIps(String value) {
        Matcher matcher = IPV4_PATTERN.matcher(value);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(maskIp(matcher.group())));
        }
        matcher.appendTail(buffer);
        return buffer.toString().replaceAll("(?i)\\b([0-9a-f]{1,4}:){2,}[0-9a-f]{1,4}\\b", "****:****");
    }

    private static String repeat(char character, int count) {
        return String.valueOf(character).repeat(Math.max(count, 0));
    }
}
