package io.dcloud.feature.audio;

import android.content.Context;
import android.media.AudioManager;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.MessageHandler;
import io.dcloud.common.adapter.util.MessageHandler.IMessages;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.feature.audio.recorder.RecordOption;


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
                    AudioRecorderMgr _recorder = AudioRecorderMgr.startRecorder(_option, _funId);
                    _recorder.mUuid = _uuid;
                    putAppObjList(_appid, _recorder);
                } else if ("pause".equals(_methodName)) {
                    ((AudioRecorderMgr) findAppObj(_appid, _uuid)).pause();
                } else if ("stop".equals(_methodName)) {
                    AudioRecorderMgr ar = ((AudioRecorderMgr) findAppObj(_appid, _uuid));
                    ar.stop();
                    ar.successCallback();
                    removeAppObjFromList(_appid, ar);
                } else if("resume".equals(_methodName)) {
                    ((AudioRecorderMgr) findAppObj(_appid, _uuid)).resume();
                }
            } catch (Exception e) {
                e.printStackTrace();
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



