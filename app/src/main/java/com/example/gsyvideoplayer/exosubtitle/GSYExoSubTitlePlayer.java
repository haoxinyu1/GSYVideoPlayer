package com.example.gsyvideoplayer.exosubtitle;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import tv.danmaku.ijk.media.exo2.IjkExo2MediaPlayer;
import tv.danmaku.ijk.media.exo2.demo.EventLogger;

import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.common.text.CueGroup;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector;

public class GSYExoSubTitlePlayer extends IjkExo2MediaPlayer {

    private String mSubTitile;
    private Player.Listener mTextOutput;

    public GSYExoSubTitlePlayer(Context context) {
        super(context);
    }


    @Override
    public void onCues(CueGroup cueGroup) {
        super.onCues(cueGroup);
        /// 这里
    }

    @Override
    protected void prepareAsyncInternal() {
        new Handler(Looper.getMainLooper()).post(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mTrackSelector == null) {
                            ///todo 这里设置  setSelectUndeterminedTextLanguage 无视语言选择
                            mTrackSelector = new DefaultTrackSelector(mAppContext,
                                new TrackSelectionParameters.Builder(mAppContext).
                                    setSelectUndeterminedTextLanguage(true).build());
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
                        mInternalPlayer.addListener(GSYExoSubTitlePlayer.this);
                        mInternalPlayer.addAnalyticsListener(GSYExoSubTitlePlayer.this);
                        if (mTextOutput != null) {
                            mInternalPlayer.addListener(mTextOutput);
                        }
                        mInternalPlayer.addListener(mEventLogger);
                        if (mSpeedPlaybackParameters != null) {
                            mInternalPlayer.setPlaybackParameters(mSpeedPlaybackParameters);
                        }
                        if (mSurface != null)
                            mInternalPlayer.setVideoSurface(mSurface);


                        ///其实如果你没什么特色需求，可以直接用下面这种方式
//                        List<MediaItem.SubtitleConfiguration> list = new ArrayList<>();
//                        if (mSubTitile != null) {
//                            MediaItem.SubtitleConfiguration subtitle
//                                = new MediaItem.SubtitleConfiguration.Builder(Uri.parse(mSubTitile))
//                                .setMimeType(MimeTypes.APPLICATION_SUBRIP)
//                                .setLanguage(null)
//                                .setSelectionFlags(C.SELECTION_FLAG_FORCED)
//                                .build();
//                            list.add(subtitle);
//                        }
//                        MediaItem mediaItem = new MediaItem.Builder()
//                            .setUri(mDataSource).
//                            setSubtitleConfigurations(list).build();
//                        mInternalPlayer.setMediaItem(mediaItem);

                        // 外挂字幕由 GSY 通用字幕层异步加载和渲染。这里不再把字幕合并进
                        // MediaSource，避免字幕下载/解析失败影响视频主播放。
                        mInternalPlayer.setMediaSource(mMediaSource);




                        mInternalPlayer.prepare();
                        mInternalPlayer.setPlayWhenReady(false);
                    }
                }
        );
    }

    public String getSubTitile() {
        return mSubTitile;
    }

    public void setSubTitile(String subTitile) {
        this.mSubTitile = subTitile;
    }

    public Player.Listener getTextOutput() {
        return mTextOutput;
    }

    public void setTextOutput(Player.Listener textOutput) {
        this.mTextOutput = textOutput;
    }

    public void addTextOutputPlaying(Player.Listener textOutput) {
        if (mInternalPlayer != null) {
            mInternalPlayer.addListener(textOutput);
        }
    }

    public void removeTextOutput(Player.Listener textOutput) {
        if (mInternalPlayer != null) {
            mInternalPlayer.removeListener(textOutput);
        }
    }

}
