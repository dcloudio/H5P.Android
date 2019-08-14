package io.dcloud.oauth.qihoosdk;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.dcloud.application.DCloudApplication;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.AESHelper;
import io.dcloud.feature.oauth.BaseOAuthService;
import io.dcloud.oauth.qihoosdk.LifeHelper.OnResult;


public class QihooOAuthService extends BaseOAuthService{

	private static final String KEY_APPKEY = "appkey";
	private static final String KEY_ACCESS_TOKEN = "access_token";
	@Override
	public void init(Context context) {
		super.init(context);
		id = "qihoo";
		description = "360账号登陆";
	}
	@Override
	public void login(IWebview pWebViewImpl,JSONArray pJsArgs) {
		super.login(pWebViewImpl, pJsArgs);
		String appKey = "08158bf9f09b919790a63f10c381be52";
		if(mLoginOptions != null && mLoginOptions.has(KEY_APPKEY)){
			appKey = mLoginOptions.optString(KEY_APPKEY);
		}
		LifeHelper.getInstance().login(pWebViewImpl,appKey, new OnResult() {
			@Override
			public void onResult(int resultCode, String resultMsg, JSONObject resultData){
				if(resultCode == 0 && resultData != null){
					saveValue(KEY_AUTHRESULT, resultData.toString());
					authResult = resultData;
					onLoginFinished(makeResultJSONObject(), true);
				}else{
					JSONObject sucJSON = new JSONObject();
					try {
						sucJSON.put(DOMException.CODE,DOMException.CODE_BUSINESS_INTERNAL_ERROR);
						sucJSON.put(DOMException.MESSAGE, "["+getFullDescription()+":"+resultCode+"]"+getDesc(resultCode) + resultMsg);
					} catch (JSONException e) {
						e.printStackTrace();
					}
					onLoginFinished(sucJSON, false);
				}
			}
		});
		
	}

