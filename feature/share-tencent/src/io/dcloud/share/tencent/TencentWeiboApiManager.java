package io.dcloud.share.tencent;

import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.ISysEventListener.SysEventType;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.adapter.util.DeviceInfo;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.PlatformUtil;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.StringUtil;
import io.dcloud.share.AbsWebviewClient;
import io.dcloud.share.IFShareApi;
import io.dcloud.share.ShareAuthorizeView;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import com.tencent.weibo.sdk.android.api.WeiboAPI;
import com.tencent.weibo.sdk.android.api.util.Util;
import com.tencent.weibo.sdk.android.component.ReAddActivity;
import com.tencent.weibo.sdk.android.component.sso.AuthHelper;
import com.tencent.weibo.sdk.android.component.sso.OnAuthListener;
import com.tencent.weibo.sdk.android.component.sso.WeiboToken;
import com.tencent.weibo.sdk.android.model.AccountModel;
import com.tencent.weibo.sdk.android.model.ModelResult;
import com.tencent.weibo.sdk.android.network.HttpCallback;

/**
 * <p>
 * Description:腾讯微博API管理者
 * </p>
 * 
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-5-27 下午4:22:56 created.
 * 
 *       <pre>
 * <p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-27 下午4:22:56
 * </pre>
 */
public class TencentWeiboApiManager implements IFShareApi {
	private static final String PACKAGENAME = "com.tencent.WBlog";

	private static final String TENCENTWEIBO_DES = "腾讯微博";
	public static final String TENCENTWEIBO_ID = "tencentweibo";

	private String APP_SECRET;
	private static String APP_KEY;
	private static String REDIRECT_URL;

	private static final int SEND_RESULT_CODE = 3;
	private static final int AUTHORIZE_RESULT_CODE = 4;

	private JSONObject mTencentWeibo;
	private String mAccessToken;
	private long mExpires_in;
    public static final String KEY_APP_SECRET = "appsecret";
    public static final String KEY_APP_KEY = "appkey";
    public static final String KEY_REDIRECT_URL = "redirect_uri";
    private static final String TAG = "TencentWeiboApiManager";
	/**
	 * 
	 * Description:初始化json对象
	 * 
	 * @throws JSONException
	 * 
	 *             <pre>
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-28 下午12:32:16
	 * </pre>
	 */
	private void initJsonObject(Context context) throws JSONException {
		mAccessToken = Util.getSharePersistent(context,"ACCESS_TOKEN");
		mTencentWeibo = new JSONObject();
		mTencentWeibo.put(StringConst.JSON_SHARE_ID, TENCENTWEIBO_ID);
		mTencentWeibo.put(StringConst.JSON_SHARE_DESCRIPTION, TENCENTWEIBO_DES);
		mTencentWeibo.put(StringConst.JSON_SHARE_AUTHENTICATED, !PdrUtil.isEmpty(mAccessToken));
		mTencentWeibo.put(StringConst.JSON_SHARE_ACCESSTOKEN, mAccessToken);
		mTencentWeibo.put(StringConst.JSON_SHARE_NATIVECLIENT, PlatformUtil.hasAppInstalled(context, PACKAGENAME));
	}

	@Override
	public void initConfig() {
		initData();
	}

	public void initData() {
        if (!PdrUtil.isEmpty(AndroidResources.getMetaValue("TENCENT_APPKEY"))){
            APP_KEY = AndroidResources.getMetaValue("TENCENT_APPKEY").substring(1);
        }
		REDIRECT_URL = AndroidResources.getMetaValue("TENCENT_REDIRECT_URI");
		APP_SECRET = AndroidResources.getMetaValue("TENCENT_SECRET");
	}

	/**
	 * 
	 * Description:获取json对象的字符串
	 * 
	 * @return
	 * 
	 *         <pre>
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-28 下午12:32:31
	 * </pre>
	 */
	@Override
	public String getJsonObject(IWebview pWebViewImpl) {
		String _json = null;
		try {
			initJsonObject(pWebViewImpl.getActivity());
			_json = mTencentWeibo.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return _json;
	}

	@Override
	public String getId() {
		return TENCENTWEIBO_ID;
	}

	WeiboAPI api;

	class MyHttpCallback implements HttpCallback {
		IWebview mWebViewImpl = null;
		MyHttpCallback(IWebview pWebViewImpl){
			mWebViewImpl = pWebViewImpl;
		}
		public void onResult(Object object) {
			ModelResult result = (ModelResult) object;
			if (result.isExpires()) {// 到期了，需要打开授权界面
				Logger.d("TXWBMgr", "token=" + mAccessToken + " Expires");
				api = null;// 需要重新初始化
				Util.clearSharePersistent(mWebViewImpl.getActivity());
				authorize(mWebViewImpl, null,null);
			} else if (SEND_CALLBACKID != null) {
				if (result.isSuccess()) {
					Logger.d("TXWBMgr", "send successful");
					JSUtil.execCallback(mWebViewImpl, SEND_CALLBACKID, "", JSUtil.OK, false, false);
				} else {
					Logger.d("TXWBMgr", "send failed");
					String msg = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString(result.getError_message(), "Share腾讯微博分享", result.getError_message(), mLink));
					JSUtil.execCallback(mWebViewImpl, SEND_CALLBACKID, msg, JSUtil.ERROR, true, false);
				}
				SEND_CALLBACKID = null;
				mShareMsg = null;
			}
		}
	};

