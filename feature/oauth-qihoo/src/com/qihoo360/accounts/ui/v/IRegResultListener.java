package com.qihoo360.accounts.ui.v;

import com.qihoo360.accounts.api.auth.model.UserTokenInfo;

/**
 * 注册成功
 * 
 * @author wangzefeng
 *
 */
public interface IRegResultListener {

    void onRegisterSuccess(UserTokenInfo info);

    void onRegisterError(int errorType, int errorCode, String errorMessage);
}
