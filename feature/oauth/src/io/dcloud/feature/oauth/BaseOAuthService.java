package io.dcloud.feature.oauth;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.dcloud.common.DHInterface.BaseFeature.BaseModule;
import io.dcloud.common.DHInterface.IReflectAble;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.PlatformUtil;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.AESHelper;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.NetTool;
import io.dcloud.common.util.PdrUtil;

public abstract class BaseOAuthService extends BaseModule implements IReflectAble {
    /**
     * 授权认证结果数据<br/>
     * 用于标识此授权登录认证服务是否已经授权登录认证过，如果已经授权登录过则保存授权登录信息，否则为undefined。<br/>
     * 例如“微信”，则可能保存以下数据：<br/>
     * code - 用户换取access_token的code; <br/>
     * lang - 微信客户端当前语言; <br/>
     * country - 微信用户当前国家信息;<br/>
     * access_token - 接口调用凭证; <br/>
     * expires_in - access_token接口调用凭证超时时间，单位（秒）;<br/>
     * refresh_token - 用户刷新access_token; <br/>
     * openid - 授权用户唯一标识; <br/>
     * scope - 用户授权的作用域，使用逗号（,）分隔。<br/>
     */
    protected JSONObject authResult;
    /**
     * 授权登录认证用户信息<br/>
     * 用于标识此授权登录认证服务是否已经获取过用户信息，如果已经通过授权登录并获取用户信息成功则保存用户相关信息，否则为undefined。<br/>
     * 例如“微信”，则可能保存以下数据： <br/>
     * openid - 普通用户的标识，对当前开发者帐号唯一;<br/>
     * nickname - 普通用户昵称; <br/>
     * sex - 普通用户性别，1为男性，2为女性;<br/>
     * province - 普通用户个人资料填写的省份; <br/>
     * city - 普通用户个人资料填写的城市; <br/>
     * country - 国家，如中国为CN; <br/>
     * headimgurl -
     * 用户头像，最后一个数值代表正方形头像大小（有0、46、64、96、132数值可选，0代表640*640正方形头像），用户没有头像时该项为空;<br/>
     * privilege - 用户特权信息，json数组，如微信沃卡用户为（chinaunicom）; <br/>
     * unionid - 用户统一标识。针对一个微信开放平台帐号下的应用，同一用户的unionid是唯一的。<br/>
     */
    protected JSONObject userInfo;

    static final protected String NULL = "null";
    static final protected String KEY_AUTHRESULT = "authResult";
    static final protected String KEY_USERINFO = "userInfo";
    static final protected String KEY_SCOPE = "scope";
    static final protected String KEY_STATE = "state";
    static final protected String KEY_APPID = "appid";
    static final protected String KEY_APPKEY = "appkey";
    static final protected String KEY_APSECRET = "appsecret";
    static final protected String KEY_REDIRECT_URI = "redirect_uri";

    protected IWebview mLoginWebViewImpl = null;
    protected String mLoginCallbackId = null;

    protected JSONObject mLoginOptions = null;
    private static final String TAG = "BaseOAuthService";

    static final protected String KEY_OPENID = "openid";
    static final protected String KEY_ACCESS_TOKEN = "access_token";
    static final protected String KEY_EXPIRES_IN = "expires_in";
    static final protected String KEY_REFRESH_TOKEN = "refresh_token";

    static final protected String KEY_HEADIMGURL = "headimgurl";
    static final protected String KEY_NICKNAME = "nickname";

    /**
     * 当用户点击登录选项，程序取AndroidManifest.xml中的appId，appSecret，redirectUri，appKEY作为默认参数，
     * 然后判断前端传过来的参数pJsArgs中有无AuthOptions  Json对象，有就对上述参数重新赋值。
     *
     * @param pWebViewImpl
     * @param pJsArgs
     */
    public void login(IWebview pWebViewImpl, JSONArray pJsArgs) {


        initMetaData();
        mLoginWebViewImpl = pWebViewImpl;
        try {
            mLoginCallbackId = pJsArgs.getString(1);
            mLoginOptions = pJsArgs.optJSONObject(2);
            initAuthOptions(mLoginOptions);

        } catch (JSONException e) {
            e.printStackTrace();
        }
    }


    protected IWebview mAuthWebview = null;
    protected String mAuthCallbackId = null;
    protected JSONObject mAuthOptions = null;
    //写死只能是微信
    public void authorize(IWebview pwebview, JSONArray pJsArgs){
        initMetaData();
        mAuthWebview = pwebview;
        mAuthCallbackId = pJsArgs.optString(1,"");
        mAuthOptions = pJsArgs.optJSONObject(2);
        initAuthOptions(mAuthOptions);
    }

    /**
     * 子类重写此方法，获取AndroidMenifest.xml中的值。
     */
    public void initMetaData() {
    }

    /**
     * 子类重写此方法，获取login方法参数传过来的值。
     */
    public void initAuthOptions(JSONObject mLoginOptions) {
    }

