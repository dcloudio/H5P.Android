package io.dcloud.feature.oauth.weixin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;


import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelmsg.SendAuth;
import com.tencent.mm.opensdk.modelmsg.SendMessageToWX;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.dcloud.ProcessMediator;
import io.dcloud.common.DHInterface.FeatureMessageDispatcher;
import io.dcloud.common.DHInterface.FeatureMessageDispatcher.MessageListener;
import io.dcloud.common.DHInterface.IActivityHandler;
import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.PlatformUtil;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.NetTool;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.StringUtil;
import io.dcloud.common.util.ThreadPool;
import io.dcloud.feature.oauth.BaseOAuthService;

public class WeiXinOAuthService extends BaseOAuthService {
    //https://open.weixin.qq.com/cgi-bin/showdocument?action=dir_list&t=resource/res_list&verify=1&lang=zh_CN
    private IWXAPI api;

    private static final String DEFAULT_SCOPE = "snsapi_userinfo";
    private static final String DEFAULT_STATE = "wechat_sdk_dcloud_weixin_oauth";

    private static final String KEY_ERRCODE = "errcode";
    private static final String KEY_ERRMSG = "errmsg";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_OPENID = "openid";

    private static final String URL_GET_ACCESS_TOKEN = "https://api.weixin.qq.com/sns/oauth2/access_token?appid=%s&secret=%s&code=%s&grant_type=authorization_code";
    private static final String URL_REFRESH_TOKEN = "https://api.weixin.qq.com/sns/oauth2/refresh_token?appid=%s&grant_type=refresh_token&refresh_token=%s";
    private static final String URL_GET_USERINFO = "https://api.weixin.qq.com/sns/userinfo?access_token=%s&openid=%s&lang=zh_CN";
    private static final String URL_CHECK_TOKEN = "https://api.weixin.qq.com/sns/auth?access_token=%s&openid=%s";

    private static final String ERR_MSG_AUTH_DENIED = "Authentication failed";
    private static final String ERR_MSG_COMM = "General errors";
    private static final String ERR_MSG_SENT_FAILED = "Unable to send";
    private static final String ERR_MSG_UNSUPPORT = "Unsupport error";
    private static final String ERR_MSG_USER_CANCEL = "User canceled";

    // 标志login是否回调  false表示并未回调  true表示已回调
    private boolean isLoginReceiver = false;
    private static final String TAG = "WeiXinOAuthService";
    protected static String appId = null;
    protected static String appSecret = null;
    protected static String redirectUri = null;
    protected static String appKEY = null;

    public boolean hasFullConfigData() {
        return !TextUtils.isEmpty(appId) && !TextUtils.isEmpty(appSecret);
    }


    @Override
    public void initAuthOptions(JSONObject mLoginOptions) {
        if (mLoginOptions != null) {
            appId = mLoginOptions.optString(BaseOAuthService.KEY_APPID, appId);
            Logger.e(TAG, "initAuthOptions: appId" + appId);
            appSecret = mLoginOptions.optString(BaseOAuthService.KEY_APSECRET, appSecret);
            redirectUri = mLoginOptions.optString(BaseOAuthService.KEY_REDIRECT_URI, redirectUri);
            appKEY = mLoginOptions.optString(BaseOAuthService.KEY_APPKEY, appKEY);
        }
    }

    @Override
    public void initMetaData() {
        appId = AndroidResources.getMetaValue("WX_APPID");
        appSecret = AndroidResources.getMetaValue("WX_SECRET");
    }

    @Override
    public void init(Context context) {
        super.init(context);
        id = "weixin";
        description = "微信";
    }

