package com.example.gsyvideoplayer.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.util.AttributeSet;
import android.view.PixelCopy;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;

import com.shuyu.gsyvideoplayer.listener.GSYVideoShotListener;
import com.shuyu.gsyvideoplayer.listener.GSYVideoShotSaveListener;
import com.shuyu.gsyvideoplayer.render.GSYRenderView;
import com.shuyu.gsyvideoplayer.render.glrender.GSYVideoGLViewBaseRender;
import com.shuyu.gsyvideoplayer.render.view.GSYVideoGLView;
import com.shuyu.gsyvideoplayer.render.view.IGSYRenderView;
import com.shuyu.gsyvideoplayer.render.view.listener.IGSYSurfaceListener;
import com.shuyu.gsyvideoplayer.utils.Debuger;
import com.shuyu.gsyvideoplayer.utils.FileUtils;
import com.shuyu.gsyvideoplayer.utils.MeasureHelper;

import java.io.File;

/**
 * 自定义渲染层
 * Created by guoshuyu on 2018/1/30.
 */

public class CustomTextureSurface extends SurfaceView implements IGSYRenderView, SurfaceHolder.Callback2, MeasureHelper.MeasureFormVideoParamsListener {

    private IGSYSurfaceListener mIGSYSurfaceListener;

    private MeasureHelper measureHelper;

    private MeasureHelper.MeasureFormVideoParamsListener mVideoParamsListener;

    public CustomTextureSurface(Context context) {
        super(context);
        init();
    }

