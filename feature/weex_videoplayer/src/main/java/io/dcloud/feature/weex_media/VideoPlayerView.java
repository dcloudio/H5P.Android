package io.dcloud.feature.weex_media;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.nostra13.dcloudimageloader.core.ImageLoaderL;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.adapter.URIAdapter;
import com.taobao.weex.bridge.WXBridgeManager;
import com.taobao.weex.ui.action.GraphicSize;
import com.taobao.weex.ui.component.WXComponent;
import com.taobao.weex.ui.component.WXVContainer;

import java.util.HashMap;
import java.util.Map;

import io.dcloud.common.util.PdrUtil;
import io.dcloud.media.weex.weex_video.ijkplayer.OnPlayerChangedListener;
import io.dcloud.media.weex.weex_video.ijkplayer.media.AssetsDataSourceProvider;
import io.dcloud.media.weex.weex_video.ijkplayer.media.IjkPlayerView;
import io.dcloud.media.weex.weex_video.ijkplayer.media.MediaPlayerParams;
import tv.danmaku.ijk.media.player.IMediaPlayer;

public class VideoPlayerView extends FrameLayout implements IMediaPlayer.OnInfoListener, OnPlayerChangedListener, IMediaPlayer.OnBufferingUpdateListener {

    /**
     * 视频属性
     */
    private String mSrc = "";
    private String willBeSetSrc = "";
    private boolean autoplay = false;
    private boolean loop = false;
    private String poster = "";
    private float duration = -1;
    private float initialTime = 0;
    private boolean isFinishLayout = false;
    private int seek = 0;

    private IjkPlayerView mPlayerView;

    private WXVContainer component;

    private Context mContext;
    // 子component放置view
    private FrameLayout subViewContainer;

