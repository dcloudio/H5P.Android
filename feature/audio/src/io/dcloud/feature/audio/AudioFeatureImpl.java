package io.dcloud.feature.audio;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnBufferingUpdateListener;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.MediaPlayer.OnSeekCompleteListener;
import android.media.MediaPlayer.OnTimedTextListener;
import android.media.MediaPlayer.OnVideoSizeChangedListener;
import android.media.MediaRecorder;
import android.media.TimedText;
import android.net.Uri;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.IEventCallback;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.io.DHFile;
import io.dcloud.common.adapter.ui.AdaFrameView;
import io.dcloud.common.adapter.util.DeviceInfo;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.MessageHandler;
import io.dcloud.common.adapter.util.MessageHandler.IMessages;
import io.dcloud.common.adapter.util.PermissionUtil;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.feature.audio.AudioRecorder.RecordOption;


public class AudioFeatureImpl implements IFeature,IMessages {

    HashMap<String, ArrayList> mAppsAudioObj = null;
    static final String TAG = "AudioFeatureImpl";

    @Override
    public String execute(final IWebview pWebViewImpl, final String pActionName,
                          final String[] pJsArgs) {
        String _ret = null;
        String _appid = pWebViewImpl.obtainFrameView().obtainApp().obtainAppId();
        Logger.d(TAG, "execute pJsArgs[0]=" + pJsArgs[0]);
        if ("AudioSyncExecMethod".equals(pActionName)) {
            String _methodName = pJsArgs[0];//函数名字
            JSONArray _args = JSONUtil.createJSONArray(pJsArgs[1]);
            if ("getDuration".equals(_methodName)) {
                AudioPlayer _player = null;
                String _uuid_ = JSONUtil.getString(_args, 0);
                _player = ((AudioPlayer) findAppObj(_appid, _uuid_));
                _ret = String.valueOf(_player.getDuration());
            } else if ("getPosition".equals(_methodName)) {
                AudioPlayer _player = null;
                String _uuid_ = JSONUtil.getString(_args, 0);
                _player = ((AudioPlayer) findAppObj(_appid, _uuid_));
                _ret = String.valueOf(_player.getPosition());
            } else if ("CreatePlayer".equals(_methodName)) {
                String _uuid_ = JSONUtil.getString(_args, 0);
                String _path = JSONUtil.getString(_args, 1);
                AudioPlayer _player = AudioPlayer.createAudioPlayer(_path);
                _player.mUuid = _uuid_;
                _player.mWebview = pWebViewImpl;
                putAppObjList(pWebViewImpl.obtainFrameView().obtainApp().obtainAppId(), _player);
            }
        } else {
            MessageHandler.sendMessage(this, new Object[]{pWebViewImpl, pActionName, pJsArgs});
        }
        return _ret;
    }

    private Object findAppObj(String mAppid, String pUuid) {
        ArrayList _array = getAppObjList(mAppid);
        Object _ret = null;
        if (!_array.isEmpty()) {
            for (Object _obj : _array) {
                if (_obj instanceof AbsAudio) {
                    if ((((AbsAudio) _obj).mUuid).equals(pUuid)) {
                        _ret = _obj;
                        break;
                    }
                }
            }
        }
        return _ret;
    }

    private ArrayList getAppObjList(String mAppid) {
        ArrayList _array = mAppsAudioObj.get(mAppid);
        if (_array == null) {
            _array = new ArrayList(2);
            mAppsAudioObj.put(mAppid, _array);
        }
        return _array;
    }

    private void putAppObjList(String mAppid, Object mAppObj) {
        ArrayList _array = getAppObjList(mAppid);
        _array.add(mAppObj);
    }

    private void removeAppObjFromList(String mAppid, Object mAppObj) {
        ArrayList _array = getAppObjList(mAppid);
        if (_array != null) {
            _array.remove(mAppObj);
        }
    }

