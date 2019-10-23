package io.dcloud.feature.apsGt;

import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.util.BaseInfo;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.feature.aps.APSFeatureImpl;
import io.dcloud.feature.aps.AbsPushService;
import io.dcloud.feature.aps.PushMessage;

import org.json.JSONObject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;

import com.igexin.sdk.PushConsts;

/**
 * <p>
 * Description:接收APS推送广播
 * </p>
 * 
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-3-15 上午9:52:36 created.
 * 
 *       <pre>
 * <p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-15 上午9:52:36
 * </pre>
 */
public class PushMessageReceiver extends BroadcastReceiver {

	@Override
	public void onReceive(Context context, Intent intent) {
		Bundle bundle = intent.getExtras();
		int action = 0;
		try {
			action = bundle.getInt(PushConsts.CMD_ACTION);
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		switch (action) {

		case PushConsts.GET_MSG_DATA:
			// 获取透传数据
			byte[] dataBase = bundle.getByteArray("payload");

			boolean isOnClick = false;
			if (dataBase != null) {
				String data = new String(dataBase);
				if (PdrUtil.isEmpty(data)) {// payload数据为空时，认为是消息中心点击进入的，需要触发click事件
					isOnClick = true;
				} else {// 获得的payload不为空时触发receive逻辑
					String appid = BaseInfo.sDefaultBootApp;
					isOnClick = false;
					PushMessage _pushMessage = new PushMessage(data,appid, getApplicationName(context));
					Log.d("GTPush", "Got Payload:" + data);
					boolean needPush = AbsPushService.getAutoNotification(context, appid, GTPushService.ID);
					if (needPush && _pushMessage.needCreateNotifcation()) {
						APSFeatureImpl.sendCreateNotificationBroadcast(context, appid, _pushMessage);
					}else if (!APSFeatureImpl.execScript(context,"receive", _pushMessage.toJSON())) {// 添加receive执行队列
						APSFeatureImpl.addNeedExecReceiveMessage(context,_pushMessage);
					}
					APSFeatureImpl.addPushMessage(context,appid, _pushMessage);
					
				}
			} else {
				isOnClick = true;
			}
			if (isOnClick) {// 触发click事件
//				Intent _intent = new Intent(APSFeatureImpl.CLILK_NOTIFICATION);
//				context.sendBroadcast(_intent);
			}
			break;
		case PushConsts.GET_CLIENTID:
			// 获取ClientID(CID)
			String clientid = bundle.getString("clientid");
			SharedPreferences _sp = context.getSharedPreferences(AbsPushService.CLIENTID + GTPushService.ID, Context.MODE_PRIVATE);
			Editor ed = _sp.edit();
			ed.putString(AbsPushService.PUSH_CLIENT_ID_NAME, clientid);
			ed.commit();
			Logger.e("PushMessageReceiver", "CLIENTID=" + clientid);
			break;
		default:
			break;
		}
	}

	public String getApplicationName(Context context) {
		PackageManager packageManager = null;
		ApplicationInfo applicationInfo = null;
		try {
			packageManager = context.getApplicationContext().getPackageManager();
			applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
		} catch (PackageManager.NameNotFoundException e) {
			applicationInfo = null;
		}
		String applicationName = (String) packageManager.getApplicationLabel(applicationInfo);
		return applicationName;
	}
}
