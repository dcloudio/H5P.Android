package io.dcloud.feature.speech;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.IMgr.MgrEvent;
import io.dcloud.common.DHInterface.IMgr.MgrType;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.PdrUtil;

import java.util.HashMap;

import org.json.JSONObject;

import android.content.Context;

/**
 * 语音特征Feature实现类
 *
 * @version 1.0
 * @author yanglei Email:yanglei@dcloud.io
 * @Date 2013-5-30 下午06:00:11 created.
 * 
 * <br/>Create By: yanglei Email:yanglei@dcloud.io at 2013-5-30 下午06:00:11
 */
public class SpeechFeatureImpl implements IFeature {
	AbsSpeechEngine mSpeechEngine = null;
	HashMap<String, String> mSpeechMap = null;
	@Override
	public String execute(IWebview pWebViewImpl, String pActionName,
			String[] pJsArgs) {
		if("startRecognize".equals(pActionName)){//启动语音识别
			String callbackId = pJsArgs[0];
			JSONObject speechOption = JSONUtil.createJSONObject(pJsArgs[1]);
			JSONObject eventCallbackIds = JSONUtil.createJSONObject(pJsArgs[2]);
			String engine = JSONUtil.getString(speechOption, StringConst.JSON_KEY_ENGINE).toLowerCase();
			try {
				stopRecognize(false);
				String engine_value = mSpeechMap.get(engine);
				if(!PdrUtil.isEmpty(engine_value)){//指定engine语音识别引擎存在时，实例化对象
					mSpeechEngine = (AbsSpeechEngine)Class.forName(engine_value).newInstance();
					mSpeechEngine.init(pWebViewImpl.getActivity(), pWebViewImpl);
					mSpeechEngine.startRecognize(callbackId, speechOption, eventCallbackIds);
				}
			} catch (InstantiationException e) {
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				e.printStackTrace();
			} catch (Exception e){
				Logger.e("not found engine=" + engine);
			}
		}else if("stopRecognize".equals(pActionName)){
			stopRecognize(true);
		}
		return null;
	}
	
	@Override
	public void init(AbsMgr pFeatureMgr, String pFeatureName) {
		//获取基座自带的语音识别引擎
		mSpeechMap = (HashMap<String, String>) pFeatureMgr.processEvent(MgrType.FeatureMgr, MgrEvent.OBTAIN_FEATURE_EXT_HASHMAP, pFeatureName);
	}
	
	private void stopRecognize(boolean isExeOnEnd){
		if(mSpeechEngine != null){
			mSpeechEngine.stopRecognize(isExeOnEnd);
			mSpeechEngine = null;
		}
	}
	@Override
	public void dispose(String pAppid) {
		stopRecognize(false);
	}
}
