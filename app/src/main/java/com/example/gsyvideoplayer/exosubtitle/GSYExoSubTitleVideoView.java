package com.example.gsyvideoplayer.exosubtitle;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

import androidx.media3.common.Player;
import androidx.media3.common.text.Cue;
import androidx.media3.common.text.CueGroup;

import com.shuyu.gsyvideoplayer.subtitle.GSYSubtitleMime;
import com.shuyu.gsyvideoplayer.subtitle.GSYSubtitleSource;
import com.shuyu.gsyvideoplayer.utils.Debuger;
import com.shuyu.gsyvideoplayer.video.NormalGSYVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer;

import java.util.HashMap;

public class GSYExoSubTitleVideoView extends NormalGSYVideoPlayer implements Player.Listener {

    private String mSubTitle;

    public GSYExoSubTitleVideoView(Context context, Boolean fullFlag) {
        super(context, fullFlag);
    }

    public GSYExoSubTitleVideoView(Context context) {
        super(context);
    }

    public GSYExoSubTitleVideoView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void init(Context context) {
        super.init(context);
    }

    @Override
    public int getLayoutId() {
        return com.shuyu.gsyvideoplayer.R.layout.video_layout_standard;
    }


    @Override
    protected void startPrepare() {
        if (getGSYVideoManager().listener() != null) {
            getGSYVideoManager().listener().onCompletion();
        }
        if (mVideoAllCallBack != null) {
            Debuger.printfLog("onStartPrepared");
            mVideoAllCallBack.onStartPrepared(mOriginUrl, mTitle, this);
        }
        getGSYVideoManager().setListener(this);
        getGSYVideoManager().setPlayTag(mPlayTag);
        getGSYVideoManager().setPlayPosition(mPlayPosition);
        // Audio focus is now handled by the base class GSYAudioFocusManager
        try {
            if (mContext instanceof Activity) {
                ((Activity) mContext).getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mBackUpPlayingBufferState = -1;

        if (mSubTitle != null) {
            setSubtitleSource(new GSYSubtitleSource.Builder(mSubTitle)
                .setId("legacy-exo-subtitle")
                .setMimeType(GSYSubtitleMime.infer(mSubTitle, null))
                .setLabel("外挂字幕")
                .setDefault(true)
                .setHeaders(mMapHeadData)
                .build());
        }
        getGSYVideoManager().prepare(mUrl, null, this, (mMapHeadData == null) ? new HashMap<String, String>() : mMapHeadData, mLooping, mSpeed, mCache, mCachePath, mOverrideExtension);
        setStateAndUi(CURRENT_STATE_PREPAREING);
    }


    @Override
    public void onCues(CueGroup cueGroup) {
        if (cueGroup == null || cueGroup.cues == null || cueGroup.cues.isEmpty()) {
            clearSubtitleTextFromPlayer();
            return;
        }
        StringBuilder builder = new StringBuilder();
        for (Cue cue : cueGroup.cues) {
            if (cue.text == null) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append('\n');
            }
            builder.append(cue.text);
        }
        setSubtitleTextFromPlayer(builder.toString());
    }

    @Override
    protected void cloneParams(GSYBaseVideoPlayer from, GSYBaseVideoPlayer to) {
        super.cloneParams(from, to);
        if (from instanceof GSYExoSubTitleVideoView && to instanceof GSYExoSubTitleVideoView) {
            ((GSYExoSubTitleVideoView) to).mSubTitle = ((GSYExoSubTitleVideoView) from).mSubTitle;
        }
    }

    public String getSubTitle() {
        return mSubTitle;
    }

    public void setSubTitle(String subTitle) {
        this.mSubTitle = subTitle;
        if (subTitle != null) {
            setSubtitleSource(new GSYSubtitleSource.Builder(subTitle)
                .setId("legacy-exo-subtitle")
                .setMimeType(GSYSubtitleMime.infer(subTitle, null))
                .setLabel("外挂字幕")
                .setDefault(true)
                .build());
        }
    }


    /**********以下重载 GSYVideoPlayer 的 全屏 SubtitleView 相关实现***********/


    @Override
    public GSYBaseVideoPlayer startWindowFullscreen(Context context, boolean actionBar, boolean statusBar) {
        GSYBaseVideoPlayer gsyBaseVideoPlayer = super.startWindowFullscreen(context, actionBar, statusBar);
        registerTextOutput(gsyBaseVideoPlayer);
        return gsyBaseVideoPlayer;
    }

    @Override
    public GSYBaseVideoPlayer showSmallVideo(Point size, boolean actionBar, boolean statusBar) {
        GSYBaseVideoPlayer gsyBaseVideoPlayer = super.showSmallVideo(size, actionBar, statusBar);
        registerTextOutput(gsyBaseVideoPlayer);
        return gsyBaseVideoPlayer;
    }

    @Override
    public void hideSmallVideo() {
        GSYVideoPlayer smallWindowPlayer = getSmallWindowPlayer();
        super.hideSmallVideo();
        unregisterTextOutput(smallWindowPlayer);
    }

    @Override
    protected void resolveNormalVideoShow(View oldF, ViewGroup vp, GSYVideoPlayer gsyVideoPlayer) {
        super.resolveNormalVideoShow(oldF, vp, gsyVideoPlayer);
        unregisterTextOutput(gsyVideoPlayer);

    }

    private void registerTextOutput(GSYBaseVideoPlayer gsyBaseVideoPlayer) {
        if (gsyBaseVideoPlayer instanceof GSYExoSubTitleVideoView
            && GSYExoSubTitleVideoManager.instance().getPlayer() instanceof GSYExoSubTitlePlayerManager) {
            ((GSYExoSubTitlePlayerManager) GSYExoSubTitleVideoManager.instance().getPlayer())
                .addTextOutputPlaying((GSYExoSubTitleVideoView) gsyBaseVideoPlayer);
        }
    }

    private void unregisterTextOutput(GSYVideoPlayer gsyVideoPlayer) {
        if (gsyVideoPlayer instanceof GSYExoSubTitleVideoView
            && GSYExoSubTitleVideoManager.instance().getPlayer() instanceof GSYExoSubTitlePlayerManager) {
            ((GSYExoSubTitlePlayerManager) GSYExoSubTitleVideoManager.instance().getPlayer())
                .removeTextOutput((GSYExoSubTitleVideoView) gsyVideoPlayer);
        }
    }


    /**********以下重载GSYVideoPlayer的GSYVideoViewBridge相关实现***********/


    @Override
    public GSYExoSubTitleVideoManager getGSYVideoManager() {
        GSYExoSubTitleVideoManager.instance().initContext(getContext().getApplicationContext());
        return GSYExoSubTitleVideoManager.instance();
    }

    @Override
    protected boolean backFromFull(Context context) {
        return GSYExoSubTitleVideoManager.backFromWindowFull(context);
    }

    @Override
    protected void releaseVideos() {
        GSYExoSubTitleVideoManager.releaseAllVideos();
    }

    @Override
    protected int getFullId() {
        return GSYExoSubTitleVideoManager.FULLSCREEN_ID;
    }

    @Override
    protected int getSmallId() {
        return GSYExoSubTitleVideoManager.SMALL_ID;

    }
}
