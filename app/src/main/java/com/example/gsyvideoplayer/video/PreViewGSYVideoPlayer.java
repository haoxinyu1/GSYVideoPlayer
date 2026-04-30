package com.example.gsyvideoplayer.video;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.Key;
import com.bumptech.glide.load.engine.bitmap_recycle.BitmapPool;
import com.bumptech.glide.load.resource.bitmap.BitmapTransformation;
import com.bumptech.glide.request.target.Target;
import com.example.gsyvideoplayer.R;
import com.shuyu.gsyvideoplayer.utils.CommonUtil;
import com.shuyu.gsyvideoplayer.utils.Debuger;
import com.shuyu.gsyvideoplayer.preview.GSYVideoPreviewFrame;
import com.shuyu.gsyvideoplayer.preview.GSYVideoPreviewProvider;
import com.shuyu.gsyvideoplayer.preview.GSYVideoPreviewVttParser;
import com.shuyu.gsyvideoplayer.video.NormalGSYVideoPlayer;
import com.shuyu.gsyvideoplayer.video.base.GSYBaseVideoPlayer;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 进度图小图预览实现，使用 WebVTT 指向独立缩略图或 sprite 坐标。
 * Created by shuyu on 2016/12/10.
 */

public class PreViewGSYVideoPlayer extends NormalGSYVideoPlayer {

    private static final ExecutorService PREVIEW_EXECUTOR = Executors.newSingleThreadExecutor();

    private RelativeLayout mPreviewLayout;

    private ImageView mPreView;

    //是否因为用户点击
    private boolean mIsFromUser;

    //是否打开滑动预览
    private boolean mOpenPreView = true;

    private int mPreProgress = -2;

    private String mPreviewVttUrl;

    private GSYVideoPreviewProvider mPreviewProvider;

    private final Handler mMainHandler = new Handler(Looper.getMainLooper());

    private int mPreviewLoadId;

    /**
     * 1.5.0开始加入，如果需要不同布局区分功能，需要重载
     */
    public PreViewGSYVideoPlayer(Context context, Boolean fullFlag) {
        super(context, fullFlag);
    }

    public PreViewGSYVideoPlayer(Context context) {
        super(context);
    }

    public PreViewGSYVideoPlayer(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void init(Context context) {
        super.init(context);
        initView();
    }

    private void initView() {
        mPreviewLayout = (RelativeLayout) findViewById(R.id.preview_layout);
        mPreView = (ImageView) findViewById(R.id.preview_image);
        mPreView.setScaleType(ImageView.ScaleType.CENTER_CROP);
    }

    @Override
    public int getLayoutId() {
        return R.layout.video_layout_preview;
    }


    @Override
    protected void prepareVideo() {
        super.prepareVideo();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, final int progress, boolean fromUser) {
        super.onProgressChanged(seekBar, progress, fromUser);
        if (fromUser && canShowPreView()) {
            int width = seekBar.getWidth();
            long time = progress * getDuration() / 100;
            int offset = (int) (width - (getResources().getDimension(R.dimen.seek_bar_image) / 2)) / 100 * progress;
            showPreView(time);
            RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) mPreviewLayout.getLayoutParams();
            layoutParams.leftMargin = offset;
            //设置帧预览图的显示位置
            mPreviewLayout.setLayoutParams(layoutParams);
            if (mHadPlay) {
                mPreProgress = progress;
            }
        }
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
        super.onStartTrackingTouch(seekBar);
        if (canShowPreView()) {
            mIsFromUser = true;
            mPreviewLayout.setVisibility(VISIBLE);
            mPreProgress = -2;
        }
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
        if (canShowPreView()) {
            if (mPreProgress >= 0) {
                seekBar.setProgress(mPreProgress);
            }
            super.onStopTrackingTouch(seekBar);
            mIsFromUser = false;
            mPreviewLayout.setVisibility(GONE);
        } else {
            super.onStopTrackingTouch(seekBar);
        }
    }

    @Override
    protected void setTextAndProgress(int secProgress) {
        if (mIsFromUser) {
            return;
        }
        super.setTextAndProgress(secProgress);
    }

    @Override
    public GSYBaseVideoPlayer startWindowFullscreen(Context context, boolean actionBar, boolean statusBar) {
        GSYBaseVideoPlayer gsyBaseVideoPlayer = super.startWindowFullscreen(context, actionBar, statusBar);
        PreViewGSYVideoPlayer customGSYVideoPlayer = (PreViewGSYVideoPlayer) gsyBaseVideoPlayer;
        customGSYVideoPlayer.mOpenPreView = mOpenPreView;
        customGSYVideoPlayer.mPreviewVttUrl = mPreviewVttUrl;
        customGSYVideoPlayer.mPreviewProvider = mPreviewProvider;
        if (customGSYVideoPlayer.mPreviewProvider == null && customGSYVideoPlayer.mPreviewVttUrl != null) {
            customGSYVideoPlayer.loadPreviewVtt(customGSYVideoPlayer.mPreviewVttUrl);
        }
        return gsyBaseVideoPlayer;
    }


