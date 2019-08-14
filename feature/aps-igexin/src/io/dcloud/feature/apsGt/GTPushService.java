package io.dcloud.feature.apsGt;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.igexin.sdk.PushManager;

import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.feature.aps.AbsPushService;

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
public class GTPushService extends AbsPushService{

	public static final String ID = "igexin";
//    private GTNotificationReceiver mNotificationReceiver;

	@Override
	public void onStart(Context pContext, Bundle pSaveBundle,String[] pArgs) {
		id=ID;
		PushManager.getInstance().initialize(pContext.getApplicationContext(),null);
        // GTNormalIntentService 为第三方自定义的推送服务事件接收类
        PushManager.getInstance().registerPushIntentService(pContext.getApplicationContext(),GTNormalIntentService.class);
        SharedPreferences _sp = pContext.getSharedPreferences(AbsPushService.CLIENTID + ID, Context.MODE_PRIVATE);
		clientid = _sp.getString(AbsPushService.PUSH_CLIENT_ID_NAME, clientid);
		appid = AndroidResources.getMetaValue("PUSH_APPID");
		appkey = AndroidResources.getMetaValue("PUSH_APPKEY");
		appsecret = AndroidResources.getMetaValue("PUSH_APPSECRET");

		//单独注册个推的通知
//        mNotificationReceiver = new GTNotificationReceiver(mApplicationContext);
//        IntentFilter mFilter = new IntentFilter();
//        mFilter.addAction(APSFeatureImpl.CLILK_NOTIFICATION);
//        mFilter.addAction(APSFeatureImpl.CLEAR_NOTIFICATION);
//        mFilter.addAction(APSFeatureImpl.REMOVE_NOTIFICATION);
//        mFilter.addAction(APSFeatureImpl.CREATE_NOTIFICATION);
//        mApplicationContext.registerReceiver(mNotificationReceiver, mFilter);
	}

	@Override
	public String getClientInfo(Context context) {
		if(clientid == null){
			clientid = PushManager.getInstance().getClientid(context);
            saveClientId(context);
		}
		return super.getClientInfo(context);
	}

    @Override
    public void onStop() {
        super.onStop();
//        mApplicationContext.unregisterReceiver(mNotificationReceiver);
    }

    //	@Override
//	public boolean setAutoNotification(IWebview pWebViewImpl, JSONArray pJsArgs,
//			String _appId) throws JSONException {
//		boolean needPush = super.setAutoNotification(pWebViewImpl, pJsArgs, _appId);
//		if(needPush){
//			PushManager.getInstance().turnOnPush(pWebViewImpl.getContext());
//		}else{
//			PushManager.getInstance().turnOffPush(pWebViewImpl.getContext());
//		}
//		return needPush;
//	}
}
