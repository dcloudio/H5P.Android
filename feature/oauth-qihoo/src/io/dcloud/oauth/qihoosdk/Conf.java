package io.dcloud.oauth.qihoosdk;

public class Conf {

    public static final boolean DEBUG = true;

    /**
     * 业务来源标识, 业务方需要向用户中心按产品线申请
     */
    public static final String FROM = "mpc_lifeassist_and";

    /**
     * 签名私钥, 业务方需要向用户中心按产品线申请
     */
    public static final String SIGN_KEY = "9232b2ef8";

    /**
     * 加密私钥, 业务方需要向用户中心按产品线申请
     */
    public static final String CRYPT_KEY = "76b76b87";
    
    /**
     * 设置需要的邮箱注册方式,可设置的值为：
     * SdkOptionAdapt.NO_EMAIL
     * SdkOptionAdapt.EMAIL_ACTIVIE
     * SdkOptionAdapt.EMAIL_NO_ACTIVIE
     * 具体值代表的含义，见SdkOptionAdapt.java文件
     */
    public static final int EMAIL_REG_TYPE = SdkOptionAdapt.EMAIL_ACTIVIE;
    
    /**
     * 设置需要的手机注册方式，可设置的值为：
     * SdkOptionAdapt.UP_SMS
     * SdkOptionAdapt.DOWN_SMS
     * 具体值代表的含义，见SdkOptionAdapt.java文件
     */
    public static final int MOBILE_REG_TYPE = SdkOptionAdapt.DOWN_SMS;
    
}
