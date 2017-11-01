package io.dcloud.feature.aps;

import io.dcloud.common.DHInterface.BaseFeature;
import io.dcloud.common.DHInterface.BaseFeature.BaseModule;
import io.dcloud.common.DHInterface.IEventCallback;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.ui.AdaFrameItem;
import io.dcloud.common.adapter.ui.AdaFrameView;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.BaseInfo;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;

/**
 * <p>
 * Description:消息管理者
 * </p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-3-13 下午2:52:47 created.
 * 
 *       <pre>
 * <p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-13 下午2:52:47
 * </pre>
 */
class PushManager {
	
	/**
	 * 根据APPID保存消息集合
	 */
	public HashMap<String, ArrayList<PushMessage>> mAppsmMessages;
	/**
	 * 推送管理者对象
	 */
	protected static PushManager mPushManager;
	/**
	 * 所有页面的集合
	 */
	protected HashMap<IWebview, HashMap<String, ArrayList<String>>> mWebViewCallbackIds;
	/** 未被通知到js层的消息集合，需要添加了click listener时立即执行 */
	protected ArrayList<PushMessage> mNeedExecMessages;
	/** 未被通知到js层的消息集合，需要添加了receive listener时立即执行 */
	protected ArrayList<PushMessage> mNeedExecMessages_receive;

	private AbsPushService mBaseAbsPushService = null;
	APSFeatureImpl apsFeatureImpl;
	/**
	 * 
	 * Description:获取PushManager对象
	 * 
	 * @param context
	 *            TODO
	 * @return
	 *
	 *         <pre>
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-18 下午5:16:45
	 * </pre>
	 */
	public static PushManager getInstance(Context context) {
		// IFeature m_CurrentPushPlugin;
		APSFeatureImpl.initNotification(context);
		if (mPushManager == null) {
			mPushManager = new PushManager();
		}
		return mPushManager;
	}
	/**
	 * 
	 * Description: 构造函数
	 *
	 * <pre>
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-18 下午5:17:11
	 * </pre>
	 */
	protected PushManager() {
		mAppsmMessages = new HashMap<String, ArrayList<PushMessage>>();
		mWebViewCallbackIds = new HashMap<IWebview, HashMap<String, ArrayList<String>>>();
		mNeedExecMessages = new ArrayList<PushMessage>();
		mNeedExecMessages_receive = new ArrayList<PushMessage>();
	}

	public void sendCreateNotificationBroadcast(Context _context, String _appId, PushMessage _message) {
		Intent _intent = new Intent(APSFeatureImpl.CREATE_NOTIFICATION);
		_intent.putExtras(_message.toBundle());
		if(BaseInfo.isForQihooHelper(_context)){//插件包，为了仅使用一个service，亦支持监听click、create等push事件action
			_intent.setClassName(_context.getPackageName(), "io.dcloud.streamdownload.DownloadService");
			_context.startService(_intent);
		}else{
			_context.sendBroadcast(_intent);
		}
	}

	public String execute(IWebview pWebViewImpl, String pActionName, JSONArray pJsArgs,BaseFeature baseFeature) throws Exception{

		String _ret = null;
		String _appId = pWebViewImpl.obtainFrameView().obtainApp().obtainAppId();
		final Context _context = pWebViewImpl.getActivity();
		ArrayList<BaseModule> list = baseFeature.loadModules();
		AbsPushService channel = null;
		boolean isStreamBase = BaseInfo.isStreamApp(_context);
		if(!list.isEmpty()){
			channel = (AbsPushService)baseFeature.loadModules().get(0);
		}else{
			if(mBaseAbsPushService == null){
				mBaseAbsPushService = new AbsPushService() {
					@Override
					public JSONObject toJSONObject() throws JSONException {
						return super.toJSONObject();
					}
				};
				IntentFilter mFilter = new IntentFilter();
				mFilter.addAction(APSFeatureImpl.CLILK_NOTIFICATION);
				mFilter.addAction(APSFeatureImpl.CLEAR_NOTIFICATION);
				mFilter.addAction(APSFeatureImpl.REMOVE_NOTIFICATION);
				mFilter.addAction(APSFeatureImpl.CREATE_NOTIFICATION);
				final NotificationReceiver mNotificationReceiver = new NotificationReceiver();
				_context.registerReceiver(mNotificationReceiver,mFilter);
				pWebViewImpl.obtainApp().registerSysEventListener(new ISysEventListener() {
					@Override
					public boolean onExecute(SysEventType pEventType, Object pArgs) {
						if(pEventType == ISysEventListener.SysEventType.onWebAppStop){
							_context.unregisterReceiver(mNotificationReceiver);
						}
						return false;
					}
				}, ISysEventListener.SysEventType.onWebAppStop);
			}
			channel = mBaseAbsPushService;
		}
		if (pActionName.equals("getClientInfo")) {
			if(channel != null){
				_ret = channel.getClientInfo(pWebViewImpl.getContext());
			}else if(isStreamBase){
				String json = String.format(AbsPushService.CLIENT_INFO_TEMPLATE,"", "", "", "", "");
				_ret =  JSUtil.wrapJsVar(json, false);
			}
		} else if (pActionName.equals("createMessage")) {
			if(channel != null){
				_ret = channel.createMessage(pWebViewImpl, pJsArgs, _appId, _context);
			}
		} else if (pActionName.equals("clear")) {
			if(channel != null){
				channel.clear(_context, _appId);
			}
		} else if (pActionName.equals("addEventListener")) {
			if(channel != null){
				channel.addEventListener(_context, pWebViewImpl,pJsArgs);
			}
		} else if (pActionName.equals("remove")) {
			if(channel != null){
				channel.remove(_context, pJsArgs, _appId);
			}
		} else if (pActionName.equals("getAllMessage")) {
			_ret = getAllMessages(_appId);
		}else if(pActionName.equals("setAutoNotification")){
			if(channel != null){
				channel.setAutoNotification(pWebViewImpl, pJsArgs, _appId);
			}
		}
		return _ret;
	}
	
