package io.dcloud.oauth.qihoosdk;

import static com.qihoo360.accounts.base.env.BuildEnv.LOGD_ENABLED;

import java.util.Map;

import org.json.JSONObject;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import com.qihoo360.accounts.QihooAccount;
import com.qihoo360.accounts.base.utils.DesUtil;

/****
 * demo 中用该类来记录demo app中当前登录的用户信息；
 * 业务方如果实现自己的用户登录信息管理，可以删除文件
 * @author zhuribing
 *
 */
public class MangeLogin {
	private static final String TAG = "ACCOUNT.MangeLogin";
	private static final String mSpKey = "login_account_id";
	private static final String mKey = "qihoo360";
	private static String mSpName = "qihoo360_login_account";

	public static QihooAccount get(Context context) {
		SharedPreferences sp = context.getSharedPreferences(mSpName, 0);
		Map<String, ?> all = sp.getAll();
		if (all == null || !sp.contains(mSpKey)) {
			return null;
		}

		String str = all.get(mSpKey).toString();
		QihooAccount acc = null;
		if (!TextUtils.isEmpty(str)) {
			str = DesUtil.decryptDES(str, mKey);
			if (!TextUtils.isEmpty(str)) {
				try {
					JSONObject jo = new JSONObject(str);
					acc = new QihooAccount(jo);
				} catch (Throwable e) {
					//
				}
			}
		}
		if (TextUtils.isEmpty(str) && LOGD_ENABLED) {
			Log.e(TAG, "decrypt is empty");
		}

		return acc;
	}

	/**
	 * 帐号本地存储
	 * @param context
	 * @param account
	 */
	public static void store(Context context, QihooAccount account) {
		if (account == null) {
			return;
		}
		
		JSONObject jo = account.toJSONObject();
		if (jo != null) {
			String str = jo.toString();
			if (!TextUtils.isEmpty(str)) {
				str = DesUtil.encryptDES(str, mKey);
				if (!TextUtils.isEmpty(str)) {
					context.getSharedPreferences(mSpName, 0).edit()
							.putString(mSpKey, str).commit();

				} 
				if (TextUtils.isEmpty(str) && LOGD_ENABLED) {
					Log.e(TAG, "encrypt is empty");
				}
			}
		}
	}

	/**
	 * 删除本地帐号
	 * @param context
	 * @param account
	 */
	public static void clear(Context context) {
		context.getSharedPreferences(mSpName, 0).edit().remove(mSpKey).commit();
	}

}