    @Override
    public void init(AbsMgr pFeatureMgr, String pFeatureName) {
        mAppsAudioObj = new HashMap<String, ArrayList>(2);
    }

    @Override
    public void dispose(String pAppid) {
    }

    @Override
    public void execute(Object pArgs) {
        Object[] _params = (Object[]) pArgs;
        IWebview pWebViewImpl = (IWebview) _params[0];
        String pActionName = String.valueOf(_params[1]);//暂不使用此变量
        String[] pJsArgs = (String[]) _params[2];
        IApp _app = pWebViewImpl.obtainFrameView().obtainApp();
        String _appid = _app.obtainAppId();
        String _methodName = pJsArgs[0];//函数名字
        JSONArray _args = JSONUtil.createJSONArray(pJsArgs[1]);
        String _uuid = JSONUtil.getString(_args, 0);
        if ("RecorderExecMethod".equals(pActionName)) {//录音实现
            try {
                if ("record".equals(_methodName)) {
                    String _funId = JSONUtil.getString(_args, 1);
                    JSONObject _recordOption = JSONUtil.getJSONObject(_args, 2);
                    RecordOption _option = new RecordOption(pWebViewImpl, _recordOption);
                    if (JSUtil.checkOperateDirErrorAndCallback(pWebViewImpl, _funId, _option.mFileName)) {
                        return;
                    }
                    AudioRecorder _recorder = AudioRecorder.startRecorder(_option, _funId);
                    _recorder.mUuid = _uuid;
                    putAppObjList(_appid, _recorder);
                } else if ("pause".equals(_methodName)) {
                    ((AudioRecorder) findAppObj(_appid, _uuid)).pause();
                } else if ("stop".equals(_methodName)) {
                    AudioRecorder ar = ((AudioRecorder) findAppObj(_appid, _uuid));
                    ar.stop();
                    ar.successCallback();
                    removeAppObjFromList(_appid, ar);
                }
            } catch (Exception e) {
                Logger.e("RecorderExecMethod _methodName=" + _methodName + "; e =" + e);
            }
        } else if ("AudioExecMethod".equals(pActionName)) {//player实现
            AudioPlayer _player = null;
            try {
                _player = ((AudioPlayer) findAppObj(_appid, _uuid));
                if ("play".equals(_methodName)) {
                    String _funId = JSONUtil.getString(_args, 1);
                    _player.mFunId = _funId;
                    _player.play();
                } else if ("pause".equals(_methodName)) {
                    _player.pause();
                } else if ("resume".equals(_methodName)) {
                    _player.resume();
                } else if ("stop".equals(_methodName)) {
                    _player.stop();
                    removeAppObjFromList(_appid, _player);
                } else if ("seekTo".equals(_methodName)) {
                    int position = 0;
                    try {
                        position = Integer.parseInt(JSONUtil.getString(_args, 1));//能转为int 说明传入的是整数
                        if (position > 0) {
                            _player.seekTo(position * 1000);
                        }
                    } catch (Exception e) {
                        try {
                            position = (int) (Double.parseDouble(JSONUtil.getString(_args, 1)) * 1000);//否则传入的是小数
                            if (position > 0) {
                                _player.seekTo(position);
                            }
                        } catch (Exception e2) {
                            //当传入值非法时候应该不做处理
                        }
                    }
                } else if ("setRoute".equals(_methodName)) {
                    Context context = pWebViewImpl.getContext();
                    AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
                    int aRoute = Integer.parseInt(JSONUtil.getString(_args, 1));
                    if (aRoute == 1) {//听筒
                        setSpeakerphoneOn(am, false);
                    } else {//喇叭
                        setSpeakerphoneOn(am, true);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                if (_player != null)
                    _player.failCallback(DOMException.CODE_PARAMETER_ERRORP, DOMException.MSG_PARAMETER_ERROR);
            }
        }
    }

    /**
     * plus.audio.ROUTE_EARPIECE  1
     * plus.audio.ROUTE_SPEAKER  0
     *
     * @param am
     * @param on
     */
    private void setSpeakerphoneOn(AudioManager am, boolean on) {
        if (on) {
            // 为true打开喇叭扩音器；
            am.setSpeakerphoneOn(true);
            am.setMode(AudioManager.STREAM_SYSTEM);
        } else {//为false关闭喇叭扩音器.
            am.setSpeakerphoneOn(false);//关闭扬声器
            am.setRouting(AudioManager.MODE_NORMAL, AudioManager.ROUTE_EARPIECE, AudioManager.ROUTE_ALL);
            am.setMode(AudioManager.MODE_IN_COMMUNICATION);
        }
    }

}

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
            mMediaPlayer.setOnPreparedListener(new OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    Logger.d(AudioFeatureImpl.TAG, "onPrepared ");
                    isPrepared = true;
                    mMediaPlayer.start();
                }
            });
            mMediaPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener() {

                @Override
                public void onSeekComplete(MediaPlayer mp) {
                    // TODO Auto-generated method stub
                    Logger.d(AudioFeatureImpl.TAG, "onSeekComplete ");
                }
            });
            mMediaPlayer.setOnVideoSizeChangedListener(new OnVideoSizeChangedListener() {
                @Override
                public void onVideoSizeChanged(MediaPlayer mp, int width, int height) {
                    // TODO Auto-generated method stub
                    Logger.d(AudioFeatureImpl.TAG, "onVideoSizeChanged width=" + width + ";height=" + height);
                }
            });
            mMediaPlayer.setOnInfoListener(new OnInfoListener() {

                @Override
                public boolean onInfo(MediaPlayer mp, int what, int extra) {
                    Logger.d(AudioFeatureImpl.TAG, "onInfo what=" + what + ";extra=" + extra);
                    return false;
                }
            });
            if (DeviceInfo.sDeviceSdkVer >= 16) {
                mMediaPlayer.setOnTimedTextListener(new OnTimedTextListener() {

                    @Override
                    public void onTimedText(MediaPlayer mp, TimedText text) {
                        // TODO Auto-generated method stub
                        Logger.d(AudioFeatureImpl.TAG, "onTimedText " + text);
                    }
                });
            }
            mMediaPlayer.setOnErrorListener(new OnErrorListener() {
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
            mMediaPlayer.setOnBufferingUpdateListener(new OnBufferingUpdateListener() {

                @Override
                public void onBufferingUpdate(MediaPlayer mp, int percent) {
                    // TODO Auto-generated method stub
                    Logger.d(AudioFeatureImpl.TAG, "onBufferingUpdate " + percent);
                }
            });
            mMediaPlayer.setOnCompletionListener(new OnCompletionListener() {
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

class AudioRecorder extends AbsAudio {
    String mFunId;//成功失败时回调functionid
    private static AudioRecorder mInstance;
    MediaRecorder mNativeRecorder;
    RecordOption mOption;

    private AudioRecorder() {
    }

	static AudioRecorder startRecorder(final RecordOption pOption,final String pFunId) {
		if (mInstance == null) {
			mInstance = new AudioRecorder();
		}
        mInstance.mOption = pOption;
        mInstance.mFunId = pFunId;
		PermissionUtil.usePermission(mInstance.mOption.mWebview.getActivity(), mInstance.mOption.mWebview.obtainApp().isStreamApp(), PermissionUtil.PMS_RECORD, new PermissionUtil.StreamPermissionRequest(mInstance.mOption.mWebview.obtainApp()) {
			@Override
			public void onGranted(String streamPerName) {
				mInstance.mNativeRecorder = new MediaRecorder();
				MediaRecorder mRecorder = mInstance.mNativeRecorder;
				try {
					mRecorder.reset();
					mRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);// 使用设备默认音源
					String _fullPath = pOption.mFileName;
					if (!DHFile.isExist(_fullPath)) {
						DHFile.createNewFile(DHFile.createFileHandler(_fullPath));
					}
					mRecorder.setOutputFile(_fullPath);
					try {
//				mRecorder.setAudioChannels(2);// 设置立体声
						// mRecorder.setAudioEncodingBitRate(bitRate);//设置比特率
						mRecorder.setAudioSamplingRate(pOption.mSamplingRate);
						if (pOption.mSamplingRate == 44100) {
							mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
							mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);// AMR_WB\AAC\AMR_NB\DEFAULT
						} else if (pOption.mSamplingRate == 16000) {
							mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_WB);
							mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_WB);// AMR_WB\ACC\AMR_NB\DEFAULT
						} else if (pOption.mSamplingRate == 8000) {
							mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
							mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);// AMR_WB\ACC\AMR_NB\DEFAULT
						} else {
							mRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);// AMR_WB\ACC\AMR_NB\DEFAULT
							mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT);// AMR_WB\ACC\AMR_NB\DEFAULT
						}
					} catch (Exception e) {//出现设置异常时，需要重新设置参数
						mRecorder.reset();
						mRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);// 使用设备默认音源
						mRecorder.setOutputFile(_fullPath);
						Logger.w("AudioRecorder.getRecorderInstence", e);
						if (PdrUtil.isEquals(mInstance.mOption.mFormat, "3gp")) {
							mRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
						} else if (DeviceInfo.sDeviceSdkVer >= 10) {
							mRecorder.setOutputFormat(MediaRecorder.OutputFormat.AMR_NB);
						} else {
							mRecorder.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT);
						}
						mRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB);
					}
					mRecorder.prepare();
					mRecorder.start();
				} catch (Exception e) {
					e.printStackTrace();
					mInstance.failCallback(e.getMessage());
					mInstance.stop();
				}
			}

			@Override
			public void onDenied(String streamPerName) {
				mInstance.failCallback(DOMException.MSG_NO_PERMISSION);
			}
		});
		return mInstance;
	}
	
	 void successCallback(){
		String _filePath = mOption.mFileName;
		_filePath = mOption.mWebview.obtainFrameView().obtainApp().convert2RelPath(_filePath);
		JSUtil.excCallbackSuccess(mOption.mWebview, AudioRecorder.this.mFunId, _filePath);
	}
	private void failCallback(String msg){
		String error_json = String.format(DOMException.JSON_ERROR_INFO,DOMException.CODE_RECORDER_ERROR,msg);
		JSUtil.excCallbackError(mOption.mWebview, AudioRecorder.this.mFunId, error_json,true);
	}
	public void pause(){
		
	}
	
	public void stop(){
		if(mInstance != null && mInstance.mNativeRecorder != null){
			mInstance.mNativeRecorder.stop();
			mInstance.mNativeRecorder.release();
			mInstance.mNativeRecorder = null;
			mInstance = null;
		}
	}
	
	static class RecordOption{
		String mFileName;
		int mSamplingRate;
		//录音输出格式类型：3gp/amr_nb/amr_wb,默认amr
		String mFormat;
		IWebview mWebview;
		RecordOption(IWebview pWebview,JSONObject pOption){
			mWebview = pWebview;
			mSamplingRate = PdrUtil.parseInt(JSONUtil.getString(pOption, StringConst.JSON_KEY_SAMPLERATE),8000);
			String filename = JSONUtil.getString(pOption, StringConst.JSON_KEY_FILENAME);
			mFormat = PdrUtil.isEquals(JSONUtil.getString(pOption, StringConst.JSON_KEY_FORMAT),"3gp") ? "3gp" : "amr";
			filename = PdrUtil.getDefaultPrivateDocPath(filename, mFormat);
			mFileName = mWebview.obtainFrameView().obtainApp().convert2AbsFullPath(mWebview.obtainFullUrl(),filename);
		}
	}
	
}

abstract class AbsAudio {
    String mUuid;
}

