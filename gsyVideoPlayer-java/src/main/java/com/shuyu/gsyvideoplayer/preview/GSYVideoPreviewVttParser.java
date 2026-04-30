package com.shuyu.gsyvideoplayer.preview;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses WebVTT thumbnail tracks with either independent images or sprite xywh fragments.
 */
public final class GSYVideoPreviewVttParser {

    private static final String TIME_SEPARATOR = "-->";
    private static final String XYWH_PREFIX = "#xywh=";

    private GSYVideoPreviewVttParser() {
    }

    public static GSYVideoPreviewProvider parse(String vttContent, String baseUrl) {
        return new GSYVideoPreviewListProvider(parseFrames(vttContent, baseUrl));
    }

    public static List<GSYVideoPreviewFrame> parseFrames(String vttContent, String baseUrl) {
        List<GSYVideoPreviewFrame> frames = new ArrayList<>();
        if (vttContent == null || vttContent.length() == 0) {
            return frames;
        }
        String[] lines = vttContent.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (!line.contains(TIME_SEPARATOR)) {
                continue;
            }
            String[] timeRange = line.split(TIME_SEPARATOR, 2);
            long startMs = parseTimeMs(timeRange[0].trim());
            long endMs = parseTimeMs(timeRange[1].trim().split("\\s+")[0]);
            String imageSpec = null;
            while (++i < lines.length) {
                String nextLine = lines[i].trim();
                if (nextLine.length() == 0) {
                    break;
                }
                if (nextLine.startsWith("NOTE")) {
                    continue;
                }
                imageSpec = nextLine;
                break;
            }
            if (startMs >= 0 && endMs > startMs && imageSpec != null) {
                frames.add(parseFrame(startMs, endMs, imageSpec, baseUrl));
            }
        }
        return frames;
    }

    public static long parseTimeMs(String timeText) {
        if (timeText == null) {
            return -1;
        }
        String[] parts = timeText.trim().split(":");
        try {
            double seconds;
            if (parts.length == 3) {
                seconds = Long.parseLong(parts[0]) * 3600
                        + Long.parseLong(parts[1]) * 60
                        + Double.parseDouble(parts[2]);
            } else if (parts.length == 2) {
                seconds = Long.parseLong(parts[0]) * 60
                        + Double.parseDouble(parts[1]);
            } else {
                return -1;
            }
            return (long) (seconds * 1000);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private static GSYVideoPreviewFrame parseFrame(long startMs, long endMs, String imageSpec, String baseUrl) {
        String imageUrl = imageSpec;
        int cropX = -1;
        int cropY = -1;
        int cropWidth = -1;
        int cropHeight = -1;

        int xywhIndex = imageSpec.indexOf(XYWH_PREFIX);
        if (xywhIndex >= 0) {
            imageUrl = imageSpec.substring(0, xywhIndex);
            String[] xywh = imageSpec.substring(xywhIndex + XYWH_PREFIX.length()).split(",");
            if (xywh.length == 4) {
                try {
                    cropX = Integer.parseInt(xywh[0].trim());
                    cropY = Integer.parseInt(xywh[1].trim());
                    cropWidth = Integer.parseInt(xywh[2].trim());
                    cropHeight = Integer.parseInt(xywh[3].trim());
                } catch (NumberFormatException e) {
                    cropX = cropY = cropWidth = cropHeight = -1;
                }
            }
        }
        return new GSYVideoPreviewFrame(startMs, endMs, resolveUrl(baseUrl, imageUrl),
                cropX, cropY, cropWidth, cropHeight);
    }

    private static String resolveUrl(String baseUrl, String imageUrl) {
        if (imageUrl == null || imageUrl.length() == 0 || baseUrl == null || baseUrl.length() == 0) {
            return imageUrl;
        }
        try {
            return new URI(baseUrl).resolve(imageUrl).toString();
        } catch (Exception e) {
            return imageUrl;
        }
    }
}
