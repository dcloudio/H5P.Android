package io.dcloud.feature.oauth.sina;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

import com.sina.weibo.sdk.WbSdk;
import com.sina.weibo.sdk.auth.AuthInfo;
import com.sina.weibo.sdk.auth.Oauth2AccessToken;
import com.sina.weibo.sdk.auth.WbConnectErrorMessage;
import com.sina.weibo.sdk.auth.sso.SsoHandler;
import com.sina.weibo.sdk.auth.AccessTokenKeeper;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.ISysEventListener.SysEventType;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.NetTool;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.ThreadPool;
import io.dcloud.feature.oauth.BaseOAuthService;

import static io.dcloud.common.util.JSONUtil.createJSONObject;

/**
 * 新浪微博登陆
 *
 * @author shutao
 * @date 2015-05-07
 */
public class SinaOAuthService extends BaseOAuthService {

    public static final String TAG = "SinaOAuthService";

    private static final String SCOPE =
            "email,direct_messages_read,direct_messages_write,"
                    + "friendships_groups_read,friendships_groups_write,statuses_to_me_read,"
                    + "follow_app_official_microblog," + "invitation_write";


    private static final String UID = "uid";
    private static final String TOKEN = "token";
    private static final String EXPIRES_TIME = "expires_time";


    private Context mContext;

    protected static String redirectUri = null;
    protected static String appKEY = null;


    /**
     * 封装了 "access_token"，"expires_in"，"refresh_token"，并提供了他们的管理功能
     */
    private Oauth2AccessToken mAccessToken;
    /**
     * 注意：SsoHandler 仅当 SDK 支持 SSO 时有效
     */
    private SsoHandler mSsoHandler;
    /**
     * 获取用户信息接口
     */
    private static final String URL_GET_USERINFO = "https://api.weibo.com/2/users/show.json?access_token=%s&uid=%s";
    /**
     * 通过获取uid验证token是否有效
     */
    private static final String URL_GETUID = "https://api.weibo.com/2/account/get_uid.json?access_token=%s";

    /**
     * 授权回收接口，帮助开发者主动取消用户的授权。
     */
    private static final String URL_REVOKE_OAUTH = "https://api.weibo.com/oauth2/revokeoauth2";

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
        super.login(pWebViewImpl, pJsArgs);
        WbSdk.install(pWebViewImpl.getActivity(), new AuthInfo(pWebViewImpl.getActivity(), appKEY, redirectUri, SCOPE));
        if (hasGeneralError(mLoginWebViewImpl, mLoginCallbackId)) {
            return;
        }

