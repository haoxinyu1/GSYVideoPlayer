package com.shuyu.gsyvideoplayer.preview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A simple immutable preview provider backed by sorted frame metadata.
 */
public class GSYVideoPreviewListProvider implements GSYVideoPreviewProvider {

    private final List<GSYVideoPreviewFrame> frames;

    public GSYVideoPreviewListProvider(List<GSYVideoPreviewFrame> frames) {
        List<GSYVideoPreviewFrame> sortedFrames = new ArrayList<>(frames);
        Collections.sort(sortedFrames, new Comparator<GSYVideoPreviewFrame>() {
            @Override
            public int compare(GSYVideoPreviewFrame left, GSYVideoPreviewFrame right) {
                return Long.compare(left.getStartTimeMs(), right.getStartTimeMs());
            }
        });
        this.frames = Collections.unmodifiableList(sortedFrames);
    }

    @Override
    public GSYVideoPreviewFrame getPreviewFrame(long positionMs) {
        if (frames.isEmpty()) {
            return null;
        }
        int low = 0;
        int high = frames.size() - 1;
        while (low <= high) {
            int middle = (low + high) >>> 1;
            GSYVideoPreviewFrame frame = frames.get(middle);
            if (positionMs < frame.getStartTimeMs()) {
                high = middle - 1;
            } else if (positionMs >= frame.getEndTimeMs()) {
                low = middle + 1;
            } else {
                return frame;
            }
        }
        if (positionMs < frames.get(0).getStartTimeMs()) {
            return frames.get(0);
        }
        return frames.get(frames.size() - 1);
    }

    @Override
    public List<GSYVideoPreviewFrame> getFrames() {
        return frames;
    }

    @Override
    public void release() {
    }
}
