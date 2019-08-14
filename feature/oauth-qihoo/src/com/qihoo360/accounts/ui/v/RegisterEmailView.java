
package com.qihoo360.accounts.ui.v;
import static com.qihoo360.accounts.base.env.BuildEnv.LOGE_ENABLED;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.qihoo360.accounts.R;
import com.qihoo360.accounts.api.auth.Captcha;
import com.qihoo360.accounts.api.auth.EmailRegister;
import com.qihoo360.accounts.api.auth.i.ICaptchaListener;
import com.qihoo360.accounts.api.auth.i.IEmailRegisterListener;
import com.qihoo360.accounts.api.auth.model.CaptchaData;
import com.qihoo360.accounts.api.auth.model.UserTokenInfo;
import com.qihoo360.accounts.base.common.ErrorCode;
import com.qihoo360.accounts.ui.model.AddAccountsUtils;

/**
 * 邮箱注册界面
 * @author wangzefeng
 *
 */
public class RegisterEmailView extends LinearLayout implements View.OnClickListener,OnCheckedChangeListener {

    private static final String TAG = "ACCOUNT.RegisterEmailView";

    private Context mContext;

    private IContainer mContainer;

    // 帐号编辑框
    private QAccountEditText mAccountEditText;

    // 密码编辑框
    private EditText mPswEdit;

    private Button mShowPswBtn;

    private Button mDeletePswBtn;

    private static Boolean mIsShowPsw = false;

    // 自动阅读用户注册协议
    private CheckBox mAutoRead;

    private boolean mIsAutoRead = true;
    
    private TextView mRegisterPhoneTv;
    
    // 图片验证码
    private View mCaptchaLayout;

    private EditText mCaptchaText;

    private Button mCaptchaDeleteBtn;

    private ImageView mCaptchaImage;

    // 正在注册对话框
    private AccountCustomDialog mRegistingDialog;

    // 注册失败对话框
    private Dialog mRegErrorDialog;
    
    //初次显示的时候， 是否已经请求过验证吗
    private boolean mInitReqCaptcha = false;

    // 点击下拉列表中的某一项的事件监听器
    private final QAccountEditText.SelectedCallback mSelectedCallback = new QAccountEditText.SelectedCallback() {

        @Override
        public void run() {
            // 密码输入框获得焦点
            AddAccountsUtils.setViewFocus(mPswEdit);
        }
    };
    
    /**
	 * 在自定义UI时或手机桌面需求，在登录、注册成功后可能会有些耗时操作，此时先不关闭Dialog,
	 * 而是在activity的destroy时关闭正在登录对话和正在注册对话框
	 */
    private final AccountCustomDialog.ITimeoutListener mDialogTimeoutListener = new AccountCustomDialog.ITimeoutListener() {

        @Override
        public void onTimeout(Dialog dialog) {
            mRegisterPending = false;
        }
    };

    //账号输入后回车键
  	private final OnKeyListener onKey = new OnKeyListener() {
  		
  		public boolean onKey(View v, int keyCode, KeyEvent event) {

  			// TODO Auto-generated method stub
  			if (keyCode == KeyEvent.KEYCODE_ENTER) {
  				AddAccountsUtils.setViewFocus(mPswEdit);// 获取光标焦点
  				AddAccountsUtils.displaySoftInput(mContext, mPswEdit);// 显示键盘
  				mPswEdit.setSelection(mPswEdit.getText().toString().length());// EditText设置光标在内容的最尾端
  				return true;
  			}
  			return false;
  		}
  	};
  	
