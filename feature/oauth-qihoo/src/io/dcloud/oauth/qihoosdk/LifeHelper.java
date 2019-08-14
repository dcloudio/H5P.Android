/*
 * 创建日期：2015年4月3日 下午9:28:11
 */
package io.dcloud.oauth.qihoosdk;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.ResultReceiver;
import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.widget.Toast;

import com.qihoo.appstore.utils.user.ApplicationData;
import com.qihoo.payment2jar.PayManager;
import com.qihoo.payment2jar.UIManager;
import com.qihoo360.accounts.QihooAccount;
import com.qihoo360.accounts.api.auth.model.UserTokenInfo;

import org.apache.http.HttpStatus;
import org.apache.http.protocol.HTTP;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import io.dcloud.application.DCloudApplication;
import io.dcloud.common.DHInterface.ICallBack;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.util.NetTool;
import io.dcloud.oauth.qihoosdk.AutoLoginUtil.SimpleLoginCallBack;

/**
 * 版权所有 2005-2015 奇虎360科技有限公司。 保留所有权利。<br>
 * 项目名：360手机助手-Android客户端<br>
 * 描述：生活助手接口类<br>
 * 网页应用的调试地址：http://test1.baohe.mobilem.360.cn/html/zsbeta/payDemo.html
 * 网页应用的调试地址(dcloud提供的接口)：http://openbox.mobilem.360.cn/html/zsbeta/dclouddemo.html?__streamapp
 * 清除领取优惠券记录地址：http://pre.profile.sj.360.cn/raffle/sh_raffle/clear?activity_id=101
 *
 * create on 2015/09/07
 * @author caiyingyuan
 * @version 1.0
 * @since JDK1.6
 */
public class LifeHelper {
	public static String sAppkey = "0";
    private static final String TAG = "LifeHelper";
    private static final String URL_OPENAPI_USER_ME = "https://openapi.360.cn/user/me.json?access_token=%1$s&fields=%2$s";
    private static final String URL_OPENAPI_OAUTH2_AUTHORIZE = "https://openapi.360.cn/oauth2/authorize.json?client_id=%1$s&response_type=token&redirect_uri=oob&state=%2$s&scope=basic&version=Qhopensdk-1.1.6&mid=%3$s&DChannel=default&display=mobile.cli_v1&oauth2_login_type=%4$d";
    private static final String SUFFIX_QIHOOPAY_RESULT = "com.qihoopay.result";
    private static final String PAYMENT_METHODS_OPEN_RESULT = "com.paymemts.open.result";
    private static final String PAYMENT_METHODS_ORDERINFO_RESULT = "com.paymemts.orderinfo.result";

    private static final String KEY_TOKEN = "access_token";
    private static final String ACTION_OPEN_PAYS = "action_open_pays";
    public static final String ACTION_NFC = "action_nfc";

    private static LifeHelper sInstance;
    public static final String FROM_INNER_WEBVIEW = "innerWebView";
    public static final String FROM_DCLOUD = "dcloud";
    private static final int RETRY_COUNT  = 2; // 网络请求最多重试次数；

    private static final String KEY_CP_MENU = "key_cp_menu_";
    private static final String KEY_CP_MENU_UPDATE_TIME = "key_cp_menu_updata_time_";
    private static final long timeInterval = 1000 * 60 * 60 * 24;//24小时
    public static final String KEY_REMOVE_TIPS_DOMAIN = "key_remove_tips_domain";
    public static final String KEY_REMOVE_TIPS_RULE_AND_JS = "key_remove_tips_rule_and_js";

    public static boolean bAppdebug = true;
    public static LifeHelper getInstance() {
        if (sInstance == null) {
            synchronized (LifeHelper.class) {
                sInstance = new LifeHelper();
            }
        }
        return sInstance;
    }

    public static boolean isHasLogin(Context context) {
    	QihooAccount qa = MangeLogin.get(context);
        return qa != null && qa.isValid();
    }

