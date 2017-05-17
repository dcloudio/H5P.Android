package io.dcloud.feature.oauth.sina;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WeiboAuthListener;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.exception.WeiboException;
import com.sina.weibo.sdk.net.RequestListener;
import com.sina.weibo.sdk.openapi.LogoutAPI;
import com.sina.weibo.sdk.openapi.UsersAPI;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.ISysEventListener.SysEventType;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.feature.oauth.BaseOAuthService;

/**
 * 新浪微博登陆
 *
 * @author shutao
 * @date 2015-05-07
 */
public class SinaOAuthService extends BaseOAuthService {

    public static final String TAG = "SinaOAuthService";

    /**
     * Scope 是 OAuth2.0 授权机制中 authorize 接口的一个参数。通过 Scope，平台将开放更多的微博
     * 核心功能给开发者，同时也加强用户隐私保护，提升了用户体验，用户在新 OAuth2.0 授权页中有权利
     * 选择赋予应用的功能。
     * <p>
     * 我们通过新浪微博开放平台-->管理中心-->我的应用-->接口管理处，能看到我们目前已有哪些接口的
     * 使用权限，高级权限需要进行申请。
     * <p>
     * 目前 Scope 支持传入多个 Scope 权限，用逗号分隔。
     * <p>
     * 有关哪些 OpenAPI 需要权限申请，请查看：http://open.weibo.com/wiki/%E5%BE%AE%E5%8D%9AAPI
     * 关于 Scope 概念及注意事项，请查看：http://open.weibo.com/wiki/Scope
     */
    private static final String SCOPE =
            "email,direct_messages_read,direct_messages_write,"
                    + "friendships_groups_read,friendships_groups_write,statuses_to_me_read,"
                    + "follow_app_official_microblog," + "invitation_write";

    private AuthInfo mWeiboAuth;
    /**
     * 注意：SsoHandler 仅当 SDK 支持 SSO 时有效
     */
    private SsoHandler mSsoHandler;

    /**
     * 封装了 "access_token"，"expires_in"，"refresh_token"，并提供了他们的管理功能
     */
    private Oauth2AccessToken mAccessToken;

    private static final String UID = "uid";
    private static final String TOKEN = "token";
    private static final String EXPIRES_TIME = "expires_time";


    private Context mContext;

    protected static String redirectUri = null;
    protected static String appKEY = null;

    @Override
    public void initMetaData() {
        if (!TextUtils.isEmpty(AndroidResources.getMetaValue("SINA_APPKEY"))) {
            appKEY = AndroidResources.getMetaValue("SINA_APPKEY").substring(1);
        }
        redirectUri = AndroidResources.getMetaValue("SINA_REDIRECT_URI");
    }

    @Override
    public void initAuthOptions(JSONObject mLoginOptions) {
        if (mLoginOptions != null) {
            redirectUri = mLoginOptions.optString(BaseOAuthService.KEY_REDIRECT_URI, redirectUri);
            appKEY = mLoginOptions.optString(BaseOAuthService.KEY_APPKEY, appKEY);
        }
    }

    @Override
    public boolean hasFullConfigData() {
        return !TextUtils.isEmpty(redirectUri) && !TextUtils.isEmpty(appKEY);
    }

    @Override
    public void init(Context context) {
        // TODO Auto-generated method stub
        super.init(context);
        mContext = context;
        id = "sinaweibo";
        description = "新浪微博";

    }

    @Override
    public void login(final IWebview pWebViewImpl, JSONArray pJsArgs) {
        // TODO Auto-generated method stub
        super.login(pWebViewImpl, pJsArgs);
        if (hasGeneralError(mLoginWebViewImpl, mLoginCallbackId)) {
            return;
        }
        // 从 SharedPreferences 中读取上次已保存好 AccessToken 等信息，
        // 第一次启动本应用，AccessToken 不可用
        mAccessToken = AccessTokenKeeper.readAccessToken(pWebViewImpl.getActivity());
        if (mAccessToken != null && mAccessToken.isSessionValid()) {
            authResult = getSinaAuthResultJB(mAccessToken);
            UsersAPI usersAPI = new UsersAPI(pWebViewImpl.getActivity(), appKEY, mAccessToken);
            long uid = Long.parseLong(mAccessToken.getUid());
            usersAPI.show(uid, mRequestListener);
        } else {
            if (mWeiboAuth == null) {
                mWeiboAuth = new AuthInfo(pWebViewImpl.getActivity(), appKEY, redirectUri, SCOPE);
            }
            if (mSsoHandler == null) {
                mSsoHandler = new SsoHandler(pWebViewImpl.getActivity(), mWeiboAuth);
            }
            mSsoHandler.authorize(new AuthListener());
        }
        pWebViewImpl.obtainApp().registerSysEventListener(new ISysEventListener() {

            @Override
            public boolean onExecute(SysEventType pEventType, Object pArgs) {
                // TODO Auto-generated method stub
                Object[] _args = (Object[]) pArgs;
                int requestCode = (Integer) _args[0];
                int resultCode = (Integer) _args[1];
                Intent data = (Intent) _args[2];
                if (mSsoHandler != null) {
                    mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
                }
                if (pWebViewImpl != null) {
                    pWebViewImpl.obtainApp().unregisterSysEventListener(this, SysEventType.onActivityResult);
                }
                return false;
            }
        }, SysEventType.onActivityResult);
    }


    /**
     * 新浪微博登陆授权回调接口
     */
    class AuthListener implements WeiboAuthListener {

