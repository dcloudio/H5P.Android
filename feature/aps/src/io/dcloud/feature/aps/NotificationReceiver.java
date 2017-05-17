package io.dcloud.feature.aps;

import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.constant.IntentConst;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

/**
 * <p>
 * Description:通知管理类
 * </p>
 * 处理通知的点击、创建、清除
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-3-18 下午2:34:01 created.
 * 
 *       <pre>
 * <p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-18 下午2:34:01
 * </pre>
 */
public class NotificationReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		sOnReceiver(context, intent);
	}

	public static void sOnReceiver(Context context, Intent intent){

		APSFeatureImpl.initNotification(context);
		NotificationManager _notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		String _action = intent.getAction();
		if (APSFeatureImpl.CREATE_NOTIFICATION.equals(_action)) {
			String title = intent.getStringExtra("title");
			String message = intent.getStringExtra("content");
			int nId = intent.getIntExtra("nId", 0);
			long when = intent.getLongExtra("when", 0);
//			String appid = intent.getStringExtra("appid");
//			String uuid = intent.getStringExtra("uuid");
			String sound = intent.getStringExtra("sound");
			Intent i = new Intent(APSFeatureImpl.CLILK_NOTIFICATION);
//			Bundle b = new Bundle(intent.getExtras());
			i.putExtras(intent.getExtras());
			// 创建一个Notification
			Notification notification = new Notification();
			notification.icon = context.getApplicationInfo().icon;
			// 添加声音提示
			if("system".equals(sound)){
				notification.defaults = Notification.DEFAULT_SOUND;
			}
			notification.flags = Notification.FLAG_AUTO_CANCEL;
			notification.when = when;
			// PendingIntent
			PendingIntent contentIntent = PendingIntent.getBroadcast(context, nId, i, PendingIntent.FLAG_ONE_SHOT);
			notification.setLatestEventInfo(context, title, message, contentIntent);
			_notificationManager.notify(nId, notification);
		} else if (APSFeatureImpl.REMOVE_NOTIFICATION.equals(_action)) {
			int _id = intent.getIntExtra("id", 0);
			_notificationManager.cancel(_id);
		} else if (APSFeatureImpl.CLEAR_NOTIFICATION.equals(_action)) {
			_notificationManager.cancelAll();
			String _appid = intent.getStringExtra("_appId");
			HashMap<String, ArrayList<PushMessage>> appMsg = PushManager.getInstance(context).mAppsmMessages;
			appMsg.remove(_appid);
		} else if (APSFeatureImpl.CLILK_NOTIFICATION.equals(_action)) {
			clickHandle(context, intent, _notificationManager);
			String packagename = context.getPackageName();// 启动类所在包名
			PackageManager pm = context.getPackageManager();
			Intent _intent = pm.getLaunchIntentForPackage(packagename);
//			if(BaseInfo.useStreamApp(context)){//流应用时候需要将appid作为参数，启动时候能直接进入到对应的应用
				String appid = intent.getStringExtra("appid");
				_intent.putExtra("appid", appid);
//			}
			boolean isStartWeb = intent.getBooleanExtra(IntentConst.IS_START_FIRST_WEB, false);
			if (isStartWeb) {
				_intent.putExtra(IntentConst.IS_START_FIRST_WEB, isStartWeb);
				_intent.putExtra(IntentConst.FIRST_WEB_URL, intent.getStringExtra(IntentConst.FIRST_WEB_URL));
			}
			_intent.putExtra(IntentConst.START_FROM,IntentConst.START_FROM_PUSH);
			_intent.putExtra(IntentConst.PUSH_PAYLOAD, intent.getStringExtra("payload"));
			_intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(_intent);
		}
	}
	
	/**
	 * 当点击了状态栏中消息是执行此方法
	 * @param context
	 * @param intent
	 * @param _notificationManager
	 */
	public static void clickHandle(Context context,Intent intent,NotificationManager _notificationManager){
		PushManager pushMgr = PushManager.getInstance(context);
		Bundle _bundle = intent.getExtras();
		String appid = _bundle.getString("appid");
		String uuid = _bundle.getString("uuid");
		if(_notificationManager != null){//作为插件时，手助负责创建通知栏消息
			int _id = intent.getIntExtra("id", 0);
			_notificationManager.cancel(_id);
		}
		PushMessage _pushMessage = pushMgr.findPushMessage(appid, uuid);
		if (_pushMessage != null) {
			boolean isStartWeb = false;
			if (!TextUtils.isEmpty(_pushMessage.mPayload)) {
				try {
					JSONObject payLoadJson = new JSONObject(_pushMessage.mPayload);
					String url = payLoadJson.optString("__adurl");
					if (!TextUtils.isEmpty(url)) {
						intent.putExtra(IntentConst.IS_START_FIRST_WEB, true);
						intent.putExtra(IntentConst.FIRST_WEB_URL, url);
						isStartWeb = true;
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (!isStartWeb && !pushMgr.execScript("click", _pushMessage.toJSON())) {
				pushMgr.addNeedExecMessage(_pushMessage);
				Logger.d("addNeedExecMessage:");
			}
			// 点击后的消息，需要移除消息记录，避免getAllMessage时不正确
			pushMgr.removePushMessage(appid, _pushMessage);
		}else{
			_pushMessage = new PushMessage(_bundle);
			if (!TextUtils.isEmpty(_pushMessage.mPayload)) {
				try {
					JSONObject payLoadJson = new JSONObject(_pushMessage.mPayload);
					String url = payLoadJson.optString("__adurl");
					if (!TextUtils.isEmpty(url)) {
						intent.putExtra(IntentConst.IS_START_FIRST_WEB, true);
						intent.putExtra(IntentConst.FIRST_WEB_URL, url);
					}
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			pushMgr.addNeedExecMessage(_pushMessage);
		}
		
 		_bundle.clear();
 		
	}
	
}
