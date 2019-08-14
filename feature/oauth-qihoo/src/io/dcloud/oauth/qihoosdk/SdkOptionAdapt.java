package io.dcloud.oauth.qihoosdk;
import android.os.Bundle;

import com.qihoo360.accounts.base.common.Constant;

/***
 * 适配用户中心sdk需要的配置
 * @author zhuribing
 *
 */
public class SdkOptionAdapt {
  
    public static String USER_OP_KEY = "user_op";
    // 显示登录界面
    public static int USER_LOGIN = 1;

    // 显示注册界面
    public static int USER_REGISTER = 2;
    public static String INIT_USER_KEY = "init_user";


    /*******************************************************************************************
     * 下面的配置设置系统支持的注册方式：包括邮箱注册的设置和手机号注册设置：
     * 具体注册方式设置可以参照下面的getBundle（）函数; 
     * 请业务根据自身需求选择自身需要的注册方式。将对应的注册方式值配置在自己项目的配置文件内。
     * 注：如果用户不设置，sdk默认设置支持的注册方式为：验证邮箱注册和下行短信注册（手机号注册）
     *******************************************************************************************/
    public static String EMAIL_REGISTER_TYPE_KEY = "email_reg_type";
    // 不需要邮箱注册
    public static int NO_EMAIL = 1;

    // 需要邮箱注册（邮箱需要激活）
    public static int EMAIL_ACTIVIE = 2;

    // 需要邮箱注册（邮箱不需要激活）
    public static int EMAIL_NO_ACTIVIE = 3;

    public static String MOBILE_REGISTER_TYPE_KEY = "mobile_reg_type";
    // 设置为上行短信（用户发送一条短信注册，不推荐使用这样注册方式）
    public static int UP_SMS = 1;
    // 注册方式:设置为下行短信（用户输入手机号，系统下发短信）
    public static int DOWN_SMS = 2;
  

    /**
     * 从配置文件里（Conf.java）获取业务方配置的注册方式 ；
     * 业务方使用，请使用这方法
     * @param viewType
     * @param initUser
     * @return
     */
    public static Bundle getBundle(int viewType, String initUser)
    {
        return SdkOptionAdapt.getBundle(viewType, Conf.EMAIL_REG_TYPE, Conf.MOBILE_REG_TYPE, initUser);
    }
    
    /**
     * demo 代码为演示SDK支持发所有注册方式；业务请使用上面的get方法。
     * @param viewType
     * @param emailRegType
     * @param mobileRegType
     * @param initUser
     * @return
     */
    public static Bundle getBundle(int viewType, int emailRegType,  int mobileRegType, String initUser)
    {
        Bundle initBundle=new Bundle();
        // 控制ui接口展示界面类型
        if (viewType == SdkOptionAdapt.USER_LOGIN) {
            // 登录界面
            initBundle.putInt(Constant.KEY_ADD_ACCOUNT_TYPE, Constant.VALUE_ADD_ACCOUNT_LOGIN);
        } else {
            // 注册界面
            initBundle.putInt(Constant.KEY_ADD_ACCOUNT_TYPE, Constant.VALUE_ADD_ACCOUNT_REGISTER);
        }

        // 注册方式设定,业务方请按照自己的需求选择注册方式；
        //注：如果用户不设置，sdk默认设置支持的注册方式为：验证邮箱注册和下行短信注册（手机号注册）
        
        //设置邮箱注册方式
        if (emailRegType == SdkOptionAdapt.NO_EMAIL) {
            //不需要邮箱注册方式
            initBundle.putInt(Constant.KEY_ADD_ACCOUNT_EMAIL_TYPE, Constant.VALUE_ADD_ACCOUNT_EMAIL_REGISTER);
            initBundle.putInt(Constant.KEY_ADD_ACCOUNT_EMAIL, Constant.VALUE_ADD_ACCOUNT_NO_EMAIL);

        } else if (emailRegType == SdkOptionAdapt.EMAIL_NO_ACTIVIE) {
            // 设置为手机号注册和邮箱注册（邮箱不需要激活）
            initBundle.putInt(Constant.KEY_ADD_ACCOUNT_EMAIL_TYPE, Constant.VALUE_ADD_ACCOUNT_EMAIL_REGISTER);
            initBundle.putInt(Constant.KEY_ADD_ACCOUNT_EMAIL, Constant.VALUE_ADD_ACCOUNT_HAS_EMAIL);

        } else {
            // 设置为手机号注册和邮箱注册（邮箱需要激活）
            initBundle.putInt(Constant.KEY_ADD_ACCOUNT_EMAIL_TYPE, Constant.VALUE_ADD_ACCOUNT_EMAIL_REGISTER_ACTIVE);
            initBundle.putInt(Constant.KEY_ADD_ACCOUNT_EMAIL, Constant.VALUE_ADD_ACCOUNT_HAS_EMAIL);
        }

        //设置手机注册方式
        if(mobileRegType == SdkOptionAdapt.UP_SMS){
            //设置为上行短信（用户发送一条短信注册，不推荐使用这样注册方式）
            initBundle.putInt(Constant.KEY_ADD_ACCOUNT_MOBILE_TYPE, Constant.VALUE_ADD_ACCOUNT_UP_SMS_REGISTER);
        } else {
            //设置为下行短信（用户输入手机号，系统下发短信）
           initBundle.putInt(Constant.KEY_ADD_ACCOUNT_MOBILE_TYPE, Constant.VALUE_ADD_ACCOUNT_DOWN_SMS_REGISTER);
        }
        
        // 账号输入框默认初始化的用户
        initBundle.putString(Constant.KEY_ADD_ACCOUNT_USER, initUser);

        //调用用户中心接口需要的来源标识和密钥
        initBundle.putString(Constant.KEY_CLIENT_AUTH_FROM, Conf.FROM);
        initBundle.putString(Constant.KEY_CLIENT_AUTH_SIGN_KEY, Conf.SIGN_KEY);
        initBundle.putString(Constant.KEY_CLIENT_AUTH_CRYPT_KEY, Conf.CRYPT_KEY);
        return initBundle;
    }

    
}