	protected static final String EVENT_TEMPLATE = "window.__Mkey__Push__.execCallback_Push('%s', '%s', %s);";

	protected void dispatchEvent(IWebview webview, String callBackId, String evtType) {
		if ("click".equals(evtType)) {
			if (!mNeedExecMessages.isEmpty()) {// 存在需要立即执行的消息
				for (PushMessage _message : mNeedExecMessages) {
					String _json = String.format(EVENT_TEMPLATE, callBackId, evtType, _message.toJSON());
					webview.executeScript(_json);
					mNeedExecMessages.remove(_message);
				}
			}
		} else if ("receive".equals(evtType)) {
			if (!mNeedExecMessages_receive.isEmpty()) {// 存在需要立即执行的消息
				for (PushMessage _message : mNeedExecMessages_receive) {
					String _json = String.format(EVENT_TEMPLATE, callBackId, evtType, _message.toJSON());
					webview.executeScript(_json);
					mNeedExecMessages_receive.remove(_message);
				}
			}
		}
	}

	/**
	 * 
	 * Description:添加消息对象
	 * 
	 * @param pAppid
	 *            应用ID
	 * @param pMsg
	 *            消息对象
	 *
	 *            <pre>
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-18 上午11:58:29
	 * </pre>
	 */
	public void addPushMessage(String pAppid, PushMessage pMsg) {
		if (pAppid == null) {
			pAppid = BaseInfo.PDR;
		}
		ArrayList<PushMessage> _arr = mAppsmMessages.get(pAppid);
		if (_arr == null) {
			_arr = new ArrayList<PushMessage>();
			mAppsmMessages.put(pAppid, _arr);
		}
		_arr.add(pMsg);
	}

	public void addNeedExecMessage(PushMessage pMsg) {
		if(null!=mNeedExecMessages&&0<mNeedExecMessages.size()){
			mNeedExecMessages.clear();
		}
		mNeedExecMessages.add(pMsg);
	}

	public void addNeedExecReceiveMessage(PushMessage pMsg) {
		mNeedExecMessages_receive.add(pMsg);
	}

	/**
	 * 
	 * Description:删除消息对象
	 * 
	 * @param appid
	 *            应用ID
	 * @param pMsg
	 *            消息对象
	 *
	 *            <pre>
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-18 上午11:58:29
	 * </pre>
	 */
	public void removePushMessage(String pAppid, PushMessage pPushMsg) {
		if (pAppid == null) {
			pAppid = BaseInfo.PDR;
		}
		ArrayList<PushMessage> _arr = mAppsmMessages.get(pAppid);
		if (_arr != null && _arr.contains(pPushMsg)) {
			_arr.remove(pPushMsg);
			Logger.d("push","removePushMessage" + _arr.size());
		}
	}

	/**
	 * 
	 * Description:查找消息对象
	 * 
	 * @param appid
	 * @param pUuid
	 * @return
	 *
	 *         <pre>
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-18 下午2:02:20
	 * </pre>
	 */
	public PushMessage findPushMessage(String pAppid, String pUuid) {
		if (pAppid == null) {
			pAppid = BaseInfo.PDR;
		}
		PushMessage _ret = null;
		ArrayList<PushMessage> _arr = mAppsmMessages.get(pAppid);
		if (_arr == null) {//若没有通过appid获取到消息集合，则通过uuid遍历所有消息
			Set<String> keys = mAppsmMessages.keySet();
			for(String key : keys) {
				_arr = mAppsmMessages.get(key);
				int count = _arr.size();
				for (int i = 0; i < count; i++) {
					PushMessage _pushMessage = _arr.get(i);
					if (pUuid.equals(_pushMessage.mUUID)) {
						_ret = _pushMessage;
						break;
					}
				}
			}
		}else  if (_arr != null) {
			int count = _arr.size();
			for (int i = 0; i < count; i++) {
				PushMessage _pushMessage = _arr.get(i);
				if (pUuid.equals(_pushMessage.mUUID)) {
					_ret = _pushMessage;
					break;
				}
			}
		}
		return _ret;
	}

