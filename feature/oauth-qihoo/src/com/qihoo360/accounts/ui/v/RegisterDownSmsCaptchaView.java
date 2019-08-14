
package com.qihoo360.accounts.ui.v;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.qihoo360.accounts.R;
import com.qihoo360.accounts.api.auth.DownSmsRegister;
import com.qihoo360.accounts.api.auth.i.IDownSmsRegListener;
import com.qihoo360.accounts.api.auth.model.UserTokenInfo;
import com.qihoo360.accounts.ui.model.AddAccountsUtils;

/**下行短信提交验证码界面
 * 
 * @author wangzefeng
 *
 */
public class RegisterDownSmsCaptchaView extends LinearLayout implements View.OnClickListener {

//    private static final String TAG = "ACCOUNT.RegisterDownSmsView";
	
    private Context mContext;

    private IContainer mContainer;

    // 验证码编辑框
    private EditText mCaptchaText;

    private Button mCaptchaDeleteBtn;
    
    //获取验证码
    private Button mCaptchaClickBtn;

    // 正在提交验证码对话框
    private AccountCustomDialog mCommitingDialog;

    // 正在重新发送对话框
    private AccountCustomDialog mSendAgainDialog;

    /**
	 * 在自定义UI时或手机桌面需求，在登录、注册成功后可能会有些耗时操作，此时先不关闭Dialog,
	 * 而是在activity的destroy时关闭正在登录对话和正在注册对话框
	 */
    private final AccountCustomDialog.ITimeoutListener mCommitingDialogTimeoutListener = new AccountCustomDialog.ITimeoutListener() {

        @Override
        public void onTimeout(Dialog dialog) {
            mCommitCaptchaPending = false;
        }
    };

    private final AccountCustomDialog.ITimeoutListener mSendAgainDialogTimeoutListener = new AccountCustomDialog.ITimeoutListener() {

        @Override
        public void onTimeout(Dialog dialog) {
        	dialog.dismiss();
            mSendAgainPending = false;
        }
    };

    //短信验证码输入后回车键
  	private final OnKeyListener onSendSmsCodeKey = new OnKeyListener() {
  		
  		public boolean onKey(View v, int keyCode, KeyEvent event) {

  			// TODO Auto-generated method stub
  			if (keyCode == KeyEvent.KEYCODE_ENTER) {
  				AddAccountsUtils.hideSoftInput(mContext, mCaptchaText);// 隐藏键盘
  				mCaptchaText.setSelection(mCaptchaText.getText().toString().length());// EditText设置光标在内容的最尾端
  				doCommandCommitCaptcha();
  				return true;
  			}
  			return false;
  		}
  	};
    
