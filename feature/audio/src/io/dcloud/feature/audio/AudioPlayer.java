package io.dcloud.feature.audio;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;

import org.json.JSONObject;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.IEventCallback;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;

/**
 * Created by shutao on 2018/5/22.
 */
class AudioPlayer extends AbsAudio implements ISysEventListener, IEventCallback {
    private IWebview mWebview;
    private IApp _app;
    String mFunId;
    private MediaPlayer mMediaPlayer;
    private Map<String, String> events;

    private JSONObject params;
    private int bufferPercent = 0;
    private int startTime = Integer.MIN_VALUE;
    private String mSrcPath = "";
    private float volume = 1f;
    private boolean autoplay = false;
    /**
     * AudioManager
     * 用于监听音频焦点
     */
    private AudioManager mAudioMgr;

    private AudioPlayer(JSONObject params, IWebview mWebview) {
        mMediaPlayer = new MediaPlayer();
        events = new HashMap<>();
        this.params = params;
        this.mWebview = mWebview;
        addListener();
        _app = mWebview.obtainFrameView().obtainApp();
        //注册页面状态事件，监听页面关闭或重新载入事件，当发生事件的时候需要停止播放
        mWebview.obtainFrameView().addFrameViewListener(AudioPlayer.this);
        //注册应用关闭事件
        _app.registerSysEventListener(AudioPlayer.this, SysEventType.onStop);
        setStyle(this.params);
        this.requestAudioFocus();
    }

