
package com.qihoo360.accounts.ui.model;


import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.CountDownTimer;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.qihoo360.accounts.QihooAccount;
import com.qihoo360.accounts.R;
import com.qihoo360.accounts.api.auth.model.UserTokenInfo;
import com.qihoo360.accounts.base.common.ErrorCode;
import com.qihoo360.accounts.base.utils.InputChecker;
import com.qihoo360.accounts.base.utils.MultiSimUtil;
import com.qihoo360.accounts.base.utils.PmUtils;
import com.qihoo360.accounts.ui.a.WebViewActivity;
import com.qihoo360.accounts.ui.v.AccountCustomDialog;
import com.qihoo360.accounts.ui.v.IContainer;

/**
 * UI基础库，提供基本服务
 * @author wangzefeng
 *
 */
public class AddAccountsUtils {
    private static final String SMS_URI = "content://sms/";
    private static CountDownTimer codeTimer;
    private static String emailUrl;
    private static String emailName;
    private static String emailPasswd;
    public static final String MAIL_HEAD="http://mail.";
    
    public static final String FIND_PWD_URL="http://i.360.cn/findpwdwap?client=app";
    public static final String LISENCE_URL="http://i.360.cn/reg/protocol";
    public static final String CH_PWD_URL="http://i.360.cn/profile/chuserpwdwap?client=app&appJumpNotify=1&isShowSuccess=1";
    public static final String BIND_MOBILE_URL="http://i.360.cn/security/bindmobilewap?client=app&appJumpNotify=1&isShowSuccess=1";
    
    /**
     * Dialog的宽度
     */
    private static int mWidth;

    /**
     * 对话框的message和errorcode的映射
     */
    private static final int DIALOG_ERROR_MESSAGE[][] = {
        {ErrorCode.ERR_CODE_BAD_JSON_DATA, R.string.qihoo_accounts_dialog_error_bad_data},
        {ErrorCode.ERR_CODE_BAD_SERVER_DATA, R.string.qihoo_accounts_dialog_error_bad_data},
        {ErrorCode.ERR_CODE_CANNOT_GET_CHPTCHA_FROM_DOWN_SMS, R.string.qihoo_accounts_dialog_error_no_captcha},
        {ErrorCode.ERR_CODE_LISTENING_DOWN_SMS_TIMEOUT, R.string.qihoo_accounts_dialog_error_no_captcha},
        {ErrorCode.ERR_CODE_CANNOT_SEND_SMS, R.string.qihoo_accounts_dialog_error_up_reg_cannot_send},
        {ErrorCode.ERR_CODE_OUT_OF_CERT_VALID_TIME, R.string.qihoo_accounts_dialog_error_out_of_valid_time},
        {ErrorCode.ERR_CODE_EMPTY_CAPTCHA, R.string.qihoo_accounts_dialog_error_empty_captcha},
        {ErrorCode.ERR_CODE_SSL_EXCEPTION, R.string.qihoo_accounts_dialog_error_ssl_exception},
        {ErrorCode.ERR_CODE_NETWORK_UNAVAILABLE, R.string.qihoo_accounts_dialog_error_no_network},
        {ErrorCode.ERR_CODE_CLIENT_PROTOCOL, R.string.qihoo_accounts_dialog_error_trans_data},
        {ErrorCode.ERR_CODE_TRANS_DATA, R.string.qihoo_accounts_dialog_error_trans_data},
        {ErrorCode.ERR_CODE_CONNECT_TIMEOUT, R.string.qihoo_accounts_dialog_error_connect_timeout},
        {ErrorCode.ERR_CODE_TRANS_TIMEOUT, R.string.qihoo_accounts_dialog_error_trans_timeout},
        {ErrorCode.ERR_CODE_HTTP_ERRCODE, R.string.qihoo_accounts_dialog_error_http_error},
        {ErrorCode.ERR_CODE_EAMIL_ACTIVE, R.string.qihoo_accounts_dialog_error_message_active},
        {ErrorCode.ERR_CODE_READ_LISENCE, R.string.qihoo_accounts_register_error_license},
        {ErrorCode.ERR_CODE_UNKNOWN, R.string.qihoo_accounts_dialog_error_trans_data},
        {ErrorCode.ERR_CODE_EAMIL_NEED_ACTIVE, R.string.qihoo_accounts_login_error_active_email},
        {ErrorCode.ERR_CODE_IMAGE_CAPTCHE, R.string.qihoo_accounts_image_captcha_error},
    };

    // 输入的手机号、邮箱、密码合法
    private static final int VALUE_VALID = 0;

