package com.shuyu.gsyvideoplayer.subtitle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GSYWebVttSubtitleParser implements GSYSubtitleParser {

    @Override
    public List<GSYSubtitleCue> parse(String text) {
        List<GSYSubtitleCue> cues = new ArrayList<>();
        if (text == null || text.length() == 0) {
            return cues;
        }

        String[] lines = text.replace("\r\n", "\n").replace('\r', '\n').replace("\uFEFF", "").split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0 || "WEBVTT".equalsIgnoreCase(line)) {
                continue;
            }
            if (line.startsWith("NOTE") || "STYLE".equalsIgnoreCase(line) || "REGION".equalsIgnoreCase(line)) {
                while (i + 1 < lines.length && lines[i + 1].trim().length() > 0) {
                    i++;
                }
                continue;
            }
            if (!line.contains("-->") && i + 1 < lines.length && lines[i + 1].contains("-->")) {
                i++;
                line = lines[i].trim();
            }
            if (!line.contains("-->")) {
                continue;
            }
            String[] times = line.split("-->");
            if (times.length < 2) {
                continue;
            }
            long start = GSYSubtitleTimeParser.parse(cleanTime(times[0]));
            long end = GSYSubtitleTimeParser.parse(cleanTime(times[1]));
            if (end <= start) {
                continue;
            }

            StringBuilder builder = new StringBuilder();
            while (i + 1 < lines.length && lines[i + 1].trim().length() > 0) {
                i++;
                if (builder.length() > 0) {
                    builder.append('\n');
                }
                builder.append(lines[i].trim());
            }
            if (builder.length() > 0) {
                cues.add(new GSYSubtitleCue(start, end, builder.toString()));
            }
        }
        Collections.sort(cues);
        return cues;
    }

    private String cleanTime(String value) {
        String trimmed = value.trim();
        int spaceIndex = trimmed.indexOf(' ');
        if (spaceIndex > 0) {
            return trimmed.substring(0, spaceIndex);
        }
        return trimmed;
    }
}
