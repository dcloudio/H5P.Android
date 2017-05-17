package io.dcloud.feature.oauth.qq;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.tencent.connect.UserInfo;
import com.tencent.connect.common.Constants;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.ISysEventListener.SysEventType;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.constant.DOMException;
import io.dcloud.feature.oauth.BaseOAuthService;


/**
 * QQ登录登录服务功能
 * 
 * @author shutao
 * @date 2015-04-28
 */
public class QQOAuthService extends BaseOAuthService {
	private Tencent mTencent;
	private Context mContext;
    protected static  String appId = null;
    protected static  String appSecret = null;
    protected static  String redirectUri = null;
    protected static String  appKEY=null;
    private static final String TAG = "QQOAuthService";
	@Override
	public void init(Context context) {
		// TODO Auto-generated method stub
		super.init(context);
		mContext = context;
		id = "qq";
		description = "QQ";

	}

    @Override
    public boolean hasFullConfigData() {
        return !TextUtils.isEmpty(appId);
    }
    @Override
    public void initMetaData() {
        appId = AndroidResources.getMetaValue("QQ_APPID");
    }
    @Override
    public void initAuthOptions(JSONObject mLoginOptions) {
        if(mLoginOptions != null){
            appId = mLoginOptions.optString(BaseOAuthService.KEY_APPID, appId);
            Logger.e(TAG, "initAuthOptions: appId"+appId );
            appSecret = mLoginOptions.optString(BaseOAuthService.KEY_APSECRET,appSecret);
            redirectUri = mLoginOptions.optString(BaseOAuthService.KEY_REDIRECT_URI,redirectUri);
            appKEY = mLoginOptions.optString(BaseOAuthService.KEY_APPKEY,appKEY);
        }
    }
    IUiListener mIUiListener = new IUiListener() {

		@Override
		public void onError(UiError arg0) {
			// TODO Auto-generated method stub
			// 错误信息回调
			onLoginFinished(getErrorJsonbject(DOMException.CODE_BUSINESS_INTERNAL_ERROR, arg0.errorMessage, arg0.errorCode), false);
		}

		@Override
		public void onComplete(Object response) {
			// TODO Auto-generated method stub
			if (response == null) { // 登录失败 返回值为空
				onLoginFinished(getErrorJsonbject(DOMException.CODE_GET_TOKEN_ERROR, DOMException.MSG_GET_TOKEN_ERROR), false);
				return;
			} else {
				JSONObject object = (JSONObject) response;
				if (null != object && object.length() == 0) { // 登录失败											
					// 返回值为空
					onLoginFinished(getErrorJsonbject(DOMException.CODE_GET_TOKEN_ERROR, DOMException.MSG_GET_TOKEN_ERROR), false);
					return;
				}
			}
			authResult = (JSONObject) response;
			// 运行此处表示QQ登录 成功
			initOpenidAndToken(authResult);
			UserInfo info = new UserInfo(mContext, mTencent.getQQToken());
			updateUserInfo(info);
		}

		@Override
		public void onCancel() {
			// TODO Auto-generated method stub
			onLoginFinished(getErrorJsonbject(DOMException.CODE_USER_CANCEL, DOMException.MSG_USER_CANCEL), false);
		}
	};

	@Override
	public void login(final IWebview pWebViewImpl, JSONArray pJsArgs) {
		// TODO Auto-generated method stub
		super.login(pWebViewImpl, pJsArgs);
        if(hasGeneralError(mLoginWebViewImpl, mLoginCallbackId)){
            return;
        }
		if (mTencent == null) {
			// 运行QQ登录需要正式版RUMTIEM签名才能正常使用，如果测试可以使用222222 为测试appId
			// 如果发现代码为222222请修改为appId
			mTencent = Tencent.createInstance(appId, mContext);
		}
		pWebViewImpl.obtainApp().registerSysEventListener(
				new ISysEventListener() {
					@Override
					public boolean onExecute(SysEventType pEventType,
							Object pArgs) {
						// TODO Auto-generated method stub
						// 在某些低端机上调用登录后，由于内存紧张导致APP被系统回收，登录成功后无法成功回传数据。
						// 通过onActivityResult解决上诉问题
						Object[] _args = (Object[]) pArgs;
						int requestCode = (Integer) _args[0];
						int resultCode = (Integer) _args[1];
						Intent data = (Intent) _args[2];
						if (pEventType == SysEventType.onActivityResult) {
							if (requestCode == Constants.REQUEST_API) {
								if (resultCode == Constants.RESULT_LOGIN) {
									mTencent.handleLoginData(data, mIUiListener);
								}
							}
						}
						if (pWebViewImpl != null) {
							pWebViewImpl.obtainApp().unregisterSysEventListener(this, SysEventType.onActivityResult);
						}
						return false;
					}
				}, SysEventType.onActivityResult);
		String value = getValue(BaseOAuthService.KEY_AUTHRESULT);
		if (!TextUtils.isEmpty(value)) {
			initOpenidAndToken(value);
		}
		if (mTencent.isSessionValid()) {
			UserInfo info = new UserInfo(mContext, mTencent.getQQToken());
			updateUserInfo(info);
		} else {
			mTencent.login(pWebViewImpl.getActivity(), "all",
					mIUiListener);
		}
	}



