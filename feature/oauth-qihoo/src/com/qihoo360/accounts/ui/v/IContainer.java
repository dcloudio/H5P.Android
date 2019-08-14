package com.qihoo360.accounts.ui.v;

import android.os.Looper;
import android.view.View;
import com.qihoo360.accounts.api.auth.p.ClientAuthKey;
import com.qihoo360.accounts.base.common.DefaultLocalAccounts;

/**容器类，供各个view处理
 *
 * @author wangzefeng
 *
 */
public interface IContainer {

    /**
     * 处理返回键
     */
    public void back();

    /**
     * 容器结束
     */
    public void finish();

    /**
     * @return
     */
    public ILoginResultListener loginListener();

    /**
     * @return
     */
    public IRegResultListener registerListener();

    /**
     * 获取Looper
     */
    public Looper getLooper();


    /**
     * 获取InitUser
     */
    public String getInitUser();
    
    /**
     * 获取是否单卡
     */
    public boolean getIsSingleSimCard();
    
    /**
     * 获取邮箱注册是否需要激活
     */
    public boolean getIsNeedActiveEmail();
    
    /**
     * 获取是否提供邮箱注册
     */
    public boolean getIsNeedEmailRegister();
    
    /**
     * 获取是否启动上行短信注册
     */
    public boolean getIsNeedUpSmsRegister();

    /**
     * 获取下行短信页面
     */
    public View getLoginView();
    /**
     * 获取下行短信页面
     */
    public View getRegDownSmsView();

    /**
     * 获取下行短信的验证码提交界面
     */
    public View getRegDownSmsCaptchaView();
    
    /**
     * 获取邮箱注册激活界面
     */
    public View getRegEmailActiveView();
    
    /**
     * 获取手机找回密码短信验证码界面
     */
    public View getFindPwdByMobileView();
    
    /**
     * 获取手机找回密码短信验证码界面
     */
    public View getFindPwdByMobileCaptchaView();

    /**
     * 获取手机找回密码保存新密码界面
     */
    public View getFindPwdByMobileSavePwdView();
    
    /**
     * 控制在Activity中显示哪个view
     */
    public void showAddAccountsView(int viewType);


	public ClientAuthKey getClientAuthKey();

	public DefaultLocalAccounts getUiAccounts();

}