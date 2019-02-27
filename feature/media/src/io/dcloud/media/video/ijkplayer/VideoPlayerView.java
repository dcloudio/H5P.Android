package io.dcloud.media.video.ijkplayer;

import android.app.Activity;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.nostra13.dcloudimageloader.core.ImageLoaderL;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.DHInterface.IVideoPlayer;
import io.dcloud.media.video.VideoPlayerMgr;
import io.dcloud.media.video.ijkplayer.media.IjkPlayerView;
import io.dcloud.media.video.ijkplayer.media.MediaPlayerParams;
import tv.danmaku.ijk.media.player.IMediaPlayer;

public class VideoPlayerView extends FrameLayout implements IVideoPlayer{

    private IjkPlayerView mPlayerView;
    private IWebview mIWebview;
    private String mUrl;
    private String mPosterUrl;
    private JSONObject mOptions;
    //播放基础事件回调栈
    private HashMap<String, HashMap<String,String>> mCallbacks;
    private boolean isAutoPlay = false;
    private boolean isLoopPlay = false;

    public JSONObject fullScreenOptions = null;

    public void setFullScreenOptions(JSONObject fullScreenOptions) {
        this.fullScreenOptions = fullScreenOptions;
    }

    public VideoPlayerView(@NonNull Activity context, IWebview webview, JSONObject style) {
        super(context);
        mIWebview = webview;
        mPlayerView =  new IjkPlayerView(context);
        addView(mPlayerView, new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        mCallbacks = new HashMap<String, HashMap<String, String>>();
        mPlayerView.init().setPlayerRootView(this);
        mPlayerView.setOnInfoListener(new IMediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(IMediaPlayer iMediaPlayer, int status, int extra) {
                if(status == MediaPlayerParams.STATE_COMPLETED) {
                    statusChanged("ended", "");
                    if(isLoopPlay) {
                        play();
                    }
                } else if(status == MediaPlayerParams.STATE_PLAYING) {
                    statusChanged("play", "");
                } else if(status == MediaPlayerParams.STATE_PAUSED) {
                    statusChanged("pause", "");
                } else if(status == MediaPlayerParams.STATE_ERROR) {
                    statusChanged("error", "");
                } else if(status == MediaPlayerParams.STATE_PREPARING || status == iMediaPlayer.MEDIA_INFO_BUFFERING_START) {
                    statusChanged("waiting", "");
                }
                return false;
            }
        });

        mPlayerView.setOnPlayerChangedListener(new OnPlayerChangedListener() {
            @Override
            public void onChanged(String type, String msg) {
                if (type.equals("fullscreenchange")){
                    try {
                        JSONObject object = new JSONObject(msg);
                        if (object.optBoolean("fullScreen")){
                        } else {
                            if (null != fullScreenOptions)
                                setOptions(fullScreenOptions);
                        }
                    } catch (JSONException e) {
                    }
                }
                statusChanged(type, msg);
            }
        });
        initOptionsPlayerView(mPlayerView, style);
    }

    //修改视频横竖屏切换时中间按钮状态，修改横竖屏切换时导致的从头开始播放的问题
    public void initOptionsPlayerView(IjkPlayerView playerView, JSONObject options) {
        mOptions = options;
        String url = mOptions.optString("src");
        if (!PdrUtil.isNetPath(url)) {
            url = mIWebview.obtainApp().convert2AbsFullPath(mIWebview.obtainFullUrl(),url);
        }
        if(TextUtils.isEmpty(url)) {
            return;
        }
        isAutoPlay = mOptions.optBoolean("autoplay", isAutoPlay);
        isLoopPlay = mOptions.optBoolean("loop", isLoopPlay);
        setPoster(mOptions.optString("poster"));
        playerView.setMutePlayer(mOptions.optBoolean("muted", false));
        playerView.setControls(mOptions.optBoolean("controls", true));
        playerView.setPageGesture(mOptions.optBoolean("page-gesture", false));
        playerView.setProgressVisibility(mOptions.optBoolean("show-progress", true));
        playerView.setFullscreenBntVisibility(mOptions.optBoolean("show-fullscreen-btn", true));
        playerView.setPlayBntVisibility(mOptions.optBoolean("show-play-btn", true));
        playerView.setIsEnableProgressGesture(mOptions.optBoolean("enable-progress-gesture", true));
        int orientation = mOptions.optInt("direction", -90);
        playerView.setDirection(orientation);
        playerView.enableDanmaku(mOptions.optBoolean("enable-danmu",false));
        playerView.enableDanmuBtn(mOptions.optBoolean("danmu-btn",false));
        playerView.setmDanmuList(mOptions.optString("danmu-list"));
        playerView.setScaleType(mOptions.optString("objectFit","contain"));
        playerView.setCenterPlayBntVisibility(mOptions.optBoolean("show-center-play-btn", true));

        if(TextUtils.isEmpty(mUrl)) {
            mPlayerView.setVideoPath(url);
            resetSeek(playerView);
        } else if(!mUrl.equalsIgnoreCase(url)){
            mPlayerView.switchVideoPath(url);
            resetSeek(playerView);
        }

        playerView.setDuration(mOptions.optInt("duration",-1)*1000);
        mUrl = url;
    }

    private void resetSeek(IjkPlayerView playerView) {
        int seek = mOptions.optInt("initial-time");
        playerView.seekTo(seek * 1000);
        mPlayerView.clearDanma();
        if (isAutoPlay)
            play();
    }