    // 密码：错误类型和错误提醒的映射
    private static final int PASSWORD_ERRORS_MAP[][] = {
        {InputChecker.VALUE_PASSWORD_SUCCESS, VALUE_VALID},
        {InputChecker.VALUE_PASSWORD_ERROR_NULL, R.string.qihoo_accounts_valid_password_error_null},
        {InputChecker.VALUE_PASSWORD_ERROR_BLANKSPACE, R.string.qihoo_accounts_valid_password_error_blankspace},
        {InputChecker.VALUE_PASSWORD_ERROR_LENTH_SHORT, R.string.qihoo_accounts_valid_password_error_length_short},
        {InputChecker.VALUE_PASSWORD_ERROR_LENTH_LONG, R.string.qihoo_accounts_valid_password_error_length_long},
        {InputChecker.VALUE_PASSWORD_ERROR_CHINESE, R.string.qihoo_accounts_valid_password_error_chinese},
        {InputChecker.VALUE_PASSWORD_ERROR_SAMECHARS, R.string.qihoo_accounts_valid_password_error_samechars},
        {InputChecker.VALUE_PASSWORD_ERROR_CONTINUOUS, R.string.qihoo_accounts_valid_password_error_continuous},
        {InputChecker.VALUE_PASSWORD_ERROR_WEAK, R.string.qihoo_accounts_valid_password_error_weak},
    };

    // 手机号：错误类型和错误提醒的映射
    private static final int PHONE_ERRORS_MAP[] [] = {
        {InputChecker.VALUE_PHONE_SUCCESS, VALUE_VALID},
        {InputChecker.VALUE_PHONE_ERROR_NULL, R.string.qihoo_accounts_valid_phone_error_null},
        {InputChecker.VALUE_PHONE_ERROR_BLANKSPACE, R.string.qihoo_accounts_valid_phone_error_blankspace},
        {InputChecker.VALUE_PHONE_ERROR_NO_NUMBER, R.string.qihoo_accounts_valid_phone_error_no_number},
    };

    // 邮箱：错误类型和错误提醒的映射
    private static final int EMAIL_ERRORS_MAP[] [] = {
        {InputChecker.VALUE_EMAIL_SUCCESS, VALUE_VALID},
        {InputChecker.VALUE_EMAIL_ERROR_NULL, R.string.qihoo_accounts_valid_email_error_null},
        {InputChecker.VALUE_EMAIL_ERROR_BLANKSPACE, R.string.qihoo_accounts_valid_email_error_blankspace},
        {InputChecker.VALUE_EMAIL_ERROR_NO_EMAIL, R.string.qihoo_accounts_valid_email_error_no_email},
    };
    
    /**
     * webview Intent名称
     */
    public static final String KEY_ACCOUNTS_WEBVIEW = "webview";
    
    /**
     * 账号名称
     */
    public static final String KEY_ACCOUNTS_FINDPWD = "account";
    
    /**
     * webview intent
     * “找回密码”</p>
     */
    public static final int VALUE_ACCOUNTS_FINDPWD = 0x0011;
    
    /**
     * webview intent
     * “用户协议”</p>
     */
    public static final int VALUE_ACCOUNTS_LISENCE = 0x1100;

    /**
     * <p>正在进行的动作：类型</p>
     */
    public static final String KEY_ADD_ACCOUNTS_DIALOG_TYPE = "add_accounts_dialog_type";

    /**
     * <p>正在进行的动作：类型：登录</p>
     */
    public static final int VALUE_DIALOG_LOGIN = 1;

    /**
     * <p>正在进行的动作：类型：注册</p>
     */
    public static final int VALUE_DIALOG_REGISTER = 2;

    /**
     * <p>正在进行的动作：类型：提交验证码</p>
     */
    public static final int VALUE_DIALOG_COMMIT = 3;

    /**
     * <p>正在进行的动作：类型：重新发送</p>
     */
    public static final int VALUE_DIALOG_SEND_AGIAN = 4;

    /**
     * <p>正在进行的动作：类型：发送</p>
     */
    public static final int VALUE_DIALOG_SEND = 5;

    /**
     * <p>正在进行的动作：类型：进入邮箱激活链接</p>
     */
    public static final int VALUE_DIALOG_EMAIL_ACTIVE = 6;
    
    /**
     * <p>正在进行的动作：类型：进入webview</p>
     */
    public static final int VALUE_DIALOG_WEBVIEW = 7;
    /**
     * <p>页面显示：类型</p>
     */
    public static final String KEY_SHOW_VIEW_TYPE = "show_view_type";

    /**
     * <p>页面显示：登录</p>
     */
    public static final int VALUE_SHOW_LOGIN_VIEW = 0;

    /**
     * <p>页面显示：邮箱注册</p>
     */
    public static final int VALUE_SHOW_EMAIL_VIEW = 1;

    /**
     * <p>页面显示：上行短信</p>
     */
    public static final int VALUE_SHOW_UP_SMS_VIEW = 2;

    /**
     * <p>页面显示：下行短信</p>
     */
    public static final int VALUE_SHOW_DOWN_SMS_VIEW = 3;

