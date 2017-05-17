package io.dcloud.feature.apsmkey;

import io.dcloud.common.util.BaseInfo;
import io.dcloud.feature.aps.APSFeatureImpl;
import io.dcloud.feature.aps.AbsPushService;
import io.dcloud.feature.aps.PushMessage;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.util.Log;

import com.dheaven.push.MKeyManager;
import com.dheaven.push.MKeyMessage;
import com.dheaven.push.MKeyMessageLtr;

/**
 * <p>Description:接收APS推送广播</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-3-15 上午9:52:36 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-15 上午9:52:36</pre>
 */
public class PushMessageReceiver implements MKeyMessageLtr {
	
	private static Context context;
	private static PushMessageReceiver mPushMessageReceiver;
	
	protected static PushMessageReceiver getInstance(){
		if(mPushMessageReceiver == null){
			mPushMessageReceiver = new PushMessageReceiver();
		}
		return mPushMessageReceiver;
	}
	
	protected static void setContext(Context pContext){
		context = pContext;
	}
	
	private void parseMessage(MKeyMessage pMKeyMessage){
		if (pMKeyMessage != null) {
			try {
				JSONObject jsobj = new JSONObject(pMKeyMessage.getContent());
				String	appid = jsobj.optString("appid");
				if(appid == null || appid == ""){
					appid = BaseInfo.sDefaultBootApp;
				}
				String jsonStr = "{'message':'%s','payload':'%s'}";
				PushMessage _pushMessage = new PushMessage(String.format(jsonStr,pMKeyMessage.getContent(),pMKeyMessage.getAction()),pMKeyMessage.getID(),pMKeyMessage.getTitle());
				Log.d("PushMessageReceiver", "Got Payload:" + pMKeyMessage.getContent());
				boolean needPush = AbsPushService.getAutoNotification(context, appid, MkeyPushService.ID);
				if(needPush){
					Intent _intent = new Intent(APSFeatureImpl.CREATE_NOTIFICATION);
					_intent.putExtra("appid", appid);
					_intent.putExtra("title", _pushMessage.mTitle);
					_intent.putExtra("content", _pushMessage.mContent);
					_intent.putExtra("nId", _pushMessage.nID);
					_intent.putExtra("uuid", _pushMessage.mUUID);
					context.sendBroadcast(_intent);
				}
				APSFeatureImpl.addPushMessage(context,appid, _pushMessage);
				APSFeatureImpl.execScript(context,"receive", _pushMessage.toJSON());
			} catch (JSONException e) {
				e.printStackTrace();
			}
			
		}
	}

	@Override
	public void onBombMessage() {
	}

	@Override
	public void onInitfinsh(String pToken) {
		// 获取ClientID(CID)
		SharedPreferences _sp = context.getSharedPreferences(AbsPushService.CLIENTID + MkeyPushService.ID, Context.MODE_PRIVATE);
		Editor ed = _sp.edit();
		ed.putString(AbsPushService.PUSH_CLIENT_ID_NAME, pToken);
		ed.commit();
	}

	@Override
	public void onClickMessage(Context pContext, MKeyMessage pMKeyMessage) {
		//启动程序
		String packagename = pContext.getPackageName();
		PackageManager pm = pContext.getPackageManager();
		Intent _intent = pm.getLaunchIntentForPackage(packagename);
		_intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_NEW_TASK);            
		pContext.startActivity(_intent);
		String jsonStr = "{'message':'%s','payload':'%s'}";
		PushMessage _pushMessage = new PushMessage(String.format(jsonStr,pMKeyMessage.getContent(),pMKeyMessage.getAction()),pMKeyMessage.getID(),pMKeyMessage.getTitle());
		//触发JS中通知点击监听者
		APSFeatureImpl.execScript(context,"click", _pushMessage.toJSON());
	}

	@Override
	public void onPushMessage(MKeyMessage pMKeyMessage) {
		String type = pMKeyMessage.getType();
		//判断是否为普通消息 普通消息直接使用默认方式推上通知栏
		if(type != null && type.equals("msg")){
			//使用默认的方式推上通知栏
			MKeyManager.messageToNotification(pMKeyMessage, false);			
		}else{
			parseMessage(pMKeyMessage);
		}		
	}
}

