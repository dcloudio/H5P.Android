package io.dcloud.share.sina;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.text.TextUtils;
import android.webkit.URLUtil;

import com.sina.weibo.sdk.api.ImageObject;
import com.sina.weibo.sdk.api.TextObject;
import com.sina.weibo.sdk.api.share.BaseResponse;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.constant.WBConstants;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.net.RequestListener;
import com.sina.weibo.sdk.openapi.LogoutAPI;
import com.sina.weibo.sdk.openapi.StatusesAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.net.URL;

import io.dcloud.common.DHInterface.FeatureMessageDispatcher;
import io.dcloud.common.DHInterface.IApp;
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
import io.dcloud.common.util.PdrUtil;
import io.dcloud.share.AbsWebviewClient;
import io.dcloud.share.IFShareApi;
import io.dcloud.share.ShareAuthorizeView;


/**
 * <p>
 * Description:新浪微博API管理者
 * </p>
 *
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @version 1.0
 * @Date 2013-5-27 下午4:22:56 created.
 * <p/>
 * <pre>
 * <p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-27 下午4:22:56
 * </pre>
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
    /**
     * Scope 是 OAuth2.0 授权机制中 authorize 接口的一个参数。通过 Scope，平台将开放更多的微博
     * 核心功能给开发者，同时也加强用户隐私保护，提升了用户体验，用户在新 OAuth2.0 授权页中有权利
     * 选择赋予应用的功能。
     * <p/>
     * 我们通过新浪微博开放平台-->管理中心-->我的应用-->接口管理处，能看到我们目前已有哪些接口的
     * 使用权限，高级权限需要进行申请。
     * <p/>
     * 目前 Scope 支持传入多个 Scope 权限，用逗号分隔。
     * <p/>
     * 有关哪些 OpenAPI 需要权限申请，请查看：http://open.weibo.com/wiki/%E5%BE%AE%E5%8D%9AAPI
     * 关于 Scope 概念及注意事项，请查看：http://open.weibo.com/wiki/Scope
     */
    private static String SCOPE = "email,direct_messages_read,direct_messages_write,"
            + "friendships_groups_read,friendships_groups_write,statuses_to_me_read,"
            + "follow_app_official_microblog";

    private static final String SINAWEIBO_DES = "新浪微博";
    public static final String SINAWEIBO_ID = "sinaweibo";

    public static final String KEY_APPKEY = "appkey";
    public static final String KEY_REDIRECT_URI = "redirect_uri";

    private static final String TAG = "SinaWeiboApiManager";

    private Oauth2AccessToken mAccessToken;

    /**
     * Description:初始化json对象
     *
     * @throws JSONException <pre>
     *                       <p>ModifiedLog:</p>
     *                       Log ID: 1.0 (Log编号 依次递增)
     *                       Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-28 下午12:32:16
     *                       </pre>
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
/*        APP_KEY = AndroidResources.getMetaValue("SINA_APPKEY").substring(1);
        REDIRECT_URL = AndroidResources.getMetaValue("SINA_REDIRECT_URI");*/
        if (!TextUtils.isEmpty(AndroidResources.getMetaValue("SINA_APPKEY"))){
            APP_KEY = AndroidResources.getMetaValue("SINA_APPKEY").substring(1);
        }
        REDIRECT_URL = AndroidResources.getMetaValue("SINA_REDIRECT_URI");
