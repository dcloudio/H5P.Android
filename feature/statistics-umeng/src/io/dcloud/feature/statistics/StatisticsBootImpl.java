package io.dcloud.feature.statistics;

import io.dcloud.common.DHInterface.IBoot;
import io.dcloud.common.adapter.util.AndroidResources;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;

import com.umeng.analytics.AnalyticsConfig;
import com.umeng.analytics.MobclickAgent;

/**
 * <p>Description:友盟统计开机启动项</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-5-6 上午10:52:20 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-6 上午10:52:20</pre>
 */
public class StatisticsBootImpl implements IBoot {

	//http://dev.umeng.com/analytics/android-doc/integration#8   QQ 800083942
	
	private Context mContext;
	@Override
	public boolean onExecute(SysEventType pEventType, Object pArgs) {
		return false;
	}

	@Override
	public void onStart(Context pContext, Bundle pSavedInstanceState, String[] pRuntimeArgs) {
		mContext = pContext;
		String umkey = AndroidResources.getMetaValue("UMENG_APPKEY");
		String channel = AndroidResources.getMetaValue("UMENG_CHANNEL");
		if(TextUtils.isEmpty(umkey)){
			umkey = "55b1b625e0f55a138300449d";
		}
		if(TextUtils.isEmpty(channel)){
			channel = mContext.getPackageName().replace(".", "_");
		}
		MobclickAgent.UMAnalyticsConfig umc = new MobclickAgent.UMAnalyticsConfig(mContext,umkey,channel);
		MobclickAgent.startWithConfigure(umc);
		onResume();
	}

	@Override
	public void onStop() {
		
	}

	@Override
	public void onPause() {
		MobclickAgent.onPause(mContext);
	}

	@Override
	public void onResume() {
		MobclickAgent.onResume(mContext);
	}

}