    /**
     * <p>页面显示：下行短信，提交验证码</p>
     */
    public static final int VALUE_SHOW_DOWN_SMS_CAPTCHA_VIEW = 4;
    
    /**
     * <p>页面显示：邮箱注册激活页面</p>
     */
    public static final int VALUE_SHOW_EMAIL_ACTIVE_VIEW = 5;
    
    /**
     * <p>页面显示：手机找回密码第一步：输入手机号</p>
     */
    public static final int VALUE_SHOW_FINDPWD_MOBILE_VIEW = 6;
    
    /**
     * <p>页面显示：手机找回密码第二步：短信验证码</p>
     */
    public static final int VALUE_SHOW_FINDPWD_MOBILE_CAPTCHA_VIEW= 7;
    
    /**
     * <p>页面显示：手机找回密码第三步：保存新密码</p>
     */
    public static final int VALUE_SHOW_FINDPWD_MOBILE_SAVEPWD_VIEW= 8;
    
    /**
     * <p>找回密码手机号没有绑定错误码</p>
     */
    public static final int VALUE_FINDPWD_MOBILE_NOT_BIND = 1660;
    
    /**
     * <p>找回密码手机号不存在错误码</p>
     */
    public static final int VALUE_FINDPWD_MOBILE_NOT_EXIST = 1105;
    
    /**
     * <p>上行注册和邮箱注册账号已存在错误码</p>
     */
    public static final int VALUE_REGISTER_UP_AND_EMAIL_EXIST = 1037;
    
    /**
     * <p>下行注册账号已存在错误码</p>
     */
    public static final int VALUE_REGISTER_DOWN_EXIST = 1106;
    
    /**
     * <p>密码错误</p>
     */
    public static final int VALUE_LOGIN_PWD_ERROR = 5009;
    
    /**
     * <p>短信验证码错误</p>
     */
    public static final int VALUE_SMS_CODE_ERROR = 1351;
    
    /**
     * 当前正在做的事情，正在注册，正在登录，正在提交验证码（下行短信时），正在重新发送（下行短信时）
     * @param context
     * @param doingType
     * @return
     */
    public static AccountCustomDialog showDoingDialog(final Context context, int doingType) {
    	@SuppressLint("InflateParams")
        View doingView = ((Activity) context).getLayoutInflater().inflate(R.layout.qihoo_accounts_dialog_doing, null, false);
        View rotateView = doingView.findViewById(R.id.dialog_rotate_layout);
        rotateView.setVisibility(View.GONE);
        showRotateView(context, doingType, rotateView);
        final AccountCustomDialog doingDialog = new AccountCustomDialog(context, R.style.qihoo_accounts_dialog_style);
        // 如果点击下行短信的重新发送按钮，弹出的Dialog，timeout为3秒，默认60秒
        if (doingType == VALUE_DIALOG_SEND_AGIAN) {
            doingDialog.setTimeout(3 * 1000);
        }
        doingDialog.setContentView(doingView);
        doingDialog.setCancelable(false);
        doingDialog.getWindow().setLayout(getWidthOfDialog(context), ViewGroup.LayoutParams.WRAP_CONTENT);
        if (!((Activity) context).isFinishing()) {
            doingDialog.show();

            return doingDialog;
        }
        return null;
    }

    /**
     * 带转圈动画的Dialog(正在登录Dialog、正在提交验证码Dialog、正在重新发送Dialog）
     * @param context
     * @param type
     * @param rotateView
     */
    private static void showRotateView(Context context, int type, View rotateView) {
        rotateView.setVisibility(View.VISIBLE);
        TextView rotateText = (TextView) rotateView.findViewById(R.id.dialog_rotate_text);
        if (type == VALUE_DIALOG_LOGIN) {
            rotateText.setText(R.string.qihoo_accounts_dialog_doing_login);
        } else if (type == VALUE_DIALOG_REGISTER) {
            rotateText.setText(R.string.qihoo_accounts_dialog_doing_register);
        } else if (type == VALUE_DIALOG_COMMIT) {
            rotateText.setText(R.string.qihoo_accounts_dialog_doing_commit);
        } else if (type == VALUE_DIALOG_SEND) {
            rotateText.setText(R.string.qihoo_accounts_dialog_doing_send);
        } else if (type == VALUE_DIALOG_SEND_AGIAN) {
            rotateText.setText(R.string.qihoo_accounts_dialog_doing_send_again);
        }else if(type==VALUE_DIALOG_WEBVIEW){
        	rotateText.setText(R.string.qihoo_accounts_dialog_doing_loading);
        }
        Animation am = AnimationUtils.loadAnimation(context, R.anim.qihoo_accounts_logining_rotate);
        LinearInterpolator lin = new LinearInterpolator();
        am.setInterpolator(lin);
        ImageView imageView = (ImageView) rotateView.findViewById(R.id.dialog_rotate_image);
        if (am != null) {
            imageView.startAnimation(am);
        }
    }