        ThreadPool.self().addThreadTask(
                new Runnable() {
                    @Override
                    public void run() {
                        loginInThread();
                    }
                }
        );
    }

    /**
     * 因需要请求网络，所以将之放入子线程
     */
    private void loginInThread() {

        //首先判断sharepreference中的token是否为空，
        // 不为空检验是否有效，无效时需要继续授权操作，有效时就回调前端sharepreference保存的授权结果
        //为空 需要继续授权操作
        if (mAccessToken == null) {
            mAccessToken = AccessTokenKeeper.readAccessToken(mContext);
        }
        if (mAccessToken != null && mAccessToken.isSessionValid()) {
            if (!PdrUtil.isEmpty(mAccessToken.getToken())) {
                String get_uid_url = String.format(URL_GETUID, mAccessToken.getToken());
                String resultStr = getToken(get_uid_url);
                if (resultStr!=null){
                    JSONObject getUidResult = JSONUtil.createJSONObject(resultStr);
                    if(!PdrUtil.isEmpty(getUidResult.optString("uid"))) {
                        authResult = getSinaAuthResultJB(mAccessToken);
                        onLoginFinished(makeResultJson(), true);
                        return;
                    }
                }
            }
        }
        if (mSsoHandler == null) {
                mSsoHandler = new SsoHandler(mLoginWebViewImpl.getActivity());
            }
            mSsoHandler.authorize(new SelfWbAuthListener());
            mLoginWebViewImpl.obtainApp().registerSysEventListener(new ISysEventListener() {
                @Override
                public boolean onExecute(SysEventType pEventType, Object pArgs) {
                    // TODO Auto-generated method stub
                    Object[] _args = (Object[]) pArgs;
                    int requestCode = (Integer) _args[0];
                    int resultCode = (Integer) _args[1];
                    Intent data = (Intent) _args[2];
                    // SSO 授权回调
                    // 重要：发起 SSO 登陆的 Activity 必须重写 onActivityResults
                    if (mSsoHandler != null) {
                        mSsoHandler.authorizeCallBack(requestCode, resultCode, data);
                    }
                    if (mLoginWebViewImpl != null) {
                        mLoginWebViewImpl.obtainApp().unregisterSysEventListener(this, SysEventType.onActivityResult);
                    }
                    return false;
                }
            }, SysEventType.onActivityResult);

    }

    /**
     * 新浪微博登陆授权回调接口
     */
    private class SelfWbAuthListener implements com.sina.weibo.sdk.auth.WbAuthListener {
        @Override
        public void onSuccess(final Oauth2AccessToken token) {
            mAccessToken = token;
            if (mAccessToken.isSessionValid()) {
                // 保存 Token 到 SharedPreferences
                AccessTokenKeeper.writeAccessToken(mContext, mAccessToken);
                authResult = getSinaAuthResultJB(mAccessToken);
                JSONObject sucJSON = makeResultJSONObject();
                onLoginFinished(sucJSON, true);
            }
        }

        @Override
        public void cancel() {
            onLoginFinished(getErrorJsonbject(DOMException.CODE_USER_CANCEL, DOMException.MSG_USER_CANCEL), false);
        }

        @Override
        public void onFailure(WbConnectErrorMessage errorMessage) {
            onLoginFinished(getErrorJsonbject(errorMessage.getErrorCode(), errorMessage.getErrorMessage()), false);
        }
    }

    @Override
    public void logout(IWebview pWebViewImpl, JSONArray pJsArgs) {
        // TODO Auto-generated method stub
        super.logout(pWebViewImpl, pJsArgs);
        if (hasGeneralError(mLogoutWebViewImpl, mLogoutCallbackId)) {
            return;
        }
        ThreadPool.self().addThreadTask(
                new Runnable() {
                    @Override
                    public void run() {
                        if (mAccessToken == null) {
                            mAccessToken = AccessTokenKeeper.readAccessToken(mContext);
                        }
                        if (!PdrUtil.isEmpty(mAccessToken.getToken())) {

                            StringBuffer buffer = new StringBuffer();
                            buffer.append("access_token=" + mAccessToken.getToken());
                            byte[] resultByte = NetTool.httpPost(URL_REVOKE_OAUTH, buffer.toString(), null);
                            if (null != resultByte && 0 < resultByte.length) {
                                String resultStr = new String(resultByte);
                                Logger.e("ian", "logout resultStr==" + resultStr);
                                try {
                                    JSONObject resultJsonObj = new JSONObject(resultStr);
                                    if (PdrUtil.isEquals(resultJsonObj.optString("result"), "true")) {
                                        AccessTokenKeeper.clear(mContext);
                                        mAccessToken = null;
                                        userInfo = null;
                                        authResult = null;
                                        removeToken();
                                        onLogoutFinished(makeResultJson(), true);
                                    }
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                            }

                        }
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
        ThreadPool.self().addThreadTask(
                new Runnable() {
                    @Override
                    public void run() {
                        boolean suc = initUserInfo();
                        if (suc) {
                            JSONObject sucJSON = makeResultJSONObject();
                            onGetUserInfoFinished(sucJSON, suc);
                        } else {
                            // 运行此处表示登录无效或未登录 直接提示登录失效
                            onGetUserInfoFinished(getErrorJsonbject(DOMException.CODE_OAUTH_FAIL, DOMException.MSG_OAUTH_FAIL),
                                    false);
                        }
                    }
                }
        );
    }

    /**
     * 从SharedPreferences中获取授权结果，并取出token，请求服务器获取用户信息，并保存到SharedPreferences
     *
     * @return
     */
    private boolean initUserInfo() {
        boolean suc = false;
        if (mAccessToken == null) {
            mAccessToken = AccessTokenKeeper.readAccessToken(mContext);
        }
        if (mAccessToken != null && mAccessToken.isSessionValid()) {
            String get_userInfo_url = String.format(URL_GET_USERINFO, mAccessToken.getToken(), mAccessToken.getUid());
            String s_userInfoResult = getUserInfo(get_userInfo_url);
            Logger.e("ian", "inituserinfo  s_userinforesult" + s_userInfoResult);
            if (s_userInfoResult != null) {
                JSONObject userInfoResult = createJSONObject(s_userInfoResult);
                userInfo = userInfoResult;
                try {
                    userInfo.put(KEY_HEADIMGURL, userInfoResult.optString("profile_image_url"));
                    userInfo.put(KEY_NICKNAME, userInfoResult.optString("screen_name"));
                    userInfo.put(KEY_OPENID, userInfoResult.optString("idstr"));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                saveValue(BaseOAuthService.KEY_USERINFO, userInfo.toString());
                suc = true;
            }
        }
        return suc;
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
