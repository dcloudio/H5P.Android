package io.dcloud.feature.speech;

import android.content.Context;
import android.content.DialogInterface;
import android.os.Build;

import com.iflytek.cloud.InitListener;
import com.iflytek.cloud.RecognizerResult;
import com.iflytek.cloud.SpeechConstant;
import com.iflytek.cloud.SpeechError;
import com.iflytek.cloud.SpeechUtility;
import com.iflytek.cloud.ui.RecognizerDialog;
import com.iflytek.cloud.ui.RecognizerDialogListener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.util.HashMap;
import java.util.LinkedHashMap;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.DialogUtil;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.feature.speech.ifly.R;

public class IflySpeechEngine extends AbsSpeechEngine {
    private static final String TAG = "IflySpeechEngine";

    static String sIflyAppid;// = "5177d8fe";
    MyRecognizerDialog mIsrDialog = null;

    public void init(Context context, IWebview pWebview) {
        super.init(context, pWebview);
        if(isApply()) {
            sIflyAppid = AndroidResources.getMetaValue("IFLY_APPKEY");
            if (!PdrUtil.isEmpty(sIflyAppid)) {
                SpeechUtility.createUtility(context, SpeechConstant.APPID + "=" + sIflyAppid);
            }
        } else {
            DialogUtil.showDialog(pWebview.getActivity(), null, context.getString(R.string.sp_ifly_error_tips), new String[]{null});
        }
    }

    public boolean isApply() {
        if(Build.CPU_ABI.equalsIgnoreCase("x86")) {
            return false;
        }
        return true;
    }


    public void startRecognize(JSONObject pOption) {
        if(isApply()) {
            mIsrDialog = new MyRecognizerDialog(mContext, null);
            mIsrDialog.startRecognize(pOption);
        }
    }

    public void stopRecognize(boolean isExeOnEnd) {
        if (mIsrDialog != null) {
            mIsrDialog.stopRecognize(isExeOnEnd);
            mIsrDialog = null;
        }
    }

    class MyRecognizerDialog extends RecognizerDialog {


        public MyRecognizerDialog(Context arg0, InitListener initListener) {
            super(arg0, initListener);
        }

        void startRecognize(JSONObject pOption) {
            String mlang = "zh_cn";
            String mtimeout = "4000";
            String mpunctuation = "1";
            String maccent = null;
            if (!PdrUtil.isEmpty(pOption)) {
                String lang = JSONUtil.getString(pOption, StringConst.JSON_KEY_LANG);
                if (!PdrUtil.isEmpty(lang)) {
                    lang = lang.toLowerCase();
                    if ("zh-cantonese".equals(lang)) {
                        mlang = "zh_cn";
                        maccent = "cantonese";
                    } else if ("zh-henanese".equals(lang)) {
                        mlang = "zh_cn";
                        maccent = "henanese";
                    } else if ("zh-cn".equals(lang)) {
                        mlang = "zh_cn";
                    } else if ("en-us".equals(lang)) {
                        mlang = "en_us";
                    }
                }
                int itimeout = JSONUtil.getInt(pOption, StringConst.JSON_KEY_TIMEOUT);
                if (0 != itimeout) {
                    mtimeout = String.valueOf(itimeout);
                }
                if (pOption == null || !pOption.has(StringConst.JSON_KEY_PUNCTUATION)) {
                    mpunctuation = "1";
                } else if (!JSONUtil.getBoolean(pOption, StringConst.JSON_KEY_PUNCTUATION)) {
                    mpunctuation = "0";
                }


            }
            mIatResults.clear();
            //设置语言类型
            setParameter(SpeechConstant.LANGUAGE, mlang);
            setParameter(SpeechConstant.ACCENT, maccent);
            // 设置语音时长
            setParameter(SpeechConstant.KEY_SPEECH_TIMEOUT, mtimeout);

            // 设置标点符号,设置为"0"返回结果无标点,设置为"1"返回结果有标点
            setParameter(SpeechConstant.ASR_PTT, mpunctuation);

            setListener(recognizeListener);
            setOnDismissListener(dismissListener);
            setCanceledOnTouchOutside(false);
            show();
            mListener.onStateChange(ISpeechListener.ONSTART, null, false);
        }

        void stopRecognize(boolean isExeOnEnd) {
            if (isExeOnEnd) {
                mListener.onStateChange(ISpeechListener.ONEND, null, false);
            }
            cancel();
            destroy();
        }

        //语音框取消事件监听
        OnDismissListener dismissListener = new OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                mListener.onStateChange(ISpeechListener.ONEND, null, false);
            }
        };


        RecognizerDialogListener recognizeListener = new RecognizerDialogListener() {
            @Override
            public void onResult(RecognizerResult recognizerResult, boolean b) {
                String parsedresult = printResult(recognizerResult);
                if (b) {
                    Logger.e(TAG, "onResult parsedresult==" + parsedresult);
                    mListener.onStateChange(ISpeechListener.ONSUCCESS, parsedresult, false);
                }
            }

            @Override
            public void onError(SpeechError error) {
                if (error != null) {
                    mListener.onStateChange(ISpeechListener.ONERROR, new String[]{String.valueOf(error.getErrorCode()), error.getErrorDescription()}, false);
                }
            }


        };

        // 用HashMap存储听写结果
        private HashMap<String, String> mIatResults = new LinkedHashMap<String, String>();

        /**
         * 将结果翻译为字符串
         *
         * @param results
         * @return
         */

        private String printResult(RecognizerResult results) {
            String text = parseIatResult(results.getResultString());
            Logger.e(TAG, "text==" + text);
            String sn = null;
            // 读取json结果中的sn字段
            try {
                JSONObject resultJson = new JSONObject(results.getResultString());
                sn = resultJson.optString("sn");
            } catch (JSONException e) {
                e.printStackTrace();
            }

            mIatResults.put(sn, text);

            StringBuffer resultBuffer = new StringBuffer();
            for (String key : mIatResults.keySet()) {
                Logger.e(TAG, "mIatResults.get(key)" + mIatResults.get(key));
                resultBuffer.append(mIatResults.get(key));
            }
            Logger.e(TAG, "resultBuffer.toString()==" + resultBuffer.toString());
            return resultBuffer.toString();
        }

        public String parseIatResult(String json) {
            StringBuffer ret = new StringBuffer();
            try {
                JSONTokener tokener = new JSONTokener(json);
                JSONObject joResult = new JSONObject(tokener);

                JSONArray words = joResult.getJSONArray("ws");
                for (int i = 0; i < words.length(); i++) {
                    // 转写结果词，默认使用第一个结果
                    JSONArray items = words.getJSONObject(i).getJSONArray("cw");
                    JSONObject obj = items.getJSONObject(0);
                    ret.append(obj.getString("w"));
//				如果需要多候选结果，解析数组其他字段
//				for(int j = 0; j < items.length(); j++)
//				{
//					JSONObject obj = items.getJSONObject(j);
//					ret.append(obj.getString("w"));
//				}
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return ret.toString();
        }
    }

}