    public static void showWebviewRotate(Context context,ImageView imageView){
    	  Animation am = AnimationUtils.loadAnimation(context, R.anim.qihoo_accounts_logining_rotate);
          LinearInterpolator lin = new LinearInterpolator();
          am.setInterpolator(lin);
          if (am != null) {
              imageView.startAnimation(am);
          }
    }
    
    private static String mTitle = "";

    private static String mMessage = "";

    /**
     * 出错对话框
     * @param context
     * @param listener
     * @param operateType
     * @param errorType
     * @param errorCode
     * @param errorMessage
     * @return
     */
    public static Dialog showErrorDialog(final Context context, OnClickListener listener, int operateType, int errorType, int errorCode, String errorMessage) {
    	@SuppressLint("InflateParams")
    	View errorView = ((Activity) context).getLayoutInflater().inflate(R.layout.qihoo_accounts_dialog_do_error, null, false);
        View closeAcon = (View) errorView.findViewById(R.id.add_accounts_dialog_error_title_icon);
        closeAcon.setOnClickListener(listener);
        Button cancelBtn = (Button) errorView.findViewById(R.id.add_accounts_dialog_error_cancel_btn);
        cancelBtn.setOnClickListener(listener);
        Button okBtn = (Button) errorView.findViewById(R.id.add_accounts_dialog_error_ok_btn);
        okBtn.setOnClickListener(listener);
        TextView titleText = (TextView) errorView.findViewById(R.id.add_accounts_dialog_error_title_text);
        TextView messageText = (TextView) errorView.findViewById(R.id.add_accounts_dialog_error_message_text);
        if(operateType == VALUE_DIALOG_EMAIL_ACTIVE){
        	//邮箱需要激活
        	okBtn.setText(R.string.qihoo_accounts_dialog_error_btn_confirm_active);
        	cancelBtn.setText(R.string.qihoo_accounts_dialog_error_btn_cancel_active);
        }
        //上行注册或下行注册手机号已被注册
        if(errorCode == ErrorCode.ERR_CODE_REGISTER_DOWN_EXIST || errorCode == ErrorCode.ERR_CODE_REGISTER_UP_EXIST ){
        	okBtn.setText(R.string.qihoo_accounts_dialog_error_btn_confirm_reg);
        	cancelBtn.setText(R.string.qihoo_accounts_dialog_error_btn_cancel_reg);
        }else if(errorCode==ErrorCode.ERR_CODE_REGISTER_EMAIL_EXIST){
        	//邮箱已被注册
        	okBtn.setText(R.string.qihoo_accounts_dialog_error_btn_confirm_reg);
        	cancelBtn.setText(R.string.qihoo_accounts_dialog_error_btn_cancel_reg);
        }else if(errorCode==ErrorCode.ERR_CODE_EAMIL_NEED_ACTIVE){
        	okBtn.setText(R.string.qihoo_accounts_dialog_error_btn_confirm_login_active);
        	cancelBtn.setText(R.string.qihoo_accounts_dialog_error_btn_cancel_login_active);
        }
        initErrorDialogTitleMessage(context, operateType, errorType, errorCode, errorMessage);

        titleText.setText(mTitle);
        messageText.setText(mMessage);
        if(errorCode==ErrorCode.ERR_CODE_REGISTER_UP_EXIST || errorCode == ErrorCode.ERR_CODE_REGISTER_DOWN_EXIST){
        	String firtString=context.getResources().getString(R.string.qihoo_accounts_dialog_error_reg_mobile_message_default_first);
            String lastString=context.getResources().getString(R.string.qihoo_accounts_dialog_error_reg_message_default_last);
        	String messageString=firtString+mMessage+lastString;
        	SpannableStringBuilder style=new SpannableStringBuilder(messageString);
        	//设置手机号（绿色）注册 
        	style.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.qihoo_accounts_green)),firtString.length(),firtString.length()+mMessage.length(),Spannable.SPAN_EXCLUSIVE_INCLUSIVE);     //设置指定位置文字的颜色  
        	messageText.setText(style);
        }else if(errorCode==ErrorCode.ERR_CODE_REGISTER_EMAIL_EXIST){
        	String firtString=context.getResources().getString(R.string.qihoo_accounts_dialog_error_reg_email_message_default_first);
            String lastString=context.getResources().getString(R.string.qihoo_accounts_dialog_error_reg_message_default_last);
        	String messageString=firtString+mMessage+lastString;
        	SpannableStringBuilder style=new SpannableStringBuilder(messageString);
        	//设置邮箱（绿色）注册 
        	style.setSpan(new ForegroundColorSpan(context.getResources().getColor(R.color.qihoo_accounts_green)),firtString.length(),firtString.length()+mMessage.length(),Spannable.SPAN_EXCLUSIVE_INCLUSIVE);     //设置指定位置文字的颜色  
        	messageText.setText(style);
        }
        Dialog errorDialog = new Dialog(context, R.style.qihoo_accounts_dialog_style);
        errorDialog.setCanceledOnTouchOutside(false);
        errorDialog.setContentView(errorView);
        errorDialog.getWindow().setLayout(getWidthOfDialog(context), ViewGroup.LayoutParams.WRAP_CONTENT);
        if (!((Activity) context).isFinishing()) {
            errorDialog.show();
            return errorDialog;
        }
        return null;
    }
    
    
    /**错误toast提示
     * 
     * @param context
     * @param operateType
     * @param errorType
     * @param errorCode
     * @param errorMessage
     */
    public static void showErrorToast(final Context context, int operateType, int errorType, int errorCode, String errorMessage) {
        initErrorDialogTitleMessage(context, operateType, errorType, errorCode, errorMessage);        
        Toast toast=Toast.makeText(context, mMessage,Toast.LENGTH_LONG);
        toast.show();
        
    }

    /**
     * 初始化出错对话框的标题和内容
     * @param context
     * @param operateType
     * @param errorType
     * @param errorCode
     * @param errorMessage
     * @param dialogView
     * @param confirmView
     */
    private static void initErrorDialogTitleMessage(Context context, int operateType, int errorType, int errorCode, String errorMessage) {
        int defaultId = R.string.qihoo_accounts_dialog_error_message_default;
        if (operateType == VALUE_DIALOG_REGISTER) {
            //手机号已被注册
        	if (errorCode == ErrorCode.ERR_CODE_REGISTER_DOWN_EXIST || errorCode == ErrorCode.ERR_CODE_REGISTER_UP_EXIST ) {
            	mTitle = context.getResources().getString(R.string.qihoo_accounts_dialog_error_reg_title).toString();
            }else if (errorCode==ErrorCode.ERR_CODE_REGISTER_EMAIL_EXIST) {//邮箱已被注册
            	mTitle = context.getResources().getString(R.string.qihoo_accounts_dialog_error_reg_title).toString();
            } 
            //邮箱激活
        } else if (operateType == VALUE_DIALOG_EMAIL_ACTIVE) {
        	mTitle = context.getResources().getString(R.string.qihoo_accounts_dialog_error_active_title).toString();
        	defaultId = R.string.qihoo_accounts_dialog_error_message_active;
        }
    	if(errorCode==ErrorCode.ERR_CODE_EAMIL_NEED_ACTIVE){
    		mTitle = context.getResources().getString(R.string.qihoo_accounts_dialog_error_login_title).toString();
    	}       
        if (errorType == ErrorCode.ERR_TYPE_USER_CENTER) {
            if (!TextUtils.isEmpty(errorMessage)) {
                mMessage = errorMessage;
            } else {
                mMessage =  "[" + errorType + ", " + errorCode + "]";
                mMessage += context.getResources().getString(defaultId);
            }
        } else if (errorType == ErrorCode.ERR_TYPE_NETWORK || errorType == ErrorCode.ERR_TYPE_APP_ERROR) {
            for (int i = 0; i < DIALOG_ERROR_MESSAGE.length; i++) {
                if (errorCode == DIALOG_ERROR_MESSAGE[i][0]) {
                    mMessage = context.getResources().getString(DIALOG_ERROR_MESSAGE[i][1]);
                    return;
                }
            }
            mMessage =  "[" + errorType + ", " + errorCode + "]";
            mMessage += context.getResources().getString(defaultId).toString();
        } else {
            mMessage =  "[" + errorType + ", " + errorCode + "]";
            mMessage += context.getResources().getString(defaultId).toString();
        }
    }

    /**
     * 获取Dialog的宽度
     * @return
     */
    private static int getWidthOfDialog(Context context) {
        if (mWidth > 0) {
            return mWidth; // 已初始化，直接返回数值
        }
        DisplayMetrics dm = context.getResources().getDisplayMetrics();
        int width = dm.widthPixels;
        int padding = (int) (20 * dm.density);
        mWidth = width - 2 * padding;
        return mWidth;
    }
    
    public static AccountCustomDialog showCustomDialog(final Context context, String title, String msg) {
    	@SuppressLint("InflateParams")
    	View doingView = ((Activity) context).getLayoutInflater().inflate(R.layout.qihoo_accounts_dialog_doing, null, false);
        View rotateView = doingView.findViewById(R.id.dialog_rotate_layout);
        rotateView.setVisibility(View.VISIBLE); // 显示带rotate的view(用于登录)
        TextView rotateText = (TextView) rotateView.findViewById(R.id.dialog_rotate_text);
        if (!TextUtils.isEmpty(msg)) {
            rotateText.setText(msg);//dialog提示信息
        }
        Animation am = AnimationUtils.loadAnimation(context, R.anim.qihoo_accounts_logining_rotate);
        LinearInterpolator lin = new LinearInterpolator();//线性插值器
        am.setInterpolator(lin);
        ImageView imageView = (ImageView) rotateView.findViewById(R.id.dialog_rotate_image);
        if (am != null) {
            imageView.startAnimation(am);//开始动画
        }
        final AccountCustomDialog doingDialog = new AccountCustomDialog(context, R.style.qihoo_accounts_dialog_style);
        doingDialog.setContentView(doingView);
        doingDialog.setCancelable(false);
        doingDialog.getWindow().setLayout(getWidthOfDialog(context), ViewGroup.LayoutParams.WRAP_CONTENT);
        return doingDialog;
    }
    
    /**
     * 在activity的onDestroy中dismiss
     * @param context
     * @param dialog
     */
    public static void closeDialogsOnDestroy(Dialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            if (dialog instanceof AccountCustomDialog) {
                ((AccountCustomDialog) dialog).removeTimeoutDetecter();
            }
            dialog.dismiss();
        }
    }

    /**
     * 在登陆、注册的回调中dismiss
     * @param context
     * @param dialog
     */
    public static void closeDialogsOnCallback(Context context, Dialog dialog) {
        if (dialog != null && !((Activity) context).isFinishing() && dialog.isShowing()) {
            if (dialog instanceof AccountCustomDialog) {
                ((AccountCustomDialog) dialog).removeTimeoutDetecter();
            }
            dialog.dismiss();
        }
    }

    /**
     * 密码：合法性判断
     * @param context
     * @param password
     * @return
     */
    public static boolean isPasswordValid(Context context, String password) {
        int errorType = InputChecker.isPasswordValid(password);
        return checkValid(context, errorType, PASSWORD_ERRORS_MAP, R.string.qihoo_accounts_valid_password_error_null);
    }

    /**
     * 登录密码：不需要检查弱密码，只检测是否>6位
     * @param context
     * @param password
     * @return
     */
    public static boolean isLoginPasswordValid(Context context, String password) {
    	//密码可以为空
        /*password = password.trim();*/
        int errorType=0;
        if (!TextUtils.isEmpty(password)) {
        	// 密码不足6位
            if (password.length() >=6) {
            	return true;
            }
            errorType = InputChecker.VALUE_PASSWORD_ERROR_LENTH_SHORT;
        }else{
        	errorType = InputChecker.VALUE_PASSWORD_ERROR_BLANKSPACE;
        }
        return checkValid(context, errorType, PASSWORD_ERRORS_MAP, R.string.qihoo_accounts_valid_password_error_blankspace);
    }

    /**
     * 登录用户名：只检测是否为空
     * @param context
     * @param userName
     * @return
     */
    public static boolean isLoginAccountValid(Context context, String userName) {
        userName = userName.trim();
        if (!TextUtils.isEmpty(userName)) {
            return true;
        }
        String errorMsg = context.getResources().getString(R.string.qihoo_accounts_valid_login_error_empty_username);
        Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
        return false;
    }

    /**
     * 邮箱：合法性判断
     * @param context
     * @param email
     * @return
     */
    public static boolean isEmailValid(Context context, String email) {
        int errorType = InputChecker.isEmailValid(email);
        return checkValid(context, errorType, EMAIL_ERRORS_MAP, R.string.qihoo_accounts_valid_email_error_null);
    }

    /**
     * 手机号：合法性判断
     * @param context
     * @param number
     * @return
     */
    public static boolean isPhoneNumberValid(Context context, String number) {
        int errorType = InputChecker.isPhoneNumberValid(number);
        return checkValid(context, errorType, PHONE_ERRORS_MAP, R.string.qihoo_accounts_valid_phone_error_null);
    }
    
    /**
     * 手机号：合法性判断,不弹出toast
     * @param context
     * @param number
     * @return
     */
    public static boolean isPhoneNumberValidNoToast(Context context, String number) {
        int errorType = InputChecker.isPhoneNumberValid(number);
        if(errorType==InputChecker.VALUE_PHONE_SUCCESS){
        	return true;
        }else {
			return false;
		}
    }


    /**
     *
     * @param context
     * @param errorType
     * @param arrys
     * @param defaultId
     * @return
     */
    private static boolean checkValid(Context context, int errorType, int arrys[][], int defaultId) {
        for (int i = 0; i < arrys.length; i++) {
            if (errorType == arrys[i][0]) {         // 匹配成功
                if (arrys[i][1] == VALUE_VALID) {   // 参数合法
                    return true;
                } else {                            // 参数不合法
                    Toast.makeText(context, context.getResources().getString(arrys[i][1]), Toast.LENGTH_LONG).show();
                    return false;
                }
            }
        }
        // 匹配不成功，未知错误，使用默认值
        Toast.makeText(context, context.getResources().getString(defaultId), Toast.LENGTH_LONG).show();
        return false;
    }

    /**
     * 图片验证码合法性判断，只检测是否为空
     * @param context
     * @param captcha
     * @return
     */
    public static boolean isCaptchaValid(Context context, String captcha) {
        captcha = captcha.trim();
        if (!TextUtils.isEmpty(captcha)) {
            return true;
        }
        String captchaNull = context.getResources().getString(R.string.qihoo_accounts_image_captcha_null);
        Toast.makeText(context, captchaNull, Toast.LENGTH_LONG).show();
        return false;
    }
    
    /**
     * 下行短信验证码：合法性判断，只检测是否为空
     * @param context
     * @param captcha
     * @return
     */
    public static boolean isSmsCodeValid(Context context, String captcha) {
        captcha = captcha.trim();
        if (!TextUtils.isEmpty(captcha)) {
            return true;
        }
        String captchaNull = context.getResources().getString(R.string.qihoo_accounts_sms_cdoe_null);
        Toast.makeText(context, captchaNull, Toast.LENGTH_LONG).show();
        return false;
    }

    /**
     * 隐藏EditText的软键盘
     * @param context
     * @param view
     */
    public static void hideSoftInput(Context context, View view) {
        if (context == null || view == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    /**
     * 显示EditText的软键盘
     * @param context
     * @param view
     */
    public static void displaySoftInput(Context context, View view) {
        if (context == null || view == null) {
            return;
        }
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.showSoftInput(view, 0);
    }

    /**
     * 打开指定uri，如果360浏览器存在，默认用它打开
     * @param context
     * @param strUri
     */
    public static void openBrowser(Context context, String strUri) {
        Uri uri = Uri.parse(strUri);
        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        boolean hasBrowser=PmUtils.updateIntentPriorityPackage(context, intent, true, true, "com.qihoo.browser");
        if(!hasBrowser){
        	String errorMsg = context.getResources().getString(R.string.qihoo_accounts_valid_email_error_no_browser);
            Toast.makeText(context, errorMsg, Toast.LENGTH_LONG).show();
            return;
        }
        context.startActivity(intent);     
    }

    /**
     * 添加帐号
     * @param container
     * @param context
     * @param info
     */
    public static void addAccount(IContainer container, Context context, UserTokenInfo info) {
        /*
         * 登陆页面的自动完成文本框的下拉列表，需显示已经注册、登陆成功的帐号的用户名
         * 所以需要在本地保存一份用户名信息，且只需保存用户名
         * account.qid = 用户名；account的其他字段随意赋值或设为空（如果可设为空）
         */
        QihooAccount account = new QihooAccount("noused", info.u, "noused", "noused", false, null);
        container.getUiAccounts().addAccount(context, account);
    }
    
    /**
     * 打开email官网
     * @param context
     * @param emailUrl
     */
    public static void openEmailUrl(Context context,String emailUrl) {
        openBrowser(context, emailUrl);
    }
    
    /**
     * 保存激活邮箱的官网地址
     * 为兼容接口修改
     * @param context
     * @param emailUrl
     */
    public static void setEmailUrl(Context context,String emailUrlTmp){
    	emailUrl=emailUrlTmp;
    }
    
    /**
     * 获取邮箱官网地址
     * 为兼容接口修改
     * @param context
     * @return
     */
    public static String getEmailUrl(Context context){
    	return emailUrl;
    }

    /**
     * 保存登录邮箱/手机号名称
     * 为兼容接口修改
     * @param context
     * @param emailUrl
     */
    public static void setEmailName(Context context,String name){
    	emailName=name;
    }
    
    /**
     * 获取登录邮箱/手机号
     * 为兼容接口修改
     * @param context
     * @return
     */
    public static String getEmailName(Context context){
    	return emailName;
    }

    /**
     * 保存登录邮箱密码
     * 为兼容接口修改
     * @param context
     * @param emailUrl
     */
    public static void setEmailPwd(Context context,String pwd){
    	emailPasswd=pwd;
    }
    
    /**
     * 获取登录邮箱密码
     * 为兼容接口修改
     * @param context
     * @return
     */
    public static String getEmailPwd(Context context){
    	return emailPasswd;
    }
    /**
     * 设置某view获得焦点
     * @param view
     */
    public static void setViewFocus(View view) {
        if (view != null) {
            view.setFocusable(true);
            view.setFocusableInTouchMode(true);
            view.requestFocus();
            view.requestFocusFromTouch();
        }
    }

    public static boolean isSingleSimCardExist(Context context){
    	MultiSimUtil.init(context);
        int cardCount = MultiSimUtil.getSimCardExistCount(context);
        return cardCount == 1;
    }
    
    /**
     * 自动获取短信验证码内容
     * @param mContext
     * @param mCapEditText
     */
    public static void getSmsContent(Context mContext,EditText mCapEditText){
    	// 自动填充短信验证码
		SmsContentObserver smsContent=new SmsContentObserver(mContext,mCapEditText);
		// 注册短信变化监听
		mContext.getContentResolver().registerContentObserver(
				Uri.parse(SMS_URI), true, smsContent);
    }
    
    /**
     * 120后重新获验证码
     * @param mContext
     * @param mCaptchaClickBtn
     */
    public static void startCodeTimer(final Context mContext,final Button mCaptchaClickBtn) {
    	mCaptchaClickBtn.setClickable(false);
    	mCaptchaClickBtn.setEnabled(false);
    	if(codeTimer != null){
			codeTimer.cancel();
		}
    	codeTimer=new CountDownTimer(120000, 1000) {
			public void onTick(long millisUntilFinished) {
				mCaptchaClickBtn.setText(mContext.getResources().getString(R.string.qihoo_accounts_register_down_sms_captcha_send_time_first) + millisUntilFinished / 1000 + mContext.getResources().getString(R.string.qihoo_accounts_register_down_sms_captcha_send_time_last));
			}

			public void onFinish() {
				mCaptchaClickBtn.setClickable(true);
				mCaptchaClickBtn.setEnabled(true);
				mCaptchaClickBtn.setText(mContext.getResources().getString(R.string.qihoo_accounts_register_down_sms_captcha_send_click));
			}
		}.start();
	}
    
    /**
     * 打开找回密码web view
     * @param context
     * @param account 默认帐号，可以为空
     */
    public static void toFindPwdWebView(Context context, String account){
    	Intent intent = new Intent(context, WebViewActivity.class);
    	intent.putExtra(WebViewActivity.KEY_TITILE, context.getResources().getString(R.string.qihoo_accounts_webview_findpwd));
    	String url = AddAccountsUtils.FIND_PWD_URL
    				 +"&skin="+context.getResources().getString(R.string.qihoo_accounts_webview_findpwd_skin)
    				 +"&account="+account;
    	intent.putExtra(WebViewActivity.KEY_URL, url);
    	context.startActivity(intent);
    }
    
    /**
     * 打开y用户协议web view
     * @param context
     */
    public static void toLinsenceWebView(Context context){
    	Intent intent = new Intent(context, WebViewActivity.class);
    	intent.putExtra(WebViewActivity.KEY_TITILE, context.getResources().getString(R.string.qihoo_accounts_webview_lisence));
    	intent.putExtra(WebViewActivity.KEY_URL, AddAccountsUtils.LISENCE_URL);
    	context.startActivity(intent);
    }
    
    /**
     * 打开修改密码 web view
     * @param activity
     * @param requestCode 用于startActivityForResult, 操作成功后会通知结果并返回新的QT
     * @param cookieQ 用于同步登陆状态到wap页
     * @param cookieT 用于同步登陆状态到wap页
     * @param qid 当前QT对应的用户qid, 用于确保返回的新QT是当前用户的，避免串号
     */
    public static void toChangePwdWebView(Activity activity, int requestCode, String cookieQ, String cookieT, String qid){
    	Intent intent = new Intent(activity, WebViewActivity.class);
    	intent.putExtra(WebViewActivity.KEY_TITILE, activity.getResources().getString(R.string.qihoo_accounts_webview_chpwd));
    	intent.putExtra(WebViewActivity.KEY_URL, AddAccountsUtils.CH_PWD_URL+"&skin="+activity.getResources().getString(R.string.qihoo_accounts_webview_findpwd_skin));
        intent.putExtra(WebViewActivity.KEY_COOKIE_Q, cookieQ);
        intent.putExtra(WebViewActivity.KEY_COOKIE_T, cookieT);
        intent.putExtra(WebViewActivity.KEY_QID, qid);
        activity.startActivityForResult(intent, requestCode);
    }
    
    
    /**
     * 打开绑定手机web view
     * @param activity
     * @param requestCode 用于startActivityForResult, 操作成功后会通知结果并返回新的QT
     * @param cookieQ 用于同步登陆状态到wap页
     * @param cookieT 用于同步登陆状态到wap页
     * @param qid 当前QT对应的用户qid, 用于确保返回的新QT是当前用户的，避免串号
     */
    public static void toBindMobileWebView(Activity activity, int requestCode, String cookieQ, String cookieT, String qid){
    	Intent intent = new Intent(activity, WebViewActivity.class);
    	intent.putExtra(WebViewActivity.KEY_TITILE, activity.getResources().getString(R.string.qihoo_accounts_webview_bindmobile));
    	intent.putExtra(WebViewActivity.KEY_URL, AddAccountsUtils.BIND_MOBILE_URL+"&skin="+activity.getResources().getString(R.string.qihoo_accounts_webview_findpwd_skin));
        intent.putExtra(WebViewActivity.KEY_COOKIE_Q, cookieQ);
        intent.putExtra(WebViewActivity.KEY_COOKIE_T, cookieT);
        intent.putExtra(WebViewActivity.KEY_QID, qid);
        activity.startActivityForResult(intent, requestCode);
    }
    
    
}
