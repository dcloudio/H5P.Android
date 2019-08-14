
package com.qihoo360.accounts.ui.v;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.text.style.ForegroundColorSpan;
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
import com.qihoo360.accounts.api.auth.UpSmsRegister;
import com.qihoo360.accounts.api.auth.i.IUpSmsRegListener;
import com.qihoo360.accounts.api.auth.model.UserTokenInfo;
import com.qihoo360.accounts.base.common.ErrorCode;
import com.qihoo360.accounts.ui.model.AddAccountsUtils;

/**
 * 上行短信注册界面
 * @author wangzefeng
 *
 */
public class RegisterUpSmsView extends LinearLayout implements View.OnClickListener,OnCheckedChangeListener {

//    private static final String TAG = "ACCOUNT.RegisterUpSmsView";

    private Context mContext;

    private IContainer mContainer;

    // 密码编辑框
    private EditText mPswText;

    private Button mDeletePswBtn;

    private Button mShowPswBtn;

    private static Boolean mIsShowPsw = false;

    // 自动阅读用户注册协议
    private CheckBox mAutoRead;

    private boolean mIsAutoRead = true;
    
    // 短信验证注册（即，下行注册）
    private TextView mDownSmsRegText;
    
    private TextView mPhoneTips;

    // 正在注册对话框
    private AccountCustomDialog mRegistingDialog;
 
    // 注册失败对话框
    private Dialog mRegErrorDialog;
    
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

  	/**
	 * 在自定义UI时或手机桌面需求，在登录、注册成功后可能会有些耗时操作，此时先不关闭Dialog,
	 * 而是在activity的destroy时关闭正在登录对话和正在注册对话框
	 */
    private final AccountCustomDialog.ITimeoutListener mDialogTimeoutListener = new AccountCustomDialog.ITimeoutListener() {

        @Override
        public void onTimeout(Dialog dialog) {
            mUpSmsRegisterPending = false;
        }
    };