    public VideoPlayerView(Context context, WXVContainer component) {
        super(context);
        this.mContext = context;
        this.component = component;
        subViewContainer = new FrameLayout(context);
        addView(subViewContainer,new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        if (component.getInstance().isFrameViewShow()) {
            createVideoView();
            subViewContainer.bringToFront();
        }else {
            component.getInstance().addFrameViewEventListener(new WXSDKInstance.FrameViewEventListener() {
                @Override
                public void onShowAnimationEnd() {
                    createVideoView();
                    subViewContainer.bringToFront();
                    {
                        setEnableDanmu(VideoPlayerView.this.component.getAttrs().containsKey("enableDanmu") && Boolean.parseBoolean(VideoPlayerView.this.component.getAttrs().get("enableDanmu").toString()));
                        setDanmuBtn(VideoPlayerView.this.component.getAttrs().containsKey("danmuBtn") && Boolean.parseBoolean(VideoPlayerView.this.component.getAttrs().get("danmuBtn").toString()));
                    }
                    VideoPlayerView.this.component.updateProperties(VideoPlayerView.this.component.getAttrs());
                }
            });
        }
    }

    private boolean isCreate = false;

    public void createVideoView() {
        if (isCreate) return;
        isCreate = true;
        mPlayerView = new IjkPlayerView(mContext);
        addView(mPlayerView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mPlayerView.init().setPlayerRootView(this);
        mPlayerView.setOnInfoListener(this);
        mPlayerView.setOnPlayerChangedListener(this);
        mPlayerView.setOnBufferingUpdateListener(this);// 新加

        if (fullScreenSize == null) {
            View view = ((Activity) component.getInstance().getContext()).getWindow().getDecorView().findViewById(android.R.id.content);
            fullScreenSize = new GraphicSize(view.getWidth(), view.getHeight());
        }
    }

    public ViewGroup getPlayerView() {
        return subViewContainer;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (PdrUtil.isEmpty(component.getStyles().getBackgroundColor()))
            setBackgroundColor(Color.BLACK);
    }

    public void onLayoutFinished() {
        if (mPlayerView == null) return;
        AssetsDataSourceProvider fd = null;
        if (!PdrUtil.isNetPath(willBeSetSrc)) {
            willBeSetSrc = component.getInstance().rewriteUri(Uri.parse(willBeSetSrc), URIAdapter.VIDEO).getPath();
            if (willBeSetSrc != null && !PdrUtil.isDeviceRootDir(willBeSetSrc)) {
                try {
                    if (willBeSetSrc.startsWith("/"))
                        willBeSetSrc = willBeSetSrc.replace("/", "");
                    fd = new AssetsDataSourceProvider(component.getContext().getAssets().openFd(willBeSetSrc));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        if (TextUtils.isEmpty(this.mSrc)) {
            if (fd == null)
                mPlayerView.setVideoPath(willBeSetSrc);
            else
                mPlayerView.setVideoFileDescriptor(fd);
            mPlayerView.seekTo(seek * 1000);
            mPlayerView.clearDanma();
        } else if (!this.mSrc.equalsIgnoreCase(willBeSetSrc)) {
            if (fd == null)
                mPlayerView.switchVideoPath(willBeSetSrc);
            else
                mPlayerView.switchVideoFileDescriptor(fd);
            mPlayerView.seekTo(seek * 1000);
            mPlayerView.clearDanma();
        }
        this.mSrc = willBeSetSrc;
        mPlayerView.setDuration((int) this.duration);
        // 切换src的时候，弹幕也要重新初始化
        mPlayerView.enableDanmaku(isEnableDanmu);
        mPlayerView.enableDanmuBtn(isEnableDanmuBtn);
        isFinishLayout = true;
        if (autoplay)
            play();
    }

    public void requestFullScreen(int oritation) {
        if (PdrUtil.isEmpty(oritation)) {
            oritation = 90;
        }
        if (mPlayerView != null)
            mPlayerView.fullScreen(oritation);
    }

    public void exitFullScreen() {
        if (mPlayerView != null)
            mPlayerView.exitFullScreen();
    }

    public void play() {
        if (mPlayerView != null)
            mPlayerView.start();
    }

    public void pause() {
        if (mPlayerView != null)
            mPlayerView.pause();
    }

    public void resume() {
        if (mPlayerView != null)
            mPlayerView.onResume();
    }

    public void stop() {
        if (mPlayerView != null)
            mPlayerView.stop();
    }

    public void seek(int position) {
        if (mPlayerView != null)
            mPlayerView.seekTo(seek = position);
    }

    public void sendDanmu(JSONObject danmu) {
        if (mPlayerView != null)
            mPlayerView.sendDanmaku(new org.json.JSONObject(danmu), true);
    }

    public void sendPlayBackRate(String rate) {
        if (mPlayerView != null)
            mPlayerView.playbackRate(rate);
    }

    public void destory() {
        if (mPlayerView != null) {
            mPlayerView.onDestroy();
            mPlayerView = null;
        }
    }

    public void setSrc(String mSrc) {
        if (TextUtils.isEmpty(mSrc)) return;
        this.willBeSetSrc = mSrc;
        if (isFinishLayout) {
            onLayoutFinished();
        }
    }

    /**
     * 判断逻辑，component加载完之后再执行autoplay
     *
     * @param autoplay
     */
    public void setAutoplay(boolean autoplay) {
        this.autoplay = autoplay;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public void setPoster(String poster) {
        if (mPlayerView != null)
            if (!TextUtils.isEmpty(poster) && !this.poster.equalsIgnoreCase(poster)) {
                ImageLoaderL.getInstance().displayImage(poster, mPlayerView.mPlayerThumb);
                this.poster = poster;
            }
    }

    public void setProgress(boolean isShow) {
        if (mPlayerView != null)
            mPlayerView.setProgressVisibility(isShow);
    }

    public void setPlayBtnVisibility(boolean isshow) {
        if (mPlayerView != null)
            mPlayerView.setPlayBntVisibility(isshow);
    }

    public void setMuted(boolean muted) {
        if (mPlayerView != null)
            mPlayerView.setMutePlayer(muted);
    }

    public void setControls(boolean controls) {
        if (mPlayerView != null)
            mPlayerView.setControls(controls);
    }

    public void setPageGesture(boolean pageGesture) {
        if (mPlayerView != null)
            mPlayerView.setPageGesture(pageGesture);
    }

    public void setShowFullScreenBtn(boolean showFullScreenBtn) {
        if (mPlayerView != null)
            mPlayerView.setFullscreenBntVisibility(showFullScreenBtn);
    }

    public void setEnableProgressGesture(boolean enableProgressGesture) {
        if (mPlayerView != null)
            mPlayerView.setIsEnableProgressGesture(enableProgressGesture);
    }

    public void setDirection(int direction) {
        if (mPlayerView != null)
            mPlayerView.setDirection(direction);
    }

    private boolean isEnableDanmu = false;

    public void setEnableDanmu(boolean enableDanmu) {
        if (mPlayerView != null) {
            this.isEnableDanmu = enableDanmu;
            mPlayerView.enableDanmaku(enableDanmu);
        }
    }

    private boolean isEnableDanmuBtn = false;

    public void setDanmuBtn(boolean danmuBtn) {
        if (mPlayerView != null) {
            this.isEnableDanmuBtn = danmuBtn;
            mPlayerView.enableDanmuBtn(danmuBtn);
        }
    }

    public void setDanmuList(JSONArray danmuList) {
        if (mPlayerView != null)
            mPlayerView.setmDanmuList(danmuList.toString());
    }

    public void setObjectFit(String objectFit) {
        if (TextUtils.isEmpty(objectFit)) return;
        if (mPlayerView != null)
            mPlayerView.setScaleType(objectFit);
    }

    public void setMuteBtn(boolean isshow) {
        if (mPlayerView != null)
            mPlayerView.isMuteBtnShow(isshow);
    }

    public void setTitle(String title) {
        if (PdrUtil.isEmpty(title)) return;
        if (mPlayerView != null)
            mPlayerView.setTitle(title);
    }

    public void setPlayBtnPosition(String position) {
        if (mPlayerView != null)
            mPlayerView.setPlayBtnPosition(position);
    }

    public void setShowCenterPlayBtn(boolean showCenterPlayBtn) {
        if (mPlayerView != null)
            mPlayerView.setCenterPlayBtnVisibility(showCenterPlayBtn);
    }

    public void setCodec(String codec) {
        if (mPlayerView != null)
            mPlayerView.isUseMediaCodec(codec.equals("hardware"));
    }

    /**
     * 判断逻辑，设置src之后再设置duration
     *
     * @param duration
     */
    public void setDuration(float duration) {
        if (mPlayerView != null) {
            this.duration = duration * 1000;
            if (isFinishLayout)
                mPlayerView.setDuration((int) this.duration);
        }
    }

    /**
     * 判断逻辑，设置src之后再设置initialtime
     *
     * @param initialTime
     */
    public void setInitialTime(float initialTime) {
        if (mPlayerView != null) {
            if (initialTime <= 0) return;
            this.initialTime = initialTime * 1000;
            if (isFinishLayout)
                mPlayerView.seekTo((int) this.initialTime);
        }
    }

    private GraphicSize originalSize = null;
    private GraphicSize fullScreenSize = null;

    @Override
    public void onChanged(String type, String msg) {
        Map<String, Object> values = new HashMap<>();
        if (!TextUtils.isEmpty(msg)) {
            values.put("message", msg);
        }
        execCallBack(type, values);
        if (mPlayerView == null) return;
        WXComponent child = component.getChild(0);
        if (child instanceof VideoInnerViewComponent) {
            if (originalSize == null)
                originalSize = child.getLayoutSize();
            if (type.equals("onConfigurationChanged")) {
                if (isFullScreen()) {
                    float width = fullScreenSize.getWidth();
                    float height = fullScreenSize.getHeight();
                    if (mPlayerView.orientation == 0) {
                        if (width > height) {
                            float a = width;
                            width = height;
                            height = a;
                        }
                    } else if (Math.abs(mPlayerView.orientation) == 90) {
                        if (width < height) {
                            float a = width;
                            width = height;
                            height = a;
                        }
                    }
                    WXBridgeManager.getInstance().setStyleHeight(child.getInstanceId(), child.getRef(), height);
                    WXBridgeManager.getInstance().setStyleWidth(child.getInstanceId(), child.getRef(), width);
                    removeView(subViewContainer);
                    mPlayerView.addView(subViewContainer);
                    subViewContainer.bringToFront();
                } else {
                    WXBridgeManager.getInstance().setStyleHeight(child.getInstanceId(), child.getRef(), originalSize.getHeight());
                    WXBridgeManager.getInstance().setStyleWidth(child.getInstanceId(), child.getRef(), originalSize.getWidth());
                    mPlayerView.removeView(subViewContainer);
                    addView(subViewContainer);
                    subViewContainer.bringToFront();
                }
            }
        }
    }

    public boolean isFullScreen() {
        return mPlayerView != null && mPlayerView.isFullscreen();
    }

    public boolean onBackPress() {
        return mPlayerView != null && mPlayerView.onBackPressed();
    }

    /**
     * 触发事件
     *
     * @param iMediaPlayer
     * @param status
     * @param i1
     * @return
     */
    @Override
    public boolean onInfo(IMediaPlayer iMediaPlayer, int status, int i1) {
        switch (status) {
            case MediaPlayerParams.STATE_COMPLETED:
                if (loop) play();
                execCallBack("ended", new HashMap<String, Object>());
                break;
            case MediaPlayerParams.STATE_PLAYING:
                execCallBack("play", new HashMap<String, Object>());
                break;
            case MediaPlayerParams.STATE_PAUSED:
                execCallBack("pause", new HashMap<String, Object>());
                break;
            case MediaPlayerParams.STATE_ERROR:
                execCallBack("error", new HashMap<String, Object>());
                break;
            case MediaPlayerParams.STATE_PREPARING:
            case IMediaPlayer.MEDIA_INFO_BUFFERING_START:
                execCallBack("waiting", new HashMap<String, Object>());
                break;
        }
        return false;
    }

    private void execCallBack(String type, Map<String, Object> values) {
        if (component.getEvents().contains(type)) {
            component.fireEvent(type, values);
        }
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int i) {
        Map<String, Object> values = new HashMap<>();
        Map<String, Object> detail = new HashMap<>();
        detail.put("buffered", i);
        values.put("detail", detail);
        execCallBack("progress", values);
    }
}
