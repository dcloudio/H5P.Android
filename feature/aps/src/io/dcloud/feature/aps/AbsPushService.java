package io.dcloud.feature.aps;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import io.dcloud.common.DHInterface.BaseFeature.BaseModule;
import io.dcloud.common.DHInterface.IReflectAble;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.MessageHandler;
import io.dcloud.common.adapter.util.MessageHandler.IMessages;
import io.dcloud.common.util.BaseInfo;
import io.dcloud.common.util.JSUtil;

public abstract class AbsPushService extends BaseModule implements IReflectAble{

	public static String PUSH_DB_NAME = "push_db_name";
	public static String PUSH_CLIENT_ID_NAME = "clientid";
	public static String PUSH_DB_DEFAULT_AUTO_NOTIFICATION = "auto_notification";
	
	public static final String CLIENTID = "clientid_";
	public String id = null;
	public String clientid = null;
	public String appid = null;
	public String appkey = null;
	public String appsecret = null;
	public static String CLIENT_INFO_TEMPLATE = "{id:'%s',token:'%s',clientid:'%s',appid:'%s',appkey:'%s'}";
	@Override
	public JSONObject toJSONObject() throws JSONException {
		return null;
	}

	public void onStart(Context pContext, Bundle pSaveBundle,String[] pArgs) {
		
	}
	public void onStop() {
		
	}

    @Override
    public void init(Context context) {
        super.init(context);
        initClientId(context);
    }
	public String getClientInfo(Context context){
		String json = String.format(CLIENT_INFO_TEMPLATE, id,clientid, clientid, appid, appkey);
		return JSUtil.wrapJsVar(json, false);
	}
	public void saveClientId(Context context){
		SharedPreferences _sp = context.getSharedPreferences(CLIENTID + id, Context.MODE_PRIVATE);
		Editor ed = _sp.edit();
		ed.putString(AbsPushService.PUSH_CLIENT_ID_NAME, clientid);
		ed.commit();
	}
	public void initClientId(Context context){
		SharedPreferences _sp = context.getSharedPreferences(CLIENTID+id, Context.MODE_PRIVATE);
		clientid = _sp.getString(AbsPushService.PUSH_CLIENT_ID_NAME, clientid);
	}
	public boolean setAutoNotification(IWebview pWebViewImpl, JSONArray pJsArgs,
			String _appId) throws JSONException {
		boolean needPush = Boolean.parseBoolean(pJsArgs.getString(0));
		Context context = pWebViewImpl.getContext();
		SharedPreferences sp = context.getSharedPreferences(PUSH_DB_NAME, Context.MODE_PRIVATE);
		Editor editor = sp.edit();// 获取编辑器
		if(isUseAppid(context)){
			editor.putBoolean(_appId + id, needPush);
		}else{
			editor.putBoolean(PUSH_DB_DEFAULT_AUTO_NOTIFICATION, needPush);
		}
		editor.commit();// 提交修改
		return needPush;
	}
	/**
	 * 获得对应应用使用推送服务是否自动显示通知消息
	 * @param context
	 * @param appid
	 * @param id 推送服务id(个推，MKey Push,Qihoo Push)
	 * @return
	 */
	public static boolean getAutoNotification(Context context,String appid,String id){
		SharedPreferences sp = context.getSharedPreferences(AbsPushService.PUSH_DB_NAME, Context.MODE_PRIVATE);
		boolean needPush = true;
		if(AbsPushService.isUseAppid(context)){
			needPush = sp.getBoolean(appid + id, needPush);
		}else{
			needPush = sp.getBoolean(PUSH_DB_DEFAULT_AUTO_NOTIFICATION, needPush);
		}
		return needPush;
	}

	/**当平台型应用时使用appid作为标示，并且要求推送信息中添加有appid字段*/
	public static boolean isUseAppid(Context context){
		return BaseInfo.isForQihooHelper(context);
	}
	public void remove(Context context, JSONArray pJsArgs, String appId)
			throws JSONException {
		PushManager pushManager = PushManager.getInstance(context);
		PushMessage _pushMsg = pushManager.findPushMessage(appId, pJsArgs.getString(0));
		Intent _intent = new Intent(APSFeatureImpl.REMOVE_NOTIFICATION);
		_intent.putExtra("id", _pushMsg.nID);
		if(BaseInfo.isForQihooHelper(context)){//插件包，为了仅使用一个service，亦支持监听click、create等push事件action
			context.startService(_intent);
		}else{
			context.sendBroadcast(_intent);
		}
		pushManager.removePushMessage(appId, _pushMsg);
	}
	public void addEventListener(Context context, IWebview pWebViewImpl,JSONArray pJsArgs)
			throws JSONException {
		PushManager pushManager = PushManager.getInstance(context);
		String evtType = pJsArgs.getString(2);
		ArrayList<String> _callbacks = pushManager.findWebViewCallbacks(pWebViewImpl, evtType);
		String callBackId = pJsArgs.getString(1);
		_callbacks.add(callBackId);
		pushManager.dispatchEvent(pWebViewImpl, callBackId, evtType);
	}
	public void clear(Context context, String _appId) {
		Intent _intent = new Intent(APSFeatureImpl.CLEAR_NOTIFICATION);
		_intent.putExtra("_appId", _appId);
		if(BaseInfo.isForQihooHelper(context)){//插件包，为了仅使用一个service，亦支持监听click、create等push事件action
			context.startService(_intent);
		}else{
			context.sendBroadcast(_intent);
		}
	}
	public String createMessage(IWebview pWebViewImpl, JSONArray pJsArgs,
			final String _appId,final Context _context) throws JSONException {
		final PushManager pushManager = PushManager.getInstance(_context);
		final PushMessage _message = new PushMessage(pJsArgs.getString(0), pWebViewImpl.obtainApp());
		if(_message.mDelay == 0){
			
			pushManager.addPushMessage(_appId, _message);
			pushManager.sendCreateNotificationBroadcast(_context, _appId, _message);
		}else{
			MessageHandler.sendMessage(new IMessages() {
				@Override
				public void execute(Object pArgs) {
					pushManager.addPushMessage(_appId, _message);
					pushManager.sendCreateNotificationBroadcast(_context, _appId, _message);
				}
			},_message.mDelay * 1000, null);
		}
		return JSUtil.wrapJsVar(_message.mUUID);
	}
	public boolean doHandleAction(String action) {
		return false;
	}
	public void onReceiver(Intent intent) {
		
	}
}