    @Override
    public void play() {
        if(mPlayerView != null) mPlayerView.start();
    }

    @Override
    public void pause() {
        if(mPlayerView != null) mPlayerView.pause();
    }

    @Override
    public void resume() {
        if(mPlayerView != null) mPlayerView.onResume();
    }

    @Override
    public void stop() {
        statusChanged("waiting", "stop");
        if(mPlayerView != null) {
            if(TextUtils.isEmpty(mUrl)) {
                //mPlayerView.stop();
            } else {
                mPlayerView.switchVideoPath(mUrl);
            }
        }
    }

    @Override
    public void close() {
        statusChanged("waiting", "close");
        if(mPlayerView != null) {
            mPlayerView.stop();
            mPlayerView.onDestroy();
            mPlayerView = null;
        }
    }

    @Override
    public void seek(String position) {
        int msec = Integer.parseInt(position);
        if(mPlayerView != null)
        mPlayerView.seekTo(msec*1000);
    }

    @Override
    public void sendDanmu(JSONObject danmu) {
        if (mPlayerView != null)
        mPlayerView.sendDanmaku(danmu,true);
    }

    @Override
    public void playbackRate(String rate) {
        if (mPlayerView != null)
            mPlayerView.playbackRate(rate);
    }

    public boolean onBackPressed() {
       return mPlayerView.onBackPressed();
    }

    @Override
    public void requestFullScreen(String direction) {
        int orientation = Integer.parseInt(direction);
        if(mPlayerView != null)
            mPlayerView.fullScreen(orientation);
    }

    @Override
    public void exitFullScreen() {
        if(mPlayerView != null)
            mPlayerView.exitFullScreen();
    }

    @Override
    public void setOptions(JSONObject options) {
        if(options != null) {
            mPlayerView.hiddenLoaded(true);
            initOptionsPlayerView(mPlayerView, options);
            /*Iterator<String> iterator = options.keys();
            while(iterator.hasNext()){
                String key = iterator.next();
                if(key.equalsIgnoreCase("src")) {
                    setPlayerUrl(options.optString(key));
                } else if(key.equalsIgnoreCase("controls")) {
                    mPlayerView.setControls(options.optBoolean("controls", true));
                } else if(key.equalsIgnoreCase("autoplay")) {
                    isAutoPlay = options.optBoolean("autoplay", isAutoPlay);
                } else if(key.equalsIgnoreCase("loop")) {
                    isLoopPlay = options.optBoolean("loop", isLoopPlay);
                } else if(key.equalsIgnoreCase("muted")) {
                    mPlayerView.setMutePlayer(options.optBoolean("muted", false));
                } else if(key.equalsIgnoreCase("poster")) {
                    setPoster(options.optString("poster"));
                }
            }*/
        }
    }

    private void setPlayerUrl(String url) {
        if(TextUtils.isEmpty(url)) {
            return;
        }
        if(TextUtils.isEmpty(mUrl)) {
            mPlayerView.setVideoPath(url);
        } else if(!mUrl.equalsIgnoreCase(url)){
            mPlayerView.switchVideoPath(url);
        }
        mUrl = url;
    }

    private void setPoster(String url) {
        if(TextUtils.isEmpty(url)) {
            return;
        }
        if(TextUtils.isEmpty(mPosterUrl) || !mPosterUrl.equalsIgnoreCase(url)) {
            ImageLoaderL.getInstance().displayImage(url, mPlayerView.mPlayerThumb);
        }
        mPosterUrl = url;
    }

    @Override
    public void addEventListener(String event, String jsCallback,String webId) {
//        mCallbacks.put(event, jsCallback);
        HashMap<String,String> callBacks = mCallbacks.get(event);
        if (callBacks == null) {
            callBacks = new HashMap<String,String>();
        }
        callBacks.put(jsCallback,webId);
        mCallbacks.put(event,callBacks);
    }

    @Override
    public boolean isFullScreen() {
        if (mPlayerView != null)
            return mPlayerView.isFullscreen();
        else
            return false;
    }

    public boolean isPlaying() {
        if (mPlayerView != null)
            return mPlayerView.isPlaying();
        else
            return false;
    }

    @Override
    public void release() {
        if(mPlayerView != null) {
            mPlayerView.onDestroy();
            mPlayerView = null;
        }
    }

    @Override
    public boolean isPointInRect(float x, float y) {
        Logger.e("当前的容器："+this+"；x:"+x+"；y:"+y);
        if (rect!=null) {
            if (x>rect[0]&&x<rect[2]&&y>rect[1]&&y<rect[3]){
                return true;
            }
        }
        return false;
    }

    private int[] rect;
    public void setRect(int[] rect) {
        this.rect = rect;
    }

    public void statusChanged(String type, String msg) {
        if(mCallbacks.containsKey(type)) {
            HashMap<String,String> callbacks = mCallbacks.get(type);
            for (String key:callbacks.keySet()){
                IWebview eventWebview;
                if (PdrUtil.isEmpty(callbacks.get(key))) {
                    eventWebview = mIWebview;
                } else {
                    eventWebview = VideoPlayerMgr.getInstance().findWebview(mIWebview,callbacks.get(key));
                }
                JSUtil.execCallback(eventWebview, key, msg, JSUtil.OK, !TextUtils.isEmpty(msg), true);
            }
        }
    }
}
