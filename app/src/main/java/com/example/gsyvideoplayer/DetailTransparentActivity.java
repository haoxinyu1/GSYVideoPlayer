package com.example.gsyvideoplayer;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.example.gsyvideoplayer.databinding.ActivityDetailControlBinding;
import com.example.gsyvideoplayer.databinding.ActivityDetailTransparentControlBinding;
import com.example.gsyvideoplayer.utils.CommonUtil;
import com.example.gsyvideoplayer.utils.JumpUtils;
import com.shuyu.gsyvideoplayer.GSYBaseActivityDetail;
import com.shuyu.gsyvideoplayer.builder.GSYVideoOptionBuilder;
import com.shuyu.gsyvideoplayer.listener.GSYVideoGifSaveListener;
import com.shuyu.gsyvideoplayer.listener.GSYVideoShotListener;
import com.shuyu.gsyvideoplayer.listener.LockClickListener;
import com.shuyu.gsyvideoplayer.utils.Debuger;
import com.shuyu.gsyvideoplayer.utils.FileUtils;
import com.shuyu.gsyvideoplayer.utils.GifCreateHelper;
import com.shuyu.gsyvideoplayer.video.StandardGSYVideoPlayer;

import java.io.File;
import java.io.FileNotFoundException;

import permissions.dispatcher.NeedsPermission;
import permissions.dispatcher.OnNeverAskAgain;
import permissions.dispatcher.OnPermissionDenied;
import permissions.dispatcher.OnShowRationale;
import permissions.dispatcher.PermissionRequest;
import permissions.dispatcher.RuntimePermissions;


@RuntimePermissions
public class DetailTransparentActivity extends GSYBaseActivityDetail<StandardGSYVideoPlayer> {

    //private final String url = "https://res.exexm.com/cw_145225549855002";
    private final String url = "https://res.exexm.com/cw_145225549855002";
    //private String url = "http://livecdn1.news.cn/Live_MajorEvent01Phone/manifest.m3u8";
    //private String url = "https://ruigongkao.oss-cn-shenzhen.aliyuncs.com/transcode/video/source/video/8908d124aa839d0d3fa9593855ef5957.m3u8";
    //private String url2 = "http://ruigongkao.oss-cn-shenzhen.aliyuncs.com/transcode/video/source/video/3aca1a0db8db9418dcbc765848c8903e.m3u8";


    private GifCreateHelper mGifCreateHelper;

    private float speed = 1;
    private ActivityDetailTransparentControlBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDetailTransparentControlBinding.inflate(getLayoutInflater());

        View rootView = binding.getRoot();
        setContentView(rootView);


        resolveNormalVideoUI();

        initVideoBuilderMode();

        initGifHelper();

        binding.detailPlayer.setLockClickListener(new LockClickListener() {
            @Override
            public void onClick(View view, boolean lock) {
                //if (orientationUtils != null) {
                //配合下方的onConfigurationChanged
                //orientationUtils.setEnable(!lock);
                //}
            }
        });

