package com.qihoo360.accounts.ui.v;

import static com.qihoo360.accounts.base.env.BuildEnv.LOGE_ENABLED;

import org.json.JSONObject;

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
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.qihoo360.accounts.R;
import com.qihoo360.accounts.api.auth.Captcha;
import com.qihoo360.accounts.api.auth.Login;
import com.qihoo360.accounts.api.auth.i.ICaptchaListener;
import com.qihoo360.accounts.api.auth.i.ILoginListener;
import com.qihoo360.accounts.api.auth.model.CaptchaData;
import com.qihoo360.accounts.api.auth.model.UserTokenInfo;
import com.qihoo360.accounts.base.common.ErrorCode;
import com.qihoo360.accounts.ui.model.AddAccountsUtils;

/**
 * 登录页面
 * 
 * @author wangzefeng
 * 
 */
public class LoginView extends LinearLayout implements View.OnClickListener {

	private static final String TAG = "ACCOUNT.LoginView";
	
	//密码错误次数
	private static final int pwdCount = 5;
	
	//头像大小，小写字母a/s/m/b/q(分别代表20x20/48x48/64x64/100x100/150x150)，默认为s
	private static final String headType = "s";

	private Context mContext;

	// 存放从activity传递过来的变量，通过它来获取activity中的相关数据
	private IContainer mContainer;

	// 帐号文本框
	private QAccountEditText mAccountEdit;

	// 密码文本框
	private EditText mPswEdit;

	private Button mDeletePswBtn;
	
	private TextView title;

	private Button mShowPswBtn;

	// 密码是否以明文显示
	private static boolean mIsShowPsw = false;

	// 图片验证码
	private View mCaptchaLayout;

	private EditText mCaptchaEdit;

	private Button mCaptchaDeleteBtn;

	private ImageView mCaptchaImage;

	// 正在登录对话框
	public AccountCustomDialog mLoginingDialog;
	
	// 登录失败对话框
    private Dialog mLoginErrorDialog;

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
			mLoginPending = false;
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
	private final OnKeyListener onLoinKey = new OnKeyListener() {
		
		public boolean onKey(View v, int keyCode, KeyEvent event) {

			// TODO Auto-generated method stub
			if (keyCode == KeyEvent.KEYCODE_ENTER) {
				AddAccountsUtils.hideSoftInput(mContext, mPswEdit);// 显示键盘
				mPswEdit.setSelection(mPswEdit.getText().toString().length());// EditText设置光标在内容的最尾端
				doCommandLogin();
				return true;
			}
			return false;
		}
	};