    @Override
    public void onPrepared() {
        super.onPrepared();
    }

    public boolean isOpenPreView() {
        return mOpenPreView;
    }

    /**
     * 如果是需要进度条预览的设置打开，默认关闭
     */
    public void setOpenPreView(boolean openPreView) {
        this.mOpenPreView = openPreView;
    }

    public String getPreviewVttUrl() {
        return mPreviewVttUrl;
    }

    public void setPreviewVttUrl(String previewVttUrl) {
        mPreviewVttUrl = previewVttUrl;
        mPreviewProvider = null;
        if (previewVttUrl == null || previewVttUrl.length() == 0) {
            return;
        }
        loadPreviewVtt(previewVttUrl);
    }

    public GSYVideoPreviewProvider getPreviewProvider() {
        return mPreviewProvider;
    }

    public void setPreviewProvider(GSYVideoPreviewProvider previewProvider) {
        mPreviewProvider = previewProvider;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mPreviewLoadId++;
        if (mPreviewProvider != null) {
            mPreviewProvider.release();
        }
    }

    private boolean canShowPreView() {
        return mOpenPreView && mPreviewProvider != null && !mPreviewProvider.getFrames().isEmpty();
    }

    private void showPreView(long time) {
        GSYVideoPreviewFrame frame = mPreviewProvider.getPreviewFrame(time);
        if (frame == null) {
            return;
        }
        int width = CommonUtil.dip2px(getContext(), 150);
        int height = CommonUtil.dip2px(getContext(), 100);
        if (frame.hasCrop()) {
            Glide.with(getContext().getApplicationContext())
                    .load(frame.getImageUrl())
                    .override(Target.SIZE_ORIGINAL)
                    .dontAnimate()
                    .transform(new PreviewSpriteTransformation(frame.getCropX(), frame.getCropY(),
                            frame.getCropWidth(), frame.getCropHeight()))
                    .into(mPreView);
        } else {
            Glide.with(getContext().getApplicationContext())
                    .load(frame.getImageUrl())
                    .override(width, height)
                    .dontAnimate()
                    .centerCrop()
                    .into(mPreView);
        }
    }

    private void loadPreviewVtt(final String previewVttUrl) {
        final int loadId = ++mPreviewLoadId;
        PREVIEW_EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    final String vttContent = readUrl(previewVttUrl);
                    final GSYVideoPreviewProvider provider = GSYVideoPreviewVttParser.parse(vttContent, previewVttUrl);
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (loadId == mPreviewLoadId) {
                                mPreviewProvider = provider;
                                Debuger.printfLog("Preview VTT loaded, frame count: " + provider.getFrames().size());
                            }
                        }
                    });
                } catch (final Exception e) {
                    mMainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (loadId == mPreviewLoadId) {
                                mPreviewProvider = null;
                                Debuger.printfError("Preview VTT load failed: " + e.getMessage());
                            }
                        }
                    });
                }
            }
        });
    }

    private String readUrl(String urlString) throws Exception {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        BufferedReader reader = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(15000);
            connection.setReadTimeout(15000);
            connection.setRequestMethod("GET");
            connection.connect();
            inputStream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(inputStream, Charset.forName("UTF-8")));
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line).append('\n');
            }
            return builder.toString();
        } finally {
            if (reader != null) {
                reader.close();
            } else if (inputStream != null) {
                inputStream.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private static class PreviewSpriteTransformation extends BitmapTransformation {

        private static final String ID = "com.example.gsyvideoplayer.video.PreviewSpriteTransformation";
        private final int x;
        private final int y;
        private final int width;
        private final int height;

        PreviewSpriteTransformation(int x, int y, int width, int height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        @Override
        protected Bitmap transform(@NonNull BitmapPool pool, @NonNull Bitmap toTransform,
                                   int outWidth, int outHeight) {
            int safeX = Math.max(0, Math.min(x, toTransform.getWidth() - 1));
            int safeY = Math.max(0, Math.min(y, toTransform.getHeight() - 1));
            int safeWidth = Math.max(1, Math.min(width, toTransform.getWidth() - safeX));
            int safeHeight = Math.max(1, Math.min(height, toTransform.getHeight() - safeY));
            return Bitmap.createBitmap(toTransform, safeX, safeY, safeWidth, safeHeight);
        }

        @Override
        public void updateDiskCacheKey(@NonNull MessageDigest messageDigest) {
            messageDigest.update((ID + x + "," + y + "," + width + "," + height)
                    .getBytes(Key.CHARSET));
        }

        @Override
        public boolean equals(Object object) {
            if (!(object instanceof PreviewSpriteTransformation)) {
                return false;
            }
            PreviewSpriteTransformation other = (PreviewSpriteTransformation) object;
            return x == other.x && y == other.y && width == other.width && height == other.height;
        }

        @Override
        public int hashCode() {
            int result = ID.hashCode();
            result = 31 * result + x;
            result = 31 * result + y;
            result = 31 * result + width;
            result = 31 * result + height;
            return result;
        }
    }
}
