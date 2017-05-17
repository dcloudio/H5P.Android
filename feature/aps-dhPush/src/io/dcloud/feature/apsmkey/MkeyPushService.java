package io.dcloud.feature.apsmkey;

import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.feature.aps.AbsPushService;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.dheaven.push.MKeyManager;

/**
 * <p>Description:APS管理JS接口实现</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-3-13 上午11:25:45 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-13 上午11:25:45</pre>
 */
public class MkeyPushService extends AbsPushService{

	public static final String ID = "mkey";
	@Override
	public String getClientInfo(Context context) {
		if(clientid == null){
			initClientId(context);
		}
		return super.getClientInfo(context);
	}
	
	@Override
	public void onStart(Context pContext, Bundle pSaveBundle, String[] pArgs) {
		super.onStart(pContext, pSaveBundle, pArgs);
		id="mkeypush";
		PushMessageReceiver.setContext(pContext);
		MKeyManager.initPush(pContext, PushMessageReceiver.getInstance());
		SharedPreferences _sp = pContext.getSharedPreferences(AbsPushService.CLIENTID + MkeyPushService.ID, Context.MODE_PRIVATE);
		clientid = _sp.getString(AbsPushService.PUSH_CLIENT_ID_NAME, clientid);
		appid = AndroidResources.getMetaValue("DHPUSH_APPID");
		appkey = "undefined";
		appsecret = "undefined";
	}
}
