package io.dcloud.media.weex.weex_video.ijkplayer.media;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.PointF;
import android.graphics.PorterDuff;
import android.graphics.drawable.LayerDrawable;
import android.media.AudioManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseArray;
import android.view.GestureDetector;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.dcloud.android.annotation.IntDef;
import com.dcloud.android.v4.view.MotionEventCompat;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;

import io.dcloud.media.weex.weex_video.ijkplayer.OnPlayerChangedListener;
import io.dcloud.media.weex.weex_video.ijkplayer.VideoR;
import io.dcloud.media.weex.weex_video.ijkplayer.danmaku.BaseDanmakuConverter;
import io.dcloud.media.weex.weex_video.ijkplayer.danmaku.BiliDanmukuParser;
import io.dcloud.media.weex.weex_video.ijkplayer.danmaku.OnDanmakuListener;
import io.dcloud.media.weex.weex_video.ijkplayer.danmaku.StandardDanmaKuParser;
import io.dcloud.media.weex.weex_video.ijkplayer.utils.MotionEventUtils;
import io.dcloud.media.weex.weex_video.ijkplayer.utils.NetWorkUtils;
import io.dcloud.media.weex.weex_video.ijkplayer.widgets.MarqueeTextView;
import master.flame.danmaku.controller.DrawHandler;
import master.flame.danmaku.controller.IDanmakuView;
import master.flame.danmaku.danmaku.loader.ILoader;
import master.flame.danmaku.danmaku.loader.IllegalDataException;
import master.flame.danmaku.danmaku.loader.android.DanmakuLoaderFactory;
import master.flame.danmaku.danmaku.model.BaseDanmaku;
import master.flame.danmaku.danmaku.model.DanmakuTimer;
import master.flame.danmaku.danmaku.model.android.DanmakuContext;
import master.flame.danmaku.danmaku.model.android.Danmakus;
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser;
import master.flame.danmaku.danmaku.parser.IDataSource;
import tv.danmaku.ijk.media.player.IMediaPlayer;
import tv.danmaku.ijk.media.player.IjkMediaPlayer;

import static android.view.GestureDetector.OnGestureListener;
import static android.view.GestureDetector.SimpleOnGestureListener;
import static android.widget.SeekBar.OnSeekBarChangeListener;
import static io.dcloud.media.weex.weex_video.ijkplayer.utils.StringUtils.generateTime;
import static master.flame.danmaku.danmaku.model.BaseDanmaku.TYPE_SCROLL_RL;
import static tv.danmaku.ijk.media.player.IMediaPlayer.OnInfoListener;

/**
 * Created by long on 2016/10/24.
 */
public class IjkPlayerView extends FrameLayout implements View.OnClickListener {

    // 进度条最大值
    private static final int MAX_VIDEO_SEEK = 1000;
    // 默认隐藏控制栏时间
    private static final int DEFAULT_HIDE_TIMEOUT = 5000;
    // 更新进度消息
    private static final int MSG_UPDATE_SEEK = 10086;
    // 更新播放时间
    private static final int MSG_UPDATE_TIME = 10099;
    // 使能翻转消息
    private static final int MSG_ENABLE_ORIENTATION = 10087;
    // 尝试重连消息
    private static final int MSG_TRY_RELOAD = 10088;
    // 无效变量
    private static final int INVALID_VALUE = -1;
    // 达到文件时长的允许误差值，用来判断是否播放完成
    private static final int INTERVAL_TIME = 1000;

    // 原生的IjkPlayer
    private IjkVideoView mVideoView;
    // 视频开始前的缩略图，根据需要外部进行加载
    public ImageView mPlayerThumb;
    // 加载
    private ProgressBar mLoadingView;
    // 音量
    private TextView mTvVolume;
    // 亮度
    private TextView mTvBrightness;
    // 快进
    private TextView mTvFastForward;
    // 触摸信息布局
    private FrameLayout mFlTouchLayout;
    // 全屏下的后退键
    private ImageView mIvBack;
    // 全屏下的标题
    private MarqueeTextView mTvTitle;
    // 全屏下的TopBar
    private LinearLayout mFullscreenTopBar;

    // 播放键
    private ImageView mIvPlay;
    private ImageView mIvPlayCircle;
    // 当前时间
    private TextView mTvCurTime;
    // 进度条
    private SeekBar mPlayerSeek;
    // 结束时间
    private TextView mTvEndTime;
    // 全屏切换按钮
    private ImageView mIvFullscreen;
    // BottomBar
    private LinearLayout mLlBottomBar;
    // 整个视频框架布局
    private FrameLayout mFlVideoBox;
    // 还原屏幕
    private TextView mTvRecoverScreen;
    // 静音
    private ImageView mIVMute;
    // 中间的暂停键
    private ImageView mIvPlayCenter;