	public LoginView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}

	// activity调用，用来把acitivity中初始化的变量传递到view中
	public final void setContainer(IContainer container) {
		mContainer = container;
		mAccountEdit.setText(mContainer.getInitUser());
		mAccountEdit.setLoginStatBoolean(true);
		mAccountEdit.setContainer(mContainer);
	}
	
	public String getAccount() {
        return mAccountEdit.getText().toString();
    }
	
	public String getPsw() {
        return mPswEdit.getText().toString();
    }
	
	public void setAccount(String account) {
        mAccountEdit.setText(account);
    }
	
	public void setPsw(String password) {
        mPswEdit.setText(password);
    }

	@Override
	public void onClick(View v) {
		int viewId = v.getId();
		if (viewId == R.id.login_click) {
			doCommandLogin();
		} else if (viewId == R.id.login_quick_register) {
			// 配置上行短信注册方式的情况
			if (mContainer.getIsNeedUpSmsRegister()) {
				mContainer
						.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_UP_SMS_VIEW);
			} else {
				mContainer
						.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_DOWN_SMS_VIEW);
			}
		} else if (viewId == R.id.login_delete_password) {
			mPswEdit.setText(null);
			AddAccountsUtils.setViewFocus(mPswEdit);// 获取光标焦点
			AddAccountsUtils.displaySoftInput(mContext, mPswEdit);// 显示键盘
		} else if (viewId == R.id.login_show_password) {
			mIsShowPsw = !mIsShowPsw;
			OnPwdChange();
			mPswEdit.setSelection(mPswEdit.getText().toString().length());// EditText设置光标在内容的最尾端
		} else if (viewId == R.id.login_delete_captcha_btn) {
			mCaptchaEdit.setText(null);
		} else if (viewId == R.id.login_captcha_imageView) {
			doCommandCaptcha();// 获取验证码
		} else if (viewId == R.id.login_forget_password) {
			if(TextUtils.isEmpty(getAccount().trim()) || AddAccountsUtils.isPhoneNumberValidNoToast(mContext,getAccount().trim())){
				((FindPwdByMobileView)mContainer.getFindPwdByMobileView()).setPhone(getAccount().trim());
				mContainer.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_FINDPWD_MOBILE_VIEW);
			}else{
				String account =  getAccount().trim();
				AddAccountsUtils.toFindPwdWebView(mContext, account);
			}
		}else if(viewId==R.id.add_accounts_dialog_error_title_icon){
        	closeErrorDialog();//关闭对话框
        }else if (viewId == R.id.add_accounts_dialog_error_cancel_btn) {
            closeErrorDialog();//取消登录
        }else if(viewId==R.id.add_accounts_dialog_error_ok_btn){
        	closeErrorDialog();//立即登录逻辑
        	onLoginNeedActiveEmail(); 	
        }
	}

	@Override
	protected void onFinishInflate() {
		super.onFinishInflate();
		mContext = getContext();
		initView();
		checkLoginPassword();
		checkLoginCaptcha();
	}

	private final void initView() {
		mPswEdit = (EditText) findViewById(R.id.login_password);
		mPswEdit.setOnKeyListener(onLoinKey);
		findViewById(R.id.login_click).setOnClickListener(this);
		findViewById(R.id.login_quick_register).setOnClickListener(this);
		title=(TextView)findViewById(R.id.qihoo_accounts_top_title);
		title.setText(R.string.qihoo_accounts_login_top_title);
		mDeletePswBtn = (Button) findViewById(R.id.login_delete_password);
		mDeletePswBtn.setOnClickListener(this);
		mShowPswBtn = (Button) findViewById(R.id.login_show_password);
		mShowPswBtn.setOnClickListener(this);
		mCaptchaLayout = findViewById(R.id.login_captcha_layout);
		mCaptchaEdit = (EditText) findViewById(R.id.login_captcha_text);
		mCaptchaEdit.setOnKeyListener(onLoinKey);
		mCaptchaDeleteBtn = (Button) findViewById(R.id.login_delete_captcha_btn);
		mCaptchaDeleteBtn.setOnClickListener(this);
		mCaptchaImage = (ImageView) findViewById(R.id.login_captcha_imageView);
		mCaptchaImage.setOnClickListener(this);
		findViewById(R.id.login_forget_password).setOnClickListener(this);

		final RelativeLayout accountLayout = (RelativeLayout) findViewById(R.id.qihoo_accounts_login_account_layout);
		mAccountEdit = (QAccountEditText) findViewById(R.id.login_qaet_account);
		accountLayout.setOnKeyListener(onKey);
		accountLayout.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				AddAccountsUtils.setViewFocus(mAccountEdit.getTextView());
				AddAccountsUtils.displaySoftInput(mContext,
						mAccountEdit.getTextView());
				return false;
			}
		});
		// 注册一个回调函数，当一个视图树将要绘制时调用这个回调函数
		accountLayout.getViewTreeObserver().addOnPreDrawListener(
				new ViewTreeObserver.OnPreDrawListener() {
					@Override
					public boolean onPreDraw() {
						if (accountLayout.getMeasuredWidth() == 0) {
							return true;
						}
						mAccountEdit.setDropDownWidth(accountLayout
								.getMeasuredWidth());
						mAccountEdit
								.setDropDownHeight((int) getResources()
										.getDimension(
												R.dimen.qihoo_accounts_autocompletetext_dropdown_height));
						accountLayout.getViewTreeObserver()
								.removeOnPreDrawListener(this);
						return true;
					}
				});
		//mAccountEdit.showLastLoggedAccount(false);
		mAccountEdit.setHintText(R.string.qihoo_accounts_login_account_hint);
		mAccountEdit.setTextColor(getResources().getColor(
				R.color.qihoo_accounts_black));
		// mAccountEdit.setTextSize(TypedValue.COMPLEX_UNIT_SP,
		// getResources().getDimension(R.dimen.qihoo_accounts_textsize_normal));
		// 将事件监听器注册到事件源
		mAccountEdit.setSelectedCallback(mSelectedCallback);
		// 若在xml中设置 android:inputType="textPassword"，在切换“隐藏” “显示”
		// 密码时，会删除密码输入框中的字符
		// 所以在代码中设置隐藏效果
		OnPwdChange();
		final RelativeLayout pswLayout = (RelativeLayout) findViewById(R.id.qihoo_accounts_login_psw_layout);
		pswLayout.setOnTouchListener(new OnTouchListener() {

			@Override
			public boolean onTouch(View v, MotionEvent event) {
				AddAccountsUtils.setViewFocus(mPswEdit);
				AddAccountsUtils.displaySoftInput(mContext, mPswEdit);
				return false;
			}
		});
	}

	//显示\隐藏密码
	private void OnPwdChange(){
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
	
	private void checkLoginPassword() {

		mPswEdit.addTextChangedListener(new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {
				// 可用来动态检测密码强度
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

	private void checkLoginCaptcha() {

		mCaptchaEdit.addTextChangedListener(new TextWatcher() {

			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {

			}

			@Override
			public void onTextChanged(CharSequence s, int start, int before,
					int count) {

			}

			@Override
			public void afterTextChanged(Editable s) {
				String strCaptcha = mCaptchaEdit.getText().toString();
				if (strCaptcha.length() > 0) {
					mCaptchaDeleteBtn.setVisibility(View.VISIBLE);
				} else {
					mCaptchaDeleteBtn.setVisibility(View.GONE);
				}
			}
		});
	}

	private boolean mLoginPending;

	private final ILoginListener mLoginListener = new ILoginListener() {

		@Override
		public void onLoginSuccess(UserTokenInfo info) {
			//在自定义UI时或手机桌面需求，在登录、注册成功后可能会有些耗时操作，此时先不关闭Dialog,
			//而是在activity的destroy时关闭正在登录对话和正在注册对话框
			mLoginPending = false;
			handleLoginSuccess(info);
		}

		@Override
		public void onLoginError(int errorType, int errorCode,
				String errorMessage, JSONObject errorDetail) {
			// TODO Auto-generated method stub
			mLoginPending = false;
			closeLoginDialog();
			if(mCaptcha != null){
				doCommandCaptcha();
			}
			handleLoginError(errorType, errorCode, errorMessage,errorDetail);
		}

		@Override
		public void onRegNeedCaptcha() {
			mLoginPending = false;
			closeLoginDialog();
			doCommandCaptcha();
		}

		@Override
		public void onRegWrongCaptcha() {
			mLoginPending = false;
			closeLoginDialog();
			doCommandCaptcha();
			Toast.makeText(
					mContext,
					getResources().getText(
							R.string.qihoo_accounts_login_error_captcha),
					Toast.LENGTH_SHORT).show();
		}

		@Override
		public void onLoginNeedEmailActive(String email, String mailHostUrl) {
			// TODO Auto-generated method stub
			mLoginPending = false;
			closeLoginDialog();
			if(TextUtils.isEmpty(mailHostUrl)){
				String mailUrl=mAccountEdit.getText().toString();
				int seperatorPosition = mailUrl.indexOf("@");
				mailHostUrl=AddAccountsUtils.MAIL_HEAD+mailUrl.substring(seperatorPosition+1,mailUrl.length());
			}
			AddAccountsUtils.setEmailUrl(mContext, mailHostUrl);// 打开激活邮箱页面
			AddAccountsUtils.setEmailName(mContext, email);
			mLoginErrorDialog=AddAccountsUtils.showErrorDialog(mContext,LoginView.this,AddAccountsUtils.VALUE_DIALOG_LOGIN,ErrorCode.ERR_TYPE_APP_ERROR,ErrorCode.ERR_CODE_EAMIL_NEED_ACTIVE, "");
		}
	};

	private final void onLoginNeedActiveEmail() {
		RegisterEmailActiveView emailTipsView =(RegisterEmailActiveView) mContainer.getRegEmailActiveView();
		emailTipsView.setLoginNeedEmailActive(true);
		((TextView) emailTipsView.findViewById(R.id.register_email_addr))
				.setText(AddAccountsUtils.getEmailName(mContext));
		AddAccountsUtils.setEmailPwd(mContext, mPswEdit.getText().toString());
		mContainer
				.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_EMAIL_ACTIVE_VIEW);
	}

	public final void doCommandLogin() {
		AddAccountsUtils.hideSoftInput(mContext, mAccountEdit);
		AddAccountsUtils.hideSoftInput(mContext, mPswEdit);
		if (mLoginPending) {
			// 正在登录，直接返回
			return;
		}
		String username = mAccountEdit.getText().toString();
		String password = mPswEdit.getText().toString();
		if (!AddAccountsUtils.isLoginAccountValid(mContext, username)) {
			return;// 检测是否为空
		}
		if (!AddAccountsUtils.isLoginPasswordValid(mContext, password)) {
			return;// 检测是否为空
		}
		// uc用户输入的验证码，sc服务端返回的验证码
		String uc = mCaptcha != null ? mCaptchaEdit.getText().toString() : "";
		String sc = mCaptcha != null && !TextUtils.isEmpty(uc) ? mCaptcha.sc
				: "";
		//
		if (mCaptcha != null && !AddAccountsUtils.isCaptchaValid(mContext, uc)) {
			return;// 检测是否为空和是否<6位
		}
		mLoginPending = true;
		mLoginingDialog = AddAccountsUtils.showDoingDialog(mContext,
				AddAccountsUtils.VALUE_DIALOG_LOGIN);
		mLoginingDialog.setTimeoutListener(mDialogTimeoutListener);
		Login login = new Login(mContext.getApplicationContext(),
				mContainer.getClientAuthKey(), mContainer.getLooper(),
				mLoginListener);
		login.login(username, password, sc, uc,false,headType);
	}

	private final void handleLoginSuccess(UserTokenInfo info) {
		AddAccountsUtils.addAccount(mContainer, mContext, info);// 登录成功的账号添加到本地保存
		mContainer.loginListener().onLoginSuccess(info);// 账号添加到共享池
	}

	private final void handleLoginError(int errorType, int errorCode,
			String errorMessage,JSONObject errorDetail) {
		if(errorCode==AddAccountsUtils.VALUE_LOGIN_PWD_ERROR && errorDetail!=null){
			int restTimes = -1;
			try {
				restTimes = Integer.parseInt(errorDetail.optString("restTimes", "-1"));
			} catch (Exception e) {
			}
			if( restTimes <= pwdCount && restTimes >= 0){
				errorMessage=mContext.getResources().getString(R.string.qihoo_accounts_login_pwd_error_first)+restTimes+
						mContext.getResources().getString(R.string.qihoo_accounts_login_pwd_error_last);
			}
		}
		AddAccountsUtils.showErrorToast(mContext,
				AddAccountsUtils.VALUE_DIALOG_LOGIN, errorType, errorCode,
				errorMessage);
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
		Captcha loginCaptcha = new Captcha(mContext.getApplicationContext(),
				mContainer.getClientAuthKey(), mContainer.getLooper(),
				mCaptchaListener);
		loginCaptcha.getCaptcha();
	}

	private final void handleCaptchaSuccess(CaptchaData info) {
		mCaptcha = info;
		mCaptchaLayout.setVisibility(View.VISIBLE);
		byte bytes[] = info.bytes;
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inPreferredConfig = Config.ARGB_8888;
			options.inSampleSize = 1;// >1则缩小倍数，<=1 相当于原尺寸
			Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0,
					bytes.length, options);// 从字节数组解码Bitmap
			bitmap.setDensity(DisplayMetrics.DENSITY_HIGH);// 高密度
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
		AddAccountsUtils.showErrorToast(mContext,
				AddAccountsUtils.VALUE_DIALOG_LOGIN,
				ErrorCode.ERR_TYPE_APP_ERROR, ErrorCode.ERR_CODE_IMAGE_CAPTCHE,
				"");
	}

	public final void closeDialogsOnDestroy() {
		AddAccountsUtils.closeDialogsOnDestroy(mLoginingDialog);
		AddAccountsUtils.closeDialogsOnDestroy(mLoginErrorDialog);
	}

	public final void closeLoginDialog() {
		AddAccountsUtils.closeDialogsOnCallback(mContext, mLoginingDialog);
	}

	public final void closeErrorDialog() {
		AddAccountsUtils.closeDialogsOnCallback(mContext, mLoginErrorDialog);
	}
}