    /**
     * 音频焦点监听
     */
    private AudioManager.OnAudioFocusChangeListener mAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
//            if(mMediaPlayer == null) {
//                return;
//            }
            // 来电、其它APP播放音频，失去焦点操作
            if(focusChange==AudioManager.AUDIOFOCUS_LOSS || focusChange==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT || focusChange==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK){
                pause();
            // 获得焦点之后的操作
            } else if(focusChange == AudioManager.AUDIOFOCUS_GAIN){

            }
        }
    };

    /**
     * 请求音频焦点
     */
    private void requestAudioFocus() {
        if (mAudioMgr == null) {
            mAudioMgr = (AudioManager) mWebview.getActivity().getSystemService(Context.AUDIO_SERVICE);
        }
        if (mAudioMgr != null) {
            mAudioMgr.requestAudioFocus(mAudioFocusChangeListener,AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        }
    }

    private void addListener() {
        mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                execEvents("canplay", "");
            }
        });
        mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {

            @Override
            public void onSeekComplete(MediaPlayer mp) {
                if (startTime != Integer.MIN_VALUE)
                    execEvents("seeked", "");
            }
        });
        mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                bufferPercent = percent;
            }
        });
        mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {

            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                switch (what) {
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        execEvents("waiting", "");
                        break;
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                        break;
                }
                return false;
            }
        });
        mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                int code = 0;
                String msg = null;
                switch (what) {
                    case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                        code = DOMException.CODE_UNKNOWN_ERROR;
                        msg = DOMException.MSG_UNKNOWN_ERROR;
                        break;
                    case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                        code = 1303;
                        msg = "播放异常，需重新创建";
                        break;
                }
                switch (extra) {
                    case MediaPlayer.MEDIA_ERROR_IO:
                        code = DOMException.CODE_IO_ERROR;
                        msg = DOMException.MSG_IO_ERROR;
                        break;
                    case MediaPlayer.MEDIA_ERROR_MALFORMED:
                        code = DOMException.CODE_AUDIO_ERROR_MALFORMED;
                        msg = DOMException.MSG_AUDIO_ERROR_MALFORMED;
                        break;
                    case MediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                        code = DOMException.CODE_NOT_SUPPORT;
                        msg = DOMException.MSG_NOT_SUPPORT;
                        break;
                    case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                        code = DOMException.CODE_AUDIO_ERROR_TIMED_OUT;
                        msg = DOMException.MSG_AUDIO_ERROR_TIMED_OUT;
                        break;
                }
                if (code != 0) {
                    failCallback(code, msg);
                    execEvents("error", DOMException.toJSON(code, msg));
                }
                return true;
            }
        });
        mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                execEvents("ended", "");
            }
        });
    }

    void setStyle(JSONObject styles) {
        String src = styles.optString("src");
        if (!PdrUtil.isEmpty(src))
            if (PdrUtil.isEmpty(mSrcPath)) {
                mMediaPlayer.reset();
                setSrc(src);
            } else if (!src.equals(mSrcPath)) {
                mMediaPlayer.reset();
                setSrc(src);
            }
        JSONUtil.combinJSONObject(params, styles);
        mSrcPath = styles.optString("src");
        mMediaPlayer.setLooping(params.optBoolean("loop"));
        try {
            String volumeStr = params.optString("volume", "1");
            volume = Float.parseFloat(volumeStr);
            if (volume < 0)
                volume = 0;
            else if (volume > 1)
                volume = 1;
            mMediaPlayer.setVolume(volume, volume);
            if (params.has("startTime")) {
                startTime = params.optInt("startTime") * 1000;
            }
            autoplay = params.optBoolean("autoplay", false);
        } catch (Exception ignored) {
        }
    }

    String getStyles(String key) {
        if (PdrUtil.isEmpty(key)) {
            return JSUtil.wrapJsVar(params);
        } else {
            Object values;
            switch (key) {
                case "autoplay":
                    values = params.optBoolean("autoplay", false);
                    break;
                case "startTime":
                    values = startTime < 0 ? params.has("startTime") ? params.optInt("startTime") : 0 : startTime;
                    break;
                case "volume":
                    values = volume;
                    break;
                case "loop":
                    values = mMediaPlayer.isLooping();
                    break;
                case "src":
                    values = mSrcPath;
                    break;
                default:
                    if (params.has(key)) {
                        return JSUtil.wrapJsVar(params.optString(key));
                    } else {
                        return JSUtil.wrapJsVar("undefined", false);
                    }
            }
            if (values != null) {
                return JSUtil.wrapJsVar(values.toString());
            } else {
                return JSUtil.wrapJsVar("undefined", false);
            }
        }
    }

    private void setSrc(String url) {
        try {
            if (url.startsWith("content://")) {//如content://media/internal/audio/media/8
                Uri uri = Uri.parse(url);
                mMediaPlayer.setDataSource(mWebview.getActivity(), uri);
            } else {
                if (!PdrUtil.isNetPath(url)) {
                    url = _app.checkPrivateDirAndCopy2Temp(url);
                    url = _app.convert2AbsFullPath(mWebview.obtainFullUrl(), url);
                    if (url.startsWith("/android_asset/")) {
                        url = url.replace("/android_asset/","");
                    } else if (url.startsWith("android_asset/")) {
                        url = url.replace("android_asset/","");
                    }
                    if (!PdrUtil.isDeviceRootDir(url)) {
                        // 读取assets下文件
                        AssetFileDescriptor fd = mWebview.getActivity().getAssets().openFd(url);
                        mMediaPlayer.setDataSource(fd.getFileDescriptor(),fd.getStartOffset(),fd.getLength());
                        mMediaPlayer.prepareAsync();
                        return;
                    }
                } else {
                    url = URLDecoder.decode(url, "utf-8");
                }
                mMediaPlayer.setDataSource(url);
            }
            mMediaPlayer.prepareAsync();
        } catch (IOException e) {
            stop();
            failCallback(DOMException.CODE_IO_ERROR, e.getMessage());
            execEvents("error", DOMException.toJSON(DOMException.CODE_IO_ERROR, e.getMessage()));
        }
    }

    public void setParams(JSONObject params) {
        this.params = params;
    }

    static AudioPlayer createAudioPlayer(JSONObject param, IWebview mWebview) {
        return new AudioPlayer(param, mWebview);
    }

    private void successCallback() {
        JSUtil.excCallbackSuccess(mWebview, AudioPlayer.this.mFunId, "");
    }

    void failCallback(int code, String msg) {
        String error_json = DOMException.toJSON(code, msg);
        JSUtil.excCallbackError(mWebview, AudioPlayer.this.mFunId, error_json, true);
    }

    private boolean isPrepared = false;
    private boolean isPlay = false;
    private boolean isCanplay = false;

    void play() {
        if (isStoped && !mMediaPlayer.isPlaying()) {
            try {
                mMediaPlayer.prepareAsync();
                isStoped = false;
            }catch (Exception ignored){}
        }
        try {
            isPrepared = false;
            isPlay = true;
            if (isCanplay) {
                startPlay();
            }
        } catch (IllegalStateException e) {
            e.printStackTrace();
            destory();
            failCallback(DOMException.CODE_PARAMETER_ERRORP, e.toString());
            execEvents("error", DOMException.toJSON(DOMException.CODE_PARAMETER_ERRORP, e.getMessage()));
        } catch (NumberFormatException ignored) {
        }
    }

    private void startPlay() {
        isPrepared = true;
        mMediaPlayer.start();
        execEvents("play", "");
        isPlay = false;
        this.requestAudioFocus();
    }

    void pause() {
        autoplay = false;
        mMediaPlayer.pause();
        execEvents("pause", "");
    }

    void resume() {
        mMediaPlayer.start();
        this.requestAudioFocus();
    }

    private boolean isStoped = false;
    void stop() {
        if (mMediaPlayer != null) {
            mMediaPlayer.stop();
            isStoped = true;
            isCanplay = false;
            execEvents("stop", "");
        }
    }

    void destory() {
        if (mMediaPlayer != null) {//判断当正在播放的时候才可以停止
            mMediaPlayer.stop();
            mMediaPlayer.release();
            mWebview.obtainFrameView().removeFrameViewListener(AudioPlayer.this);
            mWebview.obtainFrameView().obtainApp().unregisterSysEventListener(this, SysEventType.onStop);
            mMediaPlayer = null;
            mAudioMgr.abandonAudioFocus(mAudioFocusChangeListener);
            mAudioMgr = null;
        }
    }

    void seekTo(int sec) {//传入s设置时需要设置ms
        mMediaPlayer.seekTo(sec);
        execEvents("seeking", "");
    }

    String getBuffer() {
        int buffer = -1;
        if (mMediaPlayer != null) {
            buffer = bufferPercent * mMediaPlayer.getDuration() / 100;
        }
        return JSUtil.wrapJsVar(PdrUtil.int2DecimalStr(buffer, 1000), false);
    }

    String isPause() {
        boolean pause = true;
        if (mMediaPlayer != null) {
            pause = !mMediaPlayer.isPlaying();
        }
        return JSUtil.wrapJsVar(pause);
    }

    String getVolume() {
        return JSUtil.wrapJsVar(volume);
    }

    String getDuration() {
        if (mMediaPlayer != null) {
            int duration = mMediaPlayer.getDuration();
            if (duration < 0) {
                return JSUtil.wrapJsVar("undefined",false);
            }
            //准换单位为s
            return JSUtil.wrapJsVar(PdrUtil.int2DecimalStr(duration, 1000), false);
        } else {
            return JSUtil.wrapJsVar("undefined", false);
        }
    }

    String getPosition() {
        int position = mMediaPlayer.getCurrentPosition();
        //准换单位为s;
        return JSUtil.wrapJsVar(PdrUtil.int2DecimalStr(position, 1000), false);
    }

    void addEventListener(String event, String callbackId) {
        events.put(event, callbackId);
    }

    void removeEventListener(String event) {
        events.remove(event);
    }

    void execEvents(String event, String msg) {
        String callback = events.get(event);
        if (!PdrUtil.isEmpty(callback))
            JSUtil.execCallback(mWebview, callback, msg, JSUtil.OK, !PdrUtil.isEmpty(msg), true);
        switch (event) {
            case "canplay":
                isCanplay = true;
                if (autoplay)
                    play();
                if (isPlay) {
                    startPlay();
                }
                if (startTime != Integer.MIN_VALUE)
                    mMediaPlayer.seekTo(startTime);
                break;
            case "ended":
                pause();
                successCallback();
                break;
        }
    }

    @Override
    public Object onCallBack(String pEventType, Object pArgs) {
        if ((PdrUtil.isEquals(pEventType, StringConst.EVENTS_WINDOW_CLOSE) //页面重新载入，或当前webview载入了其他页面
                || PdrUtil.isEquals(pEventType, StringConst.EVENTS_CLOSE)) //页面关闭时候
                && pArgs instanceof IWebview) {
            destory();
        }
        return null;
    }

    @Override
    public boolean onExecute(SysEventType pEventType, Object pArgs) {
        if (pEventType == SysEventType.onStop) {
            destory();
        }
        return false;
    }
}
