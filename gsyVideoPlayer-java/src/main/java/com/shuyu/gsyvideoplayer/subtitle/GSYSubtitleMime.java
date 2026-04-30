package com.shuyu.gsyvideoplayer.subtitle;

import java.util.Locale;

public final class GSYSubtitleMime {

    public static final String APPLICATION_SUBRIP = "application/x-subrip";
    public static final String TEXT_VTT = "text/vtt";

    private GSYSubtitleMime() {
    }

    public static String infer(String url, String mimeType) {
        if (!isEmpty(mimeType)) {
            return mimeType;
        }
        if (isEmpty(url)) {
            return APPLICATION_SUBRIP;
        }
        String lower = url.toLowerCase(Locale.US);
        int queryIndex = lower.indexOf('?');
        if (queryIndex >= 0) {
            lower = lower.substring(0, queryIndex);
        }
        if (lower.endsWith(".vtt") || lower.endsWith(".webvtt")) {
            return TEXT_VTT;
        }
        return APPLICATION_SUBRIP;
    }

    public static boolean isWebVtt(String mimeType) {
        return TEXT_VTT.equalsIgnoreCase(mimeType);
    }

    private static boolean isEmpty(String value) {
        return value == null || value.length() == 0;
    }
}
