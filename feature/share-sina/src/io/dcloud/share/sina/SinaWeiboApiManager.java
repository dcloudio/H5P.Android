package io.dcloud.share.sina;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.URLUtil;

import com.sina.weibo.sdk.WbSdk;
import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.MultiImageObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.VideoSourceObject;
import com.sina.weibo.sdk.api.WebpageObject;
import com.sina.weibo.sdk.api.WeiboMultiMessage;
import com.sina.weibo.sdk.auth.AccessTokenKeeper;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WbAuthListener;
import com.sina.weibo.sdk.auth.WbConnectErrorMessage;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.share.WbShareHandler;
import com.sina.weibo.sdk.share.WbShareTransActivity;
import com.sina.weibo.sdk.utils.Utility;
import com.sina.weibo.sdk.web.WebRequestType;
import com.sina.weibo.sdk.web.param.ShareWebViewRequestParam;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;

import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.ISysEventDispatch;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.ISysEventListener.SysEventType;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.PlatformUtil;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.BaseInfo;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.NetTool;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.StringUtil;
import io.dcloud.common.util.ThreadPool;
import io.dcloud.share.IFShareApi;


/**
 * <p>
 * Description:新浪微博API管理者
 * </p>
 *
 */
public class SinaWeiboApiManager implements IFShareApi {
    private static final String PACKAGENAME = "com.sina.weibo";

    private JSONObject mSinaWeibo;
    private static String APP_KEY;
    Activity mActivity;
    /**
     * 授权回调页面
     * 必须与http://open.weibo.com/apps/%APP_KEY%/info/advanced
     * 中配置的相同或是在这个域名下的
     */
    private static String REDIRECT_URL;

    private static String SCOPE = "email,direct_messages_read,direct_messages_write,"
            + "friendships_groups_read,friendships_groups_write,statuses_to_me_read,"
            + "follow_app_official_microblog";

    private static final String SINAWEIBO_DES = "新浪微博";
    public static final String SINAWEIBO_ID = "sinaweibo";

    public static final String KEY_APPKEY = "appkey";
    public static final String KEY_REDIRECT_URI = "redirect_uri";

    private Oauth2AccessToken mAccessToken;

    /**
     * Description:初始化json对象
     *
     */
    private void initJsonObject(Context context) throws JSONException {
        if (mAccessToken == null) {
            mAccessToken = AccessTokenKeeper.readAccessToken(context);
        }
        String token = mAccessToken.getToken();
        mSinaWeibo = new JSONObject();
        mSinaWeibo.put(StringConst.JSON_SHARE_ID, SINAWEIBO_ID);
        mSinaWeibo.put(StringConst.JSON_SHARE_DESCRIPTION, SINAWEIBO_DES);
        mSinaWeibo.put(StringConst.JSON_SHARE_AUTHENTICATED, !PdrUtil.isEmpty(token));
        mSinaWeibo.put(StringConst.JSON_SHARE_ACCESSTOKEN, token);
        mSinaWeibo.put(StringConst.JSON_SHARE_NATIVECLIENT,
                PlatformUtil.hasAppInstalled(context, PACKAGENAME));
        mActivity = (Activity) context;
    }

    @Override
    public void initConfig() {
        initData();
    }

    public void initData() {
        if (!TextUtils.isEmpty(AndroidResources.getMetaValue("SINA_APPKEY"))){
            APP_KEY = AndroidResources.getMetaValue("SINA_APPKEY").substring(1);
        }
        REDIRECT_URL = AndroidResources.getMetaValue("SINA_REDIRECT_URI");
    }

