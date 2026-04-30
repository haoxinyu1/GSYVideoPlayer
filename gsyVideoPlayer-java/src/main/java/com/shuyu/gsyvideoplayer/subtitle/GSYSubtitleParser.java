package com.shuyu.gsyvideoplayer.subtitle;

import java.util.List;

public interface GSYSubtitleParser {
    List<GSYSubtitleCue> parse(String text);
}