    public RegisterDownSmsCaptchaView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public final void setContainer(IContainer container) {
        mContainer = container;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initView();
        checkCaptcha();
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.register_down_sms_captcha_delete) {
            mCaptchaText.setText(null);
            AddAccountsUtils.setViewFocus(mCaptchaText);
            AddAccountsUtils.displaySoftInput(mContext, mCaptchaText);
        } else if (viewId == R.id.register_down_sms_captcha_commit) {
            doCommandCommitCaptcha();
        } else if (viewId == R.id.register_down_sms_captcha_send_click) {
        	doCommandSendAgain();
        } 
    }

    private void initView() {
    	mContext = getContext();
        mCaptchaText = (EditText) findViewById(R.id.register_down_sms_captcha_text);
        mCaptchaText.setOnKeyListener(onSendSmsCodeKey);
        mCaptchaDeleteBtn = (Button) findViewById(R.id.register_down_sms_captcha_delete);
        mCaptchaClickBtn=(Button) findViewById(R.id.register_down_sms_captcha_send_click);
        mCaptchaDeleteBtn.setOnClickListener(this);
        findViewById(R.id.register_down_sms_captcha_commit).setOnClickListener(this);
        findViewById(R.id.register_down_sms_captcha_send_click).setOnClickListener(this);
        RelativeLayout captchaLayout = (RelativeLayout) findViewById(R.id.qihoo_accounts_reg_down_sms_captcha_layout);
        captchaLayout.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                AddAccountsUtils.setViewFocus(mCaptchaText);
                AddAccountsUtils.displaySoftInput(mContext, mCaptchaText);
                return false;
            }
        });
    }
    
    private void checkCaptcha() {
        mCaptchaText.addTextChangedListener(new TextWatcher() {

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String strCaptcha = mCaptchaText.getText().toString();
                if (strCaptcha.length() > 0) {
                    mCaptchaDeleteBtn.setVisibility(View.VISIBLE);
                } else {
                    mCaptchaDeleteBtn.setVisibility(View.GONE);
                }
            }
        });
    }

    private boolean mCommitCaptchaPending;

    private final IDownSmsRegListener mCommitCaptchaListener = new IDownSmsRegListener() {

        @Override
        public void onRegSuccess(UserTokenInfo info) {
        	//在自定义UI时或手机桌面需求，在登录、注册成功后可能会有些耗时操作，此时先不关闭Dialog,
			//而是在activity的destroy时关闭正在登录对话和正在注册对话框
            mCommitCaptchaPending = false;
            handleRegSuccess(info);
        }

        @Override
        public void onRegError(int errorType, int errorCode, String errorMessage) {
            mCommitCaptchaPending = false;
            closeCommitDialog();
            handleCommitError(errorType, errorCode, errorMessage);
        }

        @Override
        public void onRegNeedCaptcha() {
            mCommitCaptchaPending = false;
            closeCommitDialog();
        }
        
        @Override
        public void onRegWrongCaptcha(int errorType, int errorCode, String errorMessage) {
            mCommitCaptchaPending = false;
            closeCommitDialog();
        }

        @Override
        public void onRegWaitSmsTimtout() {
            mCommitCaptchaPending = false;
            closeCommitDialog();
        }

        //使用验证码注册,不需要该回调函数
        @Override
        public void onSMSRequestSuccess(boolean requestSmsOnly) {
            // TODO Auto-generated method stub

        }
    };

    DownSmsRegister mRegister;

    private void doCommandCommitCaptcha() {
        AddAccountsUtils.hideSoftInput(mContext, mCaptchaText);
        if (mCommitCaptchaPending) {
            return;
        }
        String captcha = mCaptchaText.getText().toString();
        if (!AddAccountsUtils.isSmsCodeValid(mContext, captcha)) {
            return;
        }
        mCommitCaptchaPending = true;
        mCommitingDialog = AddAccountsUtils.showDoingDialog(mContext, AddAccountsUtils.VALUE_DIALOG_COMMIT);
        mCommitingDialog.setTimeoutListener(mCommitingDialogTimeoutListener);
        DownSmsRegister register = ((RegisterDownSmsView) mContainer.getRegDownSmsView()).getDownSmsRegister();
        if (register != null) {
            register.setRegisterListener(mCommitCaptchaListener);
            register.register(captcha);
        }
    }

    private boolean mSendAgainPending;

    private final IDownSmsRegListener mSendAgainListener = new IDownSmsRegListener() {

        @Override
        public void onRegSuccess(UserTokenInfo info) {
        	//在自定义UI时或手机桌面需求，在登录、注册成功后可能会有些耗时操作，此时先不关闭Dialog,
			//而是在activity的destroy时关闭正在登录对话和正在注册对话框
            mSendAgainPending = false;
            handleRegSuccess(info);
        }

        @Override
        public void onRegError(int errorType, int errorCode, String errorMessage) {
            mSendAgainPending = false;
            closeSendDialog();
            handleSendAgainError(errorType, errorCode, errorMessage);
        }

        @Override
        public void onRegNeedCaptcha() {
            mSendAgainPending = false;
            closeSendDialog();
        }
        
        @Override
        public void  onRegWrongCaptcha(int errorType, int errorCode, String errorMessage)  {
        	mSendAgainPending = false;
            closeSendDialog();
        }

        @Override
        public void onRegWaitSmsTimtout() {
            mSendAgainPending = false;
            closeSendDialog();
        }

        @Override
        public void onSMSRequestSuccess(boolean requestSmsOnly) {
            // TODO Auto-generated method stub
        	mSendAgainPending= false;
        	closeSendDialog();
        	AddAccountsUtils.getSmsContent(mContext, mCaptchaText);//自动填充短信验证码
        	AddAccountsUtils.startCodeTimer(mContext, mCaptchaClickBtn);//启动120s倒计时
        }
    };


    //请求服务器再次发送短信验证码
    private void doCommandSendAgain() {
        AddAccountsUtils.hideSoftInput(mContext, mCaptchaText);
        if (mSendAgainPending) {
            return;
        }
        mSendAgainPending = true;

        mSendAgainDialog = AddAccountsUtils.showDoingDialog(mContext, AddAccountsUtils.VALUE_DIALOG_SEND_AGIAN);
        mSendAgainDialog.setTimeoutListener(mSendAgainDialogTimeoutListener);
        
        DownSmsRegister register = ((RegisterDownSmsView) mContainer.getRegDownSmsView()).getDownSmsRegister();
        String phone = ((RegisterDownSmsView) mContainer.getRegDownSmsView()).getPhone();
        String psw = ((RegisterDownSmsView) mContainer.getRegDownSmsView()).getPsw();
        if (register != null && !TextUtils.isEmpty(phone) && !TextUtils.isEmpty(psw)) {
            register.setRegisterListener(mSendAgainListener);
            register.register(phone, psw, true);
        }
    }

    private final void handleRegSuccess(UserTokenInfo info) {
        //TODO 确实是否弹出注册成功Dialog
        AddAccountsUtils.addAccount(mContainer, mContext, info);
        mContainer.registerListener().onRegisterSuccess(info);
    }

    private final void handleCommitError(int errorType, int errorCode, String errorMessage) {
        AddAccountsUtils.showErrorToast(mContext, AddAccountsUtils.VALUE_DIALOG_COMMIT, errorType, errorCode, errorMessage);
    }

    private final void handleSendAgainError(int errorType, int errorCode, String errorMessage) {
    	AddAccountsUtils.showErrorToast(mContext, AddAccountsUtils.VALUE_DIALOG_SEND_AGIAN, errorType, errorCode, errorMessage);	
    }

    public final void closeDialogsOnDestroy() {
        AddAccountsUtils.closeDialogsOnDestroy(mCommitingDialog);
        AddAccountsUtils.closeDialogsOnDestroy(mSendAgainDialog);
    }

    public final void closeCommitDialog() {
        AddAccountsUtils.closeDialogsOnCallback(mContext, mCommitingDialog);
    }

    private final void closeSendDialog() {
        AddAccountsUtils.closeDialogsOnCallback(mContext, mSendAgainDialog);
    }

   
}
