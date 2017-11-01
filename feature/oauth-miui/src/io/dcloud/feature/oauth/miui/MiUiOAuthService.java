package io.dcloud.feature.oauth.miui;

import android.accounts.OperationCanceledException;
import android.content.Context;
import android.text.TextUtils;

import com.xiaomi.account.openauth.XiaomiOAuthFuture;
import com.xiaomi.account.openauth.XiaomiOAuthResults;
import com.xiaomi.account.openauth.XiaomiOAuthorize;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.NetTool;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.ThreadPool;
import io.dcloud.feature.oauth.BaseOAuthService;

import static io.dcloud.common.util.JSONUtil.createJSONObject;

/**
 * 小米授权服务类 2016/11/17 created  by Ian
 */
public class MiUiOAuthService extends BaseOAuthService {

    private static final String TAG = "MiUiOAuthService";

    private static final String URL_GET_ACCESS_TOKEN = "https://account.xiaomi.com/oauth2/token?client_id=%s&redirect_uri=%s&client_secret=%s&grant_type=authorization_code&code=%s";
    private static final String URL_REFRESH_TOKEN = "https://account.xiaomi.com/oauth2/token?client_id=%s&redirect_uri=%s&client_secret=%s&grant_type=refresh_token&refresh_token=%s";
    private static final String URL_GET_USERINFO = "https://open.account.xiaomi.com/user/profile?token=%s&clientId=%s";
    private static final String KEY_CODE = "code";
    private static final String KEY_DATA = "data";
    private static final String KEY_ERROR = "error";
    private static final String KEY_ERROR_DESCRIPTION = "error_description";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final int OK = 0;
    protected static String appId = null;
    protected static String appSecret = null;
    protected static String redirectUri = null;
    protected static String appKEY = null;

    @Override
    public void init(Context context) {
        super.init(context);
        id = "xiaomi";
        description = "小米";
    }

