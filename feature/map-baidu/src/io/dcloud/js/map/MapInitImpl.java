package io.dcloud.js.map;

import io.dcloud.common.DHInterface.StandardFeature;
import io.dcloud.common.adapter.util.DeviceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.Bundle;

import com.baidu.mapapi.SDKInitializer;

public class MapInitImpl extends StandardFeature{
	
	public static boolean isKeyError = false;
	private Context context;

	@Override
	public void onStart(Context pContext, Bundle pSavedInstanceState,
			String[] pRuntimeArgs) {
		// TODO Auto-generated method stub
		context = pContext;
		if(!Build.CPU_ABI.equals("x86") && !Build.CPU_ABI.equals("x86_64")){
			SDKInitializer.initialize(getDPluginContext().getApplicationContext());
		}
		// 注册 SDK 广播监听者
		IntentFilter iFilter = new IntentFilter();
		iFilter.addAction(SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR);
		mReceiver = new SDKReceiver();
		pContext.registerReceiver(mReceiver, iFilter);
	}

	@Override
	public void onStop() {
		// TODO Auto-generated method stub
		if (mReceiver != null) {
			context.unregisterReceiver(mReceiver);
		}
	}

	@Override
	public void onPause() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onResume() {
		// TODO Auto-generated method stub
		
	}
	boolean isShowKeyDialog = false;
	private SDKReceiver mReceiver;
	public class SDKReceiver extends BroadcastReceiver {
		public void onReceive(Context context, Intent intent) {
			String _workType = DeviceInfo.getNetWorkType();
			if (!isShowKeyDialog && intent.getAction() == SDKInitializer.SDK_BROADTCAST_ACTION_STRING_PERMISSION_CHECK_ERROR
					&& !_workType.equals("1")) { // 屏蔽无网络问题
				isShowKeyDialog = true;
				isKeyError = true;
			}
		}
	}

}