    // 关联的Activity
    private Activity mAttachActivity;
    // 重试
//    private ImageView mTvReload;
//    private View mFlReload;

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            if (msg.what == MSG_UPDATE_SEEK) {
                final int pos = _setProgress();
                if (!mIsSeeking && mIsShowBar && mVideoView.isPlaying()) {
                    // 这里会重复发送MSG，已达到实时更新 Seek 的效果
                    msg = obtainMessage(MSG_UPDATE_SEEK);
                    sendMessageDelayed(msg, 1000 - (pos % 1000));
                }
            } else if (msg.what == MSG_ENABLE_ORIENTATION) {
                if (mOrientationListener != null) {
                    mOrientationListener.enable();
                }
            } else if (msg.what == MSG_TRY_RELOAD) {
                if (mIsNetConnected) {
                    reload();
                }
                msg = obtainMessage(MSG_TRY_RELOAD);
                sendMessageDelayed(msg, 3000);
            } else if (msg.what == MSG_UPDATE_TIME) {
                progressCallBack();
                msg = obtainMessage(MSG_UPDATE_TIME);
                sendMessageDelayed(msg, 250);
            }
        }
    };
    // 音量控制
    private AudioManager mAudioManager;
    // 手势控制
    private GestureDetector mGestureDetector;
    // 最大音量
    private int mMaxVolume;
    // 锁屏
    private boolean mIsForbidTouch = false;
    // 是否显示控制栏
    private boolean mIsShowBar = true;
    // 是否全屏
    private boolean mIsFullscreen;
    // 是否播放结束
    private boolean mIsPlayComplete = false;
    // 是否正在拖拽进度条
    private boolean mIsSeeking;
    // 目标进度
    private long mTargetPosition = INVALID_VALUE;
    // 当前进度
    private int mCurPosition = INVALID_VALUE;
    // 当前音量
    private int mCurVolume = INVALID_VALUE;
    // 当前亮度
    private float mCurBrightness = INVALID_VALUE;
    // 初始高度
    private int mInitHeight;
    // 屏幕宽/高度
    private int mWidthPixels;
    // 屏幕UI可见性
    private int mScreenUiVisibility;
    // 屏幕旋转角度监听
    private OrientationEventListener mOrientationListener;
    // 进来还未播放
    private boolean mIsNeverPlay = true;
    // 外部监听器
    private OnInfoListener mOutsideInfoListener;
    private IMediaPlayer.OnCompletionListener mCompletionListener;
    // 禁止翻转，默认为禁止
    private boolean mIsForbidOrientation = true;
    // 是否固定全屏状态
    private boolean mIsAlwaysFullScreen = false;
    // 记录按退出全屏时间
    private long mExitTime = 0;
    // 视频Matrix
    private Matrix mVideoMatrix = new Matrix();
    private Matrix mSaveMatrix = new Matrix();
    // 是否需要显示恢复屏幕按钮
    private boolean mIsNeedRecoverScreen = false;
    // 选项列表高度
    private int mAspectOptionsHeight;
    // 异常中断时的播放进度
    private int mInterruptPosition;
    private boolean mIsReady = false;

    // 是否rtmp地址
    private boolean isRtmpUri = false;
    // 在非全屏模式下，是否开启亮度与音量调节手势
    private boolean isPageGesture = false;
    // 是否开启控制进度的手势
    private boolean isProgressGesture = true;

    private ViewGroup mRootLayout;


    public IjkPlayerView(Context context) {
        this(context, null);
    }

    public IjkPlayerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        _initView(context);
    }

    private void _initView(Context context) {
        if (context instanceof Activity) {
            mAttachActivity = (Activity) context;
        } else {
            throw new IllegalArgumentException("Context must be Activity");
        }
        View.inflate(context, VideoR.VIDEO_IJK_LAYOUT_PLAYER_VIEW, this);
        mVideoView = (IjkVideoView) findViewById(VideoR.VIDEO_IJK_ID_VIDEO_VIEW);
        mPlayerThumb = (ImageView) findViewById(VideoR.VIDEO_IJK_ID_IV_THUMB);
        mLoadingView = (ProgressBar) findViewById(VideoR.VIDEO_IJK_ID_PD_LOADING);
        mTvVolume = (TextView) findViewById(VideoR.VIDEO_IJK_ID_TV_VOLUME);
        mTvBrightness = (TextView) findViewById(VideoR.VIDEO_IJK_ID_TV_BRIGHTNESS);
        mTvFastForward = (TextView) findViewById(VideoR.VIDEO_IJK_ID_TV_FAST_FORWARD);
        mFlTouchLayout = (FrameLayout) findViewById(VideoR.VIDEO_IJK_ID_FL_TOUCH_LAYOUT);
        mIvBack = (ImageView) findViewById(VideoR.VIDEO_IJK_ID_IV_BACK);
        mTvTitle = (MarqueeTextView) findViewById(VideoR.VIDEO_IJK_ID_TV_TITLE);
        mFullscreenTopBar = (LinearLayout) findViewById(VideoR.VIDEO_IJK_ID_FULLSCREEN_TOP_BAR);
        mIvPlay = (ImageView) findViewById(VideoR.VIDEO_IJK_ID_IV_PLAY);
        mTvCurTime = (TextView) findViewById(VideoR.VIDEO_IJK_ID_TV_CUR_TIME);
        mPlayerSeek = (SeekBar) findViewById(VideoR.VIDEO_IJK_ID_PLAYER_SEEK);
        mTvEndTime = (TextView) findViewById(VideoR.VIDEO_IJK_ID_TV_END_TIME);
        mIvFullscreen = (ImageView) findViewById(VideoR.VIDEO_IJK_ID_IV_FULLSCREEN);
        mLlBottomBar = (LinearLayout) findViewById(VideoR.VIDEO_IJK_ID_LL_BOTTOM_BAR);
        mFlVideoBox = (FrameLayout) findViewById(VideoR.VIDEO_IJK_ID_FL_VIDEO_BOX);
        mIvPlayCircle = (ImageView) findViewById(VideoR.VIDEO_IJK_ID_IV_PLAY_CIRCLE);
        mTvRecoverScreen = (TextView) findViewById(VideoR.VIDEO_IJK_ID_TV_RECOVER_SCREEN);
//        mTvReload = (ImageView) findViewById(VideoR.VIDEO_IJK_ID_TV_RELOAD);
//        mFlReload = findViewById(VideoR.VIDEO_IJK_ID_FL_RELOAD_LAYOUT);
        mIvDanmakuControl = (TextView) findViewById(VideoR.VIDEO_IJK_ID_IV_DANMAKU_CONTROL);
        mIVMute = findViewById(VideoR.VIDEO_IJK_ID_IV_MUTE);
        mIvPlayCenter = findViewById(VideoR.VIDEO_IJK_ID_IV_PLAY_CENTER);

        mAspectOptionsHeight = getResources().getDimensionPixelSize(VideoR.VIDEO_IJK_DIMEN_ASPECT_BNT_SIZE) * 4;
        _initReceiver();

        mIvPlay.setOnClickListener(this);
        mIvBack.setOnClickListener(this);
        mIvFullscreen.setOnClickListener(this);
        mIvPlayCircle.setOnClickListener(this);
        mTvRecoverScreen.setOnClickListener(this);
        mIVMute.setOnClickListener(this);
        mIvPlayCenter.setOnClickListener(this);

//        mTvReload.setOnClickListener(this);
    }

    public void setVideoVisibility(){
        if (mVideoView != null) {
            mVideoView.setVisibility(VISIBLE);
        }
    }

    private void setSeekBarColor() {
        LayerDrawable drawable = (LayerDrawable) mPlayerSeek.getProgressDrawable();
        drawable.findDrawableByLayerId(android.R.id.background).setColorFilter(Color.parseColor("#ff00ff"), PorterDuff.Mode.SRC_ATOP);
        drawable.findDrawableByLayerId(android.R.id.secondaryProgress).setColorFilter(Color.parseColor("#ffff00"), PorterDuff.Mode.SRC_ATOP);
        drawable.findDrawableByLayerId(android.R.id.progress).setColorFilter(Color.parseColor("#00ffff"), PorterDuff.Mode.SRC_ATOP);
        mPlayerSeek.getThumb().setColorFilter(Color.parseColor("#0000ff"), PorterDuff.Mode.SRC_ATOP);
    }


    private float defaultScreenBrightness;

    /**
     * 初始化
     */
    private void _initMediaPlayer() {
        // 加载 IjkMediaPlayer 库
        IjkMediaPlayer.loadLibrariesOnce(null);
        IjkMediaPlayer.native_profileBegin("libijkplayer.so");
        // 声音
        mAudioManager = (AudioManager) mAttachActivity.getSystemService(Context.AUDIO_SERVICE);
        mMaxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        // 保存初始进来的亮度，原方法当设置为自动亮度时，亮度显示不准确
        WindowManager.LayoutParams lp = mAttachActivity.getWindow().getAttributes();
        defaultScreenBrightness = lp.screenBrightness;
        // 进度
        mPlayerSeek.setMax(MAX_VIDEO_SEEK);
        mPlayerSeek.setOnSeekBarChangeListener(mSeekListener);
        // 视频监听
        mVideoView.setOnInfoListener(mInfoListener);
        mVideoView.setOnBufferingUpdateListener(onBufferingUpdateListener);
        // 触摸控制
        mGestureDetector = new GestureDetector(mAttachActivity, mPlayerGestureListener);
        mFlVideoBox.setClickable(true);
        mFlVideoBox.setOnTouchListener(mPlayerTouchListener);
        // 屏幕翻转控制
        mOrientationListener = new OrientationEventListener(mAttachActivity) {
            @Override
            public void onOrientationChanged(int orientation) {
                _handleOrientation(orientation);
            }
        };
        if (mIsForbidOrientation) {
            // 禁止翻转
            mOrientationListener.disable();
        }
    }

    @Override
    protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
        super.onLayout(changed, left, top, right, bottom);
        if (mInitHeight == 0) {
            mInitHeight = getHeight();
            mWidthPixels = getResources().getDisplayMetrics().widthPixels;
        }
    }

    /**============================ 外部调用接口 ============================*/

    /**
     * Activity.onResume() 里调用
     */
    public void onResume() {
        if (mIsScreenLocked) {
            // 如果出现锁屏则需要重新渲染器Render，不然会出现只有声音没有动画
            // 目前只在锁屏时会出现图像不动的情况，如果有遇到类似情况可以尝试按这个方法解决
//            mVideoView.setRender(IjkVideoView.RENDER_TEXTURE_VIEW);
            mIsScreenLocked = false;
        }
        //一加手机会出现上述现象，所以搬出来了
        mVideoView.setRender(IjkVideoView.RENDER_TEXTURE_VIEW);
        mVideoView.resume();
        if (!mIsForbidTouch && !mIsForbidOrientation) {
            mOrientationListener.enable();
        }
        if (mCurPosition != INVALID_VALUE) {
            // 重进后 seekTo 到指定位置播放时，通常会回退到前几秒，关键帧??
            seekTo(mCurPosition);
            mCurPosition = INVALID_VALUE;
        }
    }

    /**
     * Activity.onPause() 里调用
     */
    public void onPause() {
        mCurPosition = mVideoView.getCurrentPosition();
        mVideoView.pause();
        mIvPlay.setSelected(false);
        mIvPlayCenter.setSelected(false);
        mOrientationListener.disable();
        _pauseDanmaku();
    }

    /**
     * Activity.onDestroy() 里调用
     *
     * @return 返回播放进度
     */
    public int onDestroy() {
        // 记录播放进度
        int curPosition = mVideoView.getCurrentPosition();
        mVideoView.destroy();
        IjkMediaPlayer.native_profileEnd();
        if (mDanmakuView != null) {
            // don't forget release!
            mDanmakuView.release();
            mDanmakuView = null;
        }

        mHandler.removeMessages(MSG_TRY_RELOAD);
        mHandler.removeMessages(MSG_UPDATE_SEEK);
        mHandler.removeMessages(MSG_UPDATE_TIME);
        // 注销广播
//        mAttachActivity.unregisterReceiver(mBatteryReceiver);
        mAttachActivity.unregisterReceiver(mScreenReceiver);
        mAttachActivity.unregisterReceiver(mNetReceiver);
        // 关闭屏幕常亮
        mAttachActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // 关闭静音，部分手机
        setMutePlayer(false);
        // 退出时亮度调回默认值
        WindowManager.LayoutParams lp = mAttachActivity.getWindow().getAttributes();
        lp.screenBrightness = defaultScreenBrightness;
        mAttachActivity.getWindow().setAttributes(lp);
        return curPosition;
    }

    /**
     * 处理音量键，避免外部按音量键后导航栏和状态栏显示出来退不回去的状态
     *
     * @param keyCode
     * @return
     */
    public boolean handleVolumeKey(int keyCode) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
            _setVolume(true);
            return true;
        } else if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            _setVolume(false);
            return true;
        } else {
            return false;
        }
    }

    /**
     * 回退，全屏时退回竖屏
     *
     * @return
     */
    public boolean onBackPressed() {
        if (recoverFromEditVideo()) {
            return true;
        }
        if (mIsAlwaysFullScreen) {
            return true;
        } else if (mIsFullscreen) {
            exitFullScreen();
            if (mIsForbidTouch) {
                // 锁住状态则解锁
//                mIsForbidTouch = false;
                _setControlBarVisible(mIsShowBar);
            }
            return true;
        }
        return false;
    }

    /**
     * 初始化，必须要先调用
     *
     * @return
     */
    public IjkPlayerView init() {
        _initMediaPlayer();
        return this;
    }

    /**
     * 切换视频
     *
     * @param url
     * @return
     */
    public IjkPlayerView switchVideoPath(String url) {
        return switchVideoPath(Uri.parse(url));
    }

    /**
     * 切换视频
     *
     * @param uri
     * @return
     */
    public IjkPlayerView switchVideoPath(Uri uri) {
        reset();
        _setControlBarVisible(true);
        duration = -1;
        return setVideoPath(uri);
    }

    /**
     * 设置播放资源
     *
     * @param url
     * @return
     */
    public IjkPlayerView setVideoPath(String url) {
        return setVideoPath(Uri.parse(url));
    }

    public IjkPlayerView setPlayerRootView(ViewGroup rootView) {
        mRootLayout = rootView;
        return this;
    }

    /**
     * 设置播放资源
     *
     * @param uri
     * @return
     */
    public IjkPlayerView setVideoPath(Uri uri) {
        mVideoView.setVideoURI(uri);
        if (uri.toString().startsWith("rtmp:")) {
            isRtmpUri = true;
            mPlayerSeek.setEnabled(false);
            mPlayerSeek.setVisibility(View.INVISIBLE);
            mTvEndTime.setVisibility(View.INVISIBLE);
            mTvCurTime.setVisibility(View.INVISIBLE);
        } else {
            isRtmpUri = false;
            mPlayerSeek.setEnabled(true);
            mPlayerSeek.setVisibility(isShowProgress?VISIBLE:INVISIBLE);
            mTvEndTime.setVisibility(View.VISIBLE);
            mTvCurTime.setVisibility(View.VISIBLE);
        }
        if (mCurPosition != INVALID_VALUE) {
            seekTo(mCurPosition);
            mCurPosition = INVALID_VALUE;
        } else {
            seekTo(0);
        }
        /*if (mFlReload.getVisibility() == VISIBLE) {
            mFlReload.setVisibility(GONE);
        }*/
        return this;
    }

    /**
     * 设置播放资源，来自assets
     *
     * @param fd
     * @return
     */
    public IjkPlayerView setVideoFileDescriptor(AssetsDataSourceProvider fd) {
        mVideoView.setVideoFileDescriptor(fd);
        if (mCurPosition != INVALID_VALUE) {
            seekTo(mCurPosition);
            mCurPosition = INVALID_VALUE;
        } else {
            seekTo(0);
        }
        /*if (mFlReload.getVisibility() == VISIBLE) {
            mFlReload.setVisibility(GONE);
        }*/
        return this;
    }

    /**
     * 切换播放资源，来自assets
     *
     * @param fd
     * @return
     */
    public IjkPlayerView switchVideoFileDescriptor(AssetsDataSourceProvider fd) {
        reset();
        _setControlBarVisible(true);
        duration = -1;
        return setVideoFileDescriptor(fd);
    }

    /**
     * 设置标题，全屏的时候可见
     *
     * @param title
     */
    public IjkPlayerView setTitle(String title) {
        mTvTitle.setText(title);
        return this;
    }

    /**
     * 是否显示静音按钮
     *
     * @param isShow
     */
    public void isMuteBtnShow(boolean isShow) {
        if (isShow)
            mIVMute.setVisibility(VISIBLE);
        else
            mIVMute.setVisibility(GONE);
    }

    private boolean isPlayBtnCenter = false;

    public void setPlayBtnPosition(String position) {
        isPlayBtnCenter = position.equals("center");
        if (isPlayBtnCenter) {
//            mIvPlayCenter.setVisibility(VISIBLE);
            mIvPlay.setVisibility(GONE);
        } else if (isPlayBtnVisibility){
            mIvPlayCenter.setVisibility(GONE);
            mIvPlay.setVisibility(VISIBLE);
        }
    }

    /**
     * 设置只显示全屏状态
     */
    public IjkPlayerView alwaysFullScreen() {
        mIsAlwaysFullScreen = true;
        fullScreen(mOrientation);
        mIvFullscreen.setVisibility(GONE);
        return this;
    }

    /**
     * 开始播放
     *
     * @return
     */
    public void start() {
        if (mIsPlayComplete) {
            if (mDanmakuView != null && mDanmakuView.isPrepared()) {
                mDanmakuView.seekTo((long) 0);
                mDanmakuView.pause();
            }
            mIsPlayComplete = false;
        }
        if (!mVideoView.isPlaying()) {
            mIvPlay.setSelected(true);
            mIvPlayCenter.setSelected(true);
//            if (mInterruptPosition > 0) {
//                mLoadingView.setVisibility(VISIBLE);
//                mHandler.sendEmptyMessage(MSG_TRY_RELOAD);
//            } else {
            mVideoView.start();
            // 更新进度
            mHandler.sendEmptyMessage(MSG_UPDATE_SEEK);
            mHandler.sendEmptyMessage(MSG_UPDATE_TIME);
//            }
        }
        mIvPlayCircle.setVisibility(GONE);
        if (mIsNeverPlay) {
            mIsNeverPlay = false;

            if (mVideoView.getCurrentState() != MediaPlayerParams.STATE_ERROR)
            mLoadingView.setVisibility(VISIBLE);

            // 2019年4月27日注释掉，其余代码放到 IMediaPlayer.MEDIA_INFO_BUFFERING_END 中
            // 2019年5月20日更正，部分网络视频频繁加载是，底部状态栏一直显示
            mIsShowBar = false;

            // 放这边装载弹幕，不然会莫名其妙出现多切几次到首页会弹幕自动播放问题，这里处理下
            //因为获得时间点，所以在视频prepared中加载
            _loadDanmaku();
        }
        // 视频播放时开启屏幕常亮
        mAttachActivity.getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public void hiddenLoaded(boolean isHidden) {
//        if (isHidden) {
//            mFlReload.setVisibility(GONE);
//        } else {
//            mFlReload.setVisibility(VISIBLE);
//        }
    }

    /**
     * 重新开始
     */
    public void reload() {
//        mFlReload.setVisibility(GONE);
        mLoadingView.setVisibility(VISIBLE);
        if (mIsReady) {
            // 确保网络正常时
            if (NetWorkUtils.isNetworkAvailable(mAttachActivity)) {
                mVideoView.reload();
                mVideoView.start();
//                start();
                if (mInterruptPosition > 0) {
                    seekTo(mInterruptPosition);
                    mInterruptPosition = 0;
                }
            } else {
                if (null != mOnPlayerChangedListener) {
                    mOnPlayerChangedListener.onChanged("error", "network error");
//                    mFlReload.setVisibility(VISIBLE);
                    mLoadingView.setVisibility(GONE);
                }
                return;
            }
        } else {
            mVideoView.release(false);
            mVideoView.setRender(IjkVideoView.RENDER_TEXTURE_VIEW);
            start();
        }
        // 更新进度
        mHandler.removeMessages(MSG_UPDATE_SEEK);
        mHandler.sendEmptyMessage(MSG_UPDATE_SEEK);


        mHandler.removeMessages(MSG_UPDATE_TIME);
        mHandler.sendEmptyMessage(MSG_UPDATE_TIME);
    }

    /**
     * 是否正在播放
     *
     * @return
     */
    public boolean isPlaying() {
        return mVideoView.isPlaying();
    }

    /**
     * 暂停
     */
    public void pause() {
        mCurPosition = mVideoView.getCurrentPosition();
        mIvPlay.setSelected(false);
        mIvPlayCenter.setSelected(false);
        if (mVideoView.isPlaying()) {
            mVideoView.pause();
        }
        mHandler.removeMessages(MSG_UPDATE_TIME);
        _pauseDanmaku();
        // 视频暂停时关闭屏幕常亮
        mAttachActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /**
     * 跳转
     *
     * @param position 位置
     */
    public void seekTo(int position) {
        if (isRtmpUri) {
            return;
        }
        mVideoView.seekTo(position);
        mDanmakuTargetPosition = position;
    }

    /**
     * 停止
     */
    public void stop() {
        pause();
        mVideoView.stopPlayback();
    }

    /**
     * 重置状态
     */
    public void reset() {
        if (mIsEnableDanmaku && mDanmakuView != null) {
            mDanmakuView.release();
            mDanmakuView = null;
            mIsEnableDanmaku = false;
        }
        mIsNeverPlay = true;
        mCurPosition = 0;
        stop();
        mVideoView.setRender(IjkVideoView.RENDER_TEXTURE_VIEW);
    }

    /**============================ 控制栏处理 ============================*/

    /**
     * SeekBar监听
     */
    private final OnSeekBarChangeListener mSeekListener = new OnSeekBarChangeListener() {

        private long curPosition;

        @Override
        public void onStartTrackingTouch(SeekBar bar) {
            mIsSeeking = true;
            _showControlBar(3600000);
            mHandler.removeMessages(MSG_UPDATE_SEEK);
            curPosition = mVideoView.getCurrentPosition();
        }

        @Override
        public void onProgressChanged(SeekBar bar, int progress, boolean fromUser) {
            if (!fromUser) {
                // We're not interested in programmatically generated changes to
                // the progress bar's position.
                return;
            }
            long duration = getDuration();
            // 计算目标位置
            mTargetPosition = (duration * progress) / MAX_VIDEO_SEEK;
            int deltaTime = (int) ((mTargetPosition - curPosition) / 1000);
            String desc;
            // 对比当前位置来显示快进或后退
            if (mTargetPosition > curPosition) {
                desc = generateTime(mTargetPosition) + "/" + generateTime(duration) + "\n" + "+" + deltaTime + "秒";
            } else {
                desc = generateTime(mTargetPosition) + "/" + generateTime(duration) + "\n" + deltaTime + "秒";
            }
            _setFastForward(desc);
        }

        @Override
        public void onStopTrackingTouch(SeekBar bar) {
            _hideTouchView();
            mIsSeeking = false;
            // 视频跳转
            seekTo((int) mTargetPosition);
            mTargetPosition = INVALID_VALUE;
            _setProgress();
            _showControlBar(DEFAULT_HIDE_TIMEOUT);
        }
    };

    /**
     * 隐藏视图Runnable
     */
    private Runnable mHideBarRunnable = new Runnable() {
        @Override
        public void run() {
            _hideAllView(false);
        }
    };

    /**
     * 隐藏除视频外所有视图
     */
    private void _hideAllView(boolean isTouchLock) {
//        mPlayerThumb.setVisibility(View.GONE);
        mFlTouchLayout.setVisibility(View.GONE);
        mFullscreenTopBar.setVisibility(View.GONE);
        mIvPlayCenter.setVisibility(View.GONE);
        mLlBottomBar.setVisibility(View.GONE);
        if (!isTouchLock) {
            mIsShowBar = false;
        }
        if (mIsNeedRecoverScreen) {
            mTvRecoverScreen.setVisibility(GONE);
        }
    }

    /**
     * 设置控制栏显示或隐藏
     *
     * @param isShowBar
     */
    private void _setControlBarVisible(boolean isShowBar) {
        if (mIsNeverPlay) {
//            if (mIvPlayCircle.getVisibility() != GONE)
//                mIvPlayCircle.setVisibility(View.VISIBLE);
            if (mIvPlayCircle.getVisibility() != VISIBLE)
                mLlBottomBar.setVisibility(isShowBar ? View.VISIBLE : View.GONE);
        } else if (mIsForbidTouch) {
        } else {
            mLlBottomBar.setVisibility(isShowBar ? View.VISIBLE : View.GONE);
            if (isPlayBtnCenter && isPlayBtnVisibility)
                mIvPlayCenter.setVisibility(isShowBar ? View.VISIBLE : View.GONE);
            // 全屏切换显示的控制栏不一样
            if (mIsFullscreen) {
                // 只在显示控制栏的时候才设置时间，因为控制栏通常不显示且单位为分钟，所以不做实时更新
//                mTvSystemTime.setText(StringUtils.getCurFormatTime());
                mFullscreenTopBar.setVisibility(isShowBar ? View.VISIBLE : View.GONE);
                if (mIsNeedRecoverScreen) {
                    mTvRecoverScreen.setVisibility(isShowBar ? View.VISIBLE : View.GONE);
                }
            } else {
                mFullscreenTopBar.setVisibility(View.GONE);
                if (mIsNeedRecoverScreen) {
                    mTvRecoverScreen.setVisibility(View.GONE);
                }
            }
        }
    }

    /**
     * 开关控制栏，单击界面的时候
     */
    private void _toggleControlBar() {
        mIsShowBar = !mIsShowBar;
        _setControlBarVisible(mIsShowBar);
        if (mIsShowBar) {
            // 发送延迟隐藏控制栏的操作
            mHandler.postDelayed(mHideBarRunnable, DEFAULT_HIDE_TIMEOUT);
            // 发送更新 Seek 消息
            mHandler.sendEmptyMessage(MSG_UPDATE_SEEK);
        }
    }

    /**
     * 显示控制栏
     *
     * @param timeout 延迟隐藏时间
     */
    private void _showControlBar(int timeout) {
        if (!mIsShowBar) {
            _setProgress();
            mIsShowBar = true;
        }
        _setControlBarVisible(true);
        mHandler.sendEmptyMessage(MSG_UPDATE_SEEK);
        // 先移除隐藏控制栏 Runnable，如果 timeout=0 则不做延迟隐藏操作
        mHandler.removeCallbacks(mHideBarRunnable);
        if (timeout != 0) {
            mHandler.postDelayed(mHideBarRunnable, timeout);
        }
    }

    /**
     * 切换播放状态，点击播放按钮时
     */
    private void _togglePlayStatus() {
        if (mVideoView.isPlaying()) {
            pause();
        } else {
            start();
        }
    }

    /**
     * 刷新隐藏控制栏的操作
     */
    private void _refreshHideRunnable() {
        mHandler.removeCallbacks(mHideBarRunnable);
        mHandler.postDelayed(mHideBarRunnable, DEFAULT_HIDE_TIMEOUT);
    }

    /**
     * 切换控制锁
     */
    /*private void _togglePlayerLock() {
        mIsForbidTouch = !mIsForbidTouch;
        if (mIsForbidTouch) {
            mOrientationListener.disable();
            _hideAllView(true);
        } else {
            if (!mIsForbidOrientation) {
                mOrientationListener.enable();
            }
            mFullscreenTopBar.setVisibility(View.VISIBLE);
            mLlBottomBar.setVisibility(View.VISIBLE);
            if (mIsNeedRecoverScreen) {
                mTvRecoverScreen.setVisibility(VISIBLE);
            }
        }
    }*/

    /**
     * 是否显示控制菜单
     *
     * @param isControls
     */
    public void setControls(boolean isControls) {
        mIsForbidTouch = !isControls;
        if (mIsForbidTouch) {
            mOrientationListener.disable();
            _hideAllView(true);
        } else {
            if (!mIsForbidOrientation) {
                mOrientationListener.enable();
            }
            if(!mIsNeverPlay) {
//                mLlBottomBar.setVisibility(View.VISIBLE);
                mIsShowBar = false;/*false 为了配合_toggleControlBar*/
                _toggleControlBar();
            } else {
                if (mIvPlayCircle.getVisibility() != VISIBLE) {
                    mIsShowBar = false;
                    _toggleControlBar();
                } else {
                    mIsShowBar = false;
                    mLlBottomBar.setVisibility(View.GONE);
                }
            }
            if (mIsNeedRecoverScreen) {
                mTvRecoverScreen.setVisibility(VISIBLE);
            }
        }
    }

    /**
     * 在非全屏模式下，是否开启亮度与音量调节手势
     *
     * @param isPageGesture
     */
    public void setPageGesture(boolean isPageGesture) {
        this.isPageGesture = isPageGesture;
    }

    /**
     * 是否显示播放进度
     *
     * @param isShow
     */
    public void setProgressVisibility(boolean isShow) {
        if (mPlayerSeek != null && !isRtmpUri) {
            isShowProgress = isShow;
            int v = isShow ? View.VISIBLE : View.INVISIBLE;
            mPlayerSeek.setVisibility(v);
        }
    }
    private boolean isShowProgress = true;

    /**
     * 是否显示全屏按钮
     *
     * @param isShow
     */
    public void setFullscreenBntVisibility(boolean isShow) {
        if (mIvFullscreen != null) {
            int v = isShow ? View.VISIBLE : View.INVISIBLE;
            mIvFullscreen.setVisibility(v);
        }
    }

    private boolean isPlayBtnVisibility = true;
    /**
     * 是否显示视频底部控制栏的播放按钮
     *
     * @param isShow
     */
    public void setPlayBntVisibility(boolean isShow) {
        if (mIvPlay != null) {
            isPlayBtnVisibility = isShow;
            int v = isShow ? View.VISIBLE : View.INVISIBLE;
            if (isPlayBtnCenter) {
                mIvPlayCenter.setVisibility(v);
            } else {
                mIvPlay.setVisibility(v);
            }
        }
    }

    private boolean isCenterPlayBtnVisibility = true;
    /**
     * 是否显示视频中间的播放按钮
     *
     * @param isShow
     */
    public void setCenterPlayBtnVisibility(boolean isShow) {
        if (mIsNeverPlay && !isPlaying())
            if (mIvPlayCircle != null) {
                isCenterPlayBtnVisibility = isShow;
                int v = isShow ? View.VISIBLE : View.GONE;
                mIvPlayCircle.setVisibility(v);
            }
    }

    /**
     * 是否开启控制进度的手势
     */
    public void setIsEnableProgressGesture(boolean isProgressGesture) {
        this.isProgressGesture = isProgressGesture;
    }

    @Override
    public void onClick(View v) {
        _refreshHideRunnable();
        int id = v.getId();
        if (id == VideoR.VIDEO_IJK_ID_IV_BACK) {
            onBackPressed();
        } else if (id == VideoR.VIDEO_IJK_ID_IV_PLAY || id == VideoR.VIDEO_IJK_ID_IV_PLAY_CIRCLE || id == VideoR.VIDEO_IJK_ID_IV_PLAY_CENTER) {
            _togglePlayStatus();
        } else if (id == VideoR.VIDEO_IJK_ID_IV_FULLSCREEN) {
            _toggleFullScreen();
        } else if (id == VideoR.VIDEO_IJK_ID_IV_DANMAKU_CONTROL) {
            _toggleDanmakuShow();
        }/*else if (id == R.id.iv_cancel_send) {
            recoverFromEditVideo();
        } else if (id == R.id.input_options_more) {
            _toggleMoreColorOptions();
        }*/ else if (id == VideoR.VIDEO_IJK_ID_TV_RECOVER_SCREEN) {
            mVideoView.resetVideoView(true);
            mIsNeedRecoverScreen = false;
            mTvRecoverScreen.setVisibility(GONE);
        } /*else if (id == VideoR.VIDEO_IJK_ID_TV_RELOAD) {
            reload();
        }*/ else if (id == VideoR.VIDEO_IJK_ID_IV_MUTE) {
            isMutePlayer = !isMutePlayer;
            setMutePlayer(isMutePlayer);
            mIVMute.setSelected(isMutePlayer);
        }
    }

    /**==================== 屏幕翻转/切换处理 ====================*/

    /**
     * 使能视频翻转
     */
    public IjkPlayerView enableOrientation() {
        mIsForbidOrientation = false;
        mOrientationListener.enable();
        return this;
    }

    /**
     * 全屏切换，点击全屏按钮
     */
    private void _toggleFullScreen() {
        if (isFullscreen()) {
            exitFullScreen();
        } else {
            fullScreen(mOrientation);
        }
    }


    String fullCallFormat = "{fullScreen:%b, direction:'%s'}";

    /**
     * 设置全屏或窗口模式
     *
     * @param isFullscreen
     */
    private void _setFullScreen(boolean isFullscreen) {
        mIsFullscreen = isFullscreen;
        mIvFullscreen.setSelected(isFullscreen);
        if (mOnPlayerChangedListener != null) {
            String msg = null;
            if (isFullscreen) {
                msg = String.format(fullCallFormat, true, "horizontal");
            } else {
                msg = String.format(fullCallFormat, false, "vertical");
            }
            mOnPlayerChangedListener.onChanged("fullscreenchange", msg);
        }
        mHandler.post(mHideBarRunnable);
        // 处理三指旋转缩放，如果之前进行了相关操作则全屏时还原之前旋转缩放的状态，窗口模式则将整个屏幕还原为未操作状态
        if (mIsNeedRecoverScreen) {
            if (isFullscreen) {
                mVideoView.adjustVideoView(1.0f);
                mTvRecoverScreen.setVisibility(mIsShowBar ? View.VISIBLE : View.GONE);
            } else {
                mVideoView.resetVideoView(false);
                mTvRecoverScreen.setVisibility(GONE);
            }
        }
    }

    /**
     * 处理屏幕翻转
     *
     * @param orientation
     */
    private void _handleOrientation(int orientation) {
        if (mIsNeverPlay) {
            return;
        }
        if (mIsFullscreen && !mIsAlwaysFullScreen) {
            // 根据角度进行竖屏切换，如果为固定全屏则只能横屏切换
            if (orientation >= 0 && orientation <= 30 || orientation >= 330) {
                // 请求屏幕翻转
                mAttachActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            }
        } else {
            // 根据角度进行横屏切换
            if (orientation >= 60 && orientation <= 120) {
                mAttachActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
            } else if (orientation >= 240 && orientation <= 300) {
                mAttachActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }
    }

    /**
     * 当屏幕执行翻转操作后调用禁止翻转功能，延迟3000ms再使能翻转，避免不必要的翻转
     */
    private void _refreshOrientationEnable() {
        if (!mIsForbidOrientation) {
            mOrientationListener.disable();
            mHandler.removeMessages(MSG_ENABLE_ORIENTATION);
            mHandler.sendEmptyMessageDelayed(MSG_ENABLE_ORIENTATION, 3000);
        }
    }

    public boolean isFullscreen() {
        return mIsFullscreen;
    }

    private ViewGroup.LayoutParams mRawParams;
    private int mOrientation = -90;

    private int originOrientation;

    public int orientation = 90;
    //设置全屏
    public void fullScreen(int orientation) {
        _refreshOrientationEnable();
        if (!mIsFullscreen) {
            originOrientation = mAttachActivity.getRequestedOrientation();
            this.orientation = orientation;
            if (orientation == 0) {
                if (mAttachActivity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) {
                    mAttachActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
                }
            } else if (orientation == 90) {
                if (mAttachActivity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE) {
                    mAttachActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE);
                }
            } else if (orientation == -90) {
                if (mAttachActivity.getRequestedOrientation() != ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    mAttachActivity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
                }
            }
//            mAttachActivity.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
//                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setNavigationBar(true);
            DisplayMetrics metrics = new DisplayMetrics();
            mAttachActivity.getWindowManager().getDefaultDisplay().getMetrics(metrics);
            mRawParams = getLayoutParams();
            ViewGroup.LayoutParams fullParams;
            if (mRawParams instanceof RelativeLayout.LayoutParams) {
                fullParams = new RelativeLayout.LayoutParams(metrics.widthPixels, metrics.heightPixels);
            } else if (mRawParams instanceof LinearLayout.LayoutParams) {
                fullParams = new LinearLayout.LayoutParams(metrics.widthPixels, metrics.heightPixels);
            } else if (mRawParams instanceof FrameLayout.LayoutParams) {
                fullParams = new FrameLayout.LayoutParams(metrics.widthPixels, metrics.heightPixels);
            } else {
                new AlertDialog.Builder(getContext())
                        .setMessage("nonsupport parent layout, please do it by yourself")
                        .setPositiveButton("OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                    }
                                })
                        .setCancelable(false)
                        .show();
                return;
            }
            setLayoutParams(fullParams);
            _setFullScreen(true);
            ViewGroup rootGroup = (ViewGroup) mAttachActivity.getWindow().getDecorView().findViewById(android.R.id.content);
            if (rootGroup instanceof FrameLayout) {
                if (getParent() != rootGroup) {
                    ((ViewGroup) getParent()).removeView(this);
                    rootGroup.addView(this, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
                }
            }
        }
    }

    /**
     * 设置非全屏
     */
    public void exitFullScreen() {
        _refreshOrientationEnable();
        if (mIsFullscreen) {
            if (mAttachActivity.getRequestedOrientation() != originOrientation) {
                mAttachActivity.setRequestedOrientation(originOrientation);
            }
//            mAttachActivity.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
            setNavigationBar(false);
            setLayoutParams(mRawParams);
            _setFullScreen(false);
            if (getParent() != mRootLayout) {
                ((ViewGroup) getParent()).removeView(this);
                mRootLayout.addView(this, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
            }

        }
    }

    /**
     * 全屏时隐藏虚拟键
     *
     * @param show
     */
    private int defaultSystemUI = 0;

    private void setNavigationBar(boolean show) {
        if (!show) {
            //View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN 设置这个属性会遮盖内容
            mAttachActivity.getWindow().getDecorView().setSystemUiVisibility(defaultSystemUI);//清除所有使用setSystemUiVisibility()方法设置的标记
        } else {
            defaultSystemUI = mAttachActivity.getWindow().getDecorView().getSystemUiVisibility();
            mAttachActivity.getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION // hide nav bar
                    | View.SYSTEM_UI_FLAG_FULLSCREEN // hide status bar
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    /**
     * 设置视频表现形式
     */

    public void setScaleType(String aspect) {
        if (mVideoView != null) {
            switch (aspect) {
                case "contain":
                    mVideoView.setAspectRatio(0);
                    break;
                case "fill":
                    mVideoView.setAspectRatio(IRenderView.AR_MATCH_PARENT);
                    break;
                case "cover":
                    mVideoView.setAspectRatio(IRenderView.AR_ASPECT_FILL_PARENT);
                    break;
            }
        }
    }

    String[] rates = new String[]{"0.5", "0.8", "1.0", "1.25", "1.5", "2.0"};

    public void playbackRate(String speed) {
        if (mVideoView != null) {
            int value = Arrays.binarySearch(rates, speed);
            if (value >= 0) {
                mVideoView.setSpeed(Float.parseFloat(speed));
            } else {
                mVideoView.setSpeed(1.0f);
            }
        }
    }

    /**============================ 触屏操作处理 ============================*/

    /**
     * 手势监听
     */
    private OnGestureListener mPlayerGestureListener = new SimpleOnGestureListener() {
        // 是否是按下的标识，默认为其他动作，true为按下标识，false为其他动作
        private boolean isDownTouch;
        // 是否声音控制,默认为亮度控制，true为声音控制，false为亮度控制
        private boolean isVolume;
        // 是否横向滑动，默认为纵向滑动，true为横向滑动，false为纵向滑动
        private boolean isLandscape;
        // 是否从弹幕编辑状态返回
        private boolean isRecoverFromDanmaku;

        @Override
        public boolean onDown(MotionEvent e) {
            isDownTouch = true;
            isRecoverFromDanmaku = recoverFromEditVideo();
            return super.onDown(e);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            if (!mIsForbidTouch && !mIsNeverPlay) {
                float mOldX = e1.getX(), mOldY = e1.getY();
                float deltaY = mOldY - e2.getY();
                float deltaX = mOldX - e2.getX();
                if (isDownTouch) {
                    // 判断左右或上下滑动
                    isLandscape = Math.abs(distanceX) >= Math.abs(distanceY);
                    // 判断是声音或亮度控制
                    isVolume = mOldX > getResources().getDisplayMetrics().widthPixels * 0.5f;
                    isDownTouch = false;
                }

                if (isLandscape) {
                    _onProgressSlide(-deltaX / mVideoView.getWidth());
                } else {
                    float percent = deltaY / mVideoView.getHeight();
                    if (isVolume) {
                        _onVolumeSlide(percent);
                    } else {
                        _onBrightnessSlide(percent);
                    }
                }
            }
            return super.onScroll(e1, e2, distanceX, distanceY);
        }

        @Override
        public boolean onSingleTapConfirmed(MotionEvent e) {
            // 弹幕编辑状态返回则不执行单击操作
            if (isRecoverFromDanmaku) {
                return true;
            }

            if (mIsForbidTouch) {
                return true;
            }
            _toggleControlBar();

            return true;
        }

        @Override
        public boolean onDoubleTap(MotionEvent e) {
            // 如果未进行播放或从弹幕编辑状态返回则不执行双击操作
            if (mIsNeverPlay || isRecoverFromDanmaku) {
                return true;
            }
            if (!mIsForbidTouch) {
                _refreshHideRunnable();
                _togglePlayStatus();
            }
            return true;
        }
    };

    /**
     * 隐藏视图Runnable
     */
    private Runnable mHideTouchViewRunnable = new Runnable() {
        @Override
        public void run() {
            _hideTouchView();
        }
    };

    /**
     * 触摸监听
     */
    private OnTouchListener mPlayerTouchListener = new OnTouchListener() {
        // 触摸模式：正常、无效、缩放旋转
        private static final int NORMAL = 1;
        private static final int INVALID_POINTER = 2;
        private static final int ZOOM_AND_ROTATE = 3;
        // 触摸模式
        private int mode = NORMAL;
        // 缩放的中点
        private PointF midPoint = new PointF(0, 0);
        // 旋转角度
        private float degree = 0;
        // 用来标识哪两个手指靠得最近，我的做法是取最近的两指中点和余下一指来控制旋转缩放
        private int fingerFlag = INVALID_VALUE;
        // 初始间距
        private float oldDist;
        // 缩放比例
        private float scale;

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (MotionEventCompat.getActionMasked(event)) {
                case MotionEvent.ACTION_DOWN:
                    mode = NORMAL;
                    mHandler.removeCallbacks(mHideBarRunnable);
                    break;

                case MotionEvent.ACTION_POINTER_DOWN:
                    if (event.getPointerCount() == 3 && mIsFullscreen) {
                        _hideTouchView();
                        // 进入三指旋转缩放模式，进行相关初始化
                        mode = ZOOM_AND_ROTATE;
                        MotionEventUtils.midPoint(midPoint, event);
                        fingerFlag = MotionEventUtils.calcFingerFlag(event);
                        degree = MotionEventUtils.rotation(event, fingerFlag);
                        oldDist = MotionEventUtils.calcSpacing(event, fingerFlag);
                        // 获取视频的 Matrix
                        mSaveMatrix = mVideoView.getVideoTransform();
                    } else {
                        mode = INVALID_POINTER;
                    }
                    break;

                case MotionEvent.ACTION_MOVE:
                    if (mode == ZOOM_AND_ROTATE) {
                        // 处理旋转
                        float newRotate = MotionEventUtils.rotation(event, fingerFlag);
                        mVideoView.setVideoRotation((int) (newRotate - degree));
                        // 处理缩放
                        mVideoMatrix.set(mSaveMatrix);
                        float newDist = MotionEventUtils.calcSpacing(event, fingerFlag);
                        scale = newDist / oldDist;
                        mVideoMatrix.postScale(scale, scale, midPoint.x, midPoint.y);
                        mVideoView.setVideoTransform(mVideoMatrix);
                    }
                    break;

                case MotionEvent.ACTION_POINTER_UP:
                    if (mode == ZOOM_AND_ROTATE) {
                        // 调整视频界面，让界面居中显示在屏幕
                        mIsNeedRecoverScreen = mVideoView.adjustVideoView(scale);
                        if (mIsNeedRecoverScreen && mIsShowBar) {
                            mTvRecoverScreen.setVisibility(VISIBLE);
                        }
                    }
                    mode = INVALID_POINTER;
                    break;
            }
            // 触屏手势处理
            if (mode == NORMAL) {
                if (mGestureDetector.onTouchEvent(event)) {
                    return true;
                }
                if (MotionEventCompat.getActionMasked(event) == MotionEvent.ACTION_UP) {
                    _endGesture();
                }
            }
            return true;
        }
    };

    /**
     * 更新进度条
     *
     * @return
     */
    private int _setProgress() {
        if (mVideoView == null || mIsSeeking) {
            return 0;
        }
        // 视频播放的当前进度
        int position = Math.max(mVideoView.getCurrentPosition(), mInterruptPosition);
        // 视频总的时长
        int duration = getDuration();
        if (duration > 0) {
            // 转换为 Seek 显示的进度值
            long pos = (long) MAX_VIDEO_SEEK * position / duration;
            mPlayerSeek.setProgress((int) pos);
        }
        // 获取缓冲的进度百分比，并显示在 Seek 的次进度
        int percent = mVideoView.getBufferPercentage();
        mPlayerSeek.setSecondaryProgress(percent * 10);
        // 更新播放时间
        mTvCurTime.setText(generateTime(position));
        mTvEndTime.setText(generateTime(duration));
        // 返回当前播放进度
        return position;
    }

    String timeUpdateF = "{currentTime:%f,duration:%f}";

    /**
     * 视频时间更新回调
     */
    private void progressCallBack() {
        if (mVideoView == null || mIsSeeking) {
            return;
        }
        if (mOnPlayerChangedListener != null) {
            // 视频播放的当前进度
            float position = Math.max(mVideoView.getCurrentPosition(), mInterruptPosition);
            float duration = getDuration();
            String msg = String.format(timeUpdateF, position / 1000, duration / 1000);
            mOnPlayerChangedListener.onChanged("timeupdate", msg);
        }
    }

    /**
     * 设置快进
     *
     * @param time
     */
    private void _setFastForward(String time) {
        if (mFlTouchLayout.getVisibility() == View.GONE) {
            mFlTouchLayout.setVisibility(View.VISIBLE);
        }
        if (mTvFastForward.getVisibility() == View.GONE) {
            mTvFastForward.setVisibility(View.VISIBLE);
        }
        mTvFastForward.setText(time);
    }

    /**
     * 隐藏触摸视图
     */
    private void _hideTouchView() {
        if (mFlTouchLayout.getVisibility() == View.VISIBLE) {
            mTvFastForward.setVisibility(View.GONE);
            mTvVolume.setVisibility(View.GONE);
            mTvBrightness.setVisibility(View.GONE);
            mFlTouchLayout.setVisibility(View.GONE);
        }
    }

    /**
     * 快进或者快退滑动改变进度，这里处理触摸滑动不是拉动 SeekBar
     *
     * @param percent 拖拽百分比
     */
    private void _onProgressSlide(float percent) {
        if (isRtmpUri) {
            return;
        }
        if (!isProgressGesture) {
            return;
        }
        int position = mVideoView.getCurrentPosition();
        long duration = getDuration();
        // 单次拖拽最大时间差为100秒或播放时长的1/2
        long deltaMax = Math.min(100 * 1000, duration / 2);
        // 计算滑动时间
        long delta = (long) (deltaMax * percent);
        // 目标位置
        mTargetPosition = delta + position;
        if (mTargetPosition > duration) {
            mTargetPosition = duration;
        } else if (mTargetPosition <= 0) {
            mTargetPosition = 0;
        }
        int deltaTime = (int) ((mTargetPosition - position) / 1000);
        String desc;
        // 对比当前位置来显示快进或后退
        if (mTargetPosition > position) {
            desc = generateTime(mTargetPosition) + "/" + generateTime(duration) /*+ "\n" + "+" + deltaTime + "秒"*/;
        } else {
            desc = generateTime(mTargetPosition) + "/" + generateTime(duration) /*+ "\n" + deltaTime + "秒"*/;
        }
        _setFastForward(desc);
    }

    /**
     * 设置声音控制显示
     *
     * @param volume
     */
    private void _setVolumeInfo(int volume) {
        if (mFlTouchLayout.getVisibility() == View.GONE) {
            mFlTouchLayout.setVisibility(View.VISIBLE);
        }
        if (mTvVolume.getVisibility() == View.GONE) {
            mTvVolume.setVisibility(View.VISIBLE);
        }
        mTvVolume.setText((volume * 100 / mMaxVolume) + "%");
    }

    /**
     *
     */
    int duration = -1;

    public void setDuration(int duration) {
        if (mIsNeverPlay && !isPlaying()) {
            if (duration > 0) {
                this.duration = duration;
            } else {
                this.duration = -1;
            }
        }
    }

    public int getDuration() {
        if (duration <= -1) {
            duration = mVideoView.getDuration();
        }
        return duration;
    }

    /**
     * 滑动改变声音大小
     *
     * @param percent
     */
    private void _onVolumeSlide(float percent) {
        if (isMutePlayer) {
            return;
        }
        if (!mIsFullscreen && !isPageGesture) {
            return;
        }
        if (mCurVolume == INVALID_VALUE) {
            mCurVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (mCurVolume < 0) {
                mCurVolume = 0;
            }
        }
        int index = (int) (percent * mMaxVolume) + mCurVolume;
        if (index > mMaxVolume) {
            index = mMaxVolume;
        } else if (index < 0) {
            index = 0;
        }
        // 变更声音
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, index, 0);
        // 变更进度条
        _setVolumeInfo(index);
    }


    public void isUseMediaCodec(boolean isUse) {
        if (mVideoView != null) {
            mVideoView.setmIsUsingMediaCodec(isUse);
        }
    }

    /**
     * 静音播放
     */
    private boolean isMutePlayer = false;

    public void setMutePlayer(boolean isMute) {
        this.isMutePlayer = isMute;
        mIVMute.setSelected(this.isMutePlayer);
        if (isMute) {
            mAudioManager.abandonAudioFocus(null);
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, true);
        } else {
            mAudioManager.requestAudioFocus(null, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
            mAudioManager.setStreamMute(AudioManager.STREAM_MUSIC, false);
        }
    }


    /**
     * 递增或递减音量，量度按最大音量的 1/15
     *
     * @param isIncrease 递增或递减
     */
    private void _setVolume(boolean isIncrease) {
        if (isMutePlayer) {
            return;
        }
        int curVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        if (isIncrease) {
            curVolume += mMaxVolume / 15;
        } else {
            curVolume -= mMaxVolume / 15;
        }
        if (curVolume > mMaxVolume) {
            curVolume = mMaxVolume;
        } else if (curVolume < 0) {
            curVolume = 0;
        }
        // 变更声音
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, curVolume, 0);
        // 变更进度条
        _setVolumeInfo(curVolume);
        mHandler.removeCallbacks(mHideTouchViewRunnable);
        mHandler.postDelayed(mHideTouchViewRunnable, 1000);
    }

    /**
     * 设置亮度控制显示
     *
     * @param brightness
     */
    private void _setBrightnessInfo(float brightness) {
        if (mFlTouchLayout.getVisibility() == View.GONE) {
            mFlTouchLayout.setVisibility(View.VISIBLE);
        }
        if (mTvBrightness.getVisibility() == View.GONE) {
            mTvBrightness.setVisibility(View.VISIBLE);
        }
        mTvBrightness.setText(Math.ceil(brightness * 100) + "%");
    }

    /**
     * 滑动改变亮度大小
     *
     * @param percent
     */
    private void _onBrightnessSlide(float percent) {
        if (!mIsFullscreen && !isPageGesture) {
            return;
        }
        if (mCurBrightness < 0) {
            mCurBrightness = mAttachActivity.getWindow().getAttributes().screenBrightness;
            if (mCurBrightness < 0.0f) {
                mCurBrightness = 0.5f;
            } else if (mCurBrightness < 0.01f) {
                mCurBrightness = 0.01f;
            }
        }
        WindowManager.LayoutParams attributes = mAttachActivity.getWindow().getAttributes();
        attributes.screenBrightness = mCurBrightness + percent;
        if (attributes.screenBrightness > 1.0f) {
            attributes.screenBrightness = 1.0f;
        } else if (attributes.screenBrightness < 0.01f) {
            attributes.screenBrightness = 0.01f;
        }
        _setBrightnessInfo(attributes.screenBrightness);
        mAttachActivity.getWindow().setAttributes(attributes);
    }

    /**
     * 手势结束调用
     */
    private void _endGesture() {
        if (mTargetPosition >= 0 && mTargetPosition != mVideoView.getCurrentPosition()) {
            // 更新视频播放进度
            if (getDuration() > 0) {
                seekTo((int) mTargetPosition);
                mPlayerSeek.setProgress((int) (mTargetPosition * MAX_VIDEO_SEEK / getDuration()));
                mTargetPosition = INVALID_VALUE;
            }
        }
        // 隐藏触摸操作显示图像
        _hideTouchView();
        _refreshHideRunnable();
        mCurVolume = INVALID_VALUE;
        mCurBrightness = INVALID_VALUE;
    }

    /**
     * ============================ 播放状态控制 ============================
     */

    // 这个用来控制弹幕启动和视频同步
    private boolean mIsRenderingStart = false;
    // 缓冲开始，这个用来控制弹幕启动和视频同步
    private boolean mIsBufferingStart = false;

    // 视频播放状态监听
    private OnInfoListener mInfoListener = new OnInfoListener() {
        @Override
        public boolean onInfo(IMediaPlayer iMediaPlayer, int status, int extra) {
            _switchStatus(status);
            if (mOutsideInfoListener != null) {
                mOutsideInfoListener.onInfo(iMediaPlayer, status, extra);
            }
            return true;
        }
    };

    /**
     * 视频播放状态处理
     *
     * @param status
     */
    private void _switchStatus(int status) {
        Log.i("IjkPlayerView", "status " + status);
        switch (status) {
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                mIsBufferingStart = true;
                _pauseDanmaku();
                if (!mIsNeverPlay) {
                    mLoadingView.setVisibility(View.VISIBLE);
                }
                mHandler.removeMessages(MSG_TRY_RELOAD);
            case MediaPlayerParams.STATE_PREPARING:
                break;

            case MediaPlayerParams.STATE_PREPARED:
                //初始化弹幕，获得时间
//                _loadDanmaku();
                mIsReady = true;
                break;

            case IMediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                mIsRenderingStart = true;
            case IMediaPlayer.MEDIA_INFO_BUFFERING_END:
                mIsBufferingStart = false;
                mLoadingView.setVisibility(View.GONE);
                mPlayerThumb.setVisibility(View.GONE);
                // 更新进度
                if (mLlBottomBar.getVisibility() == VISIBLE && !mIsShowBar)
                    mIsShowBar = true;
                mHandler.removeMessages(MSG_UPDATE_SEEK);
                mHandler.sendEmptyMessage(MSG_UPDATE_SEEK);
                if (mVideoView.isPlaying() && mIsNetConnected) {
                    mInterruptPosition = 0;
                    _resumeDanmaku();   // 开启弹幕
                    if (!mIvPlay.isSelected()) {
                        // 这里处理断网重连后不会播放情况
                        mVideoView.start();
                        mIvPlay.setSelected(true);
                        mIvPlayCenter.setSelected(true);
                    }
                }
                break;

            case MediaPlayerParams.STATE_PLAYING:
                mHandler.removeMessages(MSG_TRY_RELOAD);
                if (mIsRenderingStart && !mIsBufferingStart && mVideoView.getCurrentPosition() > 0) {
                    _resumeDanmaku();   // 开启弹幕
                }
                break;
            case MediaPlayerParams.STATE_ERROR:
                mInterruptPosition = Math.max(mVideoView.getInterruptPosition(), mInterruptPosition);
                pause();
                if (mVideoView.getDuration() == -1 && !mIsReady) {
                    mLoadingView.setVisibility(View.GONE);
                    mPlayerThumb.setVisibility(View.GONE);
                    mIvPlayCircle.setVisibility(GONE);
//                    mFlReload.setVisibility(VISIBLE);
                } else {
                    mLoadingView.setVisibility(VISIBLE);
                    mHandler.sendEmptyMessage(MSG_TRY_RELOAD);
                }
                break;

            case MediaPlayerParams.STATE_COMPLETED:
                pause();
                if (mVideoView.getDuration() == -1 ||
                        (mVideoView.getInterruptPosition() + INTERVAL_TIME < mVideoView.getDuration())) {
                    mInterruptPosition = Math.max(mVideoView.getInterruptPosition(), mInterruptPosition);
//                    Toast.makeText(mAttachActivity, "网络异常", Toast.LENGTH_SHORT).show();
                    if (null != mOnPlayerChangedListener) {
                        mOnPlayerChangedListener.onChanged("error", "network error");
//                        mFlReload.setVisibility(VISIBLE);
                    }
                } else {
                    mIsPlayComplete = true;
                    if (mCompletionListener != null) {
                        mCompletionListener.onCompletion(mVideoView.getMediaPlayer());
                    }
                }
                break;
        }
    }

    /**============================ Listener ============================*/

    /**
     * Register a callback to be invoked when the media file
     * is loaded and ready to go.
     *
     * @param l The callback that will be run
     */
    public void setOnPreparedListener(IMediaPlayer.OnPreparedListener l) {
        mVideoView.setOnPreparedListener(l);
    }

    /**
     * Register a callback to be invoked when the end of a media file
     * has been reached during playback.
     *
     * @param l The callback that will be run
     */
    public void setOnCompletionListener(IMediaPlayer.OnCompletionListener l) {
        mCompletionListener = l;
//        mVideoView.setOnCompletionListener(l);
    }

    /**
     * Register a callback to be invoked when an error occurs
     * during playback or setup.  If no listener is specified,
     * or if the listener returned false, VideoView will inform
     * the user of any errors.
     *
     * @param l The callback that will be run
     */
    public void setOnErrorListener(IMediaPlayer.OnErrorListener l) {
        mVideoView.setOnErrorListener(l);
    }

    /**
     * Register a callback to be invoked when an informational event
     * occurs during playback or setup.
     *
     * @param l The callback that will be run
     */
    public void setOnInfoListener(OnInfoListener l) {
        mOutsideInfoListener = l;
    }

    /**
     * 设置弹幕监听器
     *
     * @param danmakuListener
     */
    public IjkPlayerView setDanmakuListener(OnDanmakuListener danmakuListener) {
        mDanmakuListener = danmakuListener;
        return this;
    }

    private OnPlayerChangedListener mOnPlayerChangedListener;

    public void setOnPlayerChangedListener(OnPlayerChangedListener listener) {
        this.mOnPlayerChangedListener = listener;
    }

    private IMediaPlayer.OnBufferingUpdateListener bufferingUpdateListener;

    public void setOnBufferingUpdateListener(IMediaPlayer.OnBufferingUpdateListener bufferingUpdateListener) {
        this.bufferingUpdateListener = bufferingUpdateListener;
    }

    private IMediaPlayer.OnBufferingUpdateListener onBufferingUpdateListener = new IMediaPlayer.OnBufferingUpdateListener() {
        @Override
        public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int i) {
            if (bufferingUpdateListener != null)
                bufferingUpdateListener.onBufferingUpdate(iMediaPlayer, i);
        }
    };

    /**
     * ============================ 播放清晰度 ============================
     */


    // 保存Video Url
    private SparseArray<String> mVideoSource = new SparseArray<String>();


    /**
     * ============================ 跳转提示 ============================
     */


    /**
     * 返回当前进度
     *
     * @return
     */
    public int getCurPosition() {
        return mVideoView.getCurrentPosition();
    }

    /**
     * ============================ 弹幕 ============================
     */

    /**
     * 视频编辑状态：正常未编辑状态、在播放时编辑、暂停时编辑
     */
    private static final int NORMAL_STATUS = 501;
    private static final int INTERRUPT_WHEN_PLAY = 502;
    private static final int INTERRUPT_WHEN_PAUSE = 503;

    private int mVideoStatus = NORMAL_STATUS;

    // 弹幕格式：B站、A站和自定义
    private static final int DANMAKU_TAG_BILI = 701;
    private static final int DANMAKU_TAG_ACFUN = 702;
    private static final int DANMAKU_TAG_CUSTOM = 703;

    public void setDirection(int orientation) {
        this.mOrientation = orientation;
    }

    @Retention(RetentionPolicy.SOURCE)
    @Target({ElementType.FIELD, ElementType.PARAMETER})
    @IntDef({DANMAKU_TAG_BILI, DANMAKU_TAG_ACFUN, DANMAKU_TAG_CUSTOM})
    public @interface DanmakuTag {
    }

    private
    @DanmakuTag
    int mDanmakuTag = DANMAKU_TAG_BILI;

    // 弹幕开源控件
    private IDanmakuView mDanmakuView;
    // 弹幕显示/隐藏按钮
    private TextView mIvDanmakuControl;
    // 弹幕编辑布局
//    private View mEditDanmakuLayout;
    // 取消弹幕发送
//    private ImageView mIvCancelSend;
    // 发送弹幕
//    private ImageView mIvDoSend;

    // 弹幕基础设置布局
//    private View mDanmakuOptionsBasic;
//    // 弹幕字体大小选项卡
//    private RadioGroup mDanmakuTextSizeOptions;
//    // 弹幕类型选项卡
//    private RadioGroup mDanmakuTypeOptions;
//    // 弹幕当前颜色
//    private RadioButton mDanmakuCurColor;
//    // 开关弹幕颜色选项卡
//    private ImageView mDanmakuMoreColorIcon;
//    // 弹幕更多颜色设置布局
//    private View mDanmakuMoreOptions;
//    // 弹幕颜色选项卡
//    private RadioGroup mDanmakuColorOptions;

    // 弹幕控制相关
    private DanmakuContext mDanmakuContext;
    // 弹幕解析器
    private BaseDanmakuParser mDanmakuParser;
    // 弹幕加载器
    private ILoader mDanmakuLoader;
    // 弹幕数据转换器
    private BaseDanmakuConverter mDanmakuConverter;
    // 弹幕监听器
    private OnDanmakuListener mDanmakuListener;
    // 是否使能弹幕
    private boolean mIsEnableDanmaku = false;
    // 弹幕颜色
    private int mDanmakuTextColor = Color.WHITE;
    // 弹幕字体大小
    private float mDanmakuTextSize = INVALID_VALUE;
    // 弹幕类型
    private int mDanmakuType = TYPE_SCROLL_RL;
    // 弹幕基础设置布局的宽度
    private int mBasicOptionsWidth = INVALID_VALUE;
    // 弹幕更多颜色设置布局宽度
    private int mMoreOptionsWidth = INVALID_VALUE;
    // 弹幕要跳转的目标位置，等视频播放再跳转，不然老出现只有弹幕在动的情况
    private long mDanmakuTargetPosition = INVALID_VALUE;
    //弹幕数据
    private String mDanmuList = "";

    public void setmDanmuList(String mDanmuList) {
        this.mDanmuList = mDanmuList;
    }

    /**
     * 弹幕初始化
     */
    private void _initDanmaku() {
        // 弹幕控制
        mDanmakuView = findViewById(VideoR.VIDEO_IJK_ID_SV_DANMAKU);
//        int navigationBarHeight = NavUtils.getNavigationBarHeight(mAttachActivity);
//        if (navigationBarHeight > 0) {
        // 对于有虚拟键的设备需要将弹幕编辑布局右偏移防止被覆盖
//            mEditDanmakuLayout.setPadding(0, 0, navigationBarHeight, 0);
//        }
        mIvDanmakuControl.setOnClickListener(this);
        // 这些为弹幕配置处理
        int oneBtnWidth = getResources().getDimensionPixelOffset(VideoR.VIDEO_IJK_DIMEN_DANMAKU_INPUT_BTN_SIZE);
        // 布局宽度为每个选项卡宽度 * 12 个，有12种可选颜色
        mMoreOptionsWidth = oneBtnWidth * 12;
    }

    /**
     * 装载弹幕，在视频按了播放键才装载
     */
    private void _loadDanmaku() {
        if (mIsEnableDanmaku) {
            // 设置弹幕
            mDanmakuContext = DanmakuContext.create();

            //同步弹幕和video，貌似没法保持同步，可能我用的有问题，先注释掉- -
//            mDanmakuContext.setDanmakuSync(new VideoDanmakuSync(this));
            if (mDanmakuParser == null) {
                mDanmakuParser = new BaseDanmakuParser() {
                    @Override
                    protected Danmakus parse() {
                        return new Danmakus();
                    }
                };
            }
            try {
                InputStream is = new ByteArrayInputStream(mDanmuList.getBytes("utf-8"));
                setDanmakuSource(is);
            } catch (UnsupportedEncodingException e) {
                return;
            }
            mDanmakuView.setCallback(new DrawHandler.Callback() {
                @Override
                public void prepared() {
                    // 这里处理下有时调用 _resumeDanmaku() 时弹幕还没 prepared 的情况
                    if (mVideoView.isPlaying() && !mIsBufferingStart) {
                        mDanmakuView.start();
                    }
                }

                @Override
                public void updateTimer(DanmakuTimer timer) {
                }

                @Override
                public void danmakuShown(BaseDanmaku danmaku) {
                }

                @Override
                public void drawingFinished() {
                }
            });
            mDanmakuView.enableDanmakuDrawingCache(true);
            mDanmakuView.prepare(mDanmakuParser, mDanmakuContext);
        }
    }

    /**
     * 使能弹幕功能
     *
     * @return
     */
    public IjkPlayerView enableDanmaku() {
        mIsEnableDanmaku = true;
        _initDanmaku();
        return this;
    }

    /**
     * 使能弹幕功能
     *
     * @param isEnable
     * @return
     */
    public void enableDanmaku(boolean isEnable) {
        mIsEnableDanmaku = isEnable;
        if (mIsEnableDanmaku) {
            _initDanmaku();
        } else {
            mIvDanmakuControl.setVisibility(GONE);
        }
    }

    /**
     * 弹幕按钮是否可见
     *
     * @param isEnable
     * @return
     */
    public void enableDanmuBtn(boolean isEnable) {
        if (mIsEnableDanmaku) {
            if (isEnable) {
                mIvDanmakuControl.setVisibility(VISIBLE);
            } else {
                mIvDanmakuControl.setVisibility(GONE);
            }
        } else {
            mIvDanmakuControl.setVisibility(GONE);
        }
    }

    /**
     * 设置弹幕资源，默认资源格式需满足 bilibili 的弹幕文件格式，
     * 配合{@link #setDanmakuCustomParser}来进行自定义弹幕解析方式，{@link #setDanmakuCustomParser}必须先调用
     *
     * @param stream 弹幕资源
     * @return
     */
    public IjkPlayerView setDanmakuSource(InputStream stream) {
        if (stream == null) {
            return this;
        }
        if (!mIsEnableDanmaku) {
            throw new RuntimeException("Danmaku is disable, use enableDanmaku() first");
        }
        if (mDanmakuLoader == null) {
            mDanmakuLoader = DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_ACFUN);
        }
        try {
            mDanmakuLoader.load(stream);
        } catch (IllegalDataException e) {
            e.printStackTrace();
        }
        IDataSource<?> dataSource = mDanmakuLoader.getDataSource();
        mDanmakuParser = new StandardDanmaKuParser();
        mDanmakuParser.load(dataSource);
        return this;
    }

    /**
     * 设置弹幕资源，默认资源格式需满足 bilibili 的弹幕文件格式，
     * 配合{@link #setDanmakuCustomParser}来进行自定义弹幕解析方式，{@link #setDanmakuCustomParser}必须先调用
     *
     * @param uri 弹幕资源
     * @return
     */
    public IjkPlayerView setDanmakuSource(String uri) {
        if (TextUtils.isEmpty(uri)) {
            return this;
        }
        if (!mIsEnableDanmaku) {
            throw new RuntimeException("Danmaku is disable, use enableDanmaku() first");
        }
        if (mDanmakuLoader == null) {
            mDanmakuLoader = DanmakuLoaderFactory.create(DanmakuLoaderFactory.TAG_BILI);
        }
        try {
            mDanmakuLoader.load(uri);
        } catch (IllegalDataException e) {
            e.printStackTrace();
        }
        IDataSource<?> dataSource = mDanmakuLoader.getDataSource();
        mDanmakuParser = new BiliDanmukuParser();
        mDanmakuParser.load(dataSource);
        return this;
    }

    /**
     * 自定义弹幕解析器，配合{@link #setDanmakuSource}使用，先于{@link #setDanmakuSource}调用
     *
     * @param parser    解析器
     * @param loader    加载器
     * @param converter 转换器
     * @return
     */
    public IjkPlayerView setDanmakuCustomParser(BaseDanmakuParser parser, ILoader loader, BaseDanmakuConverter converter) {
        mDanmakuParser = parser;
        mDanmakuLoader = loader;
        mDanmakuConverter = converter;
        return this;
    }

    /**
     * 显示/隐藏弹幕
     *
     * @param isShow 是否显示
     * @return
     */
    public IjkPlayerView showOrHideDanmaku(boolean isShow) {
        if (isShow) {
            mIvDanmakuControl.setSelected(false);
            mDanmakuView.show();
        } else {
            mIvDanmakuControl.setSelected(true);
            mDanmakuView.hide();
        }
        return this;
    }

    /**
     * 发射弹幕
     *
     * @param text   内容
     * @param isLive 是否直播
     * @return 弹幕数据
     */
    public void sendDanmaku(JSONObject text, boolean isLive) {
        if (!mIsEnableDanmaku) {
//            throw new RuntimeException("Danmaku is disable, use enableDanmaku() first");
            return;
        }

        if (TextUtils.isEmpty(text.optString("text"))) {
//            Toast.makeText(mAttachActivity, "内容为空", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!mDanmakuView.isPrepared()) {
//            Toast.makeText(mAttachActivity, "弹幕还没准备好", Toast.LENGTH_SHORT).show();
            return;
        }
//            mDanmakuType = text.optInt("type",TYPE_SCROLL_RL);
        BaseDanmaku danmaku = mDanmakuContext.mDanmakuFactory.createDanmaku(mDanmakuType);
        if (danmaku == null || mDanmakuView == null) {
            return;
        }
        if (mDanmakuTextSize == INVALID_VALUE) {
            mDanmakuTextSize = 25f * (mDanmakuParser.getDisplayer().getDensity() - 0.6f);
        }
        danmaku.text = text.optString("text", "....");
        danmaku.padding = 5;
        danmaku.isLive = isLive;
        danmaku.priority = 0;  // 可能会被各种过滤器过滤并隐藏显示
        danmaku.textSize = mDanmakuTextSize;
        danmaku.textColor = Color.parseColor(text.optString("color", "#ffffff"));
//        danmaku.underlineColor = Color.GREEN;
        danmaku.setTime(mDanmakuView.getCurrentTime() + 500);
        mDanmakuView.addDanmaku(danmaku);

        if (mDanmakuListener != null) {
            if (mDanmakuConverter != null) {
                mDanmakuListener.onDataObtain(mDanmakuConverter.convertDanmaku(danmaku));
            } else {
                mDanmakuListener.onDataObtain(danmaku);
            }
        }
    }

    /**
     * 编辑操作前调用，会控制视频的播放状态，如在编辑弹幕前调用，配合{@link #recoverFromEditVideo()}使用
     */
    public void editVideo() {
        if (mVideoView.isPlaying()) {
            pause();
            mVideoStatus = INTERRUPT_WHEN_PLAY;
        } else {
            mVideoStatus = INTERRUPT_WHEN_PAUSE;
        }
        _hideAllView(false);
    }

    /**
     * 从编辑状态返回，如取消编辑或发射弹幕后配合{@link #editVideo()}调用
     *
     * @return 是否从编辑状态回退
     */
    public boolean recoverFromEditVideo() {
        if (mVideoStatus == NORMAL_STATUS) {
            return false;
        }
        if (mIsFullscreen) {
            _recoverScreen();
        }
        if (mVideoStatus == INTERRUPT_WHEN_PLAY) {
            start();
        }
        mVideoStatus = NORMAL_STATUS;
        return true;
    }

    /**
     * 清除弹幕
     */
    public void clearDanma() {
        if (mDanmakuView != null && mDanmakuView.isPrepared()) {
            mDanmakuView.clearDanmakusOnScreen();
        }
    }

    /**
     * 激活弹幕
     */
    private void _resumeDanmaku() {
        if (mDanmakuView != null && mDanmakuView.isPrepared() && mDanmakuView.isPaused()) {
            if (mDanmakuTargetPosition != INVALID_VALUE) {
                mDanmakuView.seekTo(mDanmakuTargetPosition);
                mDanmakuTargetPosition = INVALID_VALUE;
            } else {
                mDanmakuView.resume();
            }
        }
    }

    /**
     * 暂停弹幕
     */
    private void _pauseDanmaku() {
        if (mDanmakuView != null && mDanmakuView.isPrepared()) {
            mDanmakuTargetPosition = INVALID_VALUE;
            mDanmakuView.pause();
        }
    }

    /**
     * 切换弹幕的显示/隐藏
     */
    private void _toggleDanmakuShow() {
        if (mIvDanmakuControl.isSelected()) {
            showOrHideDanmaku(true);
        } else {
            showOrHideDanmaku(false);
        }
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mOnPlayerChangedListener.onChanged("onConfigurationChanged", null);
    }

    /**
     * 从弹幕编辑状态复原界面
     */
    private void _recoverScreen() {
        // 清除焦点
//        mEditDanmakuLayout.clearFocus();
//        mEditDanmakuLayout.setVisibility(GONE);
//        // 关闭软键盘
//        SoftInputUtils.closeSoftInput(mAttachActivity);
//        // 重新设置全屏界面UI标志位
//        _setUiLayoutFullscreen();
//        if (mDanmakuColorOptions.getWidth() != 0) {
//            _toggleMoreColorOptions();
//        }
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        if (!isFullscreen() && mIsForbidTouch && mIvPlayCircle.getVisibility() != VISIBLE ){ // 非全屏且control为false的时候
            return true;
        }
        return super.onInterceptTouchEvent(ev);
    }

    /**
     * 动画切换弹幕颜色选项卡显示
     */
    private void _toggleMoreColorOptions() {
//        if (mBasicOptionsWidth == INVALID_VALUE) {
//            mBasicOptionsWidth = mDanmakuOptionsBasic.getWidth();
//        }
//        if (mDanmakuColorOptions.getWidth() == 0) {
//            AnimHelper.doClipViewWidth(mDanmakuOptionsBasic, mBasicOptionsWidth, 0, 300);
//            AnimHelper.doClipViewWidth(mDanmakuColorOptions, 0, mMoreOptionsWidth, 300);
//            ViewCompat.animate(mDanmakuMoreColorIcon).rotation(180).setDuration(150).setStartDelay(250).start();
//        } else {
//            AnimHelper.doClipViewWidth(mDanmakuOptionsBasic, 0, mBasicOptionsWidth, 300);
//            AnimHelper.doClipViewWidth(mDanmakuColorOptions, mMoreOptionsWidth, 0, 300);
//            ViewCompat.animate(mDanmakuMoreColorIcon).rotation(0).setDuration(150).setStartDelay(250).start();
//        }
    }

    /**
     * ============================ 电量、时间、锁屏、截屏 ============================
     */

    // 电量显示
