
package com.qihoo360.accounts.ui.v;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.qihoo360.accounts.R;
import com.qihoo360.accounts.api.auth.DownSmsRegister;
import com.qihoo360.accounts.api.auth.i.IDownSmsRegListener;
import com.qihoo360.accounts.api.auth.model.UserTokenInfo;
import com.qihoo360.accounts.base.common.ErrorCode;
import com.qihoo360.accounts.base.utils.InputChecker;
import com.qihoo360.accounts.ui.model.AddAccountsUtils;

/**
 * 下行短信注册页面
 * @author wangzefeng
 *
 */
public class RegisterDownSmsView extends LinearLayout implements View.OnClickListener,OnCheckedChangeListener {

//    private static final String TAG = "ACCOUNT.RegisterDownSmsView";

    private Context mContext;

    private IContainer mContainer;

    private DownSmsRegister mRegister;

    // 手机号编辑框
    private EditText mPhoneText;

    private Button mDeletePhoneBtn;

    // 密码编辑框
    private EditText mPswText;

    private Button mDeletePswBtn;

    private Button mShowPswBtn;

    private static Boolean mIsShowPsw = false;

    // 自动阅读用户注册协议
    private CheckBox mAutoRead;

    private boolean mIsAutoRead = true;

    // 正在注册对话框
    private AccountCustomDialog mRegistingDialog;

    // 注册失败对话框
    private Dialog mRegErrorDialog;

    private final AccountCustomDialog.ITimeoutListener mDialogTimeoutListener = new AccountCustomDialog.ITimeoutListener() {

        @Override
        public void onTimeout(Dialog dialog) {
        	dialog.dismiss();
            mRegPending = false;
        }
    };
    
    //密码输入后回车键
  	private final OnKeyListener onRegisterKey = new OnKeyListener() {
  		
  		public boolean onKey(View v, int keyCode, KeyEvent event) {

  			// TODO Auto-generated method stub
  			if (keyCode == KeyEvent.KEYCODE_ENTER) {
  				AddAccountsUtils.hideSoftInput(mContext, mPswText);// 隐藏键盘
  				mPswText.setSelection(mPswText.getText().toString().length());// EditText设置光标在内容的最尾端
  				doCommandReg();
  				return true;
  			}
  			return false;
  		}
  	};

    public RegisterDownSmsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public final void setContainer(IContainer container) {
        mContainer = container;
        // 如果不是手机号码，则不初始化
        if (InputChecker.isPhoneNumberValid(mContainer.getInitUser()) != InputChecker.VALUE_PHONE_SUCCESS) {
            return;
        }
        mPhoneText.setText(mContainer.getInitUser());
    }

    public DownSmsRegister getDownSmsRegister() {
        return mRegister;
    }

    public String getPhone() {
        return mPhoneText.getText().toString();
    }

