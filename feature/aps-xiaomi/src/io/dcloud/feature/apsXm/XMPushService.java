package io.dcloud.feature.apsXm;

import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.feature.aps.AbsPushService;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.xiaomi.mipush.sdk.MiPushClient;

/**
 * <p>Description:APS管理JS接口实现</p>
 *
 * @version 1.0
 * @author vrmlpad Email:langshaopeng@dcloud.io
 * @Date 2016-1-15 下午15:20:45 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: vrmlpad Email:langshaopeng@dcloud.io at 2016-1-15 下午15:20:45</pre>
 */
public class XMPushService extends AbsPushService{

	public static final String ID = "mipush";
	/** 是否正在注册小米Push */
	public static boolean IsRegisterPushing =false;
	@Override
	public void onStart(Context pContext, Bundle pSaveBundle,String[] pArgs) {
		id=ID;
		SharedPreferences _sp = pContext.getSharedPreferences(AbsPushService.CLIENTID + ID, Context.MODE_PRIVATE);
		clientid = _sp.getString(AbsPushService.PUSH_CLIENT_ID_NAME, clientid);
		try{
			appid = AndroidResources.getMetaValue("PUSH_APPID").substring(1);
			appkey = AndroidResources.getMetaValue("PUSH_APPKEY").substring(1);
			registerPush(pContext);
		}catch (Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public String getClientInfo(Context context) {
		if(clientid == null){
			clientid = MiPushClient.getRegId(context);
			if(null==clientid||"".equals(clientid.trim())){
				registerPush(context);
			}else{
				saveClientId(context);
			}
		}
		return super.getClientInfo(context);
	}

	/**
	 * 注册推送服务
	 * @param context
     */
	private void registerPush(Context context){
		if(!IsRegisterPushing){
			IsRegisterPushing =true;
			MiPushClient.registerPush(context, appid, appkey);
		}
	}
	
}
