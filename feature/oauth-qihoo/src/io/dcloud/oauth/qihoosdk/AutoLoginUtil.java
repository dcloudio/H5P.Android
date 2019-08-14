package io.dcloud.oauth.qihoosdk;

import io.dcloud.application.DCloudApplication;
import io.dcloud.common.adapter.util.Logger;
import android.content.Context;
import android.os.Looper;
import android.text.TextUtils;

import com.qihoo360.accounts.QihooAccount;
import com.qihoo360.accounts.api.auth.RefreshUser;
import com.qihoo360.accounts.api.auth.i.IRefreshListener;
import com.qihoo360.accounts.api.auth.model.UserTokenInfo;
import com.qihoo360.accounts.api.auth.p.ClientAuthKey;
import com.qihoo360.accounts.api.auth.p.UserCenterUpdate;

/**
 * Created by maofei on 2015/8/7.
 * 自动登录
 */
public class AutoLoginUtil {

    public static final String TAG = "AutoLoginUtil";
    public interface SimpleLoginCallBack {

        public void onSuccess(UserTokenInfo info);

        public void onFailure();
    }

    public static void autoLogin(final SimpleLoginCallBack callback) {
        final Context context = DCloudApplication.getInstance();
        QihooAccount qa = MangeLogin.get(context);
        if (qa == null || TextUtils.isEmpty(qa.mQ) || TextUtils.isEmpty(qa.mT) || TextUtils.isEmpty(qa.mAccount)) {
        	Logger.d(TAG, "autoLogin failed, q or t is empty.");
            if (callback != null) {
                callback.onFailure();
            }
            return;
        }
        final String account = qa.mAccount;
        Logger.d(TAG, "autoLogin, account = " + account);
        ClientAuthKey cak = new ClientAuthKey(Conf.FROM, Conf.SIGN_KEY, Conf.CRYPT_KEY);
        RefreshUser refreshUserLogic = new RefreshUser(context, cak, Looper.myLooper(), new IRefreshListener() {

            @Override
            public void onRefreshSuccess(UserTokenInfo info) {
                try {
                	Logger.d(TAG, "autoLogin onRefreshSuccess UserTokenInfo = " + info);
                	QihooAccount tQihooAccount = info.toQihooAccount();
                	MangeLogin.store(context, tQihooAccount);
                    if (callback != null) {
                        callback.onSuccess(info);
                    }
                } catch (Exception e) {
                    Logger.d(TAG, "autoLogin onRefreshSuccess failed.", e);
                    if (callback != null) {
                        callback.onFailure();
                    }
                }
            }

            @Override
            public void onRefreshError(int errorType, int errorCode, String errorMsg) {
            	Logger.d(TAG, "autoLogin onRefreshError.errorType = " + errorType + ", errorCode = " + errorCode + ", errorMsg = " + errorMsg);
                if (callback != null) {
                    callback.onFailure();
                }
            }

            @Override
            public void onInvalidQT(String errorMsg) {
            	Logger.d(TAG, "autoLogin onInvalidQT. errorMsg = " + errorMsg);
                if (callback != null) {
                    callback.onFailure();
                }
            }
        });

        refreshUserLogic.refresh(account, qa.mQ, qa.mT, null, UserCenterUpdate.HEAD_150X150);
    }

}
