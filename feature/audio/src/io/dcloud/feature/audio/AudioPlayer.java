package io.dcloud.feature.audio;

import android.media.MediaPlayer;
import android.media.TimedText;
import android.net.Uri;

import java.io.IOException;
import java.net.URLDecoder;

import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.IEventCallback;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.ui.AdaFrameView;
import io.dcloud.common.adapter.util.DeviceInfo;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;

/**
 * Created by shutao on 2018/5/22.
 */
class AudioPlayer extends AbsAudio implements ISysEventListener, IEventCallback {
    IWebview mWebview;
    String mSrcPath;
    String mFunId;
    MediaPlayer mMediaPlayer;

    private AudioPlayer() {
        mMediaPlayer = new MediaPlayer();
    }

    static AudioPlayer createAudioPlayer(String pPath) {
        AudioPlayer _player = new AudioPlayer();
        _player.mSrcPath = pPath;
        return _player;
    }

    void successCallback() {
        JSUtil.excCallbackSuccess(mWebview, AudioPlayer.this.mFunId, "");
    }

    void failCallback(int code, String msg) {
        String error_json = DOMException.toJSON(code, msg);
        JSUtil.excCallbackError(mWebview, AudioPlayer.this.mFunId, error_json, true);
    }

    private boolean isPrepared = false;

    void play() {
        try {
            final IApp _app = mWebview.obtainFrameView().obtainApp();
            mMediaPlayer.reset();
            String _fullPath = mSrcPath;
            if (_fullPath.startsWith("content://")) {//如content://media/internal/audio/media/8
                Uri uri = Uri.parse(_fullPath);
//				uri = RingtoneManager.getActualDefaultRingtoneUri(mWebview.getContext(),RingtoneManager.TYPE_RINGTONE);
                mMediaPlayer.setDataSource(mWebview.getActivity(), uri);
            } else {
                if (!PdrUtil.isNetPath(mSrcPath)) {
                    _fullPath = _app.checkPrivateDirAndCopy2Temp(mSrcPath);
                    _fullPath = _app.convert2AbsFullPath(mWebview.obtainFullUrl(), _fullPath);
                } else {
                    _fullPath = URLDecoder.decode(mSrcPath, "utf-8");
                }
                mMediaPlayer.setDataSource(_fullPath);
            }
            isPrepared = false;
            //注册页面状态事件，监听页面关闭或重新载入事件，当发生事件的时候需要停止播放
            ((AdaFrameView) mWebview.obtainFrameView()).addFrameViewListener(AudioPlayer.this);
            //注册应用关闭事件
            _app.registerSysEventListener(AudioPlayer.this, SysEventType.onStop);
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Logger.d(AudioFeatureImpl.TAG, "onPrepared ");
                    isPrepared = true;
                    mMediaPlayer.start();
                }
            });
            mMediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {

                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    // TODO Auto-generated method stub
                    Logger.d(AudioFeatureImpl.TAG, "onSeekComplete ");
                }
            });
            mMediaPlayer.setOnVideoSizeChangedListener(new MediaPlayer.OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                    // TODO Auto-generated method stub
                    Logger.d(AudioFeatureImpl.TAG, "onVideoSizeChanged width=" + width + ";height=" + height);
                }
            });
            mMediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {

                @Override
                public boolean onInfo(MediaPlayer mp, int what, int extra) {
                    Logger.d(AudioFeatureImpl.TAG, "onInfo what=" + what + ";extra=" + extra);
                    return false;
                }
            });
            if (DeviceInfo.sDeviceSdkVer >= 16) {
                mMediaPlayer.setOnTimedTextListener(new MediaPlayer.OnTimedTextListener() {

                    @Override
                    public void onTimedText(MediaPlayer mp, TimedText text) {
                        // TODO Auto-generated method stub
                        Logger.d(AudioFeatureImpl.TAG, "onTimedText " + text);
                    }
                });
            }
            mMediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                @Override
                public boolean onError(MediaPlayer mp, int what, int extra) {
//					what the type of error that has occurred:
//					MEDIA_ERROR_UNKNOWN
//					MEDIA_ERROR_SERVER_DIED
//					extra an extra code, specific to the error. Typically implementation dependent.
//					MEDIA_ERROR_IO
//					MEDIA_ERROR_MALFORMED
//					MEDIA_ERROR_UNSUPPORTED
//					MEDIA_ERROR_TIMED_OUT
                    Logger.d(AudioFeatureImpl.TAG, "setOnErrorListener what=" + what + ";extra=" + extra);
                    stop();
                    int code = DOMException.CODE_UNKNOWN_ERROR;
                    String msg = DOMException.MSG_UNKNOWN_ERROR;
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
                    failCallback(code, msg);
                    return true;
                }
            });
            mMediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {

                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    // TODO Auto-generated method stub
                    Logger.d(AudioFeatureImpl.TAG, "onBufferingUpdate " + percent);
                }
            });
            mMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    Logger.d(AudioFeatureImpl.TAG, "onCompletion ");
                    stop();
                    successCallback();
                }
            });
            mMediaPlayer.prepareAsync();
        } catch (IllegalStateException e) {
            e.printStackTrace();
            stop();
            failCallback(DOMException.CODE_PARAMETER_ERRORP, e.getMessage());
        } catch (IOException e) {
            e.printStackTrace();
            stop();
            failCallback(DOMException.CODE_IO_ERROR, e.getMessage());
        }
    }

    void pause() {
        mMediaPlayer.pause();
    }

    void resume() {
        mMediaPlayer.start();
    }

    void stop() {
        if (mMediaPlayer != null) {//判断当正在播放的时候才可以停止
            mMediaPlayer.stop();
            mMediaPlayer.release();
            ((AdaFrameView) mWebview.obtainFrameView()).removeFrameViewListener(AudioPlayer.this);
            mWebview.obtainFrameView().obtainApp().unregisterSysEventListener(this, SysEventType.onStop);
            mMediaPlayer = null;
        }
    }

    void seekTo(int sec) {//传入s设置时需要设置ms
        mMediaPlayer.seekTo(sec);
    }

    String getDuration() {
        if (isPrepared) {
            int duration = mMediaPlayer.getDuration();
            //准换单位为s
            return JSUtil.wrapJsVar(PdrUtil.int2DecimalStr(duration, 1000), false);
        } else {
            return JSUtil.wrapJsVar("NaN", false);
        }
    }

    String getPosition() {
        int position = mMediaPlayer.getCurrentPosition();
        //准换单位为s;
        return JSUtil.wrapJsVar(PdrUtil.int2DecimalStr(position, 1000), false);
    }

    @Override
    public Object onCallBack(String pEventType, Object pArgs) {
        if ((PdrUtil.isEquals(pEventType, StringConst.EVENTS_WINDOW_CLOSE) //页面重新载入，或当前webview载入了其他页面
                || PdrUtil.isEquals(pEventType, StringConst.EVENTS_CLOSE)) //页面关闭时候
                && pArgs instanceof IWebview) {
            stop();
        }
        return null;
    }

    @Override
    public boolean onExecute(SysEventType pEventType, Object pArgs) {
        if (pEventType == SysEventType.onStop) {
            stop();
        }
        return false;
    }
}
