package io.dcloud.feature.apsGt;

import android.content.Context;
import android.content.Intent;

import com.igexin.sdk.PushManager;

import io.dcloud.common.adapter.util.Logger;
import io.dcloud.feature.aps.NotificationReceiver;

public class GTNotificationReceiver extends NotificationReceiver{
    public GTNotificationReceiver() {
    }

    public GTNotificationReceiver(Context context) {
        super(context);
    }

    @Override
	public void onReceive(Context context, Intent intent)
	{
		try {
			String _action = intent.getAction();
			if(_action != null) {
				if (_action.equals(Intent.ACTION_BOOT_COMPLETED)) {
					Logger.d("ACTION_BOOT_COMPLETED:开机初始化.");
					PushManager.getInstance().registerPushIntentService(context.getApplicationContext(), GTNormalIntentService.class);
					PushManager.getInstance().initialize(context.getApplicationContext(), GTPushDevService.class);
				} else {
					super.onReceive(context, intent);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