        binding.changeSpeed.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resolveTypeUI();
            }
        });

        /*VideoOptionModel videoOptionModel =
                new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "dns_cache_clear", 1);
        List<VideoOptionModel> list = new ArrayList<>();
        list.add(videoOptionModel);
        videoOptionModel =
                new VideoOptionModel(IjkMediaPlayer.OPT_CATEGORY_FORMAT, "rtsp_transport", "tcp");
        list.add(videoOptionModel);
        GSYVideoManager.instance().setOptionModelList(list);*/

        binding.jump.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                JumpUtils.gotoControl(DetailTransparentActivity.this);
                //startActivity(new Intent(DetailControlActivity.this, MainActivity.class));
            }
        });

        binding.shot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DetailTransparentActivityPermissionsDispatcher.shotImageWithPermissionCheck(DetailTransparentActivity.this, v);
            }
        });


        binding.startGif.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DetailTransparentActivityPermissionsDispatcher.startGifWithPermissionCheck(DetailTransparentActivity.this);
            }
        });

        binding.stopGif.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                stopGif();
            }
        });

        binding.loadingView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //do nothing
            }
        });
    }

    @Override
    public StandardGSYVideoPlayer getGSYVideoPlayer() {
        return binding.detailPlayer;
    }

    @Override
    public GSYVideoOptionBuilder getGSYVideoOptionBuilder() {
        //内置封面可参考SampleCoverVideo
        ImageView imageView = new ImageView(this);
        loadCover(imageView, url);
        return new GSYVideoOptionBuilder()
            .setThumbImageView(imageView)
            .setUrl(url)
            .setCacheWithPlay(true)
            .setVideoTitle(" ")
            .setIsTouchWiget(true)
            .setRotateViewAuto(false)
            .setLockLand(false)
            .setShowFullAnimation(true)//打开动画
            .setNeedLockFull(true)
            .setSeekRatio(1);
    }

    @Override
    public void clickForFullScreen() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mGifCreateHelper.cancelTask();
    }


    /*******************************竖屏全屏开始************************************/

    @Override
    public void initVideo() {
        super.initVideo();
        //重载后实现点击，不横屏
        if (getGSYVideoPlayer().getFullscreenButton() != null) {
            getGSYVideoPlayer().getFullscreenButton().setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    //第一个true是否需要隐藏actionbar，第二个true是否需要隐藏statusbar
                    getGSYVideoPlayer().startWindowFullscreen(DetailTransparentActivity.this, true, true);
                }
            });
        }
    }


    /**
     * 是否启动旋转横屏，true表示启动
     *
     * @return true
     */
    @Override
    public boolean getDetailOrientationRotateAuto() {
        return false;
    }

    //重载后关闭重力旋转
    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        orientationUtils.setEnable(false);
    }

    //重载后不做任何事情，实现竖屏全屏
    @Override
    public void onQuitFullscreen(String url, Object... objects) {
        super.onQuitFullscreen(url, objects);
    }

    /*******************************竖屏全屏结束************************************/

    private void initGifHelper() {
        mGifCreateHelper = new GifCreateHelper(binding.detailPlayer, new GSYVideoGifSaveListener() {
            @Override
            public void result(boolean success, File file) {
                binding.detailPlayer.post(new Runnable() {
                    @Override
                    public void run() {
                        binding.loadingView.setVisibility(View.GONE);
                        Toast.makeText(binding.detailPlayer.getContext(), "创建成功", Toast.LENGTH_LONG).show();
                    }
                });
            }

            @Override
            public void process(int curPosition, int total) {
                Debuger.printfError(" current " + curPosition + " total " + total);
            }
        });
    }


    /**
     * 开始gif截图
     */
    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void startGif() {
        //开始缓存各个帧
        mGifCreateHelper.startGif(new File(FileUtils.getPath()));

    }

    /**
     * 生成gif
     */
    void stopGif() {
        binding.loadingView.setVisibility(View.VISIBLE);
        mGifCreateHelper.stopGif(new File(FileUtils.getPath(), "GSY-Z-" + System.currentTimeMillis() + ".gif"));
    }

    @OnShowRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showRationaleForCamera(final PermissionRequest request) {
        new AlertDialog.Builder(this)
            .setMessage("快给我权限")
            .setPositiveButton("允许", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    request.proceed();
                }
            })
            .setNegativeButton("拒绝", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    request.cancel();
                }
            })
            .show();
    }

    @OnPermissionDenied(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void showDeniedForCamera() {
        Toast.makeText(this, "没有权限啊", Toast.LENGTH_SHORT).show();
    }

    @OnNeverAskAgain(Manifest.permission.CAMERA)
    void showNeverAskForCamera() {
        Toast.makeText(this, "再次授权", Toast.LENGTH_SHORT).show();
    }

    /**
     * 视频截图
     * 这里没有做读写本地sd卡的权限处理，记得实际使用要加上
     */
    @NeedsPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
    void shotImage(final View v) {
        //获取截图
        binding.detailPlayer.taskShotPic(new GSYVideoShotListener() {
            @Override
            public void getBitmap(Bitmap bitmap) {
                if (bitmap != null) {
                    try {
                        CommonUtil.saveBitmap(DetailTransparentActivity.this, bitmap);
                    } catch (FileNotFoundException e) {
                        showToast("save fail ");
                        e.printStackTrace();
                        return;
                    }
                    showToast("save success ");
                } else {
                    showToast("get bitmap fail ");
                }
            }
        });

    }


    private void loadCover(ImageView imageView, String url) {

        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        imageView.setImageResource(R.mipmap.xxx1);

        Glide.with(this.getApplicationContext())
            .setDefaultRequestOptions(
                new RequestOptions()
                    .frame(3000000)
                    .centerCrop()
                    .error(R.mipmap.xxx2)
                    .placeholder(R.mipmap.xxx1))
            .load(url)
            .into(imageView);
    }

    private void resolveNormalVideoUI() {
        //增加title
        binding.detailPlayer.getTitleTextView().setVisibility(View.GONE);
        binding.detailPlayer.getBackButton().setVisibility(View.GONE);
    }

    /**
     * 显示比例
     * 注意，GSYVideoType.setShowType是全局静态生效，除非重启APP。
     */
    private void resolveTypeUI() {
        if (speed == 1) {
            speed = 1.5f;
        } else if (speed == 1.5f) {
            speed = 2f;
        } else if (speed == 2) {
            speed = 0.5f;
        } else if (speed == 0.5f) {
            speed = 0.25f;
        } else if (speed == 0.25f) {
            speed = 1;
        }
        binding.changeSpeed.setText("播放速度：" + speed);
        binding.detailPlayer.setSpeedPlaying(speed, true);
    }


    private void showToast(final String tip) {
        binding.detailPlayer.post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(DetailTransparentActivity.this, tip, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // NOTE: delegate the permission handling to generated method
        DetailTransparentActivityPermissionsDispatcher.onRequestPermissionsResult(this, requestCode, grantResults);
    }

}
