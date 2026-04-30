package com.shuyu.gsyvideoplayer.preview;

import java.util.List;

/**
 * Provides preview frame metadata for a video position.
 */
public interface GSYVideoPreviewProvider {

    GSYVideoPreviewFrame getPreviewFrame(long positionMs);

    List<GSYVideoPreviewFrame> getFrames();

    void release();
}
