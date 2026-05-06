package com.example.gsyvideoplayer.exo;

import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;

import androidx.media3.common.C;
import androidx.media3.common.Format;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionOverride;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.Tracks;
import androidx.media3.common.Timeline;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.source.ConcatenatingMediaSource;
import androidx.media3.exoplayer.source.ConcatenatingMediaSource2;
import androidx.media3.exoplayer.source.MediaSource;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;


import java.io.FileDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer;
import tv.danmaku.ijk.media.exo2.demo.EventLogger;

/**
 * 自定义exo player，实现不同于库的exo 无缝切换效果
 */
public class GSYExo2MediaPlayer extends IjkExo2MediaPlayer {

    private static final String TAG = "GSYExo2MediaPlayer";

    private static final long MAX_POSITION_FOR_SEEK_TO_PREVIOUS = 3000;

    private final Timeline.Window window = new Timeline.Window();

    public static final int POSITION_DISCONTINUITY = 899;

    private int playIndex = 0;

    public static class VideoTrackInfo {
        public final int groupIndex;
        public final int trackIndex;
        public final int width;
        public final int height;
        public final int bitrate;
        public final String codecs;
        public final boolean selected;
        public final boolean supported;
        public final boolean adaptiveSupported;

        VideoTrackInfo(int groupIndex, int trackIndex, Format format, boolean selected,
                       boolean supported, boolean adaptiveSupported) {
            this.groupIndex = groupIndex;
            this.trackIndex = trackIndex;
            this.width = format.width;
            this.height = format.height;
            this.bitrate = format.bitrate;
            this.codecs = format.codecs;
            this.selected = selected;
            this.supported = supported;
            this.adaptiveSupported = adaptiveSupported;
        }

        public String getLabel() {
            StringBuilder builder = new StringBuilder();
            if (height > 0) {
                builder.append(height).append("P");
            } else if (width > 0) {
                builder.append(width).append("W");
            } else {
                builder.append("Unknown");
            }
            if (bitrate > 0) {
                builder.append("  ").append(Math.round(bitrate / 1000f)).append("kbps");
            }
            if (selected) {
                builder.append("  *");
            }
            return builder.toString();
        }
    }

    public GSYExo2MediaPlayer(Context context) {
        super(context);
    }

    @Override
    @Deprecated
    public void setDataSource(Context context, Uri uri) {
        throw new UnsupportedOperationException("Deprecated, try setDataSource(List<String> uris, Map<String, String> headers)");
    }

    @Override
    @Deprecated
    public void setDataSource(Context context, Uri uri, Map<String, String> headers) {
        throw new UnsupportedOperationException("Deprecated, try setDataSource(List<String> uris, Map<String, String> headers)");
    }

    @Override
    @Deprecated
    public void setDataSource(String path) {
        throw new UnsupportedOperationException("Deprecated, try setDataSource(List<String> uris, Map<String, String> headers)");
    }

    @Override
    @Deprecated
    public void setDataSource(FileDescriptor fd) {
        throw new UnsupportedOperationException("Deprecated, try setDataSource(List<String> uris, Map<String, String> headers)");
    }

