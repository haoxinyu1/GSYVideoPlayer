package com.shuyu.gsyvideoplayer.subtitle;

public class GSYSubtitleCue implements Comparable<GSYSubtitleCue> {

    private final long startTimeMs;
    private final long endTimeMs;
    private final String text;

    public GSYSubtitleCue(long startTimeMs, long endTimeMs, String text) {
        this.startTimeMs = startTimeMs;
        this.endTimeMs = endTimeMs;
        this.text = text == null ? "" : text;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public long getEndTimeMs() {
        return endTimeMs;
    }

    public String getText() {
        return text;
    }

    public boolean contains(long positionMs) {
        return positionMs >= startTimeMs && positionMs < endTimeMs;
    }

    @Override
    public int compareTo(GSYSubtitleCue other) {
        if (startTimeMs < other.startTimeMs) {
            return -1;
        }
        if (startTimeMs > other.startTimeMs) {
            return 1;
        }
        return 0;
    }
}