	private String SEND_CALLBACKID = null;
	String mShareMsg = null;

	@Override
	public void send(IWebview pWebViewImpl, final String pCallbackId, String pShareMsg) {
		JSONObject msgJson = JSONUtil.createJSONObject(pShareMsg);
		mShareMsg = pShareMsg;
		String content = JSONUtil.getString(msgJson, "content");
		JSONObject geoJSON = JSONUtil.getJSONObject(msgJson, "geo");
		String lat = JSONUtil.getString(geoJSON, "latitude") ;
		String lon = JSONUtil.getString(geoJSON, "longitude") ;
		JSONArray pics = JSONUtil.getJSONArray(msgJson, "pictures");
		SEND_CALLBACKID = pCallbackId;
		boolean useActivty = true;
		Context mContext = pWebViewImpl.getActivity();
		if(useActivty){
			Intent i = new Intent(mContext, ReAddActivity.class);
			Bundle bundle = new Bundle();
			bundle.putString("content", content);
			String file = JSONUtil.getString(pics, 0);
			file = pWebViewImpl.obtainFrameView().obtainApp().convert2AbsFullPath(pWebViewImpl.obtainFullUrl(), file);
			bundle.putString("pic_url", file);
			i.putExtras(bundle);
			mContext.startActivity(i);
		}else{
			if (this.mAccessToken == null) {
				this.mAccessToken = Util.getSharePersistent(mContext, "ACCESS_TOKEN");
			}
			AccountModel account = new AccountModel(this.mAccessToken);
			this.api = new WeiboAPI(account);
			HttpCallback	mCallBack = new MyHttpCallback(pWebViewImpl);
			try {
				if(lon != null && lat != null){
					this.api.addWeibo(mContext, content, "json", Long.parseLong(lon), Long.parseLong(lat), 1, 0, mCallBack, null, 4);
				}else{
					this.api.addWeibo(mContext, content, "json", 0, 0, 1, 0, mCallBack, null, 4);
				}
			} catch (NumberFormatException e) {
				this.api.reAddWeibo(mContext, content, JSONUtil.getString(pics, 0),
						null, null, null, null, mCallBack, null, 4);
			}
		}
		
	}

	@Override
	public void forbid(IWebview pWebViewImpl) {
		Util.clearSharePersistent(pWebViewImpl.getActivity());
		mAccessToken = null;
	}

	public static final String AUTHORIZE_TEMPLATE = "{authenticated:%s,accessToken:'%s'}";
	private String mAuthorizeCallbackId;