    public String getPsw() {
        return mPswText.getText().toString();
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initView();
        checkDownSmsPhone();
        checkDownSmsPassword();
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if(viewId==R.id.register_email_button){
        	 mContainer.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_EMAIL_VIEW);
        }else if (viewId == R.id.register_down_sms_reg) {
            doCommandReg();
        } else if (viewId == R.id.register_down_sms_delete_tel) {
            mPhoneText.setText(null);
            AddAccountsUtils.setViewFocus(mPhoneText);
            AddAccountsUtils.displaySoftInput(mContext, mPhoneText);
        } else if (viewId == R.id.register_down_sms_delete_password) {
            mPswText.setText(null);
            AddAccountsUtils.setViewFocus(mPswText);
            AddAccountsUtils.displaySoftInput(mContext, mPswText);
        } else if (viewId == R.id.register_down_sms_show_password) {
            mIsShowPsw = !mIsShowPsw;
            OnPwdChange();
            // 修改 EditText 中的光标位置，把光标移动到文本后面
            mPswText.setSelection(mPswText.getText().toString().length());
        } else if (viewId == R.id.register_down_sms_license) {
        	AddAccountsUtils.toLinsenceWebView(mContext);
        } else if(viewId==R.id.add_accounts_dialog_error_title_icon){
        	closeErrorDialog();//关闭对话框
        }else if (viewId == R.id.add_accounts_dialog_error_cancel_btn) {
            closeErrorDialog();//取消登录
        }else if(viewId==R.id.add_accounts_dialog_error_ok_btn){
        	closeErrorDialog();//立即登录逻辑
        	mContainer.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_LOGIN_VIEW);//登录界面
        	//自动填充用户名和密码
        	((LoginView)mContainer.getLoginView()).setAccount(mPhoneText.getText().toString().trim());
        	((LoginView)mContainer.getLoginView()).setPsw(mPswText.getText().toString());
        	((LoginView)mContainer.getLoginView()).doCommandLogin();
        }
    }

    private void initView() {
    	mContext = getContext();
        mPhoneText = (EditText) findViewById(R.id.register_down_sms_tel_text);
        mPswText = (EditText) findViewById(R.id.register_down_sms_password_text);
        mPswText.setOnKeyListener(onRegisterKey);
        findViewById(R.id.register_down_sms_reg).setOnClickListener(this);
        findViewById(R.id.register_email_button).setOnClickListener(this);
        findViewById(R.id.register_down_sms_license).setOnClickListener(this);
        mDeletePhoneBtn = (Button) findViewById(R.id.register_down_sms_delete_tel);
        mDeletePhoneBtn.setOnClickListener(this);
        mShowPswBtn = (Button) findViewById(R.id.register_down_sms_show_password);
        mShowPswBtn.setOnClickListener(this);
        mDeletePswBtn = (Button) findViewById(R.id.register_down_sms_delete_password);
        mDeletePswBtn.setOnClickListener(this);
        mAutoRead = (CheckBox) findViewById(R.id.register_down_sms_auto_read_lisence);
        mAutoRead.setOnCheckedChangeListener(this);

        OnPwdChange();
        
        RelativeLayout phoneLayout = (RelativeLayout) findViewById(R.id.qihoo_accounts_reg_down_sms_phone_layout);
        RelativeLayout pswLayout = (RelativeLayout) findViewById(R.id.qihoo_accounts_reg_down_sms_psw_layout);
        phoneLayout.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                AddAccountsUtils.setViewFocus(mPhoneText);
                AddAccountsUtils.displaySoftInput(mContext, mPhoneText);
                return false;
            }
        });
        pswLayout.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                AddAccountsUtils.setViewFocus(mPswText);
                AddAccountsUtils.displaySoftInput(mContext, mPswText);
                return false;
            }
        });
    }

    @Override
	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
    	if(buttonView.getId() == R.id.register_down_sms_auto_read_lisence) {
    		mIsAutoRead = isChecked;
		}
	}
    
    //显示\隐藏密码
  	private void OnPwdChange(){
  		if (mIsShowPsw) {
  			mPswText.setTransformationMethod(HideReturnsTransformationMethod
  					.getInstance());
  			mShowPswBtn.setText(R.string.qihoo_accounts_hide_password);
  		} else {
  			mPswText.setTransformationMethod(PasswordTransformationMethod
  					.getInstance());
  			mShowPswBtn.setText(R.string.qihoo_accounts_show_password);
  		}
  	}
    
    private void checkDownSmsPassword() {

        mPswText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String strPassword = mPswText.getText().toString();
                if (strPassword.length() > 0) {
                    mDeletePswBtn.setVisibility(View.VISIBLE);
                } else {
                    mDeletePswBtn.setVisibility(View.GONE);
                }
            }
        });

    }

    private void checkDownSmsPhone() {
        mPhoneText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String strTel = mPhoneText.getText().toString();
                if (strTel.length() > 0) {
                    mDeletePhoneBtn.setVisibility(View.VISIBLE);
                } else {
                    mDeletePhoneBtn.setVisibility(View.GONE);
                }
            }
        });
    }

    private boolean mRegPending;

    private final IDownSmsRegListener mRegListener = new IDownSmsRegListener() {

        @Override
        public void onRegSuccess(UserTokenInfo info) {
        	//在自定义UI时或手机桌面需求，在登录、注册成功后可能会有些耗时操作，此时先不关闭Dialog,
			//而是在activity的destroy时关闭正在登录对话和正在注册对话框
            mRegPending = false;
            handleRegSuccess(info);
        }

        @Override
        public void onRegError(int errorType, int errorCode, String errorMessage) {
            mRegPending = false;
            closeRegDialog();
            handleRegError(errorType, errorCode, errorMessage);
        }

        //短信验证码提交后回调函数
        @Override
        public void onRegNeedCaptcha() {
            mRegPending = false;
            closeRegDialog();
            // doCommandDownCaptcha();
        }
        
        //短信验证码提交后回调函数
        @Override
        public void onRegWrongCaptcha(int errorType, int errorCode, String errorMessage)  {
            mRegPending = false;
            closeRegDialog();
            // doCommandDownCaptcha();
        }

        @Override
        public void onRegWaitSmsTimtout() {
            mRegPending = false;
            closeRegDialog();
            handleRegWaitSms();
        }

        @Override
        public void onSMSRequestSuccess(boolean requestSmsOnly) {
            // TODO Auto-generated method stub
        	mRegPending = false;
        	closeRegDialog();
        	handleRegWaitSms();//收到短信后显示验证码界面
        }
    };

    private final void doCommandReg() {
    	if(!mIsAutoRead){
    		AddAccountsUtils.showErrorToast(mContext, AddAccountsUtils.VALUE_DIALOG_REGISTER, ErrorCode.ERR_TYPE_APP_ERROR, ErrorCode.ERR_CODE_READ_LISENCE, "");
    		return;
    	}
        AddAccountsUtils.hideSoftInput(mContext, mPhoneText);
        AddAccountsUtils.hideSoftInput(mContext, mPswText);
        if (mRegPending) {
            return;
        }
        String phoneNumber = mPhoneText.getText().toString();
        String password = mPswText.getText().toString();
        if (!AddAccountsUtils.isPhoneNumberValid(mContext, phoneNumber)) {
            return;
        }
        if (!AddAccountsUtils.isPasswordValid(mContext, password)) {
            return;
        }
        mRegPending = true;
        mRegistingDialog = AddAccountsUtils.showDoingDialog(mContext, AddAccountsUtils.VALUE_DIALOG_REGISTER);
        mRegistingDialog.setTimeoutListener(mDialogTimeoutListener);
        mRegister = new DownSmsRegister(mContext.getApplicationContext(), mContainer.getClientAuthKey(), mRegListener);
        mRegister.setRegisterListener(mRegListener);
        mRegister.register(phoneNumber, password,true);//向服务器请求发送短信验证码
    }

    private final void handleRegSuccess(UserTokenInfo info) {
        //TODO 确实是否弹出注册成功Dialog
        //AddAccountsBase.showRegisterSuccessDialog(mContext);
        AddAccountsUtils.addAccount(mContainer, mContext, info);
        mContainer.registerListener().onRegisterSuccess(info);
    }

    private final void handleRegError(int errorType, int errorCode, String errorMessage) {
    	if(errorCode == AddAccountsUtils.VALUE_REGISTER_DOWN_EXIST){
    		errorCode=ErrorCode.ERR_CODE_REGISTER_DOWN_EXIST;
    		errorMessage=mPhoneText.getText().toString();
    		mRegErrorDialog = AddAccountsUtils.showErrorDialog(mContext, this, AddAccountsUtils.VALUE_DIALOG_REGISTER, errorType, errorCode, errorMessage);	
    	}else{
    		AddAccountsUtils.showErrorToast(mContext, AddAccountsUtils.VALUE_DIALOG_REGISTER, errorType, errorCode, errorMessage);
    	}
        
        mContainer.registerListener().onRegisterError(errorType, errorCode, errorMessage);
    }

    private final void handleRegWaitSms() {
        View captchaView = mContainer.getRegDownSmsCaptchaView();
        ((TextView) captchaView.findViewById(R.id.register_down_sms_captcha_phone)).setText(mPhoneText.getText());
        EditText mCaptchaText=(EditText)captchaView.findViewById(R.id.register_down_sms_captcha_text);
        Button mCaptchaClickBtn=(Button)captchaView.findViewById(R.id.register_down_sms_captcha_send_click);
        AddAccountsUtils.getSmsContent(mContext, mCaptchaText);
        AddAccountsUtils.startCodeTimer(mContext, mCaptchaClickBtn);
        mContainer.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_DOWN_SMS_CAPTCHA_VIEW);
    }

    public final void closeDialogsOnDestroy() {
        AddAccountsUtils.closeDialogsOnDestroy(mRegistingDialog);
        AddAccountsUtils.closeDialogsOnDestroy(mRegErrorDialog);
    }

    public final void closeRegDialog() {
        AddAccountsUtils.closeDialogsOnCallback(mContext, mRegistingDialog);
    }

    private final void closeErrorDialog() {
        AddAccountsUtils.closeDialogsOnCallback(mContext, mRegErrorDialog);
    }
}