    /**
     * 登录（授权）
     * 账号未绑定手机号码时提示绑定手机号码
     * @param context
     * @param appKey        360移动开放平台的开发者appKey；
     * @param onResult      <pre>
     *                      onResult中：
     *                      	当resultCode等于0时，resultData为json格式的token信息
     *                          当resultCode等于-1时，resultData为空
     *                      	json格式：{"access_token":"用于获取用户信息","expires_in":"标识token还有多少秒过期，默认是10个小时"}；
     *                      </pre>
     */
    public void login(final IWebview pWebViewImpl, final String appKey, final OnResult onResult) {
    	final Context context = pWebViewImpl.getContext();
    	final boolean regAppkey = !TextUtils.equals(appKey, sAppkey);
    	//获取token成功，则认为成功，尝试完善手机号
        final Runnable getTokenRunnable = new Runnable() {
            @Override
            public void run() {
                new AsyncTask<Void, Void, Map<String, Object>>() {
                    @Override
                    protected Map<String, Object> doInBackground(Void... params) {
                        return getToken(context, appKey);
                    }

                    @Override
                    protected void onPostExecute(Map<String, Object> result) {
                        int resultCode;
                        String resultMsg;
                        if (!TextUtils.isEmpty((String) result.get(KEY_TOKEN))) {
                            resultCode = OnResult.CODE_OK;
                            resultMsg = OnResult.MSG_OK;
                            final JSONObject getTokenResult = new JSONObject(result);
                            new Thread(){//此时就代表成功了，即绑定手机号页面执行成功回调即可
                            	@Override
                            	public void run() {
                        			new AsyncTask<Void, Void, Boolean>() {
                        				@Override
                        				protected Boolean doInBackground(Void... params) {
                        					return getPhoneInfo(context);
                        				}
                        				
                        				@Override
                        				protected void onPostExecute(Boolean isExistPhone) {
                        					if (!isExistPhone) {
                        						addPhoneNum(context, new OnResult(){//无论成功，均执行login成功回调
                    								@Override
                    								public void onResult(int resultCode, String resultMsg, JSONObject resultData) {
                    									resultCode = OnResult.CODE_OK;
                    		                            resultMsg = OnResult.MSG_OK;
                    		                            onResult.onResult(resultCode, resultMsg, getTokenResult);
                    								}} );
                        					}else{//存在手机号了，执行回调
                        						int resultCode = OnResult.CODE_OK;
                                                String resultMsg = OnResult.MSG_OK;
                                                onResult.onResult(resultCode, resultMsg, getTokenResult);
                        					}
                        				}
                        			}.execute();
                            	}
                            }.start();
                        } else {//失败
                            resultCode = OnResult.CODE_UNKNOW;
                            resultMsg = OnResult.MSG_UNKNOW;
                            onResult.onResult(resultCode, resultMsg, new JSONObject());
                        }
                    }
                }.execute();
            }
        };


        if (isHasLogin(context)) {
//        	if(regAppkey){
        		getTokenRunnable.run();
//        	}else{
//        		onResult.onResult(OnResult.CODE_OK, OnResult.MSG_OK, new JSONObject());
//        	}
        } else {
            final QihooSDKHelper.LoginStatusListener mLoginStateChangeListener = new QihooSDKHelper.LoginStatusListener() {
                @Override
                public void onLoginStateChange(boolean bLogin, Object object) {
                    if (bLogin) {
                    	updateCookies(context, QIHOO_URL);
//                    	if(regAppkey){
                    		getTokenRunnable.run();
//                    	}else{
//                    		onResult.onResult(OnResult.CODE_OK, OnResult.MSG_OK, new JSONObject());
//                    	}
                    } else {
                        onResult.onResult(OnResult.CODE_UNKNOW, OnResult.MSG_UNKNOW, new JSONObject());
                    }
                }
            };
            QihooSDKHelper.self().login(pWebViewImpl, appKey, mLoginStateChangeListener);
        }

    }

    /**
     * http://wiki.dev.app.360.cn/index.php?title=OAuth2.0%E6%96%87%E6%A1%A3
     * 获得账号token（使用Implicit_Grant获取Access_Token），会在服务器端校验appKey的合法性<br>
     * 使用token获取用户信息：360移动开放平台-登录服务 http://dev.360.cn/wiki/index/id/67->3.3获取用户信息【服务端调用】(选接)<br>
     * 应用服务端获取access token后, 可调用360开放平台服务器端接口/user/me, 获取360用户id以及其它用户信息<br>
     * 接口地址为 https://openapi.360.cn/user/me
     *
     * @return access_token（token）、expires_in（标识token还有多少秒过期，默认是10个小时）
     */
    private Map<String, Object> getToken(Context context, String appKey) {
        Map<String, Object> tokens = new HashMap<String, Object>();
        if (isHasLogin(context)) {
            String clientId = appKey;
            String state = "LifeGetToken"; // state是OAUTH2给应用提供的一个安全机制 应用可以自己指定一个值进去 后边会原样返回
            String mid = ApplicationData.getIMEI2(context);
            int oauth2LoginType = 2; // LOGIN_MANUL_STATE
            String url = String.format(URL_OPENAPI_OAUTH2_AUTHORIZE, clientId, state, mid, oauth2LoginType);
            for (int i = 0; i <= RETRY_COUNT; i++) {
                HttpURLConnection huc = null;
                try {
                	final QihooAccount account = MangeLogin.get(context);
                	String qt = "Q=" + account.mQ +";T=" + account.mT;
                    huc = (HttpURLConnection) new URL(url).openConnection();
                    huc.addRequestProperty("Cookie", qt);
                    huc.setRequestMethod("GET");
                    huc.setInstanceFollowRedirects(false); // 必须设置false，否则会自动redirect到Location的地址；
                    if (HttpStatus.SC_MOVED_TEMPORARILY == huc.getResponseCode()) {
                        String location = huc.getHeaderField("Location");
                        if (!TextUtils.isEmpty(location)) {
                            Matcher matcherAccessToken = Pattern.compile("[?#]" + KEY_TOKEN + "=([0-9a-zA-Z]*)").matcher(location);
                            if (matcherAccessToken.find()) {
                                tokens.put(KEY_TOKEN, matcherAccessToken.group(1));
                            }
                            Matcher matcherExpiresIn = Pattern.compile("[?&]expires_in=([0-9]*)").matcher(location);
                            if (matcherExpiresIn.find()) {
                                tokens.put("expires_in", matcherExpiresIn.group(1));
                            }
                        }
                    } else {
                        tokens.put("Exception", inputStreamToString(huc.getInputStream(), HTTP.UTF_8, true));
                    }
                    break;
                } catch (Exception e) {
                    if (bAppdebug) {
                        e.printStackTrace();
                    }
                    tokens.put("Exception", e.getMessage());
                } finally {
                    if (huc != null) {
                        huc.disconnect();
                    }
                }
            }
        }
        return tokens;
    }

