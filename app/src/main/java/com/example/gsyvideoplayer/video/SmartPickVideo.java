package com.example.gsyvideoplayer.video;

import android.content.Context;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.example.gsyvideoplayer.R;
import com.example.gsyvideoplayer.model.SwitchVideoModel;
import com.example.gsyvideoplayer.view.LoadingDialog;
import com.example.gsyvideoplayer.view.SwitchVideoTypeDialog;
import com.shuyu.gsyvideoplayer.GSYVideoBaseManager;
import com.shuyu.gsyvideoplayer.GSYVideoManager;
import com.shuyu.gsyvideoplayer.listener.GSYMediaPlayerListener;
import com.shuyu.gsyvideoplayer.utils.Debuger;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYVideoPlayer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 无缝切换视频的DEMO
 * 这里是切换清晰度，稍微修改下也可以作为切换下一集等
 */

public class SmartPickVideo extends StandardGSYVideoPlayer {

    private static final int CHANGE_TIMEOUT_MS = 10 * 1000;
    private static final int CHANGE_SEEK_RESYNC_THRESHOLD_MS = 800;
    private static final int MAX_CHANGE_SEEK_SYNC_COUNT = 1;
    private static final int CHANGE_COMMIT_POSITION_TOLERANCE_MS = 1200;
    private static final int CHANGE_SEEK_RETRY_DELAY_MS = 300;
    private static final int MAX_CHANGE_SEEK_RETRY_COUNT = 2;
    private static final int INVALID_SOURCE_POSITION = -1;

    private TextView mSwitchSize;

    private List<SwitchVideoModel> mUrlList = new ArrayList<>();

    //记住切换数据源类型
    private int mType = 0;
    //数据源
    private int mSourcePosition = 0;
    private int mPreSourcePosition = 0;

    private String mTypeText = "标准";

    private GSYVideoBaseManager mOriginManager;
    private GSYVideoManager mTmpManager;

    //切换过程中最好弹出loading，不给其他任何操作
    private LoadingDialog mLoadingDialog;

    private boolean isChanging;
    private int mPendingSourcePosition = INVALID_SOURCE_POSITION;
    private int mChangeSessionId;
    private long mChangeSeekPosition;
    private long mLastKnownChangePosition;
    private int mChangeSeekSyncCount;
    private int mChangeSeekRetryCount;
    private boolean mWasPlayingBeforeChange;
    private boolean mNeedMuteBeforeChange;
    private boolean mPausedOriginForResync;

