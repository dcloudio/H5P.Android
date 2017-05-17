package io.dcloud.feature.apsXm;

import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.feature.aps.NotificationReceiver;
import android.content.Context;
import android.content.Intent;
import com.xiaomi.mipush.sdk.MiPushClient;

public class XMNotificationReceiver extends NotificationReceiver{
	@Override
	public void onReceive(Context context, Intent intent)
	{
		String _action = intent.getAction();
		if(_action.equals(Intent.ACTION_BOOT_COMPLETED)){
			try{
				String appid = AndroidResources.getMetaValue("PUSH_APPID").substring(1);
				String appkey = AndroidResources.getMetaValue("PUSH_APPKEY").substring(1);
				MiPushClient.registerPush(context, appid,appkey);
			}catch (Exception e ){
				e.printStackTrace();
			}
		}else{
			super.onReceive(context, intent);
		}
	}
}