        @Override
        public void onCancel() {
            // TODO Auto-generated method stub
            onLoginFinished(getErrorJsonbject(DOMException.CODE_USER_CANCEL, DOMException.MSG_USER_CANCEL), false);
        }

        @Override
        public void onComplete(Bundle values) {
            // TODO Auto-generated method stub
            mAccessToken = Oauth2AccessToken.parseAccessToken(values);
            if (mAccessToken.isSessionValid()) {
                // 保存 Token 到 SharedPreferences
                authResult = getSinaAuthResultJB(mAccessToken);
                AccessTokenKeeper.writeAccessToken(mContext, mAccessToken);
                UsersAPI usersAPI = new UsersAPI(mContext, appKEY, mAccessToken);
                long uid = Long.parseLong(mAccessToken.getUid());
                usersAPI.show(uid, mRequestListener);
            } else {
                // 以下几种情况，您会收到 Code：
                // 1. 当您未在平台上注册的应用程序的包名与签名时；
                // 2. 当您注册的应用程序包名与签名不正确时；
                // 3. 当您在平台上注册的包名和签名与您当前测试的应用的包名和签名不匹配时。
                String code = values.getString("code");
                onLoginFinished(getErrorJsonbject(DOMException.CODE_BUSINESS_INTERNAL_ERROR, code), false);
            }
        }

        @Override
        public void onWeiboException(WeiboException arg0) {
            // TODO Auto-generated method stub
            onLoginFinished(getErrorJsonbject(DOMException.CODE_BUSINESS_INTERNAL_ERROR, arg0.getMessage()), false);
        }

    }

    /**
     * 新浪微博用户信息回调接口
     */
    RequestListener mRequestListener = new RequestListener() {

        @Override
        public void onWeiboException(WeiboException arg0) {
            // TODO Auto-generated method stub
            onLoginFinished(getErrorJsonbject(DOMException.CODE_BUSINESS_INTERNAL_ERROR, arg0.getMessage()), false);
        }

        @Override
        public void onComplete(String arg0) {
            // TODO Auto-generated method stub
            if (arg0 != null) {
                JSONObject tempuserInfo = JSONUtil.createJSONObject(arg0);
                userInfo = tempuserInfo;
                try {
                    userInfo.put(KEY_HEADIMGURL, tempuserInfo.optString("profile_image_url"));
                    userInfo.put(KEY_NICKNAME, tempuserInfo.optString("screen_name"));
                    userInfo.put(KEY_OPENID, tempuserInfo.optString("idstr"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                saveValue(BaseOAuthService.KEY_USERINFO, userInfo.toString());
                onLoginFinished(makeResultJson(), true);
            } else {
                onLoginFinished(getErrorJsonbject(DOMException.CODE_GET_TOKEN_ERROR, DOMException.MSG_GET_TOKEN_ERROR), false);
            }

        }
    };


    @Override
    public void logout(IWebview pWebViewImpl, JSONArray pJsArgs) {
        // TODO Auto-generated method stub
        super.logout(pWebViewImpl, pJsArgs);
        if (hasGeneralError(mLogoutWebViewImpl, mLogoutCallbackId)) {
            return;
        }
        LogoutAPI logoutAPI = new LogoutAPI(mContext, appKEY, mAccessToken);
        logoutAPI.logout(new RequestListener() {

            @Override
            public void onWeiboException(WeiboException arg0) {
                // TODO Auto-generated method stub
                onLogoutFinished(getErrorJsonbject(DOMException.CODE_BUSINESS_INTERNAL_ERROR, arg0.getMessage()), false);
            }

            @Override
            public void onComplete(String arg0) {
                // TODO Auto-generated method stub
                userInfo = null;
                authResult = null;
                AccessTokenKeeper.clear(mContext);
                removeToken();
                onLogoutFinished(makeResultJson(), true);
            }
        });
    }

    @Override
    public void getUserInfo(IWebview pWebViewImpl, JSONArray pJsArgs) {
        // TODO Auto-generated method stub
        super.getUserInfo(pWebViewImpl, pJsArgs);
        if (hasGeneralError(mGetUserInfoWebViewImpl, mGetUserInfoCallbackId)) {
            return;
        }
        mAccessToken = AccessTokenKeeper.readAccessToken(pWebViewImpl.getActivity());
        if (mAccessToken != null && mAccessToken.isSessionValid()) {
            authResult = getSinaAuthResultJB(mAccessToken);
            String userv = getValue(BaseOAuthService.KEY_USERINFO);
            if (!TextUtils.isEmpty(userv)) {
                try {
                    userInfo = new JSONObject(userv);
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                onGetUserInfoFinished(makeResultJson(), true);
                return;
            }
        }
        // 运行此处表示登录无效或未登录 直接提示登录失效
        onGetUserInfoFinished(getErrorJsonbject(DOMException.CODE_OAUTH_FAIL, DOMException.MSG_OAUTH_FAIL),
                false);
    }

    /**
     * token 数据转换JSON
     *
     * @param token
     * @return
     */
    private JSONObject getSinaAuthResultJB(Oauth2AccessToken token) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.put(KEY_EXPIRES_IN, token.getExpiresTime());
            jsonObject.put(KEY_ACCESS_TOKEN, token.getToken());
            jsonObject.put(KEY_REFRESH_TOKEN, token.getRefreshToken());
            jsonObject.put(KEY_OPENID, token.getUid());
            jsonObject.put(TOKEN, token.getToken());
            jsonObject.put(UID, token.getUid());
            jsonObject.put(EXPIRES_TIME, token.getExpiresTime());


        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return jsonObject;
    }

}
