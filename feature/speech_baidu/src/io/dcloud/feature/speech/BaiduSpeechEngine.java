package io.dcloud.feature.speech;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.baidu.aip.core.recog.IStatus;
import io.dcloud.common.constant.StringConst;
import io.dcloud.feature.speech.dialog.BaiduSpeechDialog;

public class BaiduSpeechEngine extends AbsSpeechEngine {

    private BaiduSpeechDialog dialog;

    private boolean isContinue = false;
    private int nBest = 1;
    @Override
    public void startRecognize(JSONObject mOption) {
        dialog = new BaiduSpeechDialog(mContext);

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                BaiduSpeechEngine.this.dialog.stopRecog();
                mListener.onStateChange(ISpeechListener.ONEND, null, false);
            }
        });
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (!isContinue)
                    mListener.onStateChange(ISpeechListener.ONERROR, new String[]{"-10","语音识别失败"}, false);
            }
        });
        String lang = mOption.optString(StringConst.JSON_KEY_LANG,"zh-cn");
//        int timeout = mOption.optInt(StringConst.JSON_KEY_TIMEOUT,800); // VAD_ENDPOINT_TIMEOUT
        int timeout = 800;
        boolean punctuation = mOption.optBoolean(StringConst.JSON_KEY_PUNCTUATION,true);//标点符号
        isContinue = mOption.optBoolean(StringConst.JSON_KEY_CONTINUE,false);//持续模式 长语音
        boolean userInterface = mOption.optBoolean(StringConst.JSON_KEY_USERINTERFACE,true);//是否显示界面
        nBest = mOption.optInt(StringConst.JSON_KEY_NBEST,1);
        if (nBest<=0) {
            nBest = 1;
        }
        Map<String,Object> map = new HashMap<String, Object>();
        switch (lang) {
            case "zh-cn":
                if (punctuation) {
                    map.put("pid",15362);
                } else {
                    map.put("pid",1536);
                }
                break;
            case "en-us":
                map.put("pid",1737);
                break;
            case "zh-cantonese":
                map.put("pid",16372);
                break;
        }
        map.put("disable-punctuation",!punctuation);
        if (isContinue) {
            map.put("vad.endpoint-timeout",0);
        } else {
            map.put("vad.endpoint-timeout",timeout);
        }
        map.put("userInterface",userInterface);
        dialog.startRecog(map,mHandler);
        mListener.onStateChange(ISpeechListener.ONSTART, null, false);
    }

    @Override
    public void stopRecognize(boolean isExeOnEnd) {
        if(dialog.stopRecog())
            mListener.onStateChange(ISpeechListener.ONEND, null, false);
    }

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case IStatus.STATUS_VULUME:
                    int volume = dialog.setVolume((Integer) msg.obj);
                    mListener.onStateChange(ISpeechListener.VOLUME,volume,true);
                    break;
                case IStatus.STATUS_SPEAKING:
                    dialog.setVoiceText("正在聆听");
                    break;
                case IStatus.STATUS_READY:
                    break;
                case IStatus.STATUS_PARTICAL_RESULT:
                    dialog.setVoiceText((String) msg.obj);
                    mListener.onStateChange(ISpeechListener.PARTICALRESULT,msg.obj,true);
                    break;
                case IStatus.STATUS_FINISHED:
                    if (null != msg.obj && msg.obj instanceof String[]) {
                        String[] obj = (String[]) msg.obj;
                        dialog.setVoiceText(obj[0]);
                        if (obj.length > nBest){
                            obj = Arrays.copyOfRange(obj,0,nBest);
                        }
                        mListener.onStateChange(ISpeechListener.ONSUCCESS, obj, isContinue);
                        if (!isContinue) {
                            mHandler.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    dialog.dismiss();
                                }
                            },500);
                        }
                    }
                    break;
                case IStatus.STATUS_FINISH_ERROR:
                    try {
                        JSONObject object = new JSONObject((String) msg.obj);
                        mListener.onStateChange(ISpeechListener.ONERROR,new String[]{object.opt("subErrorCode")+"",(String) object.opt("descMessage")},false);
                    } catch (JSONException e) {
                    }
                    dialog.dismiss();
                    break;
            }
        }
    };
}