//    private ProgressBar mPbBatteryLevel;
    // 系统时间显示
//    private TextView mTvSystemTime;
    // 电量变化广播接收器
//    private BatteryBroadcastReceiver mBatteryReceiver;
    // 锁屏状态广播接收器
    private ScreenBroadcastReceiver mScreenReceiver;
    // 网络变化广播
    private NetBroadcastReceiver mNetReceiver;
    // 判断是否出现锁屏,有则需要重新设置渲染器，不然视频会没有动画只有声音
    private boolean mIsScreenLocked = false;

    /**
     * 初始化电量、锁屏、时间处理
     */
    private void _initReceiver() {
//        mPbBatteryLevel = (ProgressBar) findViewById(VideoR.VIDEO_IJK_ID_DP_BATTERY);
//        mTvSystemTime = (TextView) findViewById(VideoR.VIDEO_IJK_ID_TV_SYSTEM_TIME);
//        mTvSystemTime.setText(StringUtils.getCurFormatTime());
//        mBatteryReceiver = new BatteryBroadcastReceiver();
        mScreenReceiver = new ScreenBroadcastReceiver();
        mNetReceiver = new NetBroadcastReceiver();
        //注册接受广播
//        mAttachActivity.registerReceiver(mBatteryReceiver, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        mAttachActivity.registerReceiver(mScreenReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
        mAttachActivity.registerReceiver(mNetReceiver, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }


    /**
     * 接受电量改变广播
     */
//    class BatteryBroadcastReceiver extends BroadcastReceiver {
//
//        // 低电量临界值
//        private static final int BATTERY_LOW_LEVEL = 15;
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            // 接收电量变化信息
//            if (intent.getAction().equals(Intent.ACTION_BATTERY_CHANGED)) {
//                int level = intent.getIntExtra("level", 0);
//                int scale = intent.getIntExtra("scale", 100);
//                // 电量百分比
//                int curPower = level * 100 / scale;
//                int status = intent.getIntExtra("status", BatteryManager.BATTERY_HEALTH_UNKNOWN);
//                // SecondaryProgress 用来展示低电量，Progress 用来展示正常电量
//                if (status == BatteryManager.BATTERY_STATUS_CHARGING) {
//                    mPbBatteryLevel.setSecondaryProgress(0);
//                    mPbBatteryLevel.setProgress(curPower);
//                    mPbBatteryLevel.setBackgroundResource(VideoR.VIDEO_IJK_DRAWABLE_IC_BATTERY_CHARGING);
//                } else if (curPower < BATTERY_LOW_LEVEL) {
//                    mPbBatteryLevel.setProgress(0);
//                    mPbBatteryLevel.setSecondaryProgress(curPower);
//                    mPbBatteryLevel.setBackgroundResource(VideoR.VIDEO_IJK_DRAWABLE_IC_BATTERY_RED);
//                } else {
//                    mPbBatteryLevel.setSecondaryProgress(0);
//                    mPbBatteryLevel.setProgress(curPower);
//                    mPbBatteryLevel.setBackgroundResource(VideoR.VIDEO_IJK_DRAWABLE_IC_BATTERY);
//                }
//            }
//        }
//    }

    /**
     * 锁屏状态广播接收者
     */
    private class ScreenBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                mIsScreenLocked = true;
            }
        }
    }

    private boolean mIsNetConnected;

    public class NetBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // 如果相等的话就说明网络状态发生了变化
            if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
                mIsNetConnected = NetWorkUtils.isNetworkAvailable(mAttachActivity);
            }
        }
    }
}