    public CustomTextureSurface(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public CustomTextureSurface(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        measureHelper = new MeasureHelper(this, this);
    }


    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        measureHelper.prepareMeasure(widthMeasureSpec, heightMeasureSpec, (int) getRotation());
        setMeasuredDimension(measureHelper.getMeasuredWidth(), measureHelper.getMeasuredHeight());
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        if (mIGSYSurfaceListener != null) {
            mIGSYSurfaceListener.onSurfaceAvailable(holder.getSurface());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        if (mIGSYSurfaceListener != null) {
            mIGSYSurfaceListener.onSurfaceSizeChanged(holder.getSurface(), width, height);
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        //清空释放
        if (mIGSYSurfaceListener != null) {
            mIGSYSurfaceListener.onSurfaceDestroyed(holder.getSurface());
        }
    }

    @Override
    public void surfaceRedrawNeeded(SurfaceHolder holder) {

    }

    @Override
    public IGSYSurfaceListener getIGSYSurfaceListener() {
        return mIGSYSurfaceListener;
    }

    @Override
    public void setIGSYSurfaceListener(IGSYSurfaceListener surfaceListener) {
        getHolder().addCallback(this);
        this.mIGSYSurfaceListener = surfaceListener;
    }


    @Override
    public int getSizeH() {
        return measureHelper.getMeasuredHeight();
    }

    @Override
    public int getSizeW() {
        return measureHelper.getMeasuredWidth();
    }

    @Override
    public void taskShotPic(GSYVideoShotListener gsyVideoShotListener, boolean shotHigh) {
        if (gsyVideoShotListener == null) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            Debuger.printfLog(getClass().getSimpleName() +
                " Build.VERSION.SDK_INT < Build.VERSION_CODES.N not support taskShotPic now");
            gsyVideoShotListener.getBitmap(null);
            return;
        }

        final Bitmap bitmap = shotHigh ? initCoverHigh() : initCover();
        if (bitmap == null || getHolder() == null || getHolder().getSurface() == null
            || !getHolder().getSurface().isValid()) {
            gsyVideoShotListener.getBitmap(null);
            return;
        }

        final Handler mainHandler = new Handler(Looper.getMainLooper());
        final HandlerThread handlerThread = new HandlerThread("GSY-CustomPixelCopy");
        try {
            handlerThread.start();
            PixelCopy.request(this, bitmap, new PixelCopy.OnPixelCopyFinishedListener() {
                @Override
                public void onPixelCopyFinished(int copyResult) {
                    final Bitmap result;
                    if (copyResult == PixelCopy.SUCCESS) {
                        result = bitmap;
                    } else {
                        if (!bitmap.isRecycled()) {
                            bitmap.recycle();
                        }
                        result = null;
                    }
                    handlerThread.quitSafely();
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            gsyVideoShotListener.getBitmap(result);
                        }
                    });
                }
            }, new Handler(handlerThread.getLooper()));
        } catch (Exception e) {
            e.printStackTrace();
            handlerThread.quitSafely();
            if (!bitmap.isRecycled()) {
                bitmap.recycle();
            }
            gsyVideoShotListener.getBitmap(null);
        }
    }

    @Override
    public void saveFrame(File file, boolean high, GSYVideoShotSaveListener gsyVideoShotSaveListener) {
        taskShotPic(new GSYVideoShotListener() {
            @Override
            public void getBitmap(Bitmap bitmap) {
                boolean success = bitmap != null && FileUtils.saveBitmapToFile(bitmap, file);
                if (gsyVideoShotSaveListener != null) {
                    gsyVideoShotSaveListener.result(success, file);
                }
            }
        }, high);
    }

    @Override
    public View getRenderView() {
        return this;
    }

    @Override
    public Bitmap initCover() {
        if (getSizeW() <= 0 || getSizeH() <= 0) {
            return null;
        }
        return Bitmap.createBitmap(getSizeW(), getSizeH(), Bitmap.Config.RGB_565);
    }

    @Override
    public Bitmap initCoverHigh() {
        if (getSizeW() <= 0 || getSizeH() <= 0) {
            return null;
        }
        return Bitmap.createBitmap(getSizeW(), getSizeH(), Bitmap.Config.ARGB_8888);
    }

    @Override
    public void onRenderResume() {

    }

    @Override
    public void onRenderPause() {

    }

    @Override
    public void releaseRenderAll() {

    }

    @Override
    public void setRenderMode(int mode) {

    }

    @Override
    public void setRenderTransform(Matrix transform) {

    }

    @Override
    public void setGLRenderer(GSYVideoGLViewBaseRender renderer) {

    }

    @Override
    public void setGLMVPMatrix(float[] MVPMatrix) {

    }

    @Override
    public void setGLEffectFilter(GSYVideoGLView.ShaderInterface effectFilter) {

    }

    @Override
    public void setVideoParamsListener(MeasureHelper.MeasureFormVideoParamsListener listener) {
        mVideoParamsListener = listener;
    }

    @Override
    public int getCurrentVideoWidth() {
        if (mVideoParamsListener != null) {
            return mVideoParamsListener.getCurrentVideoWidth();
        }
        return 0;
    }

    @Override
    public int getCurrentVideoHeight() {
        if (mVideoParamsListener != null) {
            return mVideoParamsListener.getCurrentVideoHeight();
        }
        return 0;
    }

    @Override
    public int getVideoSarNum() {
        if (mVideoParamsListener != null) {
            return mVideoParamsListener.getVideoSarNum();
        }
        return 0;
    }

    @Override
    public int getVideoSarDen() {
        if (mVideoParamsListener != null) {
            return mVideoParamsListener.getVideoSarDen();
        }
        return 0;
    }

    /**
     * 添加播放的view
     */
    public static CustomTextureSurface addSurfaceView(Context context, ViewGroup textureViewContainer, int rotate,
                                                      final IGSYSurfaceListener gsySurfaceListener,
                                                      final MeasureHelper.MeasureFormVideoParamsListener videoParamsListener) {
        if (textureViewContainer.getChildCount() > 0) {
            textureViewContainer.removeAllViews();
        }
        CustomTextureSurface showSurfaceView = new CustomTextureSurface(context);
        showSurfaceView.setIGSYSurfaceListener(gsySurfaceListener);
        showSurfaceView.setRotation(rotate);
        showSurfaceView.setVideoParamsListener(videoParamsListener);
        GSYRenderView.addToParent(textureViewContainer, showSurfaceView);
        return showSurfaceView;
    }
}