    //密码输入后回车键
  	private final OnKeyListener onRegisterKey = new OnKeyListener() {
  		
  		public boolean onKey(View v, int keyCode, KeyEvent event) {

  			// TODO Auto-generated method stub
  			if (keyCode == KeyEvent.KEYCODE_ENTER) {
  				AddAccountsUtils.hideSoftInput(mContext, mPswEdit);// 隐藏键盘
  				mPswEdit.setSelection(mPswEdit.getText().toString().length());// EditText设置光标在内容的最尾端
  				doCommandReg();
  				return true;
  			}
  			return false;
  		}
  	};
    public RegisterEmailView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public final void setContainer(IContainer container) {
        mContainer = container;
        mAccountEditText.setText(mContainer.getInitUser());
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initView();
        checkPassword();
        checkCaptcha();
    }
    
    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
    	super.onLayout(changed, l, t, r, b);
    	if(!mInitReqCaptcha){
    		//Log.d("hewei", "doCommandCaptcha ");
    		 //如果非邮箱激活注册，默认触发显示验证码
            if(!mContainer.getIsNeedActiveEmail()){
            	doCommandCaptcha();
            }
    		mInitReqCaptcha = true;
    	}
    }
    
    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.register_email_click) {
            doCommandReg();
        } else if (viewId == R.id.register_email_show_password) {
            mIsShowPsw = !mIsShowPsw;
            onPwdChange();
            // 修改 EditText 中的光标位置，把光标移动到文本后面
            mPswEdit.setSelection(mPswEdit.getText().toString().length());
        } else if (viewId == R.id.register_email_delete_password) {
            mPswEdit.setText(null);
            AddAccountsUtils.setViewFocus(mPswEdit);
            AddAccountsUtils.displaySoftInput(mContext, mPswEdit);
        } else if (viewId == R.id.register_email_delete_captcha_btn) {
            mCaptchaText.setText(null);
        } else if (viewId == R.id.register_email_captcha_imageView) {
            doCommandCaptcha();
        }else if (viewId == R.id.register_email_license) {
        	AddAccountsUtils.toLinsenceWebView(mContext);
        } else if (viewId==R.id.register_phone_button){
        	mContainer.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_DOWN_SMS_VIEW);
        }else if(viewId==R.id.add_accounts_dialog_error_title_icon){
        	closeErrorDialog();//关闭对话框
        }else if (viewId == R.id.add_accounts_dialog_error_cancel_btn) {
            closeErrorDialog();//取消登录
        }else if(viewId==R.id.add_accounts_dialog_error_ok_btn){
        	closeErrorDialog();//立即登录逻辑
        	mContainer.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_LOGIN_VIEW);//登录界面
        	//自动填充用户名和密码
        	((LoginView)mContainer.getLoginView()).setAccount(mAccountEditText.getText().toString().trim());
        	((LoginView)mContainer.getLoginView()).setPsw(mPswEdit.getText().toString());
        	((LoginView)mContainer.getLoginView()).doCommandLogin();	
        }
    }

    private void initView() {
    	mContext = getContext();
        mPswEdit = (EditText) findViewById(R.id.register_email_password);
        mPswEdit.setOnKeyListener(onRegisterKey);
        findViewById(R.id.register_email_click).setOnClickListener(this);
        mRegisterPhoneTv=(TextView)findViewById(R.id.register_phone_button);
        mRegisterPhoneTv.setOnClickListener(this);
        mShowPswBtn = (Button) findViewById(R.id.register_email_show_password);
        mShowPswBtn.setOnClickListener(this);
        mDeletePswBtn = (Button) findViewById(R.id.register_email_delete_password);
        mDeletePswBtn.setOnClickListener(this);
        mCaptchaLayout = findViewById(R.id.register_email_captcha_layout);
        mCaptchaText = (EditText) findViewById(R.id.register_email_captcha_text);
        mCaptchaText.setOnKeyListener(onRegisterKey);
        mCaptchaImage = (ImageView) findViewById(R.id.register_email_captcha_imageView);
        mCaptchaImage.setOnClickListener(this);
        mCaptchaDeleteBtn = (Button) findViewById(R.id.register_email_delete_captcha_btn);
        mCaptchaDeleteBtn.setOnClickListener(this);
        findViewById(R.id.register_email_license).setOnClickListener(this);

        mAutoRead = (CheckBox) findViewById(R.id.register_email_auto_read_lisence);
        mAutoRead.setOnCheckedChangeListener(this);
        
        onPwdChange();
        
        final RelativeLayout accountLayout = (RelativeLayout) findViewById(R.id.qihoo_accounts_reg_email_account_layout);
        mAccountEditText = (QAccountEditText) findViewById(R.id.register_qaet_account);
        accountLayout.setOnKeyListener(onKey);
        accountLayout.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                if (accountLayout.getMeasuredWidth() == 0) {
                    return true;
                }
                mAccountEditText.setDropDownWidth(accountLayout.getMeasuredWidth());
                mAccountEditText.setDropDownHeight((int) getResources().getDimension(R.dimen.qihoo_accounts_autocompletetext_dropdown_height));
                accountLayout.getViewTreeObserver().removeOnPreDrawListener(this);
                return true;
            }
        });
        //mAccountEditText.showLastLoggedAccount(false);
        mAccountEditText.setHintText(R.string.qihoo_accounts_register_email_account_hint);
        mAccountEditText.setTextColor(getResources().getColor(R.color.qihoo_accounts_black));
        //mAccountEditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, getResources().getDimension(R.dimen.qihoo_accounts_textsize_normal));
        // 将事件监听器注册到事件源
        mAccountEditText.setSelectedCallback(mSelectedCallback);
        RelativeLayout pswLayout = (RelativeLayout) findViewById(R.id.qihoo_accounts_reg_email_psw_layout);
        accountLayout.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                AddAccountsUtils.setViewFocus(mAccountEditText.getTextView());
                AddAccountsUtils.displaySoftInput(mContext, mAccountEditText.getTextView());
                return false;
            }
        });
        pswLayout.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                AddAccountsUtils.setViewFocus(mPswEdit);
                AddAccountsUtils.displaySoftInput(mContext, mPswEdit);
                return false;
            }
        });
        
    }
    
    @Override
   	public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
       	if(buttonView.getId() == R.id.register_email_auto_read_lisence) {
       		mIsAutoRead = isChecked;
   		}
   	}
    
    //显示\隐藏密码
  	private void onPwdChange(){
  		if (mIsShowPsw) {
  			mPswEdit.setTransformationMethod(HideReturnsTransformationMethod
  					.getInstance());
  			mShowPswBtn.setText(R.string.qihoo_accounts_hide_password);
  		} else {
  			mPswEdit.setTransformationMethod(PasswordTransformationMethod
  					.getInstance());
  			mShowPswBtn.setText(R.string.qihoo_accounts_show_password);
  		}
  	}
    
    private void checkPassword() {

        mPswEdit.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

            }

            @Override
            public void afterTextChanged(Editable s) {
                String strPassword = mPswEdit.getText().toString();
                if (strPassword.length() > 0) {
                    mDeletePswBtn.setVisibility(View.VISIBLE);
                } else {
                    mDeletePswBtn.setVisibility(View.GONE);
                }
            }
        });
    }

    private void checkCaptcha() {

        mCaptchaText.addTextChangedListener(new TextWatcher() {

            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

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

    private boolean mRegisterPending;

    private final IEmailRegisterListener mRegListener = new IEmailRegisterListener() {

        @Override
        public void onRegSuccess(UserTokenInfo info) {
        	//在自定义UI时或手机桌面需求，在登录、注册成功后可能会有些耗时操作，此时先不关闭Dialog,
			//而是在activity的destroy时关闭正在登录对话和正在注册对话框
            mRegisterPending = false;
            handleRegSuccess(info);
        }

        @Override
        public void onRegError(int errorType, int errorCode, String errorMessage) {
            mRegisterPending = false;
            closeRegDialog();
            handleRegError(errorType, errorCode, errorMessage);
        }

        //邮箱注册是否需要图片验证码的回调函数
        @Override
        public void onRegNeedCaptcha() {
            mRegisterPending = false;
            closeRegDialog();
            doCommandCaptcha();
        }
        
       //邮箱注册图片验证码错误的回调函数
        @Override
        public void onRegWrongCaptcha(int errorType, int errorCode, String errorMessage) {
            mRegisterPending = false;
            closeRegDialog();
            doCommandCaptcha();
            if(!TextUtils.isEmpty(errorMessage)){
            	AddAccountsUtils.showErrorToast(mContext, AddAccountsUtils.VALUE_DIALOG_REGISTER, errorType, errorCode, errorMessage);
            }
        }

		@Override
		public void onRegSuccess(String mailHostUrl) {
			// TODO Auto-generated method stub
			if(TextUtils.isEmpty(mailHostUrl)){
				String mailUrl=mAccountEditText.getText().toString();
				int seperatorPosition = mailUrl.indexOf("@");
				mailHostUrl=AddAccountsUtils.MAIL_HEAD+mailUrl.substring(seperatorPosition+1,mailUrl.length());
			}
			AddAccountsUtils.setEmailUrl(mContext, mailHostUrl);
			mRegisterPending = false;
            closeRegDialog();
            registerActiveEmail();
		}
    };

    private final void doCommandReg() {
    	if(!mIsAutoRead){
    		AddAccountsUtils.showErrorToast(mContext, AddAccountsUtils.VALUE_DIALOG_REGISTER, ErrorCode.ERR_TYPE_APP_ERROR, ErrorCode.ERR_CODE_READ_LISENCE, "");
    		return;
    	}
        AddAccountsUtils.hideSoftInput(mContext, mAccountEditText);
        AddAccountsUtils.hideSoftInput(mContext, mPswEdit);
        if (mRegisterPending) {
            // 正在注册，直接返回
            return;
        }
        String username = mAccountEditText.getText().toString();
        String password = mPswEdit.getText().toString();
        if (!AddAccountsUtils.isEmailValid(mContext, username)) {
            return;
        }
        if (!AddAccountsUtils.isPasswordValid(mContext, password)) {
            return;
        }
        String uc = mCaptcha != null ? mCaptchaText.getText().toString() : "";
        String sc = mCaptcha != null && !TextUtils.isEmpty(uc) ? mCaptcha.sc : "";
        //
        if (mCaptcha != null && !AddAccountsUtils.isCaptchaValid(mContext, uc)) {
            return;
        }
        mRegisterPending = true;
        mRegistingDialog = AddAccountsUtils.showDoingDialog(mContext, AddAccountsUtils.VALUE_DIALOG_REGISTER);
        mRegistingDialog.setTimeoutListener(mDialogTimeoutListener);
        EmailRegister emailReg = new EmailRegister(mContext.getApplicationContext(), mContainer.getClientAuthKey(), mContainer.getLooper(), mRegListener);
        //是否需要激活注册邮箱
        if(mContainer.getIsNeedActiveEmail()){
        	emailReg.registerNeedActiveEmail(username, password, sc, uc);
        }else{
        	emailReg.register(username, password, sc, uc);	
        }
        
    }

    private final void registerActiveEmail() {
        View emailTipsView = mContainer.getRegEmailActiveView();
        ((TextView) emailTipsView.findViewById(R.id.register_email_addr)).setText(mAccountEditText.getText());
        AddAccountsUtils.setEmailName(mContext,mAccountEditText.getText().toString());
        AddAccountsUtils.setEmailPwd(mContext,mPswEdit.getText().toString());
        mContainer.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_EMAIL_ACTIVE_VIEW);
    }
    
    private final void handleRegSuccess(UserTokenInfo info) {
        AddAccountsUtils.addAccount(mContainer, mContext, info);//保存本地一份用户名
        mContainer.registerListener().onRegisterSuccess(info);//保存共享池一份
    }

    private final void handleRegError(int errorType, int errorCode, String errorMessage) {
    	if(errorCode == AddAccountsUtils.VALUE_REGISTER_UP_AND_EMAIL_EXIST){
    		errorCode=ErrorCode.ERR_CODE_REGISTER_EMAIL_EXIST;
    		errorMessage=mAccountEditText.getText().toString();
    		mRegErrorDialog = AddAccountsUtils.showErrorDialog(mContext, this, AddAccountsUtils.VALUE_DIALOG_REGISTER, errorType, errorCode, errorMessage);	
    	}else{
    		AddAccountsUtils.showErrorToast(mContext, AddAccountsUtils.VALUE_DIALOG_REGISTER, errorType, errorCode, errorMessage);
    	}
        
        mContainer.registerListener().onRegisterError(errorType, errorCode, errorMessage);
    }

    private boolean mCaptchaPending;

    private final ICaptchaListener mCaptchaListener = new ICaptchaListener() {

        @Override
        public void onCaptchaSuccess(CaptchaData info) {
            mCaptchaPending = false;
            handleCaptchaSuccess(info);
        }

        @Override
        public void onCaptchaError(int errorCode) {
            mCaptchaPending = false;
            handleCaptchaError(errorCode);
        }
    };

    private CaptchaData mCaptcha;

    private final void doCommandCaptcha() {
        if (mCaptchaPending) {
            return;
        }
        mCaptchaPending = true;
        Captcha regCaptcha = new Captcha(mContext.getApplicationContext(), mContainer.getClientAuthKey(), mContainer.getLooper(), mCaptchaListener);
        regCaptcha.getCaptcha();
    }

    private final void handleCaptchaSuccess(CaptchaData info) {
        mCaptcha = info;
        mCaptchaLayout.setVisibility(View.VISIBLE);
        byte bytes[] = info.bytes;
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inPreferredConfig = Config.ARGB_8888;
            options.inSampleSize = 1;
            Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
            bitmap.setDensity(DisplayMetrics.DENSITY_HIGH);
            mCaptchaImage.setImageBitmap(bitmap);
            mCaptchaImage.setAdjustViewBounds(true);
			mCaptchaImage.setMaxHeight(mShowPswBtn.getHeight());
			mCaptchaImage.setMaxWidth(mShowPswBtn.getWidth());
			mCaptchaImage.setScaleType(ScaleType.FIT_XY);
        } catch (Throwable e) {
            if (LOGE_ENABLED) {
                Log.e(TAG, e.toString(), e);
            }
        }
    }

    private final void handleCaptchaError(int errorCode) {
    	AddAccountsUtils.showErrorToast(mContext, AddAccountsUtils.VALUE_DIALOG_LOGIN, ErrorCode.ERR_TYPE_APP_ERROR, ErrorCode.ERR_CODE_IMAGE_CAPTCHE, "");
    }

    public final void closeDialogsOnDestroy() {
        AddAccountsUtils.closeDialogsOnDestroy(mRegistingDialog);
        AddAccountsUtils.closeDialogsOnDestroy(mRegErrorDialog);
        AddAccountsUtils.setEmailName(mContext,"");
        AddAccountsUtils.setEmailPwd(mContext,"");
    }

    public final void closeRegDialog() {
        AddAccountsUtils.closeDialogsOnCallback(mContext, mRegistingDialog);
    }

    private final void closeErrorDialog() {
        AddAccountsUtils.closeDialogsOnCallback(mContext, mRegErrorDialog);
    }

}
