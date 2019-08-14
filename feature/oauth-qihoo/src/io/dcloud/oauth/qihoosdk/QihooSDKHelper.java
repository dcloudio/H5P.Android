package io.dcloud.oauth.qihoosdk;

import io.dcloud.application.DCloudApplication;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.ISysEventListener.SysEventType;
import io.dcloud.common.DHInterface.IWebview;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;

import com.qihoo360.accounts.QihooAccount;
import com.qihoo360.accounts.sso.cli.QihooSsoAPI;
import com.qihoo360.accounts.ui.a.SelectAccountActivity;

public class QihooSDKHelper{

	private static final String KEY_TOKEN = "access_token";
	
	Context mApplicationContext = null;
	private static QihooSDKHelper mInstance = null;
	private QihooSDKHelper(){
		mApplicationContext = DCloudApplication.getInstance();
		mSsoApi = QihooSsoAPI.getInstance(mApplicationContext, Conf.FROM, Conf.SIGN_KEY,
				Conf.CRYPT_KEY);
	}
	public static QihooSDKHelper self(){
		if(mInstance == null){
			mInstance = new QihooSDKHelper();
		}
		return mInstance;
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
	public void login(final IWebview pWebViewImpl,final String appkey,final LoginStatusListener listener) {
		this.doSsoLogin(pWebViewImpl,true,listener);
	}

	public void logout() {
		QihooAccount account = MangeLogin.get(mApplicationContext);
		if (/*mSsoLogin !=0  && */account != null) {
			updateCookies(mApplicationContext, account);
			boolean ret = delAccount(account);
		}
		MangeLogin.clear(mApplicationContext);
	}
	
	private void updateCookies(Context context,QihooAccount account) {
		String url = ".360.cn";
        boolean isHasLogin = account.isValid();
        if (isHasLogin) { // 只有“*.360.cn”的域才能设置这些敏感数据；
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) { // 2.3及以下
                CookieSyncManager.createInstance(context);
            }
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            {
                cookieManager.setCookie(url, "Q=;path=/;domain=.360.cn");
                cookieManager.setCookie(url, "T=;path=/;domain=.360.cn");
                cookieManager.setCookie(url, "qid=;path=/;domain=.360.cn");
            }
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.GINGERBREAD_MR1) { // 2.3及以下
                CookieSyncManager.getInstance().sync();
            }
        }
    }
	
	private static final String TAG = "ACCOUNT.MainActivity";
	private static final int SELECT_ACCOUNT_REQUEST_CODE = 1;
	private static final int CHANGE_PWD_REQUEST_CODE = 2;
    // -----------------sso相关代码开始--------------------------------
	private QihooSsoAPI mSsoApi;

	private int mSsoLogin = 0;
	
	private String lastAccountName="";//保存最后一个帐号名, 某些情况下带到登录界面，减少用户输入

    private QihooAccount[] getAccounts() {
    	initSsoService();
        return mSsoApi.getAccounts();
    }

	private boolean delAccount(QihooAccount account) {
		return mSsoApi.removeAccount(account);
//		return mSsoApi.detachAccount(account);
	}
	
    private void doSsoLogin(final IWebview pWebViewImpl, boolean enableSelectAccount,final LoginStatusListener listener) {
        Intent intent;
        QihooAccount[] accounts = getAccounts();
        Activity activity = pWebViewImpl.getActivity();
        //如果获取sso 帐号列表为空，则让用户输入用户名密码进行登录。
        if (accounts != null && accounts.length > 0 && enableSelectAccount) {
            intent = new Intent(activity, ShowAccountsActivity.class);
            intent.putExtra(ShowAccountsActivity.KEY_ACCOUNTS, accounts);
            //业务方使用，可以将下面两行删掉，在配置文件里设置注册方式，此处不需要传递。
//            intent.putExtra(SdkOptionAdapt.EMAIL_REGISTER_TYPE_KEY, mEmailRegType);
            intent.putExtra(SdkOptionAdapt.MOBILE_REGISTER_TYPE_KEY, mMobileRegType);
            
        } else {
          
            intent = new Intent(activity, SsoUCActivity.class);
            intent.putExtra(SdkOptionAdapt.USER_OP_KEY, SdkOptionAdapt.USER_LOGIN);
            intent.putExtra(SdkOptionAdapt.INIT_USER_KEY, lastAccountName);
            //业务方使用，可以将下面两行删掉，在配置文件里设置注册方式，此处不需要传递。
//            intent.putExtra(SdkOptionAdapt.EMAIL_REGISTER_TYPE_KEY, mEmailRegType);
            intent.putExtra(SdkOptionAdapt.MOBILE_REGISTER_TYPE_KEY, mMobileRegType);
//            activity.startActivity(intent);
        }
        pWebViewImpl.obtainApp().registerSysEventListener(new ISysEventListener(){
        	@Override
        	public boolean onExecute(SysEventType pEventType, Object pArgs) {
        		Object[] _args = (Object[]) pArgs;
        		int requestCode = (Integer) _args[0];
        		int resultCode = (Integer) _args[1];
        		Intent data = (Intent) _args[2];
        		onActivityResult(requestCode, resultCode, data,listener);
        		pWebViewImpl.obtainApp().unregisterSysEventListener(this, SysEventType.onActivityResult);
        		return false;
        	}}, SysEventType.onActivityResult);
        activity.startActivityForResult(intent, SELECT_ACCOUNT_REQUEST_CODE);
    }

