package io.dcloud.feature.speech;

import android.content.Context;

import org.json.JSONObject;

import io.dcloud.common.DHInterface.IReflectAble;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;

/**
 * 语音识别抽象类
 *
 * @version 1.0
 * @author yanglei Email:yanglei@dcloud.io
 * @Date 2013-5-30 下午05:58:24 created.
 * 
 * <br/>Create By: yanglei Email:yanglei@dcloud.io at 2013-5-30 下午05:58:24
 */
public abstract class AbsSpeechEngine implements IReflectAble{
	public static final int ERROR_CODE_USER_STOP = 61001;//用户停止，在调用stopRecognize接口时触发
	public static final int ERROR_CODE_DEVICE_NO_SUPPORT = 61002;//此设备不支持语音识别
	public static final int ERROR_CODE_IN_USE = 61003;//调用麦克风设备错误，如设备被其它程序占用
	public static final int ERROR_CODE_PARAM_WRONG = 61004;//语音识别引擎参数错误
	public static final int ERROR_CODE_GRAMMAR = 61005;//语音识别引擎语法错误
	public static final int ERROR_CODE_INNER_WRONG = 61006;//语音识别引擎内部错误
	public static final int ERROR_CODE_DONT_RECOGNISE = 61007;//语音识别引擎无法识别
	public static final int ERROR_CODE_NETWORK = 61008;//网络问题引起的错误
	public static final int ERROR_CODE_UNKNOWN = 61009;//其它未定义的错误
	protected Context mContext;
	protected IWebview mWebview;
	//识别成功失败时回调的functionid
	private String mCallbackId;
	//事件回调所需的functionid
	private JSONObject mEventCallbackIds;
	protected ISpeechListener mListener;
	
	public void init(Context context,IWebview pWebview){
		mContext = context;
		mWebview = pWebview;
		setSpeechListener(mSpeechListener);
	}
	
	private final void setSpeechListener(ISpeechListener listener){
		mListener = listener;
	}
	/**
	 * @param pCallbackId 成功失败时候的回调所需functionid
	 * @param pOption 语音解析引擎需要设置参数
	 * @param eventCallbackIds
	 * <br/>Create By: yanglei Email:yanglei@dcloud.io at 2013-5-22 下午04:18:40
	 */
	final void startRecognize(String pCallbackId,JSONObject pOption,JSONObject eventCallbackIds){
		mCallbackId = pCallbackId;
		mEventCallbackIds = eventCallbackIds;
		startRecognize(pOption);
	}
	
	/**
	 * 启动语音解析引擎
	 * @param pOption 语音解析引擎需要设置参数
	 * <br/>Create By: yanglei Email:yanglei@dcloud.io at 2013-5-22 下午05:03:54
	 */
	public abstract void startRecognize(JSONObject pOption);
	/**
	 * 停止语音解析引擎
	 * @param isExeOnEnd 是否执行onend事件
	 * <br/>Create By: yanglei Email:yanglei@dcloud.io at 2013-5-22 下午05:04:40
	 */
	public abstract void stopRecognize(boolean isExeOnEnd);
	/**
	 * 定义语音回调接口
	 */
	ISpeechListener mSpeechListener = new ISpeechListener() {
		
		@Override
		public void onStateChange(byte state, Object args, boolean keepCallback) {
			switch (state) {
			case ISpeechListener.ONSUCCESS:{
				//String str_args = JSUtil.arrayList2JsStringArray((ArrayList<String>)args);
				//JSUtil.execCallback(mWebview, mCallbackId, (String)args, JSUtil.OK, true, keepCallback);
                Logger.e("Success",(String)args);
				JSUtil.execCallback(mWebview, mCallbackId, (String)args, JSUtil.OK, false, keepCallback);
				break;
			}
			case ISpeechListener.ONERROR:{
				String[] objs = (String[])args;
				int error_code = Integer.parseInt(objs[0]);
				String error_msg = String.valueOf(objs[1]);
				String msg =  String.format(DOMException.JSON_ERROR_INFO,error_code,error_msg);
				//msg = JSONUtil.toJSONableString(msg);
				JSUtil.execCallback(mWebview, mCallbackId, msg, JSUtil.ERROR, true, keepCallback);
				break;
			}
			case ISpeechListener.ONSTART:{
				String callbackId = JSONUtil.getString(mEventCallbackIds, StringConst.JSON_KEY_ONSTART);
				JSUtil.execCallback(mWebview, callbackId, "", JSUtil.OK, false, keepCallback);
				break;
			}
			case ISpeechListener.ONEND:{
				String callbackId = JSONUtil.getString(mEventCallbackIds, StringConst.JSON_KEY_ONEND);
				JSUtil.execCallback(mWebview, callbackId, "", JSUtil.OK, false, keepCallback);
				break;
			}
			default:
				break;
			}
		}
	};
}
