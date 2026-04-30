package com.shuyu.gsyvideoplayer.subtitle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GSYSubtitleProvider {

    private final List<GSYSubtitleCue> cues;

    public GSYSubtitleProvider(List<GSYSubtitleCue> cues) {
        this.cues = cues == null ? new ArrayList<GSYSubtitleCue>() : new ArrayList<>(cues);
        Collections.sort(this.cues);
    }

    public String findText(long positionMs) {
        StringBuilder builder = new StringBuilder();
        for (GSYSubtitleCue cue : cues) {
            if (cue.getStartTimeMs() > positionMs) {
                break;
            }
            if (cue.contains(positionMs)) {
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(cue.getText());
            }
        }
        return builder.toString();
    }

    public int size() {
        return cues.size();
    }
}