    private final Runnable mChangeTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (isChanging) {
                cancelChange("切换超时");
            }
        }
    };

    /**
     * 1.5.0开始加入，如果需要不同布局区分功能，需要重载
     */
    public SmartPickVideo(Context context, Boolean fullFlag) {
        super(context, fullFlag);
    }

    public SmartPickVideo(Context context) {
        super(context);
    }

    public SmartPickVideo(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        initView();
    }

    private void initView() {
        mSwitchSize = (TextView) findViewById(R.id.switchSize);
        //切换视频清晰度
        mSwitchSize.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mHadPlay && !isChanging) {
                    showSwitchDialog();
                }
            }
        });
    }

    /**
     * 设置播放URL
     *
     * @param url           播放url
     * @param cacheWithPlay 是否边播边缓存
     * @param title         title
     * @return
     */
    public boolean setUp(List<SwitchVideoModel> url, boolean cacheWithPlay, String title) {
        mUrlList = url;
        return setUp(url.get(mSourcePosition).getUrl(), cacheWithPlay, title);
    }

    /**
     * 设置播放URL
     *
     * @param url           播放url
     * @param cacheWithPlay 是否边播边缓存
     * @param cachePath     缓存路径，如果是M3U8或者HLS，请设置为false
     * @param title         title
     * @return
     */
    public boolean setUp(List<SwitchVideoModel> url, boolean cacheWithPlay, File cachePath, String title) {
        mUrlList = url;
        return setUp(url.get(mSourcePosition).getUrl(), cacheWithPlay, cachePath, title);
    }

    @Override
    public int getLayoutId() {
        return R.layout.sample_video_pick;
    }


    /**
     * 全屏时将对应处理参数逻辑赋给全屏播放器
     *
     * @param context
     * @param actionBar
     * @param statusBar
     * @return
     */
    @Override
    public GSYBaseVideoPlayer startWindowFullscreen(Context context, boolean actionBar, boolean statusBar) {
        SmartPickVideo sampleVideo = (SmartPickVideo) super.startWindowFullscreen(context, actionBar, statusBar);
        sampleVideo.mSourcePosition = mSourcePosition;
        sampleVideo.mListItemRect = mListItemRect;
        sampleVideo.mListItemSize = mListItemSize;
        sampleVideo.mType = mType;
        sampleVideo.mUrlList = mUrlList;
        sampleVideo.mTypeText = mTypeText;
        sampleVideo.mLastKnownChangePosition = mLastKnownChangePosition;
        sampleVideo.mSwitchSize.setText(mTypeText);
        return sampleVideo;
    }

    /**
     * 推出全屏时将对应处理参数逻辑返回给非播放器
     *
     * @param oldF
     * @param vp
     * @param gsyVideoPlayer
     */
    @Override
    protected void resolveNormalVideoShow(View oldF, ViewGroup vp, GSYVideoPlayer gsyVideoPlayer) {
        super.resolveNormalVideoShow(oldF, vp, gsyVideoPlayer);
        if (gsyVideoPlayer != null) {
            SmartPickVideo sampleVideo = (SmartPickVideo) gsyVideoPlayer;
            mSourcePosition = sampleVideo.mSourcePosition;
            mType = sampleVideo.mType;
            mTypeText = sampleVideo.mTypeText;
            mLastKnownChangePosition = sampleVideo.mLastKnownChangePosition;
            mSwitchSize.setText(mTypeText);
            setUp(mUrlList, mCache, mCachePath, mTitle);
        }
    }

    @Override
    public void onAutoCompletion() {
        super.onAutoCompletion();
        releaseTmpManager();
    }

    @Override
    public void onCompletion() {
        super.onCompletion();
        releaseTmpManager();
    }

    /**
     * 弹出切换清晰度
     */
    private void showSwitchDialog() {
        if (!mHadPlay) {
            return;
        }
        SwitchVideoTypeDialog switchVideoTypeDialog = new SwitchVideoTypeDialog(getContext());
        switchVideoTypeDialog.initList(mUrlList, new SwitchVideoTypeDialog.OnListItemClickListener() {
            @Override
            public void onItemClick(int position) {
                resolveStartChange(position);
            }
        });
        switchVideoTypeDialog.show();
    }


    private void applySourcePosition(int position) {
        if (!isValidSourcePosition(position)) {
            return;
        }
        SwitchVideoModel switchVideoModel = mUrlList.get(position);
        mSourcePosition = position;
        mTypeText = switchVideoModel.getName();
        mOriginUrl = switchVideoModel.getUrl();
        mUrl = switchVideoModel.getUrl();
        mSwitchSize.setText(mTypeText);
    }


    private GSYMediaPlayerListener createChangeMediaPlayerListener(final int sessionId) {
        return new GSYMediaPlayerListener() {
            @Override
            public void onPrepared() {
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (!isCurrentChangeSession(sessionId)) {
                            return;
                        }
                        long targetPosition = getLatestOriginChangePosition();
                        if (!isChangeTargetPositionReliable(targetPosition)) {
                            cancelChange("当前位置无法切换");
                            return;
                        }
                        mChangeSeekPosition = targetPosition;
                        mTmpManager.start();
                        mTmpManager.seekTo(mChangeSeekPosition);
                    }
                });
            }

            @Override
            public void onAutoCompletion() {

            }

            @Override
            public void onCompletion() {

            }

            @Override
            public void onBufferingUpdate(int percent) {

            }

            @Override
            public void onSeekComplete() {
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (isCurrentChangeSession(sessionId)) {
                            handleChangeSeekComplete(sessionId);
                        }
                    }
                });
            }

            @Override
            public void onError(int what, int extra) {
                post(new Runnable() {
                    @Override
                    public void run() {
                        if (isCurrentChangeSession(sessionId)) {
                            cancelChange("切换失败");
                        }
                    }
                });
            }

            @Override
            public void onInfo(int what, int extra) {

            }

            @Override
            public void onVideoSizeChanged() {

            }

            @Override
            public void onBackFullscreen() {

            }

            @Override
            public void onVideoPause() {

            }

            @Override
            public void onVideoResume() {

            }

            @Override
            public void onVideoResume(boolean seek) {

            }
        };
    }

    private void resolveStartChange(int position) {
        if (!isValidSourcePosition(position)) {
            return;
        }
        final String name = mUrlList.get(position).getName();
        if (mSourcePosition != position) {
            if ((mCurrentState == GSYVideoPlayer.CURRENT_STATE_PLAYING
                    || mCurrentState == GSYVideoPlayer.CURRENT_STATE_PAUSE)) {
                if (!isSurfaceAvailableForChange()) {
                    Toast.makeText(getContext(), "当前画面不可切换", Toast.LENGTH_LONG).show();
                    return;
                }
                final String url = mUrlList.get(position).getUrl();
                mPreSourcePosition = mSourcePosition;
                mOriginManager = GSYVideoManager.instance();
                updateLastKnownChangePosition(getCurrentPositionWhenPlaying());
                mPendingSourcePosition = position;
                mWasPlayingBeforeChange = mCurrentState == GSYVideoPlayer.CURRENT_STATE_PLAYING;
                mNeedMuteBeforeChange = mOriginManager.isNeedMute();
                mChangeSeekPosition = getLatestOriginChangePosition();
                if (!isChangeTargetPositionReliable(mChangeSeekPosition)) {
                    clearPendingChange();
                    Toast.makeText(getContext(), "当前位置无法切换", Toast.LENGTH_LONG).show();
                    return;
                }
                showLoading();
                cancelProgressTimer();
                hideAllWidget();
                if (mTitle != null && mTitleTextView != null) {
                    mTitleTextView.setText(mTitle);
                }
                mChangeSeekSyncCount = 0;
                mChangeSeekRetryCount = 0;
                isChanging = true;
                final int sessionId = ++mChangeSessionId;
                mSwitchSize.setText(name);
                //创建临时管理器执行加载播放
                try {
                    mTmpManager = GSYVideoManager.tmpInstance(createChangeMediaPlayerListener(sessionId));
                    mTmpManager.initContext(getContext().getApplicationContext());
                    mTmpManager.setNeedMute(true);
                    mTmpManager.prepare(url, mMapHeadData, mLooping, mSpeed, mCache, mCachePath, null);
                    postDelayed(mChangeTimeoutRunnable, CHANGE_TIMEOUT_MS);
                    changeUiToPlayingBufferingShow();
                } catch (Exception e) {
                    Debuger.printfError("change video prepare error " + e.getMessage());
                    cancelChange("切换失败");
                }
            }
        } else {
            Toast.makeText(getContext(), "已经是 " + name, Toast.LENGTH_LONG).show();
        }
    }

    private boolean isCurrentChangeSession(int sessionId) {
        return isChanging && mTmpManager != null && sessionId == mChangeSessionId;
    }

    private boolean resyncChangePositionIfNeeded() {
        if (!isChanging || mTmpManager == null || !mWasPlayingBeforeChange
                || mChangeSeekSyncCount >= MAX_CHANGE_SEEK_SYNC_COUNT) {
            return false;
        }
        long latestPosition = getLatestOriginChangePosition();
        if (latestPosition - mChangeSeekPosition <= CHANGE_SEEK_RESYNC_THRESHOLD_MS) {
            return false;
        }
        mChangeSeekPosition = latestPosition;
        mChangeSeekSyncCount++;
        if (mOriginManager != null) {
            mOriginManager.pause();
        }
        mPausedOriginForResync = true;
        mTmpManager.seekTo(mChangeSeekPosition);
        return true;
    }

    private void handleChangeSeekComplete(int sessionId) {
        if (!isCurrentChangeSession(sessionId)) {
            return;
        }
        if (resyncChangePositionIfNeeded()) {
            return;
        }
        if (isTmpSeekPositionAcceptable()) {
            commitChange();
            return;
        }
        if (mChangeSeekRetryCount < MAX_CHANGE_SEEK_RETRY_COUNT) {
            retryTmpSeek(sessionId);
        } else {
            Debuger.printfError("change video cancel, target seek is not accurate. target="
                    + mChangeSeekPosition + ", tmp=" + getTmpCurrentPosition());
            cancelChange("目标视频不支持精准切换");
        }
    }

    private void retryTmpSeek(final int sessionId) {
        mChangeSeekRetryCount++;
        mChangeSeekPosition = getLatestOriginChangePosition();
        if (!isChangeTargetPositionReliable(mChangeSeekPosition)) {
            cancelChange("当前位置无法切换");
            return;
        }
        Debuger.printfError("change video seek retry " + mChangeSeekRetryCount
                + ", target=" + mChangeSeekPosition + ", tmp=" + getTmpCurrentPosition());
        postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isCurrentChangeSession(sessionId)) {
                    mTmpManager.seekTo(mChangeSeekPosition);
                }
            }
        }, CHANGE_SEEK_RETRY_DELAY_MS);
    }

    private boolean isTmpSeekPositionAcceptable() {
        long tmpPosition = getTmpCurrentPosition();
        if (mChangeSeekPosition <= CHANGE_COMMIT_POSITION_TOLERANCE_MS) {
            return tmpPosition <= CHANGE_COMMIT_POSITION_TOLERANCE_MS;
        }
        return Math.abs(tmpPosition - mChangeSeekPosition) <= CHANGE_COMMIT_POSITION_TOLERANCE_MS;
    }

    private long getTmpCurrentPosition() {
        try {
            if (mTmpManager != null) {
                return mTmpManager.getCurrentPosition();
            }
        } catch (Exception e) {
            Debuger.printfError("get tmp position error " + e.getMessage());
        }
        return 0;
    }

    private void commitChange() {
        if (!isChanging || mTmpManager == null || !isValidSourcePosition(mPendingSourcePosition)) {
            return;
        }
        if (!isViewActiveForChange()) {
            cancelChangeForLifecycle();
            return;
        }
        removeCallbacks(mChangeTimeoutRunnable);
        GSYVideoBaseManager manager = mOriginManager != null ? mOriginManager : GSYVideoManager.instance();
        GSYVideoManager tmpManager = mTmpManager;
        try {
            tmpManager.setLastListener(manager.lastListener());
            tmpManager.setListener(manager.listener());
            if (mWasPlayingBeforeChange) {
                tmpManager.setNeedMute(mNeedMuteBeforeChange);
            } else {
                tmpManager.pause();
                tmpManager.setNeedMute(mNeedMuteBeforeChange);
            }

            manager.setDisplay(null);
            tmpManager.setDisplay(mSurface);

            mTmpManager = null;
            GSYVideoManager.changeManager(tmpManager);
            applySourcePosition(mPendingSourcePosition);
            resolveChangedResult();
            manager.releaseMediaPlayer();
        } catch (Exception e) {
            Debuger.printfError("commit video change error " + e.getMessage());
            try {
                manager.setDisplay(mSurface);
            } catch (Exception ignore) {
                // ignore restore failure
            }
            mTmpManager = tmpManager;
            cancelChange("切换失败");
        }
    }

    private void resolveChangedResult() {
        isChanging = false;
        clearPendingChange();
        if (mWasPlayingBeforeChange) {
            setStateAndUi(CURRENT_STATE_PLAYING);
            changeUiToPlayingClear();
        } else {
            setStateAndUi(CURRENT_STATE_PAUSE);
        }
        hideLoading();
    }

    private void releaseTmpManager() {
        removeCallbacks(mChangeTimeoutRunnable);
        if (mTmpManager != null) {
            mTmpManager.releaseMediaPlayer();
            mTmpManager = null;
        }
        if (isChanging) {
            applySourcePosition(mPreSourcePosition);
            isChanging = false;
            clearPendingChange();
            hideLoading();
        }
    }

    private void cancelChange(String message) {
        cancelChange(message, true, true);
    }

    private void cancelChangeForLifecycle() {
        cancelChange(null, false, false);
    }

    private void cancelChange(String message, boolean resumeOrigin, boolean updateUi) {
        removeCallbacks(mChangeTimeoutRunnable);
        if (mTmpManager != null) {
            mTmpManager.releaseMediaPlayer();
            mTmpManager = null;
        }
        applySourcePosition(mPreSourcePosition);
        if (resumeOrigin) {
            resumeOriginIfNeeded();
        }
        isChanging = false;
        clearPendingChange();
        if (updateUi) {
            if (mWasPlayingBeforeChange) {
                setStateAndUi(CURRENT_STATE_PLAYING);
                changeUiToPlayingClear();
            } else {
                setStateAndUi(CURRENT_STATE_PAUSE);
            }
        }
        hideLoading();
        if (message != null) {
            Toast.makeText(mContext, message, Toast.LENGTH_LONG).show();
        }
    }

    private void clearPendingChange() {
        mPendingSourcePosition = INVALID_SOURCE_POSITION;
        mOriginManager = null;
        mChangeSeekPosition = 0;
        mChangeSeekSyncCount = 0;
        mChangeSeekRetryCount = 0;
        mPausedOriginForResync = false;
    }

    private boolean isValidSourcePosition(int position) {
        return mUrlList != null && position >= 0 && position < mUrlList.size();
    }

    private long getLatestOriginChangePosition() {
        long currentPosition = 0;
        try {
            if (mOriginManager != null) {
                currentPosition = mOriginManager.getCurrentPosition();
            }
        } catch (Exception e) {
            Debuger.printfError("get origin position error " + e.getMessage());
        }
        if (currentPosition <= 0) {
            currentPosition = getCurrentPositionWhenPlaying();
        }
        if (currentPosition <= 0) {
            currentPosition = mLastKnownChangePosition;
        }
        if (currentPosition <= 0) {
            return mChangeSeekPosition;
        }
        updateLastKnownChangePosition(currentPosition);
        return Math.max(mChangeSeekPosition, currentPosition);
    }

    private boolean isChangeTargetPositionReliable(long position) {
        return position > 0;
    }

    private void updateLastKnownChangePosition(long position) {
        if (position > 0) {
            mLastKnownChangePosition = position;
        }
    }

    private boolean isSurfaceAvailableForChange() {
        return mSurface != null && mSurface.isValid();
    }

    private boolean isViewActiveForChange() {
        return getWindowToken() != null && isSurfaceAvailableForChange();
    }

    private void resumeOriginIfNeeded() {
        if (mPausedOriginForResync && mWasPlayingBeforeChange && isSurfaceAvailableForChange()
                && mOriginManager != null) {
            mOriginManager.start();
        }
    }

    private void showLoading() {
        hideLoading();
        mLoadingDialog = new LoadingDialog(mContext);
        mLoadingDialog.show();
    }

    private void hideLoading() {
        if (mLoadingDialog != null) {
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
    }

    @Override
    protected void setProgressAndTime(long progress, long secProgress, long currentTime, long totalTime, boolean forceChange) {
        updateLastKnownChangePosition(currentTime);
        super.setProgressAndTime(progress, secProgress, currentTime, totalTime, forceChange);
    }

    @Override
    public void onVideoPause() {
        if (isChanging) {
            cancelChangeForLifecycle();
        }
        super.onVideoPause();
    }

    @Override
    public boolean onSurfaceDestroyed(Surface surface) {
        if (isChanging) {
            cancelChangeForLifecycle();
        }
        //清空释放
        setDisplay(null);
        //同一消息队列中去release
        //todo 需要处理为什么全屏时全屏的surface会被释放了
        //releaseSurface(surface);
        return true;
    }

    @Override
    protected void onDetachedFromWindow() {
        if (isChanging) {
            cancelChangeForLifecycle();
        } else {
            releaseTmpManager();
        }
        super.onDetachedFromWindow();
    }
}
