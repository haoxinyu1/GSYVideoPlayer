package com.shuyu.gsyvideoplayer.subtitle;

public final class GSYSubtitleParserFactory {

    private GSYSubtitleParserFactory() {
    }

    public static GSYSubtitleParser create(String mimeType) {
        if (GSYSubtitleMime.isWebVtt(mimeType)) {
            return new GSYWebVttSubtitleParser();
        }
        return new GSYSrtSubtitleParser();
    }
}