    /**
     * 子类调用此方法，防止参数不全，程序继续执行导致出错。
     *
     * @return
     */
    public boolean hasGeneralError(IWebview pWebViewImpl, String pCallbackId) {
        if (!hasFullConfigData()) {
            String msg = String.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_BUSINESS_PARAMETER_HAS_NOT, DOMException.toString(DOMException.MSG_BUSINESS_PARAMETER_HAS_NOT));
            JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.ERROR, true, false);
            return true;
        }
        return false;
    }

    /**
     * 判断需要的参数是否为空
     *
     * @return
     */
    public boolean hasFullConfigData() {
        return false;
    }

    protected void onLoginFinished(JSONObject msg, boolean suc) {
        JSUtil.execCallback(mLoginWebViewImpl, mLoginCallbackId, msg,
                suc ? JSUtil.OK : JSUtil.ERROR, false);
        mLoginWebViewImpl = null;
        mLoginCallbackId = null;
    }

    protected IWebview mLogoutWebViewImpl = null;
    protected String mLogoutCallbackId = null;

    public void logout(IWebview pWebViewImpl, JSONArray pJsArgs) {
        mLogoutWebViewImpl = pWebViewImpl;
        try {
            mLogoutCallbackId = pJsArgs.getString(1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void onLogoutFinished(JSONObject msg, boolean suc) {
        JSUtil.execCallback(mLogoutWebViewImpl, mLogoutCallbackId, msg,
                suc ? JSUtil.OK : JSUtil.ERROR, false);
    }

    protected IWebview mGetUserInfoWebViewImpl = null;
    protected String mGetUserInfoCallbackId = null;

    public void getUserInfo(IWebview pWebViewImpl, JSONArray pJsArgs) {
        mGetUserInfoWebViewImpl = pWebViewImpl;
        try {
            mGetUserInfoCallbackId = pJsArgs.getString(1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void onGetUserInfoFinished(JSONObject msg, boolean suc) {
        JSUtil.execCallback(mGetUserInfoWebViewImpl, mGetUserInfoCallbackId,
                msg, suc ? JSUtil.OK : JSUtil.ERROR, false);
    }

    protected IWebview mAddPhoneNumberWebViewImpl = null;
    protected String mAddPhoneNumberCallbackId = null;

    public void addPhoneNumber(IWebview pWebViewImpl, JSONArray pJsArgs) {
        mAddPhoneNumberWebViewImpl = pWebViewImpl;
        try {
            mAddPhoneNumberCallbackId = pJsArgs.getString(1);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    protected void onAddPhoneNumberFinished(JSONObject msg, boolean suc) {
        JSUtil.execCallback(mAddPhoneNumberWebViewImpl, mAddPhoneNumberCallbackId,
                msg, suc ? JSUtil.OK : JSUtil.ERROR, false);
    }

    static final String T = "{id:'%s',description:'%s',authResult:%s,userInfo:%s}";

    @Override
    public JSONObject toJSONObject() throws JSONException {
        return new JSONObject(String.format(T, id, description,
                authResult != null ? authResult : NULL,
                userInfo != null ? userInfo : NULL));
    }

    protected String getUserInfo(String url) {
        byte[] res= NetTool.httpGet(url);
        if (!PdrUtil.isEmpty(res)){
            return new String(res);
        }
        return null;
    }

    protected String checkToken(String url) {
        return new String(NetTool.httpGet(url));
    }

    protected void removeToken() {
        PlatformUtil.clearBundle("oauth_" + id);
    }

    protected void saveValue(String key, String value) {
        try {
            String k = encrypt(key);//获得key的加密后值
            String v = encrypt(value);//获得value的加密后值
            PlatformUtil.setBundleData("oauth_" + id, k, v);//使用加密key存储加密后value
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void removeValue(String key) {
        try {
            String k = encrypt(key);//获得key的加密后值
            PlatformUtil.removeBundleData("oauth_" + id, k);//删除key
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected String getValue(String key) {
        try {
            String k = encrypt(key);//获得key的加密后值
            String s = PlatformUtil.getBundleData("oauth_" + id, k);//通过加密key获取存储的加密后的value值
            if (s == null) return "{}";
            return decrypt(s);//对加密的value值解密
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "{}";
    }

    protected String getToken(String url) {
        byte[] temp = NetTool.httpGet(url);
        if (null != temp) {
            return new String(temp);
        }
        return null;
    }

    protected String refreshToken(String url) {
        byte[] temp = NetTool.httpGet(url);
        if (null != temp) {
            return new String(temp);
        }
        return null;
    }

    public String encrypt(String content) throws Exception {
        return AESHelper.encrypt(content, BaseOAuthService.class.getName());
    }

    public String decrypt(String content) throws Exception {
        return AESHelper.decrypt(content, BaseOAuthService.class.getName());
    }

    public JSONObject makeResultJson() {
        JSONObject sucJSON = new JSONObject();
        try {
            sucJSON.put(BaseOAuthService.KEY_AUTHRESULT, authResult);
            sucJSON.put(BaseOAuthService.KEY_USERINFO, userInfo);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return sucJSON;
    }

    public JSONObject getErrorJsonbject(int pCode, String pMessage) {
        String json = DOMException.toJSON(pCode, pMessage);
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return new JSONObject();
        }
    }


    public JSONObject getErrorJsonbject(String pCode, String pMessage) {
        String json = DOMException.toJSON(pCode, pMessage);
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
            return new JSONObject();
        }
    }

    public JSONObject getErrorJsonbject(int pCode, String pMessage, int innerCode) {
        String json = DOMException.toJSON(pCode, pCode + ":" + pMessage, innerCode);
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return new JSONObject();
        }
    }

    public JSONObject makeResultJSONObject() {
        JSONObject sucJSON = new JSONObject();
        try {
            sucJSON.put(BaseOAuthService.KEY_AUTHRESULT, authResult);
            sucJSON.put(BaseOAuthService.KEY_USERINFO, userInfo);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return sucJSON;
    }
}
