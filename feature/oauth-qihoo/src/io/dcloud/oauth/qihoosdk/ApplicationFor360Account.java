package io.dcloud.oauth.qihoosdk;

import com.qihoo.payment2jar.PayManager;
import com.qihoo360.accounts.sso.svc.QihooServiceController;

import io.dcloud.application.DCloudApplication;

public class ApplicationFor360Account extends DCloudApplication {

	private static final String TAG = "ACCOUNT.DemoApplication";

	boolean DEBUG = true;
	@Override
	public void onCreate() {
		super.onCreate();
		initSSO();
		PayManager.init(getApplicationContext());
	}

	// 由于sso服务端在另外一个进程中运行， 请保证在application中初始化sso
	private void initSSO() {
		if (DEBUG) {
			// release版本必须去掉此次调用， 否则sso服务会异常
			QihooServiceController.openDebugMode();
		}
		// 初始化SSO服务, 需要用户中心分配的from/sign key/encrypt key
//		QihooServiceController.initSSO(Conf.FROM,
//				Conf.SIGN_KEY, Conf.CRYPT_KEY);
		 QihooServiceController.initSSO(Conf.FROM, Conf.SIGN_KEY,
					Conf.CRYPT_KEY);
	}
}