//	private void doSsoRegister(){
//        Intent intent = new Intent(mApplicationContext, SsoUCActivity.class);
//        intent.putExtra(SdkOptionAdapt.USER_OP_KEY, SdkOptionAdapt.USER_REGISTER);
//        intent.putExtra(SdkOptionAdapt.INIT_USER_KEY, lastAccountName);
//        //业务方使用，可以将下面两行删掉，在配置文件里设置注册方式，此处不需要传递。
//        intent.putExtra(SdkOptionAdapt.EMAIL_REGISTER_TYPE_KEY, mEmailRegType);
//        intent.putExtra(SdkOptionAdapt.MOBILE_REGISTER_TYPE_KEY, mMobileRegType);
//        mApplicationContext.startActivity(intent);
//	}
	
	protected void onActivityResult(int requestCode, int resultCode, Intent data,final LoginStatusListener listener) {
		boolean suc = false;
		QihooAccount account = null;
		switch (requestCode) {
			case SELECT_ACCOUNT_REQUEST_CODE:{//处理sso帐号列表选择帐号的结果
				if(resultCode == SelectAccountActivity.RESULT_CODE_PICK_OK){
					QihooAccount selectAccount = data
							.getParcelableExtra(SelectAccountActivity.KEY_SELECTED_ACCOUNT);
					if (mSsoApi != null) {
						mSsoApi.attachAccount(selectAccount);
					}//secmobile=13521379291 
					//登录成功
					MangeLogin.store(mApplicationContext, selectAccount);
					suc = true;
					account = selectAccount;
					if(listener != null){
						listener.onLoginStateChange(suc, account);
					}
				}else{
					listener.onLoginStateChange(suc, account);
				}
				break;
			}
		}
	}

	private void initOAuthData(QihooAccount account){
		if(account != null){
			lastAccountName = account.mAccount ;
		}
	}
	
    private void initSsoService() {
//	    if(TextUtils.isEmpty(Conf.FROM) || TextUtils.isEmpty(Conf.SIGN_KEY) || TextUtils.isEmpty(Conf.CRYPT_KEY)){
//	        Toast.makeText(mApplicationContext, "业务来源标识/签名加密私钥为空，请在Conf.java文件里设置相应的值; 否则demo将无法使用！", Toast.LENGTH_LONG).show();
//	        return ;
//	    }
		
		if (Conf.DEBUG) {
			Log.d("xx", "am=" + mSsoApi);
		}
		QihooAccount account = MangeLogin.get(mApplicationContext);
		initOAuthData(account);
		
	}

	protected void onDestroy() {
		if (mSsoApi != null) {
		    mSsoApi.close();
		}
	}

	// --------------以上代码仅和sso有关， 如果不需要使用sso相关功能， 可以删除----------------

	
	   
    //业务方在注册方式确定的情况下，设置Conf.java里的EMAIL_REG_TYPE和MOBILE_REG_TYPE的值就行。
	//此处写成成员变量是demo app 为了展示sdk所有支持的注册方式，业务方只需要其中的一部分，不需要全部。
    private int mEmailRegType = SdkOptionAdapt.EMAIL_ACTIVIE;
    private int mMobileRegType = SdkOptionAdapt.DOWN_SMS;


	private void doLogin() {
		Intent intent = new Intent(mApplicationContext, UCActivity.class);
		intent.putExtra(SdkOptionAdapt.USER_OP_KEY, SdkOptionAdapt.USER_LOGIN);
	    intent.putExtra(SdkOptionAdapt.INIT_USER_KEY, lastAccountName);
	    //业务方使用，可以将下面两行删掉，在配置文件里设置注册方式，此处不需要传递。
        intent.putExtra(SdkOptionAdapt.EMAIL_REGISTER_TYPE_KEY, mEmailRegType);
        intent.putExtra(SdkOptionAdapt.MOBILE_REGISTER_TYPE_KEY, mMobileRegType);
        mApplicationContext.startActivity(intent);
	}

    private void doRegister() {
        Intent intent = new Intent(mApplicationContext, UCActivity.class);
        intent.putExtra(SdkOptionAdapt.USER_OP_KEY, SdkOptionAdapt.USER_REGISTER);
        intent.putExtra(SdkOptionAdapt.INIT_USER_KEY, lastAccountName);
        //业务方使用，可以将下面两行删掉，在配置文件里设置注册方式，此处不需要传递。
        intent.putExtra(SdkOptionAdapt.EMAIL_REGISTER_TYPE_KEY, mEmailRegType);
        intent.putExtra(SdkOptionAdapt.MOBILE_REGISTER_TYPE_KEY, mMobileRegType);
        mApplicationContext. startActivity(intent);
    }

    
    public static interface LoginStatusListener{
    	void onLoginStateChange(boolean bLogin, Object object);
    }
}
