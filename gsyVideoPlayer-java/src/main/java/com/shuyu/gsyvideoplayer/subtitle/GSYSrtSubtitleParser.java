package com.shuyu.gsyvideoplayer.subtitle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GSYSrtSubtitleParser implements GSYSubtitleParser {

    @Override
    public List<GSYSubtitleCue> parse(String text) {
        List<GSYSubtitleCue> cues = new ArrayList<>();
        if (text == null || text.length() == 0) {
            return cues;
        }

        String[] blocks = text.replace("\r\n", "\n").replace('\r', '\n').replace("\uFEFF", "").split("\n\\s*\n+");
        for (String block : blocks) {
            parseBlock(block, cues);
        }
        Collections.sort(cues);
        return cues;
    }

    private void parseBlock(String block, List<GSYSubtitleCue> cues) {
        String[] lines = block.split("\n");
        int timeLineIndex = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].contains("-->")) {
                timeLineIndex = i;
                break;
            }
        }
        if (timeLineIndex < 0) {
            return;
        }

        String[] times = lines[timeLineIndex].split("-->");
        if (times.length < 2) {
            return;
        }
        long start = GSYSubtitleTimeParser.parse(cleanTime(times[0]));
        long end = GSYSubtitleTimeParser.parse(cleanTime(times[1]));
        if (end <= start) {
            return;
        }

        StringBuilder builder = new StringBuilder();
        for (int i = timeLineIndex + 1; i < lines.length; i++) {
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(lines[i].trim());
        }
        if (builder.length() > 0) {
            cues.add(new GSYSubtitleCue(start, end, builder.toString()));
        }
    }

    private String cleanTime(String value) {
        int spaceIndex = value.trim().indexOf(' ');
        if (spaceIndex > 0) {
            return value.trim().substring(0, spaceIndex);
        }
        return value;
    }
}
