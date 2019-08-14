package io.dcloud.oauth.qihoosdk;

import android.content.Intent;
import android.os.Bundle;

import com.qihoo360.accounts.QihooAccount;
import com.qihoo360.accounts.api.auth.model.UserTokenInfo;
import com.qihoo360.accounts.sso.cli.QihooSsoAPI;
import com.qihoo360.accounts.ui.a.AddAccountActivity;
import com.qihoo360.accounts.ui.a.SelectAccountActivity;

/**
 * @author zhuribing
 * 业务方需要使用sso功能时， 参考该activity
 * SsoUCActivity中的UC为user center的缩写; Sso为Single Sign On缩写。
 * QihooSsoAPI 底层应用到AsyncTask, 请确保在ui线程中调用。
 */
public class SsoUCActivity extends AddAccountActivity {

    private static final String TAG = "ACCOUNT.SsoUCActivity";
    protected long mRequestId;
	 
    @Override
    protected void onCreate(Bundle savedInstanceState){
    	super.onCreate(savedInstanceState);
    }
    
	@Override
    protected void onFinished() {
        // do something
        super.onFinished();
    }

	
	@Override
	protected void onDestroy() {
		super.onDestroy();
	}
 
	
    /**
     * 处理登录成功后的事，需业务方添加逻辑
     */
    @Override
    public void handleLoginSuccess(UserTokenInfo info) {
        //添加到sso 账号池
        QihooSsoAPI.getInstance(SsoUCActivity.this, Conf.FROM, Conf.SIGN_KEY,
                Conf.CRYPT_KEY).attachAccount(info.toQihooAccount());
       
        //此处存储登录用户信息，只是为了demo里显示用； 业务可以按自己业务特点确定是否需要存储登录用户信息。
        MangeLogin.store(this, info.toQihooAccount());
        Intent intent = new Intent();
        intent.putExtra(SelectAccountActivity.KEY_SELECTED_ACCOUNT, info.toQihooAccount());
        setResult(1, intent);
        // TODO 处理登录成功后的事，需业务方添加逻辑
        finish();
//        finishActivity(1);
    }

    /**
     * 处理注册成功后的事，需业务方添加逻辑
     */
    @Override
    public void handleRegisterSuccess(UserTokenInfo info) {
        //添加到sso 账号池
        QihooSsoAPI.getInstance(SsoUCActivity.this, Conf.FROM, Conf.SIGN_KEY,
                Conf.CRYPT_KEY).attachAccount(info.toQihooAccount());
    
        //此处存储登录用户信息，只是为了demo里显示用； 业务可以按自己业务特点确定是否需要存储登录用户信息。
    	MangeLogin.store(this, info.toQihooAccount());
    	 Intent intent = new Intent();
         intent.putExtra(SelectAccountActivity.KEY_SELECTED_ACCOUNT, info.toQihooAccount());
         setResult(1, intent);
    	// TODO 处理注册成功后的事，需业务方添加逻辑
        finish();
    }

    /**
     * 设置注册方式/传递调用用户中心接口的业务标识和密钥（FROM/SIGN_KEY/CRYPT_KEY）
     */
	@Override
	protected Bundle getInitParam() {
      
	    Intent intent = getIntent();
        int viewType = intent.getIntExtra(SdkOptionAdapt.USER_OP_KEY, SdkOptionAdapt.USER_LOGIN);
        String initUser = intent.getStringExtra(SdkOptionAdapt.INIT_USER_KEY);
        
        /**
         * 如果是业务方使用，请将下面代码注释打开；并删除后面三行。
         */
        //return SdkOptionAdapt.getBundle(viewType,initUser);
        
        int emailRegType = intent.getIntExtra(SdkOptionAdapt.EMAIL_REGISTER_TYPE_KEY, SdkOptionAdapt.EMAIL_ACTIVIE);
        int mobileRegType = intent.getIntExtra(SdkOptionAdapt.MOBILE_REGISTER_TYPE_KEY, SdkOptionAdapt.DOWN_SMS);
        return SdkOptionAdapt.getBundle(viewType, emailRegType, mobileRegType, initUser);
   
	}
	
	/**
	 * 在登录、注册成功后业务方可能会有些耗时操作，登录注册成功时的Dialog先不会关闭,
	 * 可由业务控制手动关闭dialog，
	 * 如果不调用该函数则Activity销毁时自动关闭Dialog
	 */
	@Override
	protected void closeDialogs() {
		// TODO Auto-generated method stub
		super.closeDialogs();
	}

	/**
     * 供业务调用展示dialog(正在doing...)
     * @param title
     * @param msg
     */
	@Override
	protected void showCustomDialog(String title, String msg) {
		// TODO Auto-generated method stub
		super.showCustomDialog(title, msg);
	}
}