    /**
     * Description:获取json对象的字符串
     */
    @Override
    public String getJsonObject(IWebview pWebViewImpl) {
        String _json = null;
        try {
            initJsonObject(pWebViewImpl.getActivity());
            _json = mSinaWeibo.toString();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return _json;
    }

    @Override
    public String getId() {
        return SINAWEIBO_ID;
    }


    public static final String SHARE_CANEL_ERROR = "64001";
    public static final String SHARE_AUTHOR_ERROR = "64002";
    public static final String SHARE_CONTENT_ERROR = "64003";
    private String SEND_CALLBACKID = null;
    private boolean hasSinaAppInstalled = false;
    private String mInterface = "auto";



    public void OnSendEnd(IWebview pWebViewImpl,boolean suc, int errorCode, String msg) {
        if (SEND_CALLBACKID != null) {
            if (suc) {
                JSUtil.execCallback(pWebViewImpl, SEND_CALLBACKID, "", 1, false, false);
            } else {
                String errorMsg = StringUtil.format(DOMException.JSON_ERROR_INFO, errorCode, msg);
                JSUtil.execCallback(pWebViewImpl, SEND_CALLBACKID, errorMsg, JSUtil.ERROR, true, false);
            }
            SEND_CALLBACKID = null;
        }
    }
    private WbShareHandler shareHandler;
    /**
     *  供原生代码分享调用
     * @param activity
     * @param pShareMsg
     */
    public void send(final Activity activity, final String pShareMsg) {

        initConfig();
        if (mAccessToken == null) {
            mAccessToken = AccessTokenKeeper.readAccessToken(activity);
        }
        if (mAccessToken == null||!mAccessToken.isSessionValid()) {
            final Runnable runnable=new Runnable() {
                @Override
                public void run() {
                    send(activity,pShareMsg);
                }
            };
            authorize(activity,new WbAuthListener(){
                @Override
                public void onSuccess(Oauth2AccessToken oauth2AccessToken) {
                    mAccessToken = oauth2AccessToken;
                    if (mAccessToken.isSessionValid()) {
                        // 保存 Token 到 SharedPreferences
                        AccessTokenKeeper.writeAccessToken(activity, mAccessToken);
                    }
                    runnable.run();
                }
                @Override
                public void cancel() {}
                @Override
                public void onFailure(WbConnectErrorMessage wbConnectErrorMessage) {}
            });
            return;
        }
        hasSinaAppInstalled = PlatformUtil.hasAppInstalled(activity, PACKAGENAME);
        shareHandler = new WbShareHandler(activity);
        WbSdk.install(activity, new AuthInfo(activity, APP_KEY, REDIRECT_URL, SCOPE));
        shareHandler.registerApp();

        JSONObject _msg;
        String file;
        try {
            _msg = new JSONObject(pShareMsg);
            if (_msg != null) {
                mInterface = _msg.optString("interface");
                JSONArray _pictures = _msg.optJSONArray("pictures");
                String _content = _msg.optString("content");
                file = JSONUtil.getString(_pictures, 0);
                if (URLUtil.isNetworkUrl(file)) {// TODO: 2016/10/12 根据5+runtime文档添加网络图片分享不支持的逻辑
                    return;
                }
                if ("slient".equals(mInterface)) {//interface=slient
                    startWebShare(activity,getWeiboMultiMessage(pShareMsg));
                } else if ("editable".equals(mInterface)) {//interface=editable
                    if (hasSinaAppInstalled) {
                        if (TextUtils.isEmpty(_content)){// TODO: 2016/10/12 分享新浪微博客户端时，文本内容不能为空，否则进程阻塞，分享不会回调。
                            return;
                        }
                        shareHandler.shareMessage(getWeiboMultiMessage(pShareMsg),true);
                    } else {
                        //未安装新浪微博客户端，触发回调
                    }
                } else {//interface=auto
                    if (hasSinaAppInstalled) {
                        if (TextUtils.isEmpty(_content)){// TODO: 2016/10/12 分享新浪微博客户端时，文本内容不能为空，否则进程阻塞，分享不会回调。
                            return;
                        }
                        shareHandler.shareMessage(getWeiboMultiMessage(pShareMsg),true);
                    } else {
                        startWebShare(activity,getWeiboMultiMessage(pShareMsg));
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    /**
     * 新浪微博分享消息逻辑
     * plus.share.ShareMessage
     * interface属性（ 默认值为“auto”。），可取值：
     * "auto" - 自动选择，如果已经安装微博客户端则采用编辑界面进行分享，否则采用第二种无界面分享；
     * "slient" - 静默分享，采用第二种无界面模式进行分享；
     * "editable" - 进入编辑界面，如果当前未安装微博客户端则触发错误回调。
     *
     * @param pWebViewImpl
     * @param pCallbackId
     * @param pShareMsg
     */
    @Override
    public void send(final IWebview pWebViewImpl, String pCallbackId, String pShareMsg) {
        if (mAccessToken == null) {
            mAccessToken = AccessTokenKeeper.readAccessToken(pWebViewImpl.getActivity());
        }

        SEND_CALLBACKID = pCallbackId;
        if (mAccessToken == null||!mAccessToken.isSessionValid()) {
            OnSendEnd(pWebViewImpl,false,DOMException.CODE_AUTHORIZE_FAILED,DOMException.MSG_AUTHORIZE_FAILED);
            return;
        }
        hasSinaAppInstalled = PlatformUtil.hasAppInstalled(pWebViewImpl.getActivity(), PACKAGENAME);
        final IApp app = pWebViewImpl.obtainApp();
        shareHandler = new WbShareHandler(pWebViewImpl.getActivity());
        WbSdk.install(pWebViewImpl.getActivity(), new AuthInfo(pWebViewImpl.getActivity(), APP_KEY, REDIRECT_URL, SCOPE));
        shareHandler.registerApp();
        pWebViewImpl.obtainApp().registerSysEventListener(new ISysEventListener() {
            @Override
            public boolean onExecute(SysEventType pEventType, Object pArgs) {
                Logger.e("ian","onExecute");
                if (!PdrUtil.isEmpty(pArgs)){
                    JSONObject myjson=JSONUtil.createJSONObject((String)pArgs);
                    Logger.e("ian",myjson.toString());
                    if (PdrUtil.isEquals("0",myjson.optString("_weibo_resp_errcode"))){
                        OnSendEnd(pWebViewImpl,true,-1,null);
                    }else if(PdrUtil.isEquals("1",myjson.optString("_weibo_resp_errcode"))){
                        String msg =  DOMException.MSG_USER_CANCEL;
                        OnSendEnd(pWebViewImpl,false,DOMException.CODE_USER_CANCEL,msg);
                    }else{
                        String msg =  myjson.optString("_weibo_resp_errstr");
                        int code=Integer.valueOf(myjson.optString("_weibo_resp_errcode"));
                        OnSendEnd(pWebViewImpl,true,code,msg);
                    }

                }
                if (app != null) {
                    app.unregisterSysEventListener(this, SysEventType.onNewIntent);
                }
                return false;
            }
        }, SysEventType.onNewIntent);

        JSONObject _msg;
        String file;
        try {
            _msg = new JSONObject(pShareMsg);
            if (_msg != null) {
                mInterface = _msg.optString("interface");
                String type = _msg.optString("type");
                JSONArray _pictures = _msg.optJSONArray("pictures");
                String _content = _msg.optString("content");
                file = JSONUtil.getString(_pictures, 0);
                if (URLUtil.isNetworkUrl(file)) {// TODO: 2016/10/12 根据5+runtime文档添加网络图片分享不支持的逻辑
                    OnSendEnd(pWebViewImpl,false, DOMException.CODE_SHARE_SEND_ERROR, DOMException.MSG_SHARE_SEND_PIC_ROUTE_ERROR);
                    return;
                }
                if ("slient".equals(mInterface)) {//interface=slient
                    WeiboMultiMessage message = getWeiboMultiMessage(pWebViewImpl, pShareMsg, type);
                    if(message.imageObject != null && ((message.imageObject.imageData != null && message.imageObject.imageData.length > 128000)
                            || (message.imageObject.thumbData != null && message.imageObject.thumbData.length > 128000))) {
                        OnSendEnd(pWebViewImpl,false, DOMException.CODE_SHARE_SEND_ERROR, "当前手机无新浪客户端！web分享图片不能超过128KB！");
                        return;
                    }
                    if(!PdrUtil.isEmpty(type) && (type.equals("video") || message.multiImageObject != null)) {
                        OnSendEnd(pWebViewImpl,false, DOMException.CODE_SHARE_SEND_ERROR, "当前手机无新浪客户端！无法分享视频及多图！");
                        return;
                    }
                    startWebShare(pWebViewImpl.getActivity(), message);
                } else if ("editable".equals(mInterface)) {//interface=editable
                    if (hasSinaAppInstalled) {
                        if (TextUtils.isEmpty(_content)){// TODO: 2016/10/12 分享新浪微博客户端时，文本内容不能为空，否则进程阻塞，分享不会回调。
                            OnSendEnd(pWebViewImpl,false, DOMException.CODE_SHARE_SEND_ERROR, DOMException.MSG_SHARE_SEND_CONTENT_EMPTY_ERROR);
                            return;
                        }
                        shareHandler.shareMessage(getWeiboMultiMessage(pWebViewImpl, pShareMsg, type),true);
                    } else {
                        //未安装新浪微博客户端，触发回调
                        OnSendEnd(pWebViewImpl,false, DOMException.CODE_CLIENT_UNINSTALLED, DOMException.MSG_CLIENT_UNINSTALLED);
                    }
                } else {//interface=auto
                    if (hasSinaAppInstalled) {
                        if (TextUtils.isEmpty(_content)){// TODO: 2016/10/12 分享新浪微博客户端时，文本内容不能为空，否则进程阻塞，分享不会回调。
                            OnSendEnd(pWebViewImpl,false, DOMException.CODE_SHARE_SEND_ERROR, DOMException.MSG_SHARE_SEND_CONTENT_EMPTY_ERROR);
                            return;
                        }
                        shareHandler.shareMessage(getWeiboMultiMessage(pWebViewImpl, pShareMsg, type),true);
                    } else {
                        WeiboMultiMessage message = getWeiboMultiMessage(pWebViewImpl, pShareMsg, type);
                        if(message.imageObject != null && ((message.imageObject.imageData != null && message.imageObject.imageData.length > 128000)
                                || (message.imageObject.thumbData != null && message.imageObject.thumbData.length > 128000))) {
                            OnSendEnd(pWebViewImpl,false, DOMException.CODE_SHARE_SEND_ERROR, "当前手机无新浪客户端！web分享图片不能超过128KB！");
                            return;
                        }
                        if(!PdrUtil.isEmpty(type) && (type.equals("video") || message.multiImageObject != null)) {
                            OnSendEnd(pWebViewImpl,false, DOMException.CODE_SHARE_SEND_ERROR, "当前手机无新浪客户端！无法分享视频及多图！");
                            return;
                        }
                        startWebShare(pWebViewImpl.getActivity(), message);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            OnSendEnd(pWebViewImpl,false, DOMException.CODE_SHARE_SEND_ERROR, SHARE_CONTENT_ERROR);
        }



    }

    /**
     * 授权回收接口，帮助开发者主动取消用户的授权。
     */
    private static final String URL_REVOKE_OAUTH = "https://api.weibo.com/oauth2/revokeoauth2";

    /**
     * 新浪微博取消授权
     * @param pWebViewImpl
     */
    @Override
    public void forbid(  IWebview pWebViewImpl) {
        final Context context = pWebViewImpl.getActivity();
        ThreadPool.self().addThreadTask(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mAccessToken == null) {
                            mAccessToken = AccessTokenKeeper.readAccessToken(context);
                        }
                        if (!PdrUtil.isEmpty(mAccessToken.getToken())) {
                            Logger.e("ian","forbid  mAccessToken.getToken()=="+mAccessToken.getToken());
                            StringBuffer buffer = new StringBuffer();
                            buffer.append("access_token=" + mAccessToken.getToken());
                            byte[] resultByte =NetTool.httpPost(URL_REVOKE_OAUTH, buffer.toString(), null);
                            String resultStr = new String(resultByte);
                            Logger.e("ian", "logout resultStr==" + resultStr);
                            AccessTokenKeeper.clear(context);
                            mAccessToken = null;
                        }
                    }
                });
    }

    String tAuthorizeCallbackId = null;
    /**
     * 注意：SsoHandler 仅当 SDK 支持 SSO 时有效
     */
    private SsoHandler mSsoHandler;

    /**
     * 新浪微博授权
     * @param pWebViewImpl
     * @param pCallbackId
     * @param options
     */
    @Override
    public void authorize(IWebview pWebViewImpl, String pCallbackId,String options) {

        if (mAccessToken == null) {
            mAccessToken = AccessTokenKeeper.readAccessToken(pWebViewImpl.getActivity());
        }
        if (mAccessToken != null && mAccessToken.isSessionValid()) {//目前的token还有效
            JSUtil.execCallback(pWebViewImpl, pCallbackId, getJsonObject(pWebViewImpl), JSUtil.OK, true, false);
            return;
        }
        WbSdk.install(pWebViewImpl.getActivity(), new AuthInfo(pWebViewImpl.getActivity(), APP_KEY, REDIRECT_URL, SCOPE));

        JSONObject jsonOptions=JSONUtil.createJSONObject(options);
        if(jsonOptions != null){
            APP_KEY = jsonOptions.optString(KEY_APPKEY, APP_KEY);
            REDIRECT_URL = jsonOptions.optString(KEY_REDIRECT_URI, REDIRECT_URL);
        }
        if( TextUtils.isEmpty(APP_KEY)||TextUtils.isEmpty(REDIRECT_URL)) {
            String msg = StringUtil.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_BUSINESS_PARAMETER_HAS_NOT, DOMException.toString(DOMException.MSG_BUSINESS_PARAMETER_HAS_NOT));
            JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.ERROR, true, false);
            return;
        }
        tAuthorizeCallbackId = pCallbackId;
        if (mSsoHandler == null) {
            mSsoHandler = new SsoHandler(pWebViewImpl.getActivity());
        }

        final IApp app = pWebViewImpl.obtainApp();
        app.registerSysEventListener(new ISysEventListener() {
            @Override
            public boolean onExecute(SysEventType pEventType, Object pArgs) {
                Object[] _args = (Object[]) pArgs;
                int requestCode = (Integer) _args[0];
                int resultCode = (Integer) _args[1];
                Intent data = (Intent) _args[2];
                app.unregisterSysEventListener(this, SysEventType.onActivityResult);
                if (mSsoHandler != null) {
                    mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
                }
                return false;
            }
        }, SysEventType.onActivityResult);
        mSsoHandler.authorize(new SelfWbAuthListener(pWebViewImpl));
    }

    /**
     * 新浪微博授权
     * @param wbAuthListener
     */
    public void authorize(final Activity activity, WbAuthListener wbAuthListener) {
        if (mAccessToken == null) {
            mAccessToken = AccessTokenKeeper.readAccessToken(activity);
        }
        if (mAccessToken != null && mAccessToken.isSessionValid()) {//目前的token还有效
            return;
        }
        WbSdk.install(activity, new AuthInfo(activity, APP_KEY, REDIRECT_URL, SCOPE));
        if (mSsoHandler == null) {
            mSsoHandler = new SsoHandler(activity);
        }
        if(activity instanceof ISysEventDispatch){
            ((ISysEventDispatch)activity).registerSysEventListener(new ISysEventListener() {
                @Override
                public boolean onExecute(SysEventType pEventType, Object pArgs) {
                    Object[] _args = (Object[]) pArgs;
                    int requestCode = (Integer) _args[0];
                    int resultCode = (Integer) _args[1];
                    Intent data = (Intent) _args[2];
                    ((ISysEventDispatch)activity).unRegisterSysEventListener(this,SysEventType.onActivityResult);
                    if (mSsoHandler != null) {
                        mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
                    }
                    return false;
                }
            },SysEventType.onActivityResult);
        }
        mSsoHandler.authorize(wbAuthListener);
    }

    /**
     * 新浪微博登陆授权回调接口
     */
    private class SelfWbAuthListener implements com.sina.weibo.sdk.auth.WbAuthListener {
        IWebview mWebview = null;
        SelfWbAuthListener(IWebview webview) {
            mWebview = webview;
        }
        private void OnAuthorizeEnd(boolean suc, int errorCode, String msg) {
            if (tAuthorizeCallbackId != null) {
                if (suc) {
                    JSUtil.execCallback(mWebview, tAuthorizeCallbackId, getJsonObject(mWebview), JSUtil.OK, true, false);
                } else {
                    String errorMsg = StringUtil.format(DOMException.JSON_ERROR_INFO, errorCode, msg);
                    JSUtil.execCallback(mWebview, tAuthorizeCallbackId, errorMsg, JSUtil.ERROR, true, false);
                }
                tAuthorizeCallbackId = null;
            }
        }
        @Override
        public void onSuccess(Oauth2AccessToken token) {
            mAccessToken = token;
            if (mAccessToken.isSessionValid()) {
                // 保存 Token 到 SharedPreferences
                Logger.e("ian","authorize onSuccess mAccessToken.getToken()=="+mAccessToken.getToken());
                AccessTokenKeeper.writeAccessToken(mWebview.getActivity(), mAccessToken);
                OnAuthorizeEnd(true, -1, null);
            }
        }
        @Override
        public void cancel() {
            OnAuthorizeEnd(false, DOMException.CODE_USER_CANCEL, DOMException.toString(DOMException.MSG_USER_CANCEL));
        }

        @Override
        public void onFailure(WbConnectErrorMessage errorMessage) {
            OnAuthorizeEnd(false, DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString(errorMessage.getErrorCode(), "Share新浪分享", errorMessage.getErrorMessage(), mLink));
        }
    }

    @Override
    public void dispose() {
        mActivity = null;
    }

    /**
     * 启动网页分享
     * @param activity
     * @param message
     */
    public void startWebShare(Activity activity,WeiboMultiMessage message) {
        Intent webIntent = new Intent(activity, WbShareTransActivity.class);
        String appPackage = activity.getPackageName();
        ShareWebViewRequestParam webParam = new ShareWebViewRequestParam(WbSdk.getAuthInfo(), WebRequestType.SHARE, "", 1, "微博分享", (String)null, activity);
        webParam.setContext(activity);
        webParam.setHashKey("");
        webParam.setPackageName(appPackage);
        Oauth2AccessToken token = AccessTokenKeeper.readAccessToken(activity);
        if(token != null && !TextUtils.isEmpty(token.getToken())) {
            Logger.e("ian","startWebShare token.getToken()=="+token.getToken());
            webParam.setToken(token.getToken());
        }
        webParam.setMultiMessage(message);
        Bundle bundle = new Bundle();
        webParam.fillBundle(bundle);
        webIntent.putExtras(bundle);
        webIntent.putExtra("startFlag", 0);
        webIntent.putExtra("startActivity", activity.getClass().getName());
        webIntent.putExtra("startAction", "com.sina.weibo.sdk.action.ACTION_WEIBO_ACTIVITY");
        webIntent.putExtra("gotoActivity", "com.sina.weibo.sdk.web.WeiboSdkWebActivity");
        activity.startActivity(webIntent);
    }

    /**
     * 获取分享消息
     * @param message
     * @return
     */
    public  WeiboMultiMessage getWeiboMultiMessage(String message) {
        WeiboMultiMessage weiboMessage = new WeiboMultiMessage();
        try {
            JSONObject pShareMsg = new JSONObject(message);
            weiboMessage.textObject = getTextObj(pShareMsg);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return weiboMessage;
    }

    /**
     * 获取分享消息
     * @param pWebViewImpl
     * @param message
     * @return
     */
    public  WeiboMultiMessage getWeiboMultiMessage(IWebview pWebViewImpl, String message, String type) {
        //WbSdk.supportMultiImage(pWebViewImpl.getContext());
        WeiboMultiMessage weiboMessage = new WeiboMultiMessage();
        JSONObject pShareMsg = null;
        try {
            pShareMsg = new JSONObject(message);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        weiboMessage.textObject = getTextObj(pShareMsg);
        if(!TextUtils.isEmpty(type) && type.equals("text")) {

        } else if(!TextUtils.isEmpty(type) && type.equals("image")) {
            JSONArray _pictures = pShareMsg.optJSONArray("pictures");
            if(_pictures != null && _pictures.length()>0) {
                if(_pictures.length() >1) {
                    weiboMessage.multiImageObject = getMultiImageObject(pWebViewImpl, pShareMsg);
                } else {
                    weiboMessage.imageObject = getImageObject(pWebViewImpl, pShareMsg);
                }
            }
        } else if(!TextUtils.isEmpty(type) && type.equals("video")) {
            weiboMessage.videoSourceObject = getVideoObject(pWebViewImpl, pShareMsg);
        } else if(!TextUtils.isEmpty(type) && type.equals("web")) {
            weiboMessage.imageObject = getImageObject( pWebViewImpl,pShareMsg);
        } else if(!TextUtils.isEmpty(type)) {
            String errorMsg = StringUtil.format(DOMException.JSON_ERROR_INFO, -100, "type参数无法正确识别，请按规范范围填写");
            JSUtil.execCallback(pWebViewImpl, tAuthorizeCallbackId, errorMsg, JSUtil.ERROR, true, false);
        } else {
            //weiboMessage.textObject = getTextObj(pShareMsg);
            weiboMessage.imageObject = getImageObject( pWebViewImpl,pShareMsg);
        }

        //weiboMessage.imageObject = getImageObject( pWebViewImpl,message);
//        JSONObject object = new JSONObject();
//        try {
//            object.put("content", "我正在使用HBuilder+HTML5开发移动应用，赶紧跟我一起来体验！");
//            object.put("title", "分享");
//            object.put("href", "http://www.baidu.com");
////            object.put("pictures", "[\"file:\\/\\/\\/storage\\/emulated\\/0\\/dcloud_icon_test.png\"]");
//            JSONArray array = new JSONArray();
//            array.put("file:///storage/emulated/0/dcloud_icon_test.png");
//            object.put("thumbs", array);
//            object.put("media", "file:///storage/emulated/0/movies/DOOV.mp4");
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
        return weiboMessage;
    }


    /**
     * 创建文本消息对象。
     *
     * @return 文本消息对象。
     */
    private TextObject getTextObj(JSONObject pShareMsg) {
        try {
            String _content = pShareMsg.optString("content");
            String _title = pShareMsg.optString("title");
            String href = JSONUtil.getString(pShareMsg, "href");
            TextObject textObject = new TextObject();
            textObject.text = _content + (TextUtils.isEmpty(href)? "":href);
            textObject.title=_title;
            return textObject;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 创建图片消息对象。大小不超过32k
     *
     * @param pWebViewImpl
     * @param pShareMsg
     * @return
     */
    private ImageObject getImageObject(IWebview pWebViewImpl, JSONObject pShareMsg) {
        final ImageObject imageObject = new ImageObject();
        try {
            JSONArray _pictures = pShareMsg.optJSONArray("pictures");
            String file = JSONUtil.getString(_pictures, 0);
            if (_pictures != null && _pictures.length() > 0 && !PdrUtil.isEmpty(_pictures.getString(0))) {
                if (BaseInfo.isQihooLifeHelper(pWebViewImpl.getContext()) && URLUtil.isNetworkUrl(file)) {
//                    final String f_url = file;
//                    new Thread() {
//                        public void run() {
//                            try {
//                                Bitmap bitmap = BitmapFactory.decodeStream(new URL(f_url).openStream());
//                                imageObject.setImageObject(bitmap);
//                            } catch (Exception e) {
//
//                            }
//                        }
//                    }.start();
                } else {
                    file = pWebViewImpl.obtainFrameView().obtainApp().convert2LocalFullPath(pWebViewImpl.obtainFullUrl(), file);
                    Bitmap bitmap;
                    // 适配AndroidQ，沙盒外文件皆以"content://"开头
                    if (file.startsWith("content://")) {
                        InputStream inputStream = pWebViewImpl.getContext().getContentResolver().openInputStream(Uri.parse(file));
                        bitmap = BitmapFactory.decodeStream(inputStream);
                    } else {
                        bitmap = BitmapFactory.decodeFile(file);
                    }
                    imageObject.setImageObject(bitmap);
                }
            }
            JSONArray thumbs = pShareMsg.optJSONArray("thumbs");
            String thumb = JSONUtil.getString(thumbs, 0);
            if (thumbs != null && thumbs.length() > 0 && !PdrUtil.isEmpty(thumbs.getString(0))) {
                if (BaseInfo.isQihooLifeHelper(pWebViewImpl.getContext()) && URLUtil.isNetworkUrl(thumb)) {
//                    final String f_url = thumb;
//                    new Thread() {
//                        public void run() {
//                            try {
//                                Bitmap bitmap = BitmapFactory.decodeStream(new URL(f_url).openStream());
//                                imageObject.setImageObject(bitmap);
//                            } catch (Exception e) {
//
//                            }
//                        }
//                    }.start();
                } else {
                    thumb = pWebViewImpl.obtainFrameView().obtainApp().convert2LocalFullPath(pWebViewImpl.obtainFullUrl(), thumb);
//                    Bitmap bitmap = BitmapFactory.decodeFile(thumb);
                    Bitmap bitmap;
                    // 适配AndroidQ，沙盒外文件皆以"content://"开头
                    if (file.startsWith("content://")) {
                        InputStream inputStream = pWebViewImpl.getContext().getContentResolver().openInputStream(Uri.parse(thumb));
                        bitmap = BitmapFactory.decodeStream(inputStream);
                    } else {
                        bitmap = BitmapFactory.decodeFile(thumb);
                    }
                    imageObject.setThumbImage(bitmap);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return imageObject;
    }

    /***
     * 创建多图
     * @return
     */
    private MultiImageObject getMultiImageObject(IWebview pWebViewImpl, JSONObject pShareMsg){
        MultiImageObject multiImageObject = new MultiImageObject();
        try {
            if(pShareMsg == null) {
                return null;
            }
            //pathList设置的是本地本件的路径,并且是当前应用可以访问的路径，现在不支持网络路径（多图分享依靠微博最新版本的支持，所以当分享到低版本的微博应用时，多图分享失效
            // 可以通过WbSdk.hasSupportMultiImage 方法判断是否支持多图分享,h5分享微博暂时不支持多图）多图分享接入程序必须有文件读写权限，否则会造成分享失败
            ArrayList<Uri> pathList = new ArrayList<Uri>();
            JSONArray _pictures = pShareMsg.optJSONArray("pictures");
            if(_pictures != null) {
                for(int i = 0; i < _pictures.length(); i++) {
                    String itme = _pictures.getString(i);
                    if(!PdrUtil.isEmpty(itme) && URLUtil.isNetworkUrl(itme)) {
                        itme = pWebViewImpl.obtainFrameView().obtainApp().convert2LocalFullPath(pWebViewImpl.obtainFullUrl(), itme);
                        pathList.add(Uri.fromFile(new File(itme)));
                    }
                }
            }
            multiImageObject.setImageList(pathList);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return multiImageObject;
    }

    private VideoSourceObject getVideoObject(IWebview pWebViewImpl, JSONObject pShareMsg){
        if(!WbSdk.supportMultiImage(pWebViewImpl.getContext())) {
            return null;
        }
        //获取视频
        VideoSourceObject videoSourceObject = new VideoSourceObject();
        try {

            if (pShareMsg == null) {
                return null;
            }
            String media = pShareMsg.optString("media");

            if(!TextUtils.isEmpty(media) && !URLUtil.isNetworkUrl(media)) {
                media = pWebViewImpl.obtainFrameView().obtainApp().convert2LocalFullPath(pWebViewImpl.obtainFullUrl(), media);
                videoSourceObject.videoPath = Uri.fromFile(new File(media));
            }

        } catch (Exception e){
            e.printStackTrace();
        }

        return videoSourceObject;
    }

}
