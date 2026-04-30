package com.shuyu.gsyvideoplayer.preview;

/**
 * Thumbnail metadata for a timeline preview frame.
 */
public class GSYVideoPreviewFrame {

    private final long startTimeMs;
    private final long endTimeMs;
    private final String imageUrl;
    private final int cropX;
    private final int cropY;
    private final int cropWidth;
    private final int cropHeight;

    public GSYVideoPreviewFrame(long startTimeMs, long endTimeMs, String imageUrl) {
        this(startTimeMs, endTimeMs, imageUrl, -1, -1, -1, -1);
    }

    public GSYVideoPreviewFrame(long startTimeMs, long endTimeMs, String imageUrl,
                                int cropX, int cropY, int cropWidth, int cropHeight) {
        this.startTimeMs = startTimeMs;
        this.endTimeMs = endTimeMs;
        this.imageUrl = imageUrl;
        this.cropX = cropX;
        this.cropY = cropY;
        this.cropWidth = cropWidth;
        this.cropHeight = cropHeight;
    }

    public long getStartTimeMs() {
        return startTimeMs;
    }

    public long getEndTimeMs() {
        return endTimeMs;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public int getCropX() {
        return cropX;
    }

    public int getCropY() {
        return cropY;
    }

    public int getCropWidth() {
        return cropWidth;
    }

    public int getCropHeight() {
        return cropHeight;
    }

    public boolean hasCrop() {
        return cropX >= 0 && cropY >= 0 && cropWidth > 0 && cropHeight > 0;
    }
}