	/**
	 * 
	 * Description:获取callback集合
	 * 
	 * @param pWebview
	 * @return
	 *
	 *         <pre>
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-21 下午4:45:55
	 * </pre>
	 */
	public ArrayList<String> findWebViewCallbacks(IWebview pWebview, String evtType) {
		ArrayList<String> _ret = null;
		HashMap<String, ArrayList<String>> _webEvtArr = mWebViewCallbackIds.get(pWebview);
		boolean has = false;
		if (_webEvtArr != null) {// 是否存在pWebview的事件集合
			_ret = _webEvtArr.get(evtType);
			if (_ret != null) {// 是否存在evtType事件的集合
				has = true;
				_ret = _webEvtArr.get(evtType);
				addWindowCloseListener(pWebview);
			} else {// 不存在evtType事件的集合
				_ret = new ArrayList<String>();
				_webEvtArr.put(evtType, _ret);
			}
		} else {// 不存在pWebview的事件集合
			_webEvtArr = new HashMap<String, ArrayList<String>>();
			_ret = new ArrayList<String>();
			_webEvtArr.put(evtType, _ret);
			addWindowCloseListener(pWebview);
			mWebViewCallbackIds.put(pWebview, _webEvtArr);
		}
		return _ret;
	}

	/**
	 * 移除指定webview注册的监听
	 * 
	 * @param pWebview
	 */
	public void removeWebviewCallback(IWebview pWebview) {
		mWebViewCallbackIds.remove(pWebview);
	}

	/**
	 * 添加指定webview销毁时候的监听
	 * 
	 * @param pWebview
	 */
	private void addWindowCloseListener(IWebview pWebview) {
		// 新的webview需要注册页面监听，当webview被关闭的时候需要清除webview相关的记录
		AdaFrameView frameView = (AdaFrameView) pWebview.obtainFrameView();
		frameView.addFrameViewListener(new IEventCallback() {
			@Override
			public Object onCallBack(String pEventType, Object pArgs) {
				if ((PdrUtil.isEquals(pEventType, StringConst.EVENTS_WINDOW_CLOSE) || PdrUtil.isEquals(pEventType, StringConst.EVENTS_CLOSE)) && pArgs instanceof IWebview) {
					removeWebviewCallback((IWebview) pArgs);
					((AdaFrameView) ((IWebview) pArgs).obtainFrameView()).removeFrameViewListener(this);
				}
				return null;
			}
		});
	}

	/**
	 * 
	 * Description:获取所有消息的集合的JSON字符
	 * 
	 * @param appid
	 * @return
	 *
	 *         <pre>
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-18 下午2:16:14
	 * </pre>
	 */
	protected String getAllMessages(String appid) {
		String _json = "(function(){var arr = new Array;%s;return arr;})();";
		ArrayList<PushMessage> _arr = mAppsmMessages.get(appid);
		StringBuffer _sb = new StringBuffer();
		if (_arr != null && _arr.size() > 0) {
			int count = _arr.size();
			for (int i = 0; i < count; i++) {
				_sb.append("arr[" + i + "]=");
				PushMessage _PushMessage = _arr.get(i);
				_sb.append(_PushMessage.toJSON());
				_sb.append(";");
			}
		}
		return String.format(_json, _sb.toString());
	}

	/**
	 * 
	 * Description:执行
	 * 
	 * @param pCallbackId
	 * @param pEventType
	 * @param pMeassage
	 *
	 *            <pre>
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-18 下午6:11:39
	 * </pre>
	 */
	public boolean execScript(String pEventType, String pMessage) {
		String _json = "window.__Mkey__Push__.execCallback_Push('%s', '%s', %s);";
		boolean _ret = false;
		for (IWebview _webView : mWebViewCallbackIds.keySet()) {
			if(((AdaFrameItem)_webView).isDisposed()) continue;
			ArrayList<String> _callbacks = mWebViewCallbackIds.get(_webView).get(pEventType);// 获得pEventType事件的集合
			if(_callbacks != null) {
				int length = _callbacks.size();
				String _callbackId = null;
				for (int i = length - 1; i >= 0; i--) {
					_callbackId = _callbacks.get(i);
					_json = String.format(_json, _callbackId, pEventType, pMessage);
					if (_callbackId.startsWith(pEventType)) {
						_webView.executeScript(_json);
						_ret = true;
					}
				}
			}
		}
		return _ret;
	}

}
