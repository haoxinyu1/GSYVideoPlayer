package com.shuyu.gsyvideoplayer.subtitle;

final class GSYSubtitleTimeParser {

    private GSYSubtitleTimeParser() {
    }

    static long parse(String value) {
        String normalized = value.trim().replace(',', '.');
        String[] parts = normalized.split(":");
        if (parts.length < 2) {
            return 0;
        }

        long hours = 0;
        long minutes;
        String secondsPart;
        if (parts.length == 3) {
            hours = parseLong(parts[0]);
            minutes = parseLong(parts[1]);
            secondsPart = parts[2];
        } else {
            minutes = parseLong(parts[0]);
            secondsPart = parts[1];
        }

        long seconds;
        long millis = 0;
        int dot = secondsPart.indexOf('.');
        if (dot >= 0) {
            seconds = parseLong(secondsPart.substring(0, dot));
            String fraction = secondsPart.substring(dot + 1);
            if (fraction.length() > 3) {
                fraction = fraction.substring(0, 3);
            }
            while (fraction.length() < 3) {
                fraction += "0";
            }
            millis = parseLong(fraction);
        } else {
            seconds = parseLong(secondsPart);
        }
        return hours * 3600000L + minutes * 60000L + seconds * 1000L + millis;
    }

    private static long parseLong(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