	public JSONObject makeResultJSONObject(){
		JSONObject sucJSON = new JSONObject();
		try {
			sucJSON.put(BaseOAuthService.KEY_AUTHRESULT, authResult);
			sucJSON.put(BaseOAuthService.KEY_USERINFO, userInfo);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return sucJSON;
	}
	
	@Override
	public void logout(IWebview pWebViewImpl, JSONArray pJsArgs) {
		super.logout(pWebViewImpl, pJsArgs);
		QihooSDKHelper.self().logout();
		removeValue(KEY_AUTHRESULT);
		removeValue(BaseOAuthService.KEY_USERINFO);
		userInfo = null;
		authResult = null;
		boolean suc = false;
		JSONObject sucJSON = new JSONObject();
		try {
			MangeLogin.clear(pWebViewImpl.getContext());
			sucJSON.put(BaseOAuthService.KEY_AUTHRESULT, authResult);
			sucJSON.put(BaseOAuthService.KEY_USERINFO, userInfo);
			suc = true;
		} catch (JSONException e) {
			try {
				sucJSON.put(DOMException.CODE,DOMException.CODE_NOT_SUPPORT);
				sucJSON.put(DOMException.MESSAGE, DOMException.MSG_NOT_SUPPORT);
				sucJSON.put(DOMException.INNERCODE, "undefined");
			} catch (JSONException e1) {
				e1.printStackTrace();
			}
		}
		onLogoutFinished(sucJSON, suc);
	}
	@Override
	public void getUserInfo(IWebview pWebViewImpl, JSONArray pJsArgs) {
		super.getUserInfo(pWebViewImpl, pJsArgs);
//		new Thread(){
//			public void run() {
				//{qid=109536217, nick_name=yanggulei, head_pic=http://quc.qhimg.com/dm/48_48_100/t01b2cb0b55df63b319.jpg}
					// 调试用的appKey
					String authJSON = getValue(KEY_AUTHRESULT);
					try {
						JSONObject authObj = new JSONObject(authJSON);
						if(authJSON != null && authObj.has(KEY_ACCESS_TOKEN)){
							String access_token = authObj.optString(KEY_ACCESS_TOKEN);
							LifeHelper.getInstance().getUserInfo(pWebViewImpl,access_token, new OnResult() {
								@Override
								public void onResult(int resultCode, String resultMsg, JSONObject resultData){
									JSONObject sucJSON = new JSONObject();
									try {
										if(resultCode == 0){
											userInfo = resultData;
											saveValue(BaseOAuthService.KEY_USERINFO, userInfo.toString());
											sucJSON.put(BaseOAuthService.KEY_AUTHRESULT, authResult);
											sucJSON.put(BaseOAuthService.KEY_USERINFO, userInfo);
								 			onGetUserInfoFinished(sucJSON, true);
										}else{
											try {
												sucJSON.put(DOMException.CODE,DOMException.CODE_BUSINESS_INTERNAL_ERROR);
												sucJSON.put(DOMException.MESSAGE, "["+getFullDescription()+":"+resultCode+"]"+getDesc(resultCode) + resultMsg);
											} catch (JSONException e) {
												e.printStackTrace();
											}
											onGetUserInfoFinished(sucJSON, false);
										}
									} catch (JSONException e) {
									}
								}
							});
						}else{
							JSONObject sucJSON = new JSONObject();
							try {
								int resultCode = 401;
								sucJSON.put(DOMException.CODE,DOMException.CODE_BUSINESS_INTERNAL_ERROR);
								sucJSON.put(DOMException.MESSAGE, "["+getFullDescription()+":"+resultCode+"]"+getDesc(resultCode));
							} catch (JSONException e) {
								e.printStackTrace();
							}
							onGetUserInfoFinished(sucJSON, false);
						}
					} catch (JSONException e) {
						e.printStackTrace();
					}
//			};
//		}.start();
	}

	private String getDesc(int code){
		switch(code){
		case 400:
			return "请求数据不合法，或者超过请求频率限制;";
		case 401:
			return "没有进行身份验证;";
		case 403:
			return "没有权限访问对应的资源;";
		case 404:
			return "请求的资源不存在;";
		case 405:
			return "请求方法（GET、POST、HEAD、DELETE、PUT、TRACE等）对指定资源不适用;";
		case 500:
			return "服务器内部错误;";
		case 502:
			return "接口API关闭或正在升级;";
		}
		return "";
	}
	@Override
	public void addPhoneNumber(IWebview pWebViewImpl, JSONArray pJsArgs) {
		super.addPhoneNumber(pWebViewImpl, pJsArgs);
		LifeHelper.getInstance().addPhoneNum(pWebViewImpl.getContext(),new OnResult() {
			@Override
			public void onResult(int resultCode, String resultMsg,
					JSONObject resultData) {
				JSONObject sucJSON = new JSONObject();
				try {
					if(resultCode == 0){
						sucJSON.put(BaseOAuthService.KEY_AUTHRESULT, authResult);
						sucJSON.put(BaseOAuthService.KEY_USERINFO, userInfo);
			 			onAddPhoneNumberFinished(sucJSON, true);
					}else{
						try {
							sucJSON.put(DOMException.CODE,DOMException.CODE_BUSINESS_INTERNAL_ERROR);
							sucJSON.put(DOMException.MESSAGE, "["+getFullDescription()+":"+resultCode+"]"+getDesc(resultCode) + resultMsg);
						} catch (JSONException e) {
							e.printStackTrace();
						}
						onAddPhoneNumberFinished(sucJSON, false);
					}
				} catch (JSONException e) {
				}
			}});
	}
	public String encrypt(String content) throws Exception{
		return AESHelper.encrypt(content,id);
	}
	public String decrypt(String content) throws Exception {  
		return AESHelper.decrypt(content,id);
	}
	
	//webview当loadurl时候更新cookie
	public static void updateCookie(String url){
		Context context = DCloudApplication.getInstance();
		LifeHelper.getInstance().updateCookies(context, url);
	}
	
	//更新登陆cookie
	public static void autoLogin(){
		LifeHelper.autoLogin(null);
	}
}