	@Override
	public void authorize(final IWebview pWebViewImpl, final String pCallbackId,String options) {
		mAuthorizeCallbackId = pCallbackId;
		final Context context = pWebViewImpl.getActivity();
		this.mAccessToken = Util.getSharePersistent(context,"ACCESS_TOKEN");
		if(!PdrUtil.isEmpty(this.mAccessToken)){
			onAuthorizeEnd(pWebViewImpl,true, -1, null);
			return;
		}

        JSONObject jsonOptions=JSONUtil.createJSONObject(options);
        if(jsonOptions != null){
            APP_KEY = jsonOptions.optString(KEY_APP_KEY, APP_KEY);
            Logger.e(TAG, "authorize: appkey"+APP_KEY );
            APP_SECRET = jsonOptions.optString(KEY_APP_SECRET, APP_SECRET);
            Logger.e(TAG, "authorize: APP_SECRET"+APP_SECRET );
            REDIRECT_URL = jsonOptions.optString(KEY_REDIRECT_URL, REDIRECT_URL);
            Logger.e(TAG, "authorize: REDIRECT_URL"+REDIRECT_URL );
        }
        if(PdrUtil.isEmpty(APP_KEY)||PdrUtil.isEmpty(APP_SECRET)||PdrUtil.isEmpty(REDIRECT_URL)){
            String msg = StringUtil.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_BUSINESS_PARAMETER_HAS_NOT, DOMException.toString(DOMException.MSG_BUSINESS_PARAMETER_HAS_NOT));
            JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.ERROR, true, false);
            return;
        }
		long appid = Long.parseLong(APP_KEY);
		final String app_secket = APP_SECRET;
		// 注册当前应用的appid和appkeysec，并指定一个OnAuthListener
		// OnAuthListener在授权过程中实施监听
		AuthHelper.register(context, appid, app_secket, new OnAuthListener() {
			// 如果当前设备没有安装腾讯微博客户端，走这里
			@Override
			public void onWeiBoNotInstalled() {
				onNotFoundClient();
			}

			// 如果当前设备没安装指定版本的微博客户端，走这里
			@Override
			public void onWeiboVersionMisMatch() {
				onNotFoundClient();
			}

			private void onNotFoundClient() {
//				AuthHelper.unregister(context);
				final IApp _app = pWebViewImpl.obtainFrameView().obtainApp();
				_app.registerSysEventListener(new ISysEventListener() {
					@Override
					public boolean onExecute(SysEventType pEventType,
							Object pArgs) {
						Object[] _args = (Object[]) pArgs;
						int requestCode = (Integer) _args[0];
						int resultCode = (Integer) _args[1];
						Intent data = (Intent) _args[2];
						boolean suc = false;
						if (requestCode == AUTHORIZE_RESULT_CODE) {
							if (resultCode == 0) {
								suc = true;
								mAccessToken = data
										.getStringExtra("ACCESS_TOKEN");
							}
						}
						onAuthorizeEnd(pWebViewImpl,suc, -1, null);
						_app.unregisterSysEventListener(this,
								SysEventType.onActivityResult);
						return false;
					}
				}, SysEventType.onActivityResult);
				Intent i = new Intent(context, WebAuthorize.class);
                i.putExtra(KEY_APP_KEY,APP_KEY);
                i.putExtra(KEY_APP_SECRET,APP_SECRET);
                i.putExtra(KEY_REDIRECT_URL,REDIRECT_URL);
				pWebViewImpl.getActivity().startActivityForResult(i, AUTHORIZE_RESULT_CODE);
			}

			// 如果授权失败，走这里
			@Override
			public void onAuthFail(int result, String err) {
				Toast.makeText(context.getApplicationContext(), "result : " + result, 1000).show();
//				AuthHelper.unregister(context);
				onAuthorizeEnd(pWebViewImpl,false, DOMException.CODE_BUSINESS_INTERNAL_ERROR,
						DOMException.toString(err, "Share腾讯微博分享", err, mLink));
			}

			// 授权成功，走这里
			// 授权成功后，所有的授权信息是存放在WeiboToken对象里面的，可以根据具体的使用场景，将授权信息存放到自己期望的位置，
			// 在这里，存放到了applicationcontext中
			@Override
			public void onAuthPassed(String name, WeiboToken token) {
				// 客户端 授权成功
				mAccessToken = token.accessToken;
				Context context = DeviceInfo.sApplicationContext;
				Util.saveSharePersistent(context, "ACCESS_TOKEN", 
						token.accessToken);
				Util.saveSharePersistent(context, "EXPIRES_IN",
						String.valueOf(token.expiresIn));
				Util.saveSharePersistent(context, "OPEN_ID", token.openID);
				// Util.saveSharePersistent(context, "OPEN_KEY", token.omasKey);
				Util.saveSharePersistent(context, "REFRESH_TOKEN", token.refreshToken);
				// Util.saveSharePersistent(context, "NAME", name);
				// Util.saveSharePersistent(context, "NICK", name);
				Util.saveSharePersistent(context, "CLIENT_ID",  APP_KEY);
				Util.saveSharePersistent(context, "AUTHORIZETIME",
						String.valueOf(System.currentTimeMillis() / 1000l));
//				AuthHelper.unregister(context);
				onAuthorizeEnd(pWebViewImpl,true, -1, null);
			}
		});

		AuthHelper.auth(context, REDIRECT_URL);
	}

	private void onAuthorizeEnd( IWebview pWebViewImpl, boolean suc, int errorCode, String errorMsg) {
		if (mAuthorizeCallbackId != null) {
			if (suc) {
				String msg = String.format(AUTHORIZE_TEMPLATE, true, mAccessToken);
				JSUtil.execCallback(pWebViewImpl, mAuthorizeCallbackId, msg, JSUtil.OK, true, false);
			} else {
				String msg = StringUtil.format(DOMException.JSON_ERROR_INFO, errorCode, errorMsg);
				JSUtil.execCallback(pWebViewImpl, mAuthorizeCallbackId, msg, JSUtil.ERROR, true, false);
			}
			mAuthorizeCallbackId = null;
		} else if (SEND_CALLBACKID != null) {
			send(pWebViewImpl, SEND_CALLBACKID, mShareMsg);
		}
	}
	
	public static AbsWebviewClient getWebviewClient(ShareAuthorizeView pView){
		return new TencentWebviewClient(pView);
	}

	@Override
	public void dispose() {
		if(null != api){
			api = null;
		}
	}
}
