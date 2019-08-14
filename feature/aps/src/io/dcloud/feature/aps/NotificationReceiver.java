package io.dcloud.feature.aps;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationChannelGroup;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;

import com.nostra13.dcloudimageloader.core.ImageLoader;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;

import io.dcloud.RInformation;
import io.dcloud.common.adapter.io.DHFile;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.constant.DataInterface;
import io.dcloud.common.constant.IntentConst;

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
    public NotificationReceiver() {
    }

    private static final String LOCAL_PUSH_CHANNEL_ID = "DcloudChannelID";
    private static final String LOCAL_PUSH_GROUP_ID = "DcloudGroupID";
    public NotificationReceiver(Context context) {
        if (android.os.Build.VERSION.SDK_INT >= 26) {
            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.createNotificationChannelGroup(new NotificationChannelGroup(LOCAL_PUSH_GROUP_ID, "推送消息"));
            NotificationChannel channel = new NotificationChannel(LOCAL_PUSH_CHANNEL_ID,
                    "消息推送", NotificationManager.IMPORTANCE_DEFAULT);
            channel.enableLights(true);
            channel.setShowBadge(true);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
	public void onReceive(Context context, Intent intent) {
		sOnReceiver(context, intent);
	}

	public static void sOnReceiver(Context context, Intent intent){

		APSFeatureImpl.initNotification(context);
		NotificationManager _notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
		String _action = intent.getAction();
		if (APSFeatureImpl.CREATE_NOTIFICATION.equals(_action)) {
			boolean isNDR = APSFeatureImpl.isNeedDynamicsReceiver(context);
			String title = intent.getStringExtra("title");
			String message = intent.getStringExtra("content");
			int nId = intent.getIntExtra("nId", 0);
			long when = intent.getLongExtra("when", 0);
			String appid = intent.getStringExtra("appid");
			String icon = intent.getStringExtra("icon");
			String sound = intent.getStringExtra("sound");
			boolean isstreamapp = intent.getBooleanExtra("isstreamapp",false);
			Intent i = new Intent(APSFeatureImpl.CLILK_NOTIFICATION);
			if(isNDR) {
				i.setComponent(new ComponentName(context.getPackageName(), "io.dcloud.feature.aps.ApsActionService"));
			}
//			Bundle b = new Bundle(intent.getExtras());
			i.putExtras(intent.getExtras());
			// 创建一个Notification
			Notification notification ;
//			// PendingIntent
			PendingIntent contentIntent = null;
			if(isNDR) {
				contentIntent = PendingIntent.getService(context, nId, i,PendingIntent.FLAG_ONE_SHOT);
			} else {
				contentIntent = PendingIntent.getBroadcast(context, nId, i,PendingIntent.FLAG_ONE_SHOT);
			}

            if (android.os.Build.VERSION.SDK_INT >= 16){
				Notification.Builder builder;
				if (android.os.Build.VERSION.SDK_INT >= 26) {
					builder = new Notification.Builder(context,LOCAL_PUSH_CHANNEL_ID);
				} else {
					builder = new Notification.Builder(context);
				}
				Bitmap bitmap = null;
                try {
                    if(!TextUtils.isEmpty(icon) && DHFile.isExist(icon)) {
                        bitmap = BitmapFactory.decodeFile(icon);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if(bitmap == null && isstreamapp){
					String uri = DataInterface.getIconImageUrl(appid, context.getResources().getDisplayMetrics().widthPixels + "");
					bitmap = ImageLoader.getInstance().loadImageSync(uri);
				}
				if(bitmap != null) {
					builder.setLargeIcon(bitmap);
				}
				int id_small = RInformation.getInt(context,"drawable","push_small");
				if(id_small <= 0){
					builder.setSmallIcon(context.getApplicationInfo().icon); //设置图标
				}else{
					builder.setSmallIcon(id_small); //设置图标
				}
                int id = RInformation.getInt(context,"drawable","push");
				if(bitmap == null) {
					Bitmap largeBitmap;
					if(id <= 0){
						largeBitmap = BitmapFactory.decodeResource(context.getResources(),context.getApplicationInfo().icon);
					}else{
						largeBitmap = BitmapFactory.decodeResource(context.getResources(),id);
					}
					if (null !=largeBitmap)
						builder.setLargeIcon(largeBitmap);
				}
				builder.setContentTitle(title); //设置标题
				builder.setContentText(message); //消息内容
				builder.setWhen(when); //发送时间
				// 添加声音提示
				if("system".equals(sound)){
					builder.setDefaults(Notification.DEFAULT_SOUND); //设置默认的提示音，振动方式，灯光
				}
				builder.setAutoCancel(true);//打开程序后图标消失
				builder.setContentIntent(contentIntent);
				notification = builder.build();
			}else {
				notification = new Notification();
				notification.icon = context.getApplicationInfo().icon;
				// 添加声音提示
				if("system".equals(sound)){
					notification.defaults = Notification.DEFAULT_SOUND;
				}
				notification.flags = Notification.FLAG_AUTO_CANCEL;
				notification.when = when;
				Class clazz = notification.getClass();
				try {
					Method m2 = clazz.getDeclaredMethod("setLatestEventInfo", Context.class,CharSequence.class,CharSequence.class,PendingIntent.class);
					m2.invoke(notification, context, title,message, contentIntent);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			try {
				_notificationManager.notify(nId, notification);
			}catch (Exception e){
				e.printStackTrace();
			}
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