    MessageListener sLoginMessageListener = new MessageListener() {
        @Override
        public void onReceiver(final Object msg) {
            new Thread() {
                @Override
                public void run() {
                    if (msg instanceof SendMessageToWX.Resp) {//失败回调(微信没有登陆)
                        int code = ((SendMessageToWX.Resp) msg).errCode;
                        onLoginCallBack(mLoginWebViewImpl, mLoginCallbackId, code);
                    }
                    if (msg instanceof SendAuth.Resp) {//成功回调
                        Logger.d("WeiXinOAuthService", "isLoginReceiver = true");
                        isLoginReceiver = true;
                        SendAuth.Resp resp = (SendAuth.Resp) msg;
                        String code = resp.code;
                        String state = resp.state;
                        JSONObject result = null;
                        String s_access_token = null;
                        if (isAuth) {
                            isAuth = false;
                            if (resp.errCode == BaseResp.ErrCode.ERR_OK){
                                JSONObject jsonResult = new JSONObject();
                                try {
                                    jsonResult.put("scope",mAuthOptions == null?DEFAULT_SCOPE:mAuthOptions.optString(BaseOAuthService.KEY_SCOPE, DEFAULT_SCOPE));
                                    jsonResult.put("state",state);
                                    jsonResult.put("code",code);
                                    jsonResult.put("lang",resp.lang);
                                    jsonResult.put("country",resp.country);
                                    JSUtil.execCallback(mAuthWebview, mAuthCallbackId, jsonResult.toString(), JSUtil.OK, true, false);
                                } catch (JSONException e) {
//                                    e.printStackTrace();
                                }

                            } else {
                                onLoginCallBack(mAuthWebview, mAuthCallbackId, resp.errCode);
                            }
                            FeatureMessageDispatcher.unregisterListener(sLoginMessageListener);
                            return;
                        }
                        if (code != null) {
                            String access_token_url = String.format(URL_GET_ACCESS_TOKEN, appId, appSecret, code);
                            s_access_token = getToken(access_token_url);
                            if (null != s_access_token) {
                                result = JSONUtil.createJSONObject(s_access_token);
                            }
                        }
                        if (result == null) {
                            //授权失败
                            onLoginCallBack(mLoginWebViewImpl, mLoginCallbackId, resp.errCode);
                        } else if (result.has(KEY_ERRCODE)) {
//					{"errcode":40029,"errmsg":"invalid code"}
                            int errcode = JSONUtil.getInt(result, KEY_ERRCODE);
                            String errmsg = JSONUtil.getString(result, KEY_ERRMSG);
                            String jsonResult = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, errmsg, errcode);
                            JSUtil.execCallback(mLoginWebViewImpl, mLoginCallbackId, jsonResult, JSUtil.ERROR, true, false);
                        } else {
//					{
//						"access_token":"ACCESS_TOKEN", //接口调用凭证
//						"expires_in":7200, //access_token接口调用凭证超时时间，单位（秒）
//						"refresh_token":"REFRESH_TOKEN",//用户刷新access_token
//						"openid":"OPENID", //授权用户唯一标识
//						"scope":"SCOPE",//用户授权的作用域，使用逗号（,）分隔
//						"unionid":"o6_bmasdasdsad6_2sgVt7hMZOPfL"//只有在用户将公众号绑定到微信开放平台帐号后，才会出现该字段。
//					}
//					String access_token = JSONUtil.getString(result, KEY_ACCESS_TOKEN);
                            String refresh_token = JSONUtil.getString(result, KEY_REFRESH_TOKEN);
//					String scope = JSONUtil.getString(result, "scope");
//					String openid = JSONUtil.getString(result, KEY_OPENID);
                            saveValue(BaseOAuthService.KEY_AUTHRESULT, s_access_token);
                            saveValue(BaseOAuthService.KEY_STATE, state);

                            initUserInfo();
                            onLoginCallBack(mLoginWebViewImpl, mLoginCallbackId, BaseResp.ErrCode.ERR_OK);
                        }
                        FeatureMessageDispatcher.unregisterListener(sLoginMessageListener);
                    }
                }
            }.start();

        }
    };

    private void reFreshTokenAndSave(String refresh_token) {

//		
    }

    private void onLoginCallBack(final IWebview pWebViewImpl,
                                 final String pCallbackId, int code) {
        boolean suc = false;
        String errorMsg = DOMException.MSG_SHARE_SEND_ERROR;
        if (code == BaseResp.ErrCode.ERR_OK) {
            suc = true;
        } else if (code == BaseResp.ErrCode.ERR_AUTH_DENIED) {
            errorMsg = ERR_MSG_AUTH_DENIED;
        } else if (code == BaseResp.ErrCode.ERR_COMM) {
            errorMsg = ERR_MSG_COMM;
        } else if (code == BaseResp.ErrCode.ERR_SENT_FAILED) {
            errorMsg = ERR_MSG_SENT_FAILED;
        } else if (code == BaseResp.ErrCode.ERR_UNSUPPORT) {
            errorMsg = ERR_MSG_UNSUPPORT;
        } else if (code == BaseResp.ErrCode.ERR_USER_CANCEL) {
            onLoginFinished(getErrorJsonbject(DOMException.CODE_USER_CANCEL, DOMException.MSG_USER_CANCEL), false,pWebViewImpl,pCallbackId);
            return;
        }
        if (suc) {//由于调用微信发送接口，会立马回复true，而不是真正分享成功，甚至连微信界面都没有启动，在此延迟回调，以增强体验
            JSUtil.execCallback(pWebViewImpl, pCallbackId, makeResultJSONObject(), JSUtil.OK, false);
        } else {
            String msg = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, errorMsg, code);
            JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.ERROR, true, false);
        }
    }

    public void login(IWebview pWebViewImpl, JSONArray pJsArgs) {
        super.login(pWebViewImpl, pJsArgs);
        if (hasGeneralError(mLoginWebViewImpl, mLoginCallbackId)) {
            return;
        }
        //未安装客户端提示
        if (!PlatformUtil.isAppInstalled(pWebViewImpl.getContext(), "com.tencent.mm")) {
            String msg = StringUtil.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_CLIENT_UNINSTALLED, DOMException.toString(DOMException.MSG_CLIENT_UNINSTALLED));
            JSUtil.execCallback(pWebViewImpl, mLoginCallbackId, msg, JSUtil.ERROR, true, false);
            return;
        }

        ThreadPool.self().addThreadTask(
                new Runnable() {
                    @Override
                    public void run() {
                        loginInThread(mLoginWebViewImpl,mLoginCallbackId,mLoginOptions);
                    }
                }
        );
    }

    private boolean isAuth = false;
    @Override
    public void authorize(IWebview pwebview, JSONArray pJsArgs) {
        super.authorize(pwebview, pJsArgs);
//        appId不允许为空 secret不在设置之间
        if (TextUtils.isEmpty(appId)) {
            String msg = StringUtil.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_BUSINESS_PARAMETER_HAS_NOT, DOMException.toString(DOMException.MSG_BUSINESS_PARAMETER_HAS_NOT));
            JSUtil.execCallback(pwebview, mAuthCallbackId, msg, JSUtil.ERROR, true, false);
            return;
        }
        if (!PlatformUtil.isAppInstalled(pwebview.getContext(), "com.tencent.mm")) {
            String msg = StringUtil.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_CLIENT_UNINSTALLED, DOMException.toString(DOMException.MSG_CLIENT_UNINSTALLED));
            JSUtil.execCallback(pwebview, mAuthCallbackId, msg, JSUtil.ERROR, true, false);
            return;
        }
        ThreadPool.self().addThreadTask(new Runnable() {
            @Override
            public void run() {
                isAuth = true;
                loginInThread(mAuthWebview,mAuthCallbackId,mAuthOptions);
            }
        });
    }

    /**
     * 因需要请求网络，所以将之放入子线程
     */
    private void loginInThread(final IWebview pwebview, final String callbackId, JSONObject option) {
        Logger.d("WeiXinOAuthService", "isLoginReceiver = false");
        isLoginReceiver = false;
        String s_authResult = getValue(BaseOAuthService.KEY_AUTHRESULT);
        JSONObject authResult = JSONUtil.createJSONObject(s_authResult);
        if (!isAuth) {
            if (authResult != null && authResult.has(KEY_ACCESS_TOKEN)) {
                //存在access_token
                String check_token_url = String.format(URL_CHECK_TOKEN, authResult.optString(KEY_ACCESS_TOKEN), authResult.optString(KEY_OPENID));
                byte[] temp = NetTool.httpGet(check_token_url);
                if (null != temp) {
                    String str = new String(temp);
                    JSONObject checkTokenResult = JSONUtil.createJSONObject(str);
                    if (checkTokenResult != null) {//{"errcode":0,"errmsg":"ok"}
                        if (checkTokenResult.optInt(KEY_ERRCODE) == 0) {//成功
                            //access_token依然有效，不需要重新授权
                            initUserInfo();
                            onLoginCallBack(pwebview, callbackId, BaseResp.ErrCode.ERR_OK);
                            return;
                        } else {//{"errcode":40003,"errmsg":"invalid openid"}
                            //access_token无效，需要重新授权
                            removeToken();
                            //刷新access_token
                            String refresh_token = authResult.optString(KEY_REFRESH_TOKEN);
                            //refresh_token拥有较长的有效期（30天），当refresh_token失效的后，需要用户重新授权。
                            String refresh_token_url = String.format(URL_REFRESH_TOKEN, appId, refresh_token);
                            String s_refreshToken = refreshToken(refresh_token_url);//authResult
                            JSONObject refreshTokenResult = null;
                            if (!PdrUtil.isEmpty(s_refreshToken)) {

                                refreshTokenResult = JSONUtil.createJSONObject(s_refreshToken);
                            } else {
                                onLoginCallBack(pwebview, callbackId, BaseResp.ErrCode.ERR_COMM);
                            }
//					{
//						"access_token":"ACCESS_TOKEN",  接口调用凭证
//						"expires_in":7200, access_token接口调用凭证超时时间，单位（秒）
//						"refresh_token":"REFRESH_TOKEN", 用户刷新access_token
//						"openid":"OPENID", 授权用户唯一标识
//						"scope":"SCOPE" 用户授权的作用域，使用逗号（,）分隔
//					}

                            if (!PdrUtil.isEmpty(refreshTokenResult)) {
                                if (refreshTokenResult.has(KEY_ERRCODE)) {//失败时走重新授权逻辑
                                    //{"errcode":40030,"errmsg":"invalid refresh_token"}
//						int errcode = JSONUtil.getInt(refreshTokenResult, KEY_ERRCODE);
//						String errmsg = JSONUtil.getString(refreshTokenResult, KEY_ERRMSG);
//						String jsonResult = String.format(DOMException.JSON_ERROR_INFO, errcode,errmsg);
//						JSUtil.execCallback(mLoginWebViewImpl, mLoginCallbackId, jsonResult, JSUtil.ERROR, true, false);


                                } else {


//						access_token = JSONUtil.getString(result, KEY_ACCESS_TOKEN);
//						refresh_token = JSONUtil.getString(result, "refresh_token");
//						scope = JSONUtil.getString(result, "scope");
//						openid = JSONUtil.getString(result, KEY_OPENID);


                                    saveValue(BaseOAuthService.KEY_AUTHRESULT, s_refreshToken);
                                    initUserInfo();
                                    onLoginCallBack(pwebview, callbackId, BaseResp.ErrCode.ERR_OK);
                                    return;
                                }
                            } else {
                                onLoginCallBack(pwebview, callbackId, BaseResp.ErrCode.ERR_COMM);
                            }


                        }
                    }
                } else {
                    onLoginCallBack(pwebview, callbackId, BaseResp.ErrCode.ERR_COMM);
                }

            }
        }

        //第一次授权或access_token失效后，重新授权
        final SendAuth.Req req = new SendAuth.Req();
        req.scope = WeiXinOAuthService.DEFAULT_SCOPE;
        req.state = WeiXinOAuthService.DEFAULT_STATE;
        if (option != null) {
            req.scope = option.optString(BaseOAuthService.KEY_SCOPE, req.scope);
            req.state = option.optString(BaseOAuthService.KEY_STATE, req.state);
        }
        if(pwebview.getActivity() instanceof IActivityHandler && ((IActivityHandler)pwebview.getActivity()).isMultiProcessMode()){//多进程模式
            startWeiXinMediator(req,pwebview,callbackId);
            return;
        }
        if (api == null) {
            api = WXAPIFactory.createWXAPI(pwebview.getActivity(), appId, true);
            api.registerApp(appId);
        }
        final boolean suc = api.sendReq(req);
        final IApp app = pwebview.obtainFrameView().obtainApp();
        app.registerSysEventListener(new ISysEventListener() {

            @Override
            public boolean onExecute(SysEventType pEventType, Object pArgs) {
                // TODO Auto-generated method stub
                // isLoginReceiver = false 表示login并未回调。 表示用户并未实施登录而是返回关闭了登录请求页面。
                Logger.d("WeiXinOAuthService", "isLoginReceiver1 "+ isLoginReceiver);
                if (!isLoginReceiver) { //用户取消
                    Logger.d("WeiXinOAuthService", "isLoginReceiver2 "+ isLoginReceiver);
                    onLoginFinished(getErrorJsonbject(DOMException.CODE_USER_CANCEL, DOMException.MSG_USER_CANCEL), false,pwebview,callbackId);
                }
//                FeatureMessageDispatcher.unregisterListener(sLoginMessageListener);

                if (app != null) {
                    app.unregisterSysEventListener(this, SysEventType.onResume);
                }
                return false;
            }
        }, ISysEventListener.SysEventType.onResume);
        if (suc && hasWXEntryActivity(pwebview.getContext())) {
            isLoginReceiver = true;         // 解决调用被杀死微信时会回调"用户取消"问题
            // 未注册才注册监听，防止注册多个监听（否则用户取消登陆后再次登陆，会报code复用错误）
            if (!FeatureMessageDispatcher.sFeatureMessage.contains(sLoginMessageListener)) {
                FeatureMessageDispatcher.registerListener(sLoginMessageListener);
            }
        } else {
            pwebview.obtainWindowView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    onLoginCallBack(pwebview, callbackId, BaseResp.ErrCode.ERR_SENT_FAILED);
                }
            }, 500);
        }
    }

    protected void onLoginFinished(JSONObject msg, boolean suc,IWebview pwebview, String callbackId) {
        JSUtil.execCallback(pwebview, callbackId, msg,
                suc ? JSUtil.OK : JSUtil.ERROR, false);
        if (isAuth) {
            mAuthWebview = null;
            mAuthCallbackId = null;
        } else {
            mLoginCallbackId = null;
            mLoginWebViewImpl = null;
        }
    }

    private void startWeiXinMediator(SendAuth.Req req, final IWebview pwebview, final String callbackId) {
        Intent intent = new Intent();
        intent.putExtra(ProcessMediator.LOGIC_CLASS,WeiXinMediator.class.getName());
        Bundle bundle = new Bundle();
        req.toBundle(bundle);
        intent.putExtra(ProcessMediator.REQ_DATA,bundle);
        intent.setClassName(pwebview.getActivity(),ProcessMediator.class.getName());
        pwebview.getActivity().startActivityForResult(intent,ProcessMediator.CODE_REQUEST);
        pwebview.getActivity().overridePendingTransition(0,0);
        pwebview.obtainApp().registerSysEventListener(new ISysEventListener() {
            @Override
            public boolean onExecute(SysEventType pEventType, Object pArgs) {
                Object[] _args = (Object[])pArgs;
                int requestCode = (Integer)_args[0];
                int resultCode = (Integer)_args[1];
                Intent data = (Intent) _args[2];
                if(pEventType == SysEventType.onActivityResult && requestCode == ProcessMediator.CODE_REQUEST){
                    Bundle bundle = data.getBundleExtra(ProcessMediator.RESULT_DATA);
                    if(bundle == null){
                        onLoginCallBack(pwebview, callbackId, BaseResp.ErrCode.ERR_USER_CANCEL);
                    }else {
                        String s = bundle.getString(ProcessMediator.STYLE_DATA);
                        if ("BaseResp".equals(s)) {
                            SendAuth.Resp payResp = new SendAuth.Resp();
                            payResp.fromBundle(bundle);
                            sLoginMessageListener.onReceiver(payResp);
                        } else if ("BaseReq".equals(s)) {
                            BaseReq baseReq = new SendAuth.Req();
                            baseReq.fromBundle(bundle);
                            sLoginMessageListener.onReceiver(baseReq);
                        }
                    }
                }
                return false;
            }
        }, ISysEventListener.SysEventType.onActivityResult);
    }


    @Override
    public void logout(IWebview pWebViewImpl, JSONArray pJsArgs) {
        super.logout(pWebViewImpl, pJsArgs);
        if (hasGeneralError(mLogoutWebViewImpl, mLogoutCallbackId)) {
            return;
        }
        removeToken();
        userInfo = null;
        authResult = null;
        onLogoutFinished(makeResultJSONObject(), true);
        api = null;
    }

    @Override
    public void getUserInfo(IWebview pWebViewImpl, JSONArray pJsArgs) {
        super.getUserInfo(pWebViewImpl, pJsArgs);
        if (hasGeneralError(mGetUserInfoWebViewImpl, mGetUserInfoCallbackId)) {
            return;
        }
        ThreadPool.self().addThreadTask(
                new Runnable() {
                    @Override
                    public void run() {
                        boolean suc = initUserInfo();
                        if (suc) {
                            JSONObject sucJSON = makeResultJSONObject();
                            onGetUserInfoFinished(sucJSON, suc);
                        } else {
                            String msg = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.MSG_UNOAUTH_ERROR, DOMException.CODE_UNOAUTH_ERROR);
                            JSUtil.execCallback(mGetUserInfoWebViewImpl, mGetUserInfoCallbackId, msg, JSUtil.ERROR, true, false);
                        }

                    }
                }
        );
    }

    public JSONObject makeResultJSONObject() {
        JSONObject sucJSON = new JSONObject();
        try {
            sucJSON.put(BaseOAuthService.KEY_AUTHRESULT, authResult);
            sucJSON.put(BaseOAuthService.KEY_USERINFO, userInfo);
            String state = getValue(BaseOAuthService.KEY_STATE);
            sucJSON.put(BaseOAuthService.KEY_STATE, state);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return sucJSON;
    }

    boolean initUserInfo() {
        boolean suc = false;
        String s_authResult = getValue(BaseOAuthService.KEY_AUTHRESULT);
        authResult = JSONUtil.createJSONObject(s_authResult);
        if (authResult != null && authResult.has(KEY_ACCESS_TOKEN)) {
            String get_userInfo_url = String.format(URL_GET_USERINFO, authResult.opt(KEY_ACCESS_TOKEN), authResult.opt(KEY_OPENID));
            String s_userInfoResult = getUserInfo(get_userInfo_url);
            JSONObject userInfoResult = JSONUtil.createJSONObject(s_userInfoResult);
            if (!userInfoResult.has(KEY_ERRCODE)) {
                saveValue(BaseOAuthService.KEY_USERINFO, s_userInfoResult);
                userInfo = userInfoResult;
                suc = true;
            }
        }
        return suc;
    }

    private boolean hasWXEntryActivity(Context context) {
        String clsName = context.getPackageName() + ".wxapi.WXEntryActivity";
        try {
            Class.forName(clsName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

}
