package io.dcloud.feature.speech;

import java.util.HashMap;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.IMgr.MgrEvent;
import io.dcloud.common.DHInterface.IMgr.MgrType;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.PermissionUtil;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSUtil;

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
	private SpeechManager manager;
	@Override
	public String execute(final IWebview pWebViewImpl, final String pActionName,
						  final String[] pJsArgs) {
		PermissionUtil.usePermission(pWebViewImpl.getActivity(), pWebViewImpl.obtainApp().isStreamApp(),  PermissionUtil.PMS_RECORD, new PermissionUtil.StreamPermissionRequest(pWebViewImpl.obtainApp()) {
			@Override
			public void onGranted(String streamPerName) {
			    manager.execute(pWebViewImpl,pActionName,pJsArgs,mSpeechMap);
			}

			@Override
			public void onDenied(String streamPerName) {
				if("startRecognize".equals(pActionName)) {//启动语音识别
					String callbackId = pJsArgs[0];
					String msg =  String.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_RECORDER_ERROR, DOMException.MSG_NO_PERMISSION);
					JSUtil.execCallback(pWebViewImpl, callbackId, msg, JSUtil.ERROR, true, false);
				}
			}
		});

		return null;
	}
	
	@Override
	public void init(AbsMgr pFeatureMgr, String pFeatureName) {
		//获取基座自带的语音识别引擎
		mSpeechMap = (HashMap<String, String>) pFeatureMgr.processEvent(MgrType.FeatureMgr, MgrEvent.OBTAIN_FEATURE_EXT_HASHMAP, pFeatureName);
		manager = SpeechManager.getInstance();
	}

	@Override
	public void dispose(String pAppid) {
		manager.stopRecognize(false);
	}
}
