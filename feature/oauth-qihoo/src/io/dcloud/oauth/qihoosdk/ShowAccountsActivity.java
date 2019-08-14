package io.dcloud.oauth.qihoosdk;

import android.content.Intent;
import android.os.Bundle;

import com.qihoo360.accounts.QihooAccount;
import com.qihoo360.accounts.base.common.Constant;
import com.qihoo360.accounts.ui.a.SelectAccountActivity;

 /**
  * 
  * @author zhuribing
  * 业务方使用sso功能，且使用默认的帐号列表展示页时，参考该activity
  */
public class ShowAccountsActivity extends SelectAccountActivity {

	private static final String TAG = "ACCOUNT.AccountsManageActivity";
	

	@Override
	protected void handleAccountSelected(QihooAccount selectedAccount) {
		super.handleAccountSelected(selectedAccount);
	
		// TODO 业务方按需要添加逻辑
	}
	
	/**
	 * 重写该方法，传递调用用户中心接口的业务标识和密钥（FROM/SIGN_KEY/CRYPT_KEY）
	 * 
	 */
	@Override
	protected Bundle getInitParam() {
		Bundle initBundle=new Bundle();
		initBundle.putString(Constant.KEY_CLIENT_AUTH_FROM, Conf.FROM);
		initBundle.putString(Constant.KEY_CLIENT_AUTH_SIGN_KEY,Conf.SIGN_KEY);
		initBundle.putString(Constant.KEY_CLIENT_AUTH_CRYPT_KEY,Conf.CRYPT_KEY);
		return initBundle;
	}  

	@Override
	public final void handle2register() {
	    displayUI(SdkOptionAdapt.USER_REGISTER);
		finish();
	}

	@Override
	public final void handle2Login() {
        displayUI(SdkOptionAdapt.USER_LOGIN);
        finish();
	}

	private void displayUI(int op){
	    
        Intent intent = getIntent();
        int emailRegType = intent.getIntExtra(SdkOptionAdapt.EMAIL_REGISTER_TYPE_KEY, SdkOptionAdapt.EMAIL_ACTIVIE);
        int mobileRegType = intent.getIntExtra(SdkOptionAdapt.MOBILE_REGISTER_TYPE_KEY, SdkOptionAdapt.DOWN_SMS);
       
        Intent startIntent  = new Intent(this, SsoUCActivity.class);
        startIntent.putExtra(SdkOptionAdapt.INIT_USER_KEY, "");
       
        //业务方使用，可以将下面两行删掉，在配置文件里设置注册方式，此处不需要传递。
        startIntent.putExtra(SdkOptionAdapt.EMAIL_REGISTER_TYPE_KEY, emailRegType);
        startIntent.putExtra(SdkOptionAdapt.MOBILE_REGISTER_TYPE_KEY, mobileRegType);

	    if(op == SdkOptionAdapt.USER_LOGIN){
	        startIntent.putExtra(SdkOptionAdapt.USER_OP_KEY, SdkOptionAdapt.USER_LOGIN);
	    } else {
	        startIntent.putExtra(SdkOptionAdapt.USER_OP_KEY, SdkOptionAdapt.USER_REGISTER);
	    }
       
	    startActivity(startIntent);
	}
	
}