//		APP_KEY = "3721101999";
//		REDIRECT_URL= "http://d.m3w.cn/helloh5p/";
    }

    /**
     * Description:获取json对象的字符串
     *
     * @return <pre>
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

    class MyRequestListener implements RequestListener {
        IWebview mWebViewImpl = null;

        MyRequestListener(IWebview pWebViewImpl) {
            mWebViewImpl = pWebViewImpl;
        }

        @Override
        public void onComplete(String arg0) {
            OnSendEnd(true, -1, null);
        }

        public void OnSendEnd(boolean suc, int errorCode, String msg) {
            if (SEND_CALLBACKID != null) {
                if (suc) {
                    JSUtil.execCallback(mWebViewImpl, SEND_CALLBACKID, "", 1, false, false);
                } else {
                    String errorMsg = String.format(DOMException.JSON_ERROR_INFO, errorCode, msg);
                    JSUtil.execCallback(mWebViewImpl, SEND_CALLBACKID, errorMsg, JSUtil.ERROR, true, false);
                }
                SEND_CALLBACKID = null;
            }
        }

        @Override
        public void onWeiboException(WeiboException arg0) {
            if (arg0 != null) {
                arg0.printStackTrace();
                OnSendEnd(false, DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString(arg0.getMessage(), "Share新浪分享", arg0.getMessage(), mLink));
            }
        }
    };
    public static final String SHARE_CANEL_ERROR = "64001";
    public static final String SHARE_AUTHOR_ERROR = "64002";
    public static final String SHARE_CONTENT_ERROR = "64003";
    private String SEND_CALLBACKID = null;
    private String SEND_MSG = null;
    private boolean hasSinaAppInstalled = false;
    private String mInterface = "auto";
    private MyRequestListener mRequestListener;

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
    public void send(IWebview pWebViewImpl, String pCallbackId, String pShareMsg) {
        SEND_CALLBACKID = pCallbackId;
        mRequestListener = new MyRequestListener(pWebViewImpl);
        hasSinaAppInstalled = PlatformUtil.hasAppInstalled(pWebViewImpl.getActivity(), PACKAGENAME);
        JSONObject _msg = null;
        String file;
        try {
            _msg = new JSONObject(pShareMsg);
            if (_msg != null) {
                mInterface = _msg.optString("interface");
                JSONArray _pictures = _msg.optJSONArray("pictures");
                String _content = _msg.optString("content");
                file = JSONUtil.getString(_pictures, 0);
                if (URLUtil.isNetworkUrl(file)) {// TODO: 2016/10/12 根据5+runtime文档添加网络图片分享不支持的逻辑
                    mRequestListener.OnSendEnd(false, DOMException.CODE_SHARE_SEND_ERROR, DOMException.MSG_SHARE_SEND_PIC_ROUTE_ERROR);
                    return;
                }
                if ("slient".equals(mInterface)) {//interface=slient
                    slientShare(pWebViewImpl, pCallbackId, pShareMsg);
                } else if ("editable".equals(mInterface)) {//interface=editable
                    if (hasSinaAppInstalled) {
                        if (TextUtils.isEmpty(_content)){// TODO: 2016/10/12 分享新浪微博客户端时，文本内容不能为空，否则进程阻塞，分享不会回调。
                            mRequestListener.OnSendEnd(false, DOMException.CODE_SHARE_SEND_ERROR, DOMException.MSG_SHARE_SEND_CONTENT_EMPTY_ERROR);
                            return;
                        }
                        gotoSinaClientShare(pWebViewImpl, pCallbackId, pShareMsg);
                    } else {
                        //未安装新浪微博客户端，触发回调
                        mRequestListener.OnSendEnd(false, DOMException.CODE_CLIENT_UNINSTALLED, DOMException.MSG_CLIENT_UNINSTALLED);
                    }
                } else {//interface=auto
                    if (hasSinaAppInstalled) {
                        if (TextUtils.isEmpty(_content)){// TODO: 2016/10/12 分享新浪微博客户端时，文本内容不能为空，否则进程阻塞，分享不会回调。
                            mRequestListener.OnSendEnd(false, DOMException.CODE_SHARE_SEND_ERROR, DOMException.MSG_SHARE_SEND_CONTENT_EMPTY_ERROR);
                            return;
                        }
                        gotoSinaClientShare(pWebViewImpl, pCallbackId, pShareMsg);
                    } else {
                        slientShare(pWebViewImpl, pCallbackId, pShareMsg);
                    }
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
            mRequestListener.OnSendEnd(false, DOMException.CODE_SHARE_SEND_ERROR, SHARE_CONTENT_ERROR);
        }
    }

    /**
     * 注册微博客户端分享消息回调，并进行分享
     *
     * @param pWebViewImpl
     * @param pCallbackId
     * @param pShareMsg
     */
    private void gotoSinaClientShare(IWebview pWebViewImpl, String pCallbackId, String pShareMsg) {
        FeatureMessageDispatcher.registerListener(messageListener);
        registerSendCallbackMsg(new Object[]{pWebViewImpl, pCallbackId});
        startSinaCallbackActivity(pWebViewImpl, pShareMsg);
    }

    FeatureMessageDispatcher.MessageListener messageListener = new FeatureMessageDispatcher.MessageListener() {
        @Override
        public void onReceiver(Object msg) {
            if (msg instanceof BaseResponse) {
                executeSendCallbackMsg((BaseResponse) msg);
                FeatureMessageDispatcher.unregisterListener(messageListener);
            }
        }
    };
    Object[] sendCallbackMsg = null;

    /**
     * 保存webview和callbackid组成的数组
     *
     * @param args
     */
    private void registerSendCallbackMsg(Object[] args) {
        sendCallbackMsg = args;
    }

    /**
     * 收到新浪微博客户端返回的回调消息处理
     *
     * @param resp
     */
    void executeSendCallbackMsg(BaseResponse resp) {
        if (sendCallbackMsg != null) {
            IWebview pWebViewImpl = (IWebview) sendCallbackMsg[0];
            String pCallbackId = (String) sendCallbackMsg[1];
            if (resp != null) {
                onSendCallBack(pWebViewImpl, pCallbackId, resp);
            }
        }
    }

    /**
     * 处理回调返回的消息
     *
     * @param pWebViewImpl
     * @param pCallbackId
     * @param resp
     */
    private void onSendCallBack(final IWebview pWebViewImpl,
                                final String pCallbackId, BaseResponse resp) {
        if (resp.errCode == WBConstants.ErrorCode.ERR_OK) {
            JSUtil.execCallback(pWebViewImpl, SEND_CALLBACKID, "", JSUtil.OK, false, false);
        } else if (resp.errCode == WBConstants.ErrorCode.ERR_CANCEL) {
            String msg = String.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_USER_CANCEL, DOMException.MSG_USER_CANCEL);
            JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.ERROR, true, false);
        } else {
            String msg = String.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString(resp.errCode, "新浪微博分享", resp.errMsg, mLink));
            JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.ERROR, true, false);
        }

    }

    /**
     * 进入新浪微博客户端编辑界面后分享
     *
     * @param pShareMsg
     */
    private void startSinaCallbackActivity(IWebview pWebViewImpl, String pShareMsg) {
        Intent i = new Intent();
        i.putExtra(KEY_APPKEY,APP_KEY);
        i.setClassName(mActivity, mActivity.getPackageName()+".SinaCallbackActivity");
        ImageObject imageObject = getImageObject(pWebViewImpl, pShareMsg);
        i.putExtra("imageObject", imageObject);
        TextObject textObject = getTextObj(pShareMsg);
        i.putExtra("textObject", textObject);
        mActivity.startActivity(i);
    }

    /**
     * 获取分享界面所需图片大小不超过32k
     *
     * @param pWebViewImpl
     * @param pShareMsg
     * @return
     */
    private ImageObject getImageObject(IWebview pWebViewImpl, String pShareMsg) {
        final ImageObject imageObject = new ImageObject();
        try {
            JSONObject _msg = new JSONObject(pShareMsg);
            JSONArray _pictures = _msg.optJSONArray("pictures");
            String file = JSONUtil.getString(_pictures, 0);
            if (_pictures != null && _pictures.length() > 0 && !PdrUtil.isEmpty(_pictures.getString(0))) {
                if (BaseInfo.isQihooLifeHelper(pWebViewImpl.getContext()) && URLUtil.isNetworkUrl(file)) {
                    final String f_url = file;
                    new Thread() {
                        public void run() {
                            try {
                                Bitmap bitmap = BitmapFactory.decodeStream(new URL(f_url).openStream());
                                imageObject.setImageObject(bitmap);
                            } catch (Exception e) {

                            }
                        }
                    }.start();
                } else {
                    file = pWebViewImpl.obtainFrameView().obtainApp().convert2AbsFullPath(pWebViewImpl.obtainFullUrl(), file);
                    Bitmap bitmap = BitmapFactory.decodeFile(file);
                    imageObject.setImageObject(bitmap);
                }
            } else {
                return null;
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
        return imageObject;
    }

    /**
     * 创建文本消息对象。
     *
     * @return 文本消息对象。
     */
    private TextObject getTextObj(String pShareMsg) {
        try {
            JSONObject _msg = new JSONObject(pShareMsg);
            String _content = _msg.optString("content");
            String href = JSONUtil.getString(_msg, "href");
            if (!TextUtils.isEmpty(href) && PdrUtil.isNetPath(href)) {
                _content += href;
            }
            TextObject textObject = new TextObject();
            textObject.text = _content;
            return textObject;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 不进入新浪微博客户端，直接分享
     *
     * @param pWebViewImpl
     * @param pCallbackId
     * @param pShareMsg
     */
    private void slientShare(IWebview pWebViewImpl, String pCallbackId, String pShareMsg) {
        if (mAccessToken != null && mAccessToken.isSessionValid()) {//判断token是否过期
            Context context = pWebViewImpl.getActivity();
            final StatusesAPI api = new StatusesAPI(context, APP_KEY, mAccessToken);
            try {
                JSONObject _msg = new JSONObject(pShareMsg);
                String _content = _msg.optString("content");
                String href = JSONUtil.getString(_msg, "href");
                if (!TextUtils.isEmpty(href) && PdrUtil.isNetPath(href)) {
                    _content += href;
                }
                JSONArray _pictures = _msg.optJSONArray("pictures");
                JSONObject geoJSON = JSONUtil.getJSONObject(_msg, "geo");
                final String lat = JSONUtil.getString(geoJSON, "latitude");
                final String lon = JSONUtil.getString(geoJSON, "longitude");
                String file = JSONUtil.getString(_pictures, 0);
                if (_pictures != null && _pictures.length() > 0 && !PdrUtil.isEmpty(_pictures.getString(0))) {
                    if (BaseInfo.isQihooLifeHelper(pWebViewImpl.getContext()) && URLUtil.isNetworkUrl(file)) {
                        final String f_content = _content;
                        final String f_url = file;
                        new Thread() {
                            public void run() {
                                try {
                                    Bitmap bmp = BitmapFactory.decodeStream(new URL(f_url).openStream());
                                    api.upload(f_content, bmp, lat, lon, mRequestListener);
                                } catch (Exception e) {
                                    mRequestListener.OnSendEnd(false, DOMException.CODE_SHARE_SEND_ERROR, SHARE_CONTENT_ERROR);
                                }
                            }
                        }.start();
                    } else {
                        file = pWebViewImpl.obtainFrameView().obtainApp().convert2AbsFullPath(pWebViewImpl.obtainFullUrl(), file);
                        Bitmap bitmap = BitmapFactory.decodeFile(file);
                        api.upload(_content, bitmap, lat, lon, mRequestListener);
                    }

                } else {
                    api.update(_content, lat, lon, mRequestListener);
                }
            } catch (JSONException e) {
                mRequestListener.OnSendEnd(false, DOMException.CODE_SHARE_SEND_ERROR, SHARE_CONTENT_ERROR);
            }
        } else {
            mRequestListener.OnSendEnd(false, DOMException.CODE_SHARE_SEND_ERROR, "token expiry");
        }
    }

    @Override
    public void forbid(IWebview pWebViewImpl) {
        Context context = pWebViewImpl.getActivity();
        LogoutAPI logoutAPI = new LogoutAPI(context, APP_KEY, mAccessToken);
        logoutAPI.logout(new RequestListener() {

            @Override
            public void onWeiboException(WeiboException arg0) {

            }

            @Override
            public void onComplete(String arg0) {

            }
        });
        AccessTokenKeeper.clear(pWebViewImpl.getActivity());
        mAccessToken = null;
    }

    String tAuthorizeCallbackId = null;

    @Override
    public void authorize(IWebview pWebViewImpl, String pCallbackId,String options) {
        if (mAccessToken != null && mAccessToken.isSessionValid()) {//目前的token还有效
            JSUtil.execCallback(pWebViewImpl, pCallbackId, getJsonObject(pWebViewImpl), JSUtil.OK, true, false);
            return;
        }
        JSONObject jsonOptions=JSONUtil.createJSONObject(options);
        if(jsonOptions != null){
            APP_KEY = jsonOptions.optString(KEY_APPKEY, APP_KEY);
            REDIRECT_URL = jsonOptions.optString(KEY_REDIRECT_URI, REDIRECT_URL);
            Logger.e(TAG, "authorize: appkey"+APP_KEY );
            Logger.e(TAG, "authorize: REDIRECT_URL"+REDIRECT_URL );
        }
        if( TextUtils.isEmpty(APP_KEY)||TextUtils.isEmpty(REDIRECT_URL)) {
            String msg = String.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_BUSINESS_PARAMETER_HAS_NOT, DOMException.toString(DOMException.MSG_BUSINESS_PARAMETER_HAS_NOT));
            JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.ERROR, true, false);
            return;
        }
        Context context = pWebViewImpl.getActivity();
        tAuthorizeCallbackId = pCallbackId;
        AuthInfo mWeibo = new AuthInfo(context, APP_KEY, REDIRECT_URL, SCOPE);
//		mWeibo.anthorize(new AuthDialogListener(pWebViewImpl));//网页授权
        final SsoHandler mSsoHandler = new SsoHandler(pWebViewImpl.getActivity(), mWeibo);
        final IApp app = pWebViewImpl.obtainFrameView().obtainApp();
        app.registerSysEventListener(new ISysEventListener() {
            @Override
            public boolean onExecute(SysEventType pEventType, Object pArgs) {
                Object[] _args = (Object[]) pArgs;
                int requestCode = (Integer) _args[0];
                int resultCode = (Integer) _args[1];
                Intent data = (Intent) _args[2];
                app.unregisterSysEventListener(this, SysEventType.onActivityResult);
                if (mSsoHandler != null) {
                    mSsoHandler.authorizeCallBack(requestCode, resultCode, data);//调用此方法后才能触发onCancel事件
                }
                return false;
            }
        }, SysEventType.onActivityResult);
        mSsoHandler.authorize(new AuthDialogListener(pWebViewImpl));//sso授权,没有客户端自动跳转到网页授权
    }


    class AuthDialogListener implements WeiboAuthListener {
        IWebview mWebview = null;

        private void OnAuthorizeEnd(boolean suc, int errorCode, String msg) {
            if (tAuthorizeCallbackId != null) {
                if (suc) {
                    JSUtil.execCallback(mWebview, tAuthorizeCallbackId, getJsonObject(mWebview), JSUtil.OK, true, false);
                } else {
                    String errorMsg = String.format(DOMException.JSON_ERROR_INFO, errorCode, msg);
                    JSUtil.execCallback(mWebview, tAuthorizeCallbackId, errorMsg, JSUtil.ERROR, true, false);
                }
                tAuthorizeCallbackId = null;
            }
        }

        AuthDialogListener(IWebview webview) {
            mWebview = webview;
        }

        @Override
        public void onComplete(Bundle values) {
            mAccessToken = Oauth2AccessToken.parseAccessToken(values);
            if (mAccessToken.isSessionValid()) {
                AccessTokenKeeper.writeAccessToken(mWebview.getActivity(), mAccessToken);
            }
            OnAuthorizeEnd(true, -1, null);
        }

        @Override
        public void onCancel() {
            OnAuthorizeEnd(false, DOMException.CODE_USER_CANCEL, DOMException.toString(DOMException.MSG_USER_CANCEL));
        }

        @Override
        public void onWeiboException(WeiboException e) {
            OnAuthorizeEnd(false, DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString(e.getLocalizedMessage(), "Share新浪分享", e.getLocalizedMessage(), mLink));
        }

    }

    public static AbsWebviewClient getWebviewClient(ShareAuthorizeView pView) {
        return new SinaWebviewClient(pView);
    }

    /**
     * 供原生代码分享调用
     *
     * @param activity
     * @param msg
     */
    public void send(final Activity activity, final String msg) {
        try {
            initData();
            initJsonObject(activity);
            if (mAccessToken != null && mAccessToken.isSessionValid()) {//判断token是否过期
                shareSina(activity, msg);
            } else {
                AuthInfo mWeibo = new AuthInfo(activity, APP_KEY, REDIRECT_URL, SCOPE);
                SsoHandler mSsoHandler = new SsoHandler(activity, mWeibo);
                mSsoHandler.authorize(new WeiboAuthListener() {

                    @Override
                    public void onWeiboException(WeiboException arg0) {
                        // TODO Auto-generated method stub

                    }

                    @Override
                    public void onComplete(Bundle values) {
                        // TODO Auto-generated method stub
                        mAccessToken = Oauth2AccessToken.parseAccessToken(values);
                        if (mAccessToken.isSessionValid()) {
                            AccessTokenKeeper.writeAccessToken(activity, mAccessToken);
                        }
                        shareSina(activity, msg);
                    }

                    @Override
                    public void onCancel() {
                        // TODO Auto-generated method stub

                    }
                });
            }
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void shareSina(Activity activity, String msg) {
        try {
            JSONObject msgJs = new JSONObject(msg);
            String content = msgJs.getString("content");
            String href = msgJs.getString("href");
            String thumbs = msgJs.getString("thumbs");
            Bitmap thumb = BitmapFactory.decodeFile(thumbs);
            StatusesAPI api = new StatusesAPI(activity, APP_KEY, mAccessToken);
            api.upload(content + href, thumb, null, null, new RequestListener() {

                @Override
                public void onWeiboException(WeiboException arg0) {
                    // TODO Auto-generated method stub

                }

                @Override
                public void onComplete(String arg0) {
                    // TODO Auto-generated method stub

                }
            });
        } catch (Exception e) {

        }

    }

    @Override
    public void dispose() {
        mActivity = null;
        messageListener = null;
        mRequestListener = null;
    }
}