    @Override
    public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, @Player.DiscontinuityReason int reason) {
        super.onPositionDiscontinuity(oldPosition, newPosition, reason);
        notifyOnInfo(POSITION_DISCONTINUITY, reason);
    }

    public void setDataSource(List<String> uris, Map<String, String> headers, int index, boolean cache) {
        mHeaders = headers;
        if (uris == null) {
            return;
        }
        ConcatenatingMediaSource concatenatedSource = new ConcatenatingMediaSource();
        for (String uri : uris) {
            MediaSource mediaSource = mExoHelper.getMediaSource(uri, isPreview, cache, false, mCacheDir, getOverrideExtension());
            concatenatedSource.addMediaSource(mediaSource);
        }
        playIndex = index;
        mMediaSource = concatenatedSource;


        /// ConcatenatingMediaSource2 是把多个视频拼成一个播放，时间轴只有一个
//        ConcatenatingMediaSource2.Builder mediaSourceBuilder =
//            new ConcatenatingMediaSource2.Builder().useDefaultMediaSourceFactory(mAppContext);
//
//        for (String uri : uris) {
//            MediaSource mediaSource = mExoHelper.getMediaSource(uri, isPreview, cache, false, mCacheDir, getOverrideExtension());
//            mediaSourceBuilder.add(mediaSource, 0);
//        }
//        playIndex = index;
//        mMediaSource = mediaSourceBuilder.build();
    }


    /**
     * 上一集
     */
    public void previous() {
        if (mInternalPlayer == null) {
            return;
        }
        Timeline timeline = mInternalPlayer.getCurrentTimeline();
        if (timeline.isEmpty()) {
            return;
        }
        int windowIndex = mInternalPlayer.getCurrentMediaItemIndex();
        timeline.getWindow(windowIndex, window);
        int previousWindowIndex = mInternalPlayer.getPreviousMediaItemIndex();
        if (previousWindowIndex != C.INDEX_UNSET
            && (mInternalPlayer.getCurrentPosition() <= MAX_POSITION_FOR_SEEK_TO_PREVIOUS
            || (window.isDynamic && !window.isSeekable))) {
            mInternalPlayer.seekTo(previousWindowIndex, C.TIME_UNSET);
        } else {
            mInternalPlayer.seekTo(0);
        }
    }

    @Override
    protected void prepareAsyncInternal() {
        new Handler(Looper.getMainLooper()).post(
            new Runnable() {
                @Override
                public void run() {
                    if (mTrackSelector == null) {
                        mTrackSelector = new DefaultTrackSelector(mAppContext);
                    }
                    mEventLogger = new EventLogger(mTrackSelector);
                    boolean preferExtensionDecoders = true;
                    boolean useExtensionRenderers = true;//是否开启扩展
                    @DefaultRenderersFactory.ExtensionRendererMode int extensionRendererMode = useExtensionRenderers
                        ? (preferExtensionDecoders ? DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
                        : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                        : DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF;
                    if (mRendererFactory == null) {
                        mRendererFactory = new DefaultRenderersFactory(mAppContext);
                        mRendererFactory.setExtensionRendererMode(extensionRendererMode);
                    }
                    if (mLoadControl == null) {
                        mLoadControl = new DefaultLoadControl();
                    }
                    mInternalPlayer = new ExoPlayer.Builder(mAppContext, mRendererFactory)
                        .setLooper(Looper.getMainLooper())
                        .setTrackSelector(mTrackSelector)
                        .setLoadControl(mLoadControl).build();

                    mInternalPlayer.addListener(GSYExo2MediaPlayer.this);
                    mInternalPlayer.addAnalyticsListener(GSYExo2MediaPlayer.this);
                    mInternalPlayer.addListener(mEventLogger);
                    if (mSpeedPlaybackParameters != null) {
                        mInternalPlayer.setPlaybackParameters(mSpeedPlaybackParameters);
                    }
                    if (mSurface != null)
                        mInternalPlayer.setVideoSurface(mSurface);
                    ///fix start index
                    if (playIndex > 0) {
                        mInternalPlayer.seekTo(playIndex, C.INDEX_UNSET);
                    }
                    mInternalPlayer.setMediaSource(mMediaSource, false);
                    mInternalPlayer.prepare();
                    mInternalPlayer.setPlayWhenReady(false);
                }
            }
        );
    }

    /**
     * 下一集
     */
    public void next() {
        if (mInternalPlayer == null) {
            return;
        }
        Timeline timeline = mInternalPlayer.getCurrentTimeline();
        if (timeline.isEmpty()) {
            return;
        }
        int windowIndex = mInternalPlayer.getCurrentMediaItemIndex();
        int nextWindowIndex = mInternalPlayer.getNextMediaItemIndex();
        if (nextWindowIndex != C.INDEX_UNSET) {
            mInternalPlayer.seekTo(nextWindowIndex, C.TIME_UNSET);
        } else if (timeline.getWindow(windowIndex, window).isDynamic) {
            mInternalPlayer.seekTo(windowIndex, C.TIME_UNSET);
        }
    }

    public int getCurrentWindowIndex() {
        if (mInternalPlayer == null) {
            return 0;
        }
        return mInternalPlayer.getCurrentMediaItemIndex();
    }

    public List<VideoTrackInfo> getVideoTrackInfoList() {
        List<VideoTrackInfo> result = new ArrayList<>();
        Tracks tracks = getCurrentTracks();
        if (tracks == null) {
            return result;
        }
        List<Tracks.Group> groups = tracks.getGroups();
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            Tracks.Group group = groups.get(groupIndex);
            if (group.getType() != C.TRACK_TYPE_VIDEO) {
                continue;
            }
            for (int trackIndex = 0; trackIndex < group.length; trackIndex++) {
                result.add(new VideoTrackInfo(
                    groupIndex,
                    trackIndex,
                    group.getTrackFormat(trackIndex),
                    group.isTrackSelected(trackIndex),
                    group.isTrackSupported(trackIndex),
                    group.isAdaptiveSupported()));
            }
        }
        return result;
    }

    public boolean clearVideoTrackOverride() {
        if (mInternalPlayer == null) {
            return false;
        }
        TrackSelectionParameters parameters = mInternalPlayer.getTrackSelectionParameters()
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
            .clearOverridesOfType(C.TRACK_TYPE_VIDEO)
            .setForceLowestBitrate(false)
            .setForceHighestSupportedBitrate(false)
            .build();
        mInternalPlayer.setTrackSelectionParameters(parameters);
        return true;
    }

    public boolean setVideoTrackOverride(int groupIndex, int trackIndex) {
        if (mInternalPlayer == null) {
            return false;
        }
        Tracks tracks = getCurrentTracks();
        if (tracks == null || groupIndex < 0 || groupIndex >= tracks.getGroups().size()) {
            return false;
        }
        Tracks.Group group = tracks.getGroups().get(groupIndex);
        if (group.getType() != C.TRACK_TYPE_VIDEO
            || trackIndex < 0
            || trackIndex >= group.length
            || !group.isTrackSupported(trackIndex)) {
            return false;
        }
        TrackSelectionOverride override =
            new TrackSelectionOverride(group.getMediaTrackGroup(), trackIndex);
        TrackSelectionParameters parameters = mInternalPlayer.getTrackSelectionParameters()
            .buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
            .setOverrideForType(override)
            .build();
        mInternalPlayer.setTrackSelectionParameters(parameters);
        return true;
    }
}
