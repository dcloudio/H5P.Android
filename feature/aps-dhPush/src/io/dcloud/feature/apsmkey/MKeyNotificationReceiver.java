package io.dcloud.feature.apsmkey;

import io.dcloud.common.adapter.util.Logger;
import io.dcloud.feature.aps.NotificationReceiver;
import android.content.Context;
import android.content.Intent;

import com.dheaven.push.MKeyManager;

/**
 * <p>Description:通知管理类</p>
 * 处理通知的点击、创建、清除
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-3-18 下午2:34:01 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-18 下午2:34:01</pre>
 */
public class MKeyNotificationReceiver extends NotificationReceiver {
	@Override
	public void onReceive(Context context, Intent intent) {
		String _action = intent.getAction();
		if(_action.equals(Intent.ACTION_BOOT_COMPLETED)){
			Logger.d("ACTION_BOOT_COMPLETED:开机初始化.");
			PushMessageReceiver.setContext(context);
			MKeyManager.initPush(context, PushMessageReceiver.getInstance());
		}else{
			super.onReceive(context, intent);
		}
	}

}