    @Override
    public void login(IWebview pWebViewImpl, JSONArray pJsArgs) {
        super.login(pWebViewImpl, pJsArgs);
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

    @Override
    public void initMetaData() {
        appId = AndroidResources.getMetaValue("MIUI_APPID");
        if (!TextUtils.isEmpty(appId))
            appId = appId.substring(1);
        Logger.e(TAG, "initMetaData: appId" + appId);

        redirectUri = AndroidResources.getMetaValue("MIUI_REDIRECT_URI");
        appSecret = AndroidResources.getMetaValue("MIUI_APPSECRET");
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

    public boolean hasFullConfigData() {
        return !TextUtils.isEmpty(appId) && !TextUtils.isEmpty(redirectUri) && !TextUtils.isEmpty(appSecret);
    }

    /**
     * 因需要请求网络，所以将之放入子线程
     */
    private void loginInThread() {

        //获取Sharepreference中保存的授权字符串，请求服务器检测access_token是否有效
        String s_authResult = getValue(BaseOAuthService.KEY_AUTHRESULT);
        JSONObject authResultJson = createJSONObject(s_authResult);
        if (authResultJson != null && authResultJson.has(KEY_ACCESS_TOKEN)) {
            //因小米官方未提供检测token是否有效的api，所以暂时使用获取用户信息的api代替
            String check_token_url = String.format(URL_GET_USERINFO, authResultJson.optString(KEY_ACCESS_TOKEN), appId);
            String str = new String(NetTool.httpGet(check_token_url));
            if (str.contains("&&&START&&&"))
                str = str.replace("&&&START&&&", "");
            JSONObject checkTokenResult = createJSONObject(str);
            if (checkTokenResult != null) {
                if (checkTokenResult.optInt(KEY_CODE) == 0) {//成功
                    //access_token依然有效，不需要重新授权，将结果保存到Sharepreference
                    authResult = authResultJson;
                    onLoginCallBack(mLoginWebViewImpl, mLoginCallbackId, OK);
                    return;
                } else {
                    //access_token无效，需要重新授权
                    removeToken();
                    //刷新access_token
                    String refresh_token = authResultJson.optString(KEY_REFRESH_TOKEN);
                    //refresh_token拥有较长的有效期（10年），当refresh_token失效的后，需要用户重新授权。
                    String refresh_token_url = String.format(URL_REFRESH_TOKEN, appId, redirectUri, appSecret, refresh_token);
                    String s_refreshToken = refreshToken(refresh_token_url);//authResult
                    if (!PdrUtil.isEmpty(s_refreshToken)) {

                        if (s_refreshToken.contains("&&&START&&&"))
                            s_refreshToken = s_refreshToken.replace("&&&START&&&", "");
                        JSONObject refreshTokenJson = createJSONObject(s_refreshToken);
                        if (refreshTokenJson.optInt(KEY_CODE) == 0) {//成功

                            authResult = refreshTokenJson;
                            try {
                                authResult.put(KEY_OPENID,refreshTokenJson.optString("openId"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            saveValue(BaseOAuthService.KEY_AUTHRESULT, authResult.toString());
                            onLoginCallBack(mLoginWebViewImpl, mLoginCallbackId, OK);
                            return;
                        } else {
                            /*"error": "error_code",
                            "error_description": "错误描述",*/
                            int errcode = JSONUtil.getInt(refreshTokenJson, KEY_ERROR);
                            String errmsg = JSONUtil.getString(refreshTokenJson, KEY_ERROR_DESCRIPTION);
                            onLoginFinished(getErrorJsonbject(errcode, errmsg), false);
                        }
                    } else {
                        onLoginCallBack(mLoginWebViewImpl, mLoginCallbackId, DOMException.CODE_UNKNOWN_ERROR);
                    }


                }
            }
        }
        gotoAuthorize();
    }


    /**
     * 第一次授权 或 access_token失效后，重新授权,分两步：1获取code，2通过code获取token；
     */
    private void gotoAuthorize() {
        int[] scopes = {1};
        Long appId_l = null;
        if (!TextUtils.isEmpty(appId)) {

            try {
                appId_l = Long.valueOf(appId);
                Logger.e(TAG, "gotoAuthorize: appId_l" + appId_l);
            } catch (Exception e) {
                String msg = DOMException.toStringForThirdSDK("Oauth", description, "MIUI_APPID invalid");
                onLoginFinished(getErrorJsonbject(DOMException.CODE_BUSINESS_INTERNAL_ERROR, msg), false);
                return;
            }

            XiaomiOAuthFuture<XiaomiOAuthResults> future = new XiaomiOAuthorize()
                    .setAppId(appId_l)
                    .setRedirectUrl(redirectUri)
                    .setScope(scopes)
                    .startGetOAuthCode(mLoginWebViewImpl.getActivity());
            try {
                XiaomiOAuthResults results = future.getResult();
                getAccessTokenWithCode(results);
            } catch (OperationCanceledException e) {
                e.printStackTrace();
                onLoginFinished(getErrorJsonbject(DOMException.CODE_USER_CANCEL, DOMException.MSG_USER_CANCEL), false);
            } catch (Exception e) {
                e.printStackTrace();
                String msg = DOMException.toStringForThirdSDK("Oauth", description, e.getMessage());
                onLoginFinished(getErrorJsonbject(DOMException.CODE_BUSINESS_INTERNAL_ERROR, msg), false);
            }
        }

    }

    /**
     * 通过code获取token
     *
     * @param xiaomiOAuthResults
     */
    private void getAccessTokenWithCode(XiaomiOAuthResults xiaomiOAuthResults) {
        String code = xiaomiOAuthResults.getCode();

        String access_token_url = String.format(URL_GET_ACCESS_TOKEN, appId, redirectUri, appSecret, code);
        String s_access_token = getToken(access_token_url);
        if (s_access_token.contains("&&&START&&&"))
            s_access_token = s_access_token.replace("&&&START&&&", "");
        JSONObject result = createJSONObject(s_access_token);
        if (result == null) {
            //授权失败
            onLoginCallBack(mLoginWebViewImpl, mLoginCallbackId, DOMException.CODE_UNKNOWN_ERROR);
        } else if (result.has(KEY_ERROR)) {
            /*"error": "error_code",
            "error_description": "错误描述",*/
            int errcode = JSONUtil.getInt(result, KEY_ERROR);
            String errmsg = JSONUtil.getString(result, KEY_ERROR_DESCRIPTION);
            onLoginFinished(getErrorJsonbject(errcode, errmsg), false);
        } else {
            JSONObject tempauthResult = JSONUtil.createJSONObject(s_access_token);
            authResult=tempauthResult;
            try {
                authResult.put(KEY_OPENID,tempauthResult.optString("openId"));
            } catch (JSONException e) {
                e.printStackTrace();
            }
            saveValue(BaseOAuthService.KEY_AUTHRESULT, authResult.toString());
            onLoginCallBack(mLoginWebViewImpl, mLoginCallbackId, OK);
        }
    }

    /**
     * 处理结果给前端js
     *
     * @param pWebViewImpl
     * @param pCallbackId
     * @param code
     */
    private void onLoginCallBack(IWebview pWebViewImpl,
                                 String pCallbackId, int code) {
        String errorMsg = DOMException.MSG_SHARE_AUTHORIZE_ERROR;
        if (code == OK) {
            JSUtil.execCallback(pWebViewImpl, pCallbackId, makeResultJSONObject(), JSUtil.OK, false);
        } else if (code == DOMException.CODE_UNKNOWN_ERROR) {
            String msg = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.MSG_UNKNOWN_ERROR, code);
            JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.ERROR, true, false);
        } else {
            String msg = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, errorMsg, code);
            JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.ERROR, true, false);
        }
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



    /**
     * 从SharedPreferences中获取授权结果，并取出token，请求服务器获取用户信息，并保存到SharedPreferences
     *
     * @return
     */
    private boolean initUserInfo() {
        boolean suc = false;
        String s_authResult = getValue(BaseOAuthService.KEY_AUTHRESULT);

        authResult = createJSONObject(s_authResult);
        if (authResult != null && authResult.has(KEY_ACCESS_TOKEN)) {

            String get_userInfo_url = String.format(URL_GET_USERINFO, authResult.opt(KEY_ACCESS_TOKEN), appId);
            String s_userInfoResult = getUserInfo(get_userInfo_url);

            JSONObject userInfoResult = createJSONObject(s_userInfoResult);
            if (userInfoResult != null && userInfoResult.has(KEY_CODE)) {
                if (userInfoResult.optInt(KEY_CODE) == 0) {
                    try {
                    String s_data = userInfoResult.optString(KEY_DATA);
                    JSONObject data = JSONUtil.createJSONObject(s_data);
                        userInfo =data;
                        userInfo.put(KEY_NICKNAME, data.optString("miliaoNick"));
                        userInfo.put(KEY_HEADIMGURL, data.optString("miliaoIcon"));
                        userInfo.put("userId", data.optString("userId"));
                        userInfo.put(KEY_OPENID, authResult.optString("openid"));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    saveValue(BaseOAuthService.KEY_USERINFO, userInfo.toString());
                    suc = true;
                }
            }

        }
        return suc;
    }


}