    /**
     * 获取用户信息
     * @param accessToken 登录（授权）后返回的access_token；
     * @param onResult    <pre>
     *                    onResult中：
     *                    	当resultCode等于0时，resultData为json格式的用户信息
     *                    	json格式：{"id":"360用户ID","name":"360用户名","avatar":"360用户头像","sex":"360用户性别，返男，女或者未知","area":"360用户地区","nick":"用户昵称"}；
     *                      v3.4.20 返回的json格式增加 isExistPhone字段，true/false,true表示用户已绑定手机号。
     *                    	当resultCode不等于0时（如：400、401等），resultMsg为json格式的错误信息，具体参见：360移动开放平台-登录服务-APIException说明文档.docx
     *                    </pre>
     */
    public void getUserInfo(final IWebview pWebViewImpl, final String accessToken, final OnResult onResult) {
    	final Context context = pWebViewImpl.getContext();
        final AsyncTask<Boolean, Void, Void> asyncTask = new AsyncTask<Boolean, Void, Void>() {
            @Override
            protected Void doInBackground(Boolean... params) {
                String fields = "id,name,avatar,sex,area,nick";
                String url = String.format(URL_OPENAPI_USER_ME, accessToken, fields);
                String result = null;
                int responseCode = -1;
                for (int i = 0; i <= RETRY_COUNT; i++) {
                    HttpURLConnection huc = null;
                    try {
                        huc = (HttpURLConnection) new URL(url).openConnection();
                        QihooAccount qa = MangeLogin.get(context);
                        if(qa != null){
                        	huc.addRequestProperty("Cookie", "Q=" + qa.mQ +";T=" + qa.mT);
                        }
                        huc.setRequestMethod("GET");
                        responseCode = huc.getResponseCode();
                        if (HttpStatus.SC_BAD_REQUEST <= responseCode) {
                            // {"error_code":"4010201","error":"access token不可用（OAuth2）"}
                            result = inputStreamToString(huc.getErrorStream(), HTTP.UTF_8, true);
                        } else {
                            result = inputStreamToString(huc.getInputStream(), HTTP.UTF_8, true);
                        }
                        break;
                    } catch (IOException e) {
                        if (bAppdebug) {
                            e.printStackTrace();
                            Logger.d("responseCode=" + responseCode + ";"  + i + ";\n"+ e.getMessage() );
                        }
//                        result = handlerException(e);
                    } finally {
                        if (huc != null) {
                            huc.disconnect();
                        }
                    }
                }
                JSONObject resultData = null;
                if (!TextUtils.isEmpty(result)) {
                    try {
                        resultData = new JSONObject(result);
                        resultData.put("isExistPhone", params[0]);
                    } catch (JSONException e) {
                        if (bAppdebug) {
                            e.printStackTrace();
                        }
                        resultData = handlerException(e);
                    }
                }
                Logger.d("LifeHelper.getUserInfo responseCode " + responseCode);
                if (HttpStatus.SC_OK == responseCode) {
                    onResult.onResult(OnResult.CODE_OK, OnResult.MSG_OK, resultData);
                } else {
                    onResult.onResult(responseCode, resultData != null ? resultData.toString() : OnResult.MSG_UNKNOW, new JSONObject());
                }
                return null;
            }
        };

        new AsyncTask<Void, Void, Boolean>() {
            @Override
            protected Boolean doInBackground(Void... params) {
                return getPhoneInfo(context);
            }

            @Override
            protected void onPostExecute(Boolean aBoolean) {
                asyncTask.execute(aBoolean);
            }
        }.execute();
    }


    /**
     * 获取账号是否绑定了手机号码的信息
     * 返回数据格式:
     * {
     * "errno": 0,
     * "errmsg": "ok",
     * "time": 1434017593,
     * "data": {
     * "phone": true/false
     * }
     * }
     */
    private boolean getPhoneInfo(Context context) {
    	QihooAccount qa = MangeLogin.get(context);
        if (qa != null && qa.isValid() && !TextUtils.isEmpty(qa.getSecMobile())) {
            return true;
        }
        try {
        	HashMap<String, String> heads = new HashMap<String, String>();
			heads.put("Cookie", "Q=" + qa.mQ +";T=" + qa.mT);//http://profile.sj.360.cn/live/get-vc?mobile=13521379291
			byte[] request = NetTool.httpGet(UrlUtils.getPhoneInfoUrl2(),heads);
//            request.setShouldCache(false);
//            request.setTag(LifeHelper.this);
            if (request != null) {
                JSONObject result = new JSONObject(new String(request,"utf-8"));
                return result.optBoolean("data");
//                JSONObject result = new JSONObject(new String(request,"utf-8"));
//                JSONObject object = result.optJSONObject("data");
//                if (object != null) {
//                	return object.optBoolean("phone", false);
//                } else {
//                	return false;
//                }
            } else {
                return false;
            }
        } catch (Exception e) {
            if (bAppdebug) {
                e.printStackTrace();
            }
            return false;
        }
    }

    /**
     * 绑定手机号码
     * @param context
     * @param onResult <pre>
     *                 onResult中resultCode=0，绑定手机号码成功;
     *                 resultCode=-1,resultMsg=failed，绑定失败;
     *                 resultCode=-1, resultMsg=登录失败， 未登录，无法绑定；
     *             </pre>
     */
    public void addPhoneNum(final Context context, final OnResult onResult){
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                new AsyncTask<Void, Void, Boolean>() {
                    @Override
                    protected Boolean doInBackground(Void... params) {
                        return getPhoneInfo(context);
                    }
                    @Override
                    protected void onPostExecute(Boolean isExistPhone) {
                        if (isExistPhone) {
                        	 if (onResult != null) {
                                 onResult.onResult(OnResult.CODE_OK , OnResult.MSG_OK , new JSONObject());
                             }
                            return;
                        }

                        final ResultReceiver resultRcrv = new ResultReceiver(new Handler(Looper.getMainLooper())) {
                            @Override
                            protected void onReceiveResult(int resultCode, Bundle resultData) {
                                if (onResult != null) {
                                    onResult.onResult(resultCode == OnResult.CODE_OK ? OnResult.CODE_OK : OnResult.CODE_UNKNOW, resultCode == OnResult.CODE_OK ? OnResult.MSG_OK : OnResult.MSG_UNKNOW, new JSONObject());
                                }
                                super.onReceiveResult(resultCode, resultData);
                            }
                        };
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                Intent intentTemp = new Intent(context, AddPhoneNumActivity.class);
                                intentTemp.putExtra(AddPhoneNumActivity.BUNDLE_FINISH_CALLBACK, resultRcrv);
                                context.startActivity(intentTemp);

                            }
                        });
                    }
                }.execute();
            }
        };
        new Handler(Looper.getMainLooper()).post(runnable);
