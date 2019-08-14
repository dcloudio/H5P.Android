package com.qihoo360.accounts.ui.v;

import com.qihoo360.accounts.api.auth.model.UserTokenInfo;

/**
 * 登录成功
 * 
 * @author wangzefeng
 *
 */
public interface ILoginResultListener {

    void onLoginSuccess(UserTokenInfo info);

    void onLoginError(int errorType, int errorCode, String errorMessage);
}