    /**
	 * 初始化QQ登录所需TOKEN和OPENID
	 * 
	 * @param jsonObject
	 *            登录后回调的信息数据
	 */
	public void initOpenidAndToken(JSONObject jsonObject) {
		try {
			String token = jsonObject.getString(Constants.PARAM_ACCESS_TOKEN);
			String expires = jsonObject.getString(Constants.PARAM_EXPIRES_IN);
			String openId = jsonObject.getString(Constants.PARAM_OPEN_ID);
			if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(expires)
					&& !TextUtils.isEmpty(openId)) {
				mTencent.setAccessToken(token, expires);
				mTencent.setOpenId(openId);
				saveValue(BaseOAuthService.KEY_AUTHRESULT, jsonObject.toString());
			}
		} catch (Exception e) {
		}
	}
	
	/**
	 * 初始化QQ登录所需TOKEN和OPENID
	 * 
	 * 注意 此函数会判断mTencent 是否创建实例，如果没有则会创建 
	 * 赋值 authResult
	 * @param stringJson
	 *            登录后回调的信息数据
	 */
	public void initOpenidAndToken(String stringJson) {
		try {
			JSONObject jsonObject = new JSONObject(stringJson);
			String token = jsonObject.getString(Constants.PARAM_ACCESS_TOKEN);
			String expires = jsonObject.getString(Constants.PARAM_EXPIRES_IN);
			String openId = jsonObject.getString(Constants.PARAM_OPEN_ID);
			if (!TextUtils.isEmpty(token) && !TextUtils.isEmpty(expires)
					&& !TextUtils.isEmpty(openId)) {
				if (mTencent == null) {
					mTencent = Tencent.createInstance(appId, mContext);
				}
				mTencent.setAccessToken(token, expires);
				mTencent.setOpenId(openId);
				authResult = jsonObject;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 更新 USER数据返回JS层USER的JSON数据
	 * 
	 * @param info
	 *            QQ平台私有对象UserInfo
	 */
	private void updateUserInfo(UserInfo info) {
		if (mTencent != null && mTencent.isSessionValid()) {
			IUiListener listener = new IUiListener() {

				@Override
				public void onError(UiError e) {
					onLoginFinished(getErrorJsonbject(DOMException.CODE_BUSINESS_INTERNAL_ERROR, e.errorMessage, e.errorCode), false);
				}

				@Override
				public void onComplete(final Object response) {
					if (response != null) {
                        JSONObject tempuserInfo = (JSONObject) response;
                        userInfo=tempuserInfo;
                        try {
                            userInfo.put(KEY_HEADIMGURL,tempuserInfo.optString("figureurl"));
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
						saveValue(BaseOAuthService.KEY_USERINFO, userInfo.toString());
						onLoginFinished(makeResultJson(), true);
					} else {
						onLoginFinished(getErrorJsonbject(DOMException.CODE_GET_TOKEN_ERROR, DOMException.MSG_GET_TOKEN_ERROR), false);
					}
				}

				@Override
				public void onCancel() {
					onLoginFinished(getErrorJsonbject(DOMException.CODE_USER_CANCEL, DOMException.MSG_USER_CANCEL), false);
				}
			};
			info.getUserInfo(listener);
		}
	}

	@Override
	public void logout(IWebview pWebViewImpl, JSONArray pJsArgs) {
		// TODO Auto-generated method stub
		super.logout(pWebViewImpl, pJsArgs);
        if(hasGeneralError(mLogoutWebViewImpl, mLogoutCallbackId)){
            return;
        }
		userInfo = null;
		authResult = null;
		removeToken();
		onLogoutFinished(makeResultJson(), true);
		mTencent.logout(mContext);
		mTencent = null;
	}

	@Override
	public void getUserInfo(IWebview pWebViewImpl, JSONArray pJsArgs) {
		super.getUserInfo(pWebViewImpl, pJsArgs);
        if(hasGeneralError(mGetUserInfoWebViewImpl, mGetUserInfoCallbackId)){
            return;
        }
		String result = getValue(BaseOAuthService.KEY_AUTHRESULT);
		if (!TextUtils.isEmpty(result)) {
			initOpenidAndToken(result);
		}
		// 判断当前QQ信息是否登录或是登录有效
		if (mTencent != null && mTencent.isSessionValid()) {
			String infov = getValue(BaseOAuthService.KEY_USERINFO);
			try {
				userInfo = new JSONObject(infov);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			onGetUserInfoFinished(makeResultJson(), true);
			return;
		}
		// 运行此处表示登录无效或未登录 直接提示登录失效
		onGetUserInfoFinished(getErrorJsonbject(DOMException.CODE_OAUTH_FAIL, DOMException.MSG_OAUTH_FAIL),
				false);
	}

}