//        if (isHasLogin()) {
//            runnable.run();
//        } else {
//            final UserLoginManager.ILoginStateChangeListener mLoginStateChangeListener = new UserLoginManager.ILoginStateChangeListener() {
//                @Override
//                public void onLoginStateChange(boolean bLogin, Object object) {
//                    if (bLogin) {
//                        runnable.run();
//                    } else {
//                        onResult.onResult(OnResult.CODE_UNKNOW, OnResult.MSG_UNKNOW, new JSONObject());
//                    }
//                    UserLoginManager.getManagerInstance().removeUserLoginStateMonitorListener(this);
//                }
//            };
//            UserLoginManager.getManagerInstance().addUserLoginStateMonitorListener(mLoginStateChangeListener);
//            UserLoginManager.getManagerInstance().beginLogin(context);
//        }
    }

    /**
     * 获取绑定手机号码需要的手机验证码
     *
     * @author caiyingyuan
     */
    public void getMobileVerityCode(final Context context, final String phoneNum, final OnResult onResult) {
    	final QihooAccount account = MangeLogin.get(context);
    	if(account != null){
    		new Thread(){
        		public void run() {
        			HashMap<String, String> heads = new HashMap<String, String>();
        			heads.put("Cookie", "Q=" + account.mQ +";T=" + account.mT);//http://profile.sj.360.cn/live/get-vc?mobile=13521379291
        			try {
						byte[] ret = NetTool.httpGet(UrlUtils.getVerityCodeUrl() + "?mobile=" + phoneNum,heads);
						if(ret != null){
							try{
								JSONObject result = new JSONObject(new String(ret,"utf-8"));
								if (result != null) {
									Logger.d(TAG, "getMobileVerityCode-->" + result.toString());
									onResult.onResult(OnResult.CODE_OK, OnResult.MSG_OK, result);
								}
							}catch (Exception e){
								if (ApplicationData.bAppdebug) {
									e.printStackTrace();
								}
								JSONObject jobj = handlerException(e);
								onResult.onResult(OnResult.CODE_UNKNOW, e.getMessage(), jobj);
							}
						}else{
							try {
								JSONObject obj = new JSONObject();
								obj.put("errno", OnResult.CODE_UNKNOW);
								obj.put("errmsg", OnResult.MSG_UNKNOW);
								onResult.onResult(OnResult.CODE_UNKNOW, onResult.MSG_UNKNOW, obj);
							} catch (JSONException e) {
								e.printStackTrace();
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						try {
							JSONObject obj = new JSONObject();
							obj.put("errno", OnResult.CODE_UNKNOW);
							obj.put("errmsg", OnResult.MSG_UNKNOW);
							onResult.onResult(OnResult.CODE_UNKNOW, onResult.MSG_UNKNOW, obj);
						} catch (JSONException ei) {
							ei.printStackTrace();
						}
					}
        		}
        		
        	}.start();
    	}else{
    		try {
				JSONObject obj = new JSONObject();
				obj.put("errno", OnResult.CODE_UNKNOW);
				obj.put("errmsg", OnResult.MSG_UNKNOW);
				onResult.onResult(OnResult.CODE_UNKNOW, onResult.MSG_UNKNOW, obj);
			} catch (JSONException e) {
				e.printStackTrace();
			}
    	}
    	
    }

    private JSONObject handlerException(Exception e){
    	return null;
    }
    /**
     * 提交绑定手机号码
     *
     * @author caiyingyuan
     */
    public void bindPhoneNum(final Context context, final String phoneNum, final String verityCode, final OnResult onResult) {
    	final QihooAccount account = MangeLogin.get(context);
    	if(account != null){
    		new Thread(){
    			public void run(){
	    			HashMap<String, String> heads = new HashMap<String, String>();
	    			heads.put("Cookie", "Q=" + account.mQ +";T=" + account.mT);//http://profile.sj.360.cn/live/get-vc?mobile=13521379291
	    			try {
						byte[] ret = NetTool.httpGet(UrlUtils.getBindPhoneNumUrl() + "?mobile=" + phoneNum + "&vc=" + verityCode,heads);
						if(ret != null){
							try{
								JSONObject result = new JSONObject(new String(ret,"utf-8"));
								if (result != null) {
									onResult.onResult(OnResult.CODE_OK, OnResult.MSG_OK, result);
								} 
							}catch (Exception e){
								if (ApplicationData.bAppdebug) {
									e.printStackTrace();
								}
								JSONObject jobj = handlerException(e);
								onResult.onResult(OnResult.CODE_UNKNOW, e.getMessage(), jobj);
							}
						}
					} catch (Exception e) {
						e.printStackTrace();
						JSONObject jobj = handlerException(e);
						onResult.onResult(OnResult.CODE_UNKNOW, e.getMessage(), jobj);
					}
    		}}.start();
    	}else{
    		 try {
				JSONObject obj = new JSONObject();
				 obj.put("errno", OnResult.CODE_UNKNOW);
				 obj.put("errmsg", OnResult.MSG_UNKNOW);
				 onResult.onResult(OnResult.CODE_UNKNOW, OnResult.MSG_UNKNOW, obj);
			} catch (JSONException e) {
				e.printStackTrace();
			}
    	}
    }


//    /**
//     * 获取设备当前位置信息，与JavaScript中的navigator.geolocation.getCurrentPosition(showPosition, showError)返回的数据对象结构是完全一致的;
//     * 使用的时候，只需要将“navigator.geolocation.getCurrentPosition”替换成“XXX.getCurrentPosition”即可（只需要改前缀，后面的方法名和回调函数不变，完全兼容）；
//     * 返回的数据对象与navigator.geolocation是一致的，http://dev.w3.org/geo/api/spec-source.html#position_interface；
//     * 成功时的数据对象：
//     * {
//     *   "coords": {
//     *     "latitude": 39.983421,
//     *     "longitude": 116.491496,
//     *     "accuracy": 150.0,
//     *     "altitude": 0.0,
//     *     "altitudeAccuracy": 0.0,
//     *     "heading": 0.0,
//     *     "speed": 0.0
//     *   },
//     *   "timestamp": 1435090292710,
//     *   "address": {
//     *     "country": "中国",
//     *     "province": "北京市",
//     *     "city": "北京市",
//     *     "district": "朝阳区",
//     *     "street": "酒仙桥路",
//     *     "road": "798路",
//     *     "poiName": "电子城·国际电子总部",
//     *     "adCode": 110105,
//     *     "cityCode": 010
//     *   },
//     *   "addresses": "北京市朝阳区798路靠近电子城·国际电子总部"
//     * }
//     *
//     * 失败时的数据对象：
//     * {
//     *   "PERMISSION_DENIED": 1,
//     *   "POSITION_UNAVAILABLE": 2,
//     *   "TIMEOUT": 3,
//     *   "UNKNOWN_ERROR": 4,
//     *   "code": 2,
//     *   "message": "获取基站/WiFi信息为空或失败"
//     * }
//     *
//     * @param context
//     * @param option
//     *  {
//     *    //指示浏览器获取高精度的位置，默认为false
//     *    enableHighAccuracy: true,
//     *    //指定获取地理位置的超时时间，默认不限时，单位为毫秒
//     *    timeout: 5000,
//     *    //最长有效期，在重复获取地理位置时，此参数指定多久再次获取位置
//     *    maximumAge: 3000
//     *  }
//     * @param onResult
//     */
//    public void getCurrentPosition(final Context context, final JSONObject option, final OnResult onResult) {
//        final Handler handler = new Handler(Looper.getMainLooper());
//        Runnable runnable = new Runnable() {
//            @Override
//            public void run() {
//                final AtomicBoolean isCancelTimeout = new AtomicBoolean();
//                final Runnable timeoutRunnable = new Runnable() {
//                    @Override
//                    public void run() {
//                        if (!isCancelTimeout.get()) {
//                            AMapLocation amapLocation = new AMapLocation((String) null);
//                            amapLocation.setAMapException(new AMapLocException(AMapLocException.ERROR_SOCKE_TIME_OUT));
//                            getCurrentPositionResult(amapLocation, onResult);
//                            isCancelTimeout.set(true);
//                        }
//                    }
//                };
//                final LocationManagerProxy locationManager = LocationManagerProxy.getInstance(context.getApplicationContext());
//                boolean enableHighAccuracy = option != null ? option.optBoolean("enableHighAccuracy", false) : false;
//                long timeout = option != null ? option.optLong("timeout", -1L) : -1L;
//                long maximumAge = option != null ? option.optLong("maximumAge", -1L) : -1L;
//                locationManager.setGpsEnable(enableHighAccuracy);// 1.0.2版本新增方法，设置true表示混合定位中包含gps定位，false表示纯网络定位，默认是true
//                locationManager.requestLocationData(LocationProviderProxy.AMapNetwork, maximumAge, 100, new AMapLocationListener() { // Location API定位采用GPS和网络混合定位方式，第一个参数是定位provider，第二个参数时间最短是5000毫秒，-1表示只定位一次，第三个参数距离间隔单位是米，第四个参数是定位监听者
//                    @Override
//                    public void onLocationChanged(AMapLocation amapLocation) {
//                        locationManager.removeUpdates(this);
//                        locationManager.destroy();
//                        if (!isCancelTimeout.get()) {
//                            getCurrentPositionResult(amapLocation, onResult);
//                            handler.removeCallbacks(timeoutRunnable);
//                            isCancelTimeout.set(true);
//                        }
//                    }
//
//                    @Override
//                    public void onLocationChanged(Location location) {
//                    }
//
//                    @Override
//                    public void onStatusChanged(String provider, int status, Bundle extras) {
//                    }
//
//                    @Override
//                    public void onProviderEnabled(String provider) {
//                    }
//
//                    @Override
//                    public void onProviderDisabled(String provider) {
//                    }
//                });
//                handler.postDelayed(timeoutRunnable, timeout);
//            }
//        };
//
//        // 高德的定位sdk非常奇葩，如果不在主线程中执行，第三次调用requestLocationData时一定无法正常得到回调函数的执行；
//        if (Looper.myLooper() == Looper.getMainLooper()) {
//            runnable.run();
//        } else {
//            handler.post(runnable);
//        }
//    }
//
//    private void getCurrentPositionResult(AMapLocation amapLocation, OnResult onResult) {
//        LogUtils.d(TAG, "onLocationChanged.amapLocation = " + amapLocation);
//        boolean isSuccess = amapLocation != null && amapLocation.getAMapException().getErrorCode() == 0;
//        JSONObject json;
//        if (isSuccess) {
//            json = new JSONObject();
//            try {
//                double lat = amapLocation.getLatitude();
//                double lon = amapLocation.getLongitude();
//
//                // 将高德地图sdk返回的的“国测局 GCJ-02”坐标转换成“GPS WGS-84”坐标；
//                WgsGcjConverter.SimpleCoodinates coodinates = WgsGcjConverter.gcj02ToWgs84(lat, lon);
//                lat = coodinates.getLat();
//                lon = coodinates.getLon();
//
//                JSONObject coords = new JSONObject();
//                coords.put("latitude", lat);
//                coords.put("longitude", lon);
//                coords.put("accuracy", amapLocation.getAccuracy());
//                coords.put("altitude", amapLocation.getAltitude());
//                coords.put("altitudeAccuracy", 0.0);
//                coords.put("heading", 0.0);
//                coords.put("speed", amapLocation.getSpeed());
//                json.put("coords", coords);
//                json.put("timestamp", amapLocation.getTime());
//                JSONObject address = new JSONObject();
//                address.put("country", amapLocation.getCountry());
//                address.put("province", amapLocation.getProvince());
//                address.put("city", amapLocation.getCity());
//                address.put("district", amapLocation.getDistrict());
//                address.put("street", amapLocation.getStreet());
//                address.put("road", amapLocation.getRoad());
//                address.put("poiName", amapLocation.getPoiName());
//                address.put("adCode", amapLocation.getAdCode());
//                address.put("cityCode", amapLocation.getCityCode());
//                json.put("address", address);
//                json.put("addresses", amapLocation.getAddress());
//            } catch (JSONException e) {
//                if (AppEnv.bAppdebug) {
//                    e.printStackTrace();
//                }
//            }
//        } else {
//            json = new JSONObject();
//            // 要与系统常量值保持一致：http://androidxref.com/5.1.0_r1/xref/external/chromium_org/third_party/WebKit/Source/modules/geolocation/PositionError.idl
//            int PERMISSION_DENIED = 1;
//            int POSITION_UNAVAILABLE = 2;
//            int TIMEOUT = 3;
//            int UNKNOWN_ERROR = 4;
//            try {
//                json.put("PERMISSION_DENIED", PERMISSION_DENIED);
//                json.put("POSITION_UNAVAILABLE", POSITION_UNAVAILABLE);
//                json.put("TIMEOUT", TIMEOUT);
//                json.put("UNKNOWN_ERROR", UNKNOWN_ERROR);
//                switch (amapLocation.getAMapException().getErrorCode()) {
//                    case AMapLocException.ERROR_CODE_UNKNOWN:
//                        json.put("code", UNKNOWN_ERROR);
//                        break;
//                    case AMapLocException.ERROR_CODE_SOCKE_TIME_OUT:
//                        json.put("code", TIMEOUT);
//                        break;
//                    default:
//                        json.put("code", POSITION_UNAVAILABLE);
//                        break;
//                }
//                json.put("message", amapLocation.getAMapException().getErrorMessage());
//            } catch (JSONException e) {
//                if (AppEnv.bAppdebug) {
//                    e.printStackTrace();
//                }
//            }
//        }
//
//        if (onResult != null) {
//            if (LogUtils.isDebug()) { // 由于高德定位sdk需要验证release签名，故debug时直接返回模拟的位置信息；
//                // 测试代码 begin
//                isSuccess = true;
//                String testJson = "{\"coords\":{\"latitude\":39.982077,\"longitude\":116.485309,\"accuracy\":60,\"altitude\":0,\"altitudeAccuracy\":0,\"heading\":0,\"speed\":0},\"timestamp\":1436420381591,\"address\":{\"country\":\"中国\",\"province\":\"北京市\",\"city\":\"北京市\",\"district\":\"朝阳区\",\"street\":\"798路\",\"road\":\"798路\",\"poiName\":\"友谊大厦\",\"adCode\":\"110105\",\"cityCode\":\"010\"},\"addresses\":\"北京市朝阳区798路靠近友谊大厦\"}";
//                try {
//                    json = new JSONObject(testJson);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                }
//                // 测试代码 end
//            }
//            if (isSuccess) {
//                onResult.onResult(OnResult.CODE_OK, OnResult.MSG_OK, json);
//            } else {
//                onResult.onResult(OnResult.CODE_UNKNOW, OnResult.MSG_UNKNOW, json);
//            }
//        }
//    }


//    /**
//     * 支付订单
//     * json 格式：
//     * {
//     * "payType": "360securepay",
//     * "orderInfo": {
//     * "errno": 0,
//     * "errmsg": "ok",
//     * "time": 1436844019,
//     * "data": {
//     * "token": "D1BCF1AB1F324D71707A9336A8E3F120",
//     * "seckey": "4344DF777041C61FFF76C5A6DFA46460",
//     * "sign": "81ae605269c4e4e13ebbdfa4061be300"
//     * }
//     * }
//     * }
//     */
//    public void paymentByOrderInfo(final Context context, String json, final OnResult onResult) {
//        try {
//            //发送订单的信息
//            Intent intent = new Intent(PAYMENT_METHODS_ORDERINFO_RESULT);
//            if (json != null && !json.isEmpty()) {
//                JSONObject obj = new JSONObject(json);
//
//                //创建获取支付结果的广播
//                String action = PAYMENT_METHODS_ORDER_RESULT;
//                IntentFilter filter = new IntentFilter(action);
//                OrderPayResultReceiver resultReceiver = new OrderPayResultReceiver(onResult);
//                context.registerReceiver(resultReceiver, filter); // 绑定获取订单信息广播接收器；
//
//                //发送订单的信息
//                intent.putExtra("payType", obj.optString("payType", null));
//                intent.putExtra("orderInfo", obj.optString("orderInfo", null));
//                intent.putExtra("result_code", OnResult.CODE_OK);
//                intent.putExtra("result_msg", OnResult.MSG_OK);
//            } else {
//                //发送订单的信息
//                intent.putExtra("result_code", OnResult.CODE_UNKNOW);
//                intent.putExtra("result_msg", "获取不到订单信息，请稍后再试!");
//            }
//            context.sendBroadcast(intent);
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//    }
//
//    private static class OrderPayResultReceiver extends BroadcastReceiver {
//        /**
//         * 支付结果值
//         */
//        private static final String RESULT_CODE = "result_code";
//        /**
//         * 支付结果描述
//         */
//        private static final String RESULT_MSG = "result_msg";
//        private OnResult mOnResult;
//
//        public OrderPayResultReceiver(OnResult onResult) {
//            mOnResult = onResult;
//        }
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            context.unregisterReceiver(this);
//            Bundle bundle = intent.getExtras();
//            if (bundle != null) {
//                int resultCode = bundle.getInt(RESULT_CODE, OnResult.CODE_UNKNOW);
//                String resultMsg = bundle.getString(RESULT_MSG);
//                if (mOnResult != null) {
//                    if (resultMsg != null && !resultMsg.isEmpty()) {
//                        Toast.makeText(context, resultMsg, Toast.LENGTH_SHORT).show();
//                    }
//                    mOnResult.onResult(resultCode, resultMsg, new JSONObject());
//                }
//            } else {
//                mOnResult.onResult(OnResult.CODE_UNKNOW, "获取不到支付结果", new JSONObject());
//            }
//        }
//    }
//
//    private static class OpenPaysResultReceiver extends BroadcastReceiver {
//        /**
//         * 选择支付方式结果值
//         */
//        private static final String RESULT_CODE = "result_code";
//        /**
//         * 选择支付方式结果描述
//         */
//        private static final String RESULT_MSG = "result_msg";
//        private OnResult mOnResult;
//
//        public OpenPaysResultReceiver(OnResult onResult) {
//            mOnResult = onResult;
//        }
//
//        @Override
//        public void onReceive(Context context, Intent intent) {
//            context.unregisterReceiver(this);
//            Bundle bundle = intent.getExtras();
//            int resultCode = bundle.getInt(RESULT_CODE, -1);
//            String resultMsg = bundle.getString(RESULT_MSG);
//            String payType = bundle.getString("payType");
//
//            if (mOnResult != null) {
//                JSONObject obj = new JSONObject();
//                try {
//                    obj.put("payType", payType);
//                } catch (JSONException e) {
//                    e.printStackTrace();
//                    try {
//                        obj.put("errmsg", e.getMessage());
//                    } catch (JSONException e1) {
//                        e1.printStackTrace();
//                    }
//                }
//                mOnResult.onResult(resultCode, resultMsg, obj);
//            }
//        }
//    }
//
//    /**v3.4.35
//     * NFC贴卡获取公交卡信息
//     * v3.4.40
//     * 将e乐充读取公交卡信息的jar嵌入助手内部
//     * */
//    public void getBusCardInfo(final Context context, final Intent intent, final OnResult onResult) {
//        if (intent == null) {
//            return;
//        }
//
//        final ProgressDialog loadingDlg = new ProgressDialog(context, AlertDialog.THEME_HOLO_LIGHT);
//        loadingDlg.setMessage(context.getString(R.string.nfc_loading_tip));
//        loadingDlg.setCancelable(true);
//        loadingDlg.setCanceledOnTouchOutside(false);
//        loadingDlg.show();
//
//        ResultReceiver receiver = new ResultReceiver(new Handler(Looper.getMainLooper())) {
//            @Override
//            protected void onReceiveResult(int resultCode, final Bundle resultData) {
//                LogUtils.d(TAG, "resultCode:" + resultCode + " resultData:" + resultData.getString("data"));
//                final NFCCardInfo nfcCardInfo = NFCDispHelper.getCardInfoByIntent((Activity)context, intent);
//
//                Timer t = new Timer();
//                t.schedule(new TimerTask() {
//                    @Override
//                    public void run() {
//                        loadingDlg.cancel();
//
//                        JSONObject obj = new JSONObject();
//
//                        try {
//                            obj.put("iscanUse", resultData.getBoolean(NFCDispHelper.KEY_MOBILE_ISCANUSE));
//                            obj.put("cardNum", nfcCardInfo.sCardNO);
//                            obj.put("balance", nfcCardInfo.sCardBalance);
//                            obj.put("cardType", nfcCardInfo.nCardType);
//                            if (TextUtils.isEmpty(nfcCardInfo.sCardNO)) {
//                                onResult.onResult(OnResult.CODE_UNKNOW, OnResult.MSG_UNKNOW, obj);
//
//                            } else {
//                                onResult.onResult(OnResult.CODE_OK, OnResult.MSG_OK, obj);
//                            }
//
//                        } catch (JSONException e) {
//                            e.printStackTrace();
//                            onResult.onResult(OnResult.CODE_UNKNOW, OnResult.MSG_UNKNOW, obj);
//                        }
//                    }
//                }, 500);
//
//            }
//        };
//
//        NFCDispHelper.getMobileIsCanUse((Activity) context, intent, receiver);
//    }
//
//    /**
//     * 获取需要拦截tip的域名
//     * */
//    public void getRemoveTipsDomainlist() {
//        Request request = new JsonArrayRequest(UrlUtils.appendAllSegment(UrlUtils.getRemoveTipsDomainlistUrl()),
//                new Response.Listener<JSONArray>() {
//                    @Override
//                    public void onResponse(JSONArray result) {
//                        if (result != null) {
//                            String domains = result.toString();
//                            AppstoreSharePref.setStringSetting(KEY_REMOVE_TIPS_DOMAIN, domains);
//                        }
//                    }
//                }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError volleyError) {
//
//            }
//        });
//        request.setTag(LifeHelper.this.hashCode());
//        VolleyHttpClient.getInstance().addToQueue(request);
//    }
//
//    /**
//     * 查询要拦截tip的域名对应的规则和Js
//     * */
//    public void getRemoveTipsRuleAndJs(final String domain, final ResultReceiver receiver) {
//        String removeTipsDomainList = AppstoreSharePref.getStringSetting(KEY_REMOVE_TIPS_DOMAIN, "");
//        if (domain == null || TextUtils.isEmpty(removeTipsDomainList)) {
//            String data = AppstoreSharePref.getStringSetting(KEY_REMOVE_TIPS_RULE_AND_JS + Md5Utils.md5(domain), "");
//            Bundle bundle = new Bundle();
//            bundle.putString(KEY_REMOVE_TIPS_RULE_AND_JS, data);
//            receiver.send(OnResult.CODE_UNKNOW, bundle);
//            return;
//        }
//
//        try {
//            JSONArray domainArray = new JSONArray(removeTipsDomainList);
//            if (domainArray != null) {
//                ArrayList<String> domainlist = new ArrayList<>();
//                for (int i = 0; i < domainArray.length(); i++) {
//                    domainlist.add(domainArray.getString(i));
//                }
//
//                if (domainlist.indexOf(domain) == -1) {
//                    LogUtils.d(TAG, domain + " domain Not in the blacklist：" + domainlist.toString());
//                    receiver.send(OnResult.CODE_UNKNOW, null);
//                    return;
//                }
//
//            }
//        } catch (JSONException e) {
//            e.printStackTrace();
//        }
//
//        Request request = new JsonArrayRequest(UrlUtils.appendAllSegment(UrlUtils.getRomoveTipsRuleAndJsUrl() + "?domain=" + domain),
//                new Response.Listener<JSONArray>() {
//                    @Override
//                    public void onResponse(JSONArray result) {
//                        if (result != null) {
//                            LogUtils.d(TAG, domain + " getRemoveTipsRuleAndJs---->" + result.toString());
//                            AppstoreSharePref.setStringSetting(KEY_REMOVE_TIPS_RULE_AND_JS + Md5Utils.md5(domain), result.toString());
//                            Bundle bundle = new Bundle();
//                            bundle.putString(KEY_REMOVE_TIPS_RULE_AND_JS, result.toString());
//                            receiver.send(OnResult.CODE_OK, bundle);
//
//                        }
//                    }
//                }, new Response.ErrorListener() {
//            @Override
//            public void onErrorResponse(VolleyError volleyError) {
//
//            }
//        });
//        request.setTag(LifeHelper.this.hashCode());
//        VolleyHttpClient.getInstance().addToQueue(request);
//
//    }

    public static String inputStreamToString(InputStream ins, String encoding, boolean closeStream)
            throws IOException {
        StringBuilder buf = new StringBuilder();
        InputStreamReader r = new InputStreamReader(ins, encoding);
        char[] cbuf = new char[1024];
        int count = 0;
        while ((count = r.read(cbuf)) > 0) {
            buf.append(cbuf, 0, count);
        }
        if (closeStream == true) {
            ins.close();
        }
        return buf.toString();
    }
    
    public static void updateCookies(Context context,String mCurrentUrl) {
    	QihooAccount qa = MangeLogin.get(context);
        boolean isHasLogin = qa != null && qa.isValid();
        if (isHasLogin && mCurrentUrl != null /*&& is360CnPage(mCurrentUrl)*/) { //
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) { // 
                CookieSyncManager.createInstance(context);
            }
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            final QihooAccount account = qa;
        	String cookieString = "Q=" + account.mQ +";T=" + account.mT;
            if (isHasLogin && !TextUtils.isEmpty(cookieString)) {
                String[] cookieSubString = cookieString.split(";");
                for (String aCookieSubString : cookieSubString) {
                    cookieManager.setCookie(mCurrentUrl, aCookieSubString + ";path=/;domain=.360.cn");
                }
            } else {
                cookieManager.setCookie(mCurrentUrl, "Q=;path=/;domain=.360.cn");
                cookieManager.setCookie(mCurrentUrl, "T=;path=/;domain=.360.cn");
                cookieManager.setCookie(mCurrentUrl, "qid=;path=/;domain=.360.cn");
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) { // 
                CookieSyncManager.getInstance().sync();
            }
        }
    }
    
    public static void autoLogin(final ICallBack backListener) {
        Logger.d(TAG, "autoLoginFromPreference");
        AutoLoginUtil.autoLogin(new SimpleLoginCallBack() {
			@Override
			public void onSuccess(UserTokenInfo info) {
				if(backListener != null){
					backListener.onCallBack(1, null);
				}
				LifeHelper.getInstance().updateCookies(DCloudApplication.getInstance(), QIHOO_URL);
			}
			@Override
			public void onFailure() {
				if(backListener != null){
					backListener.onCallBack(-1, null);
				}
			}
		});
    }
	
    /**
     * 版权所有 2005-2015 奇虎360科技有限公司。 保留所有权利。<br>
     * 项目名：360手机助手-Android客户端<br>
     * 描述：回调接口
     *
     * @author zhangguojun
     * @version 1.0
     * @since JDK1.6
     */
    public interface OnResult {
        int CODE_OK = 0;
        String MSG_OK = "OK";
        int CODE_UNKNOW = -1;
        String MSG_UNKNOW = "FAILED";

        /**
         * 回调函数
         *
         * @param resultCode 返回状态码
         * @param resultMsg  回调状态描述
         * @param resultData 回调json格式数据
         */
        void onResult(int resultCode, String resultMsg, JSONObject resultData);
    }


    private static final String QIHOO_URL = ".360.cn";
}