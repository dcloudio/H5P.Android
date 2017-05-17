package io.dcloud.feature.aps;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.BaseFeature;
import io.dcloud.common.DHInterface.IWebview;

import java.util.ArrayList;

import org.json.JSONArray;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * <p>
 * Description:APS管理JS接口实现
 * </p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-3-13 上午11:25:45 created.
 * 
 *       <pre>
 * <p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-13 上午11:25:45
 * </pre>
 */
public class APSFeatureImpl extends BaseFeature  {

	public static String PRE = null;
	/**
	 * 创建通知
	 */
	public static final String F_CREATE_NOTIFICATION = "__CREATE_NOTIFICATION";
	/**
	 * 清除指定ID通知
	 */
	public static final String F_REMOVE_NOTIFICATION = "__REMOVE_NOTIFICATION";
	/**
	 * 清除所有通知
	 */
	public static final String F_CLEAR_NOTIFICATION = "__CLEAR_NOTIFICATION";
	/**
	 * 点击通知处理
	 */
	public static final String F_CLILK_NOTIFICATION = "__CLILK_NOTIFICATION";
	
	/**
	 * 创建通知
	 */
	public static String CREATE_NOTIFICATION = "__CREATE_NOTIFICATION";
	/**
	 * 清除指定ID通知
	 */
	public static String REMOVE_NOTIFICATION = "__REMOVE_NOTIFICATION";
	/**
	 * 清除所有通知
	 */
	public static String CLEAR_NOTIFICATION = "__CLEAR_NOTIFICATION";
	/**
	 * 点击通知处理
	 */
	public static String CLILK_NOTIFICATION = "__CLILK_NOTIFICATION";
	
	protected PushManager mPushManager;

	@Override
	public String execute(IWebview pWebViewImpl, String pActionName, JSONArray pJsArgs) {
		try {
			return mPushManager.execute(pWebViewImpl, pActionName, pJsArgs,this);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	@Override
	public void init(AbsMgr pFeatureMgr, String pFeatureName) {
		super.init(pFeatureMgr, pFeatureName);
		Context context = pFeatureMgr.getContext();
		mPushManager = PushManager.getInstance(context);
		initNotification(context);
	}

	public static void initNotification(Context context){
		if(PRE == null){
			PRE = context.getPackageName();
		}
		if (!CREATE_NOTIFICATION.startsWith(PRE)) {
			CREATE_NOTIFICATION = PRE + "." + F_CREATE_NOTIFICATION;
			REMOVE_NOTIFICATION = PRE + "." + F_REMOVE_NOTIFICATION;
			CLEAR_NOTIFICATION = PRE + "." + F_CLEAR_NOTIFICATION;
			CLILK_NOTIFICATION = PRE + "." + F_CLILK_NOTIFICATION;
		}
	}
	@Override
	public void onStart(Context pContext, Bundle pSavedInstanceState,
			String[] pRuntimeArgs) {
		super.onStart(pContext, pSavedInstanceState, pRuntimeArgs);
		ArrayList<BaseModule> modules =  loadModules();
		if(modules != null && !modules.isEmpty()){
			for(BaseModule bm : modules){
				((AbsPushService)bm).onStart(pContext, pSavedInstanceState, pRuntimeArgs);
			}
		}
	}
	@Override
	public void onReceiver(Intent intent) {
		super.onReceiver(intent);
		ArrayList<BaseModule> modules =  loadModules();
		if(modules != null && !modules.isEmpty()){
			for(BaseModule bm : modules){
				((AbsPushService)bm).onReceiver(intent);
			}
		}
	}
	@Override
	public void dispose(String pAppid) {
		ArrayList<BaseModule> modules =  loadModules();
		if(modules != null && !modules.isEmpty()){
			for(BaseModule bm : modules){
				((AbsPushService)bm).onStop();
			}
		}
	}

	public static void addPushMessage(Context context, String appid,
			PushMessage _pushMessage) {
		PushManager mgr = PushManager.getInstance(context);
		mgr.addPushMessage(appid, _pushMessage);
	}

	public static boolean execScript(Context context, String pEventType, String pMessage) {
		PushManager mgr = PushManager.getInstance(context);
		return mgr.execScript(pEventType, pMessage);
	}

	public static void sendCreateNotificationBroadcast(Context context,
			String appid, PushMessage pushMessage) {
		PushManager mgr = PushManager.getInstance(context);
		mgr.sendCreateNotificationBroadcast(context, appid, pushMessage);
	}

	public static void addNeedExecReceiveMessage(Context context,
			PushMessage pushMessage) {
		PushManager mgr = PushManager.getInstance(context);
		mgr.addNeedExecReceiveMessage(pushMessage);
	}
	public static void addNeedExecMessage(Context context,
			PushMessage pushMessage) {
		PushManager.getInstance(context).addNeedExecMessage(pushMessage);
	}
}