    public RegisterUpSmsView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public final void setContainer(IContainer container) {
        mContainer = container;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initView();
        checkPassword();
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.register_up_sms_click) {
            doCommandReg();
        } else if (viewId == R.id.register_up_sms_delete_password) {
            mPswText.setText(null);
            AddAccountsUtils.setViewFocus(mPswText);
            AddAccountsUtils.displaySoftInput(mContext, mPswText);
        } else if (viewId == R.id.register_up_sms_show_password) {
            mIsShowPsw = !mIsShowPsw;
            OnPwdChange();
            // 修改 EditText 中的光标位置，把光标移动到文本后面
            mPswText.setSelection(mPswText.getText().toString().length());
        }else if (viewId == R.id.register_up_sms_license) {
        	AddAccountsUtils.toLinsenceWebView(mContext);
        }else if(viewId==R.id.register_up_sms_free_register){
        	mContainer.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_DOWN_SMS_VIEW);
        }else if(viewId==R.id.add_accounts_dialog_error_title_icon){
        	closeErrorDialog();//关闭对话框
        }else if (viewId == R.id.add_accounts_dialog_error_cancel_btn) {
            closeErrorDialog();//取消登录
        }else if(viewId==R.id.add_accounts_dialog_error_ok_btn){
        	closeErrorDialog();//立即登录逻辑
        	mContainer.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_LOGIN_VIEW);//登录界面
        	//自动填充用户名和密码
        	((LoginView)mContainer.getLoginView()).setAccount(AddAccountsUtils.getEmailName(mContext));
        	((LoginView)mContainer.getLoginView()).setPsw(mPswText.getText().toString());
        	((LoginView)mContainer.getLoginView()).doCommandLogin();
        }
    }
    
    private void initView() {
    	mContext = getContext();
    	mPhoneTips=(TextView)findViewById(R.id.register_password_tip);
    	String firtString=getResources().getString(R.string.qihoo_accounts_register_up_sms_tips_first);
    	String middleString=getResources().getString(R.string.qihoo_accounts_register_up_sms_tips);
        String lastString=getResources().getString(R.string.qihoo_accounts_register_up_sms_tips_last);
    	String mTipString=firtString+middleString+lastString;
    	SpannableStringBuilder style=new SpannableStringBuilder(mTipString);
    	style.setSpan(new ForegroundColorSpan(getResources().getColor(R.color.qihoo_accounts_green)),firtString.length(),firtString.length()+middleString.length(),Spannable.SPAN_EXCLUSIVE_INCLUSIVE);//设置指定位置文字的颜色  
    	mPhoneTips.setText(style);  
        mPswText = (EditText) findViewById(R.id.register_up_sms_password_text);
        mPswText.setOnKeyListener(onRegisterKey);
        findViewById(R.id.register_up_sms_click).setOnClickListener(this);
        mShowPswBtn = (Button) findViewById(R.id.register_up_sms_show_password);
        mShowPswBtn.setOnClickListener(this);
        mDeletePswBtn = (Button) findViewById(R.id.register_up_sms_delete_password);
        mDeletePswBtn.setOnClickListener(this);
        findViewById(R.id.register_up_sms_license).setOnClickListener(this);
        mDownSmsRegText = (TextView) findViewById(R.id.register_up_sms_free_register);
        mDownSmsRegText.setOnClickListener(this);
        
        mAutoRead = (CheckBox) findViewById(R.id.register_up_sms_auto_read_lisence);
        mAutoRead.setOnCheckedChangeListener(this);
        
        OnPwdChange();
        RelativeLayout pswLayout = (RelativeLayout) findViewById(R.id.qihoo_accounts_reg_up_sms_psw_layout);
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
       	if(buttonView.getId() == R.id.register_up_sms_auto_read_lisence) {
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
  	
    private void checkPassword() {

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

    private boolean mUpSmsRegisterPending;

    private final IUpSmsRegListener mUpRegistListener = new IUpSmsRegListener() {

        @Override
        public void onRegSuccess(UserTokenInfo info) {
        	//在自定义UI时或手机桌面需求，在登录、注册成功后可能会有些耗时操作，此时先不关闭Dialog,
			//而是在activity的destroy时关闭正在登录对话和正在注册对话框
            mUpSmsRegisterPending = false;
            handleRegSuccess(info);
        }

		@Override
		public void onRegError(int errorType, int errorCode,
				String errorMessage, String mobile) {
			// TODO Auto-generated method stub
			 mUpSmsRegisterPending = false;
	         closeRegDialog();
	         handleRegError(errorType, errorCode, errorMessage,mobile);
		}   
    };

    private final void doCommandReg() {
    	if(!mIsAutoRead){
    		AddAccountsUtils.showErrorToast(mContext, AddAccountsUtils.VALUE_DIALOG_REGISTER, ErrorCode.ERR_TYPE_APP_ERROR, ErrorCode.ERR_CODE_READ_LISENCE, "");
    		return;
    	}
        AddAccountsUtils.hideSoftInput(mContext, mPswText);
        if (mUpSmsRegisterPending) {
            return;
        }
        String password = mPswText.getText().toString();
        if (!AddAccountsUtils.isPasswordValid(mContext, password)) {
            return;
        }
        mUpSmsRegisterPending = true;
        mRegistingDialog = AddAccountsUtils.showDoingDialog(mContext, AddAccountsUtils.VALUE_DIALOG_REGISTER);
        mRegistingDialog.setTimeoutListener(mDialogTimeoutListener);
        //AddAccountsPreference.enableAutoLoginEnabled(mContext, mEnableAutoLogin);
        UpSmsRegister register = new UpSmsRegister(mContext.getApplicationContext(), mContainer.getClientAuthKey(), mUpRegistListener);
        register.register(password);
    }

    private final void handleRegSuccess(UserTokenInfo info) {
        //TODO 确实是否弹出注册成功Dialog
        //AddAccountsBase.showRegisterSuccessDialog(mContext);
        AddAccountsUtils.addAccount(mContainer, mContext, info);
        mContainer.registerListener().onRegisterSuccess(info);
    }

    private final void handleRegError(int errorType, int errorCode, String errorMessage, String mobile) {
    	if(errorCode==AddAccountsUtils.VALUE_REGISTER_UP_AND_EMAIL_EXIST){
    		errorCode=ErrorCode.ERR_CODE_REGISTER_UP_EXIST;
    		errorMessage=mobile;
    		AddAccountsUtils.setEmailName(mContext,mobile);
    	    AddAccountsUtils.setEmailPwd(mContext,mPswText.getText().toString());
    		mRegErrorDialog = AddAccountsUtils.showErrorDialog(mContext, this, AddAccountsUtils.VALUE_DIALOG_REGISTER, errorType, errorCode, errorMessage);	
    	}else {
    		AddAccountsUtils.showErrorToast(mContext, AddAccountsUtils.VALUE_DIALOG_REGISTER, errorType, errorCode, errorMessage);
            mContainer.registerListener().onRegisterError(errorType, errorCode, errorMessage);
		}
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