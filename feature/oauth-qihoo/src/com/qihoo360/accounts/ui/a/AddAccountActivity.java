
package com.qihoo360.accounts.ui.a;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.KeyEvent;
import android.view.View;
import android.widget.TextView;

import com.qihoo360.accounts.R;
import com.qihoo360.accounts.api.auth.model.UserTokenInfo;
import com.qihoo360.accounts.api.auth.p.ClientAuthKey;
import com.qihoo360.accounts.base.common.Constant;
import com.qihoo360.accounts.base.common.DefaultLocalAccounts;
import com.qihoo360.accounts.base.exception.MultimSimSupportLibNotInstalledException;
import com.qihoo360.accounts.base.utils.MultiSimUtil;
import com.qihoo360.accounts.ui.model.AddAccountsUtils;
import com.qihoo360.accounts.ui.v.AccountCustomDialog;
import com.qihoo360.accounts.ui.v.FindPwdByMobileCaptchaView;
import com.qihoo360.accounts.ui.v.FindPwdByMobileSavePwdView;
import com.qihoo360.accounts.ui.v.FindPwdByMobileView;
import com.qihoo360.accounts.ui.v.IContainer;
import com.qihoo360.accounts.ui.v.ILoginResultListener;
import com.qihoo360.accounts.ui.v.IRegResultListener;
import com.qihoo360.accounts.ui.v.LoginView;
import com.qihoo360.accounts.ui.v.RegisterDownSmsCaptchaView;
import com.qihoo360.accounts.ui.v.RegisterDownSmsView;
import com.qihoo360.accounts.ui.v.RegisterEmailActiveView;
import com.qihoo360.accounts.ui.v.RegisterEmailView;
import com.qihoo360.accounts.ui.v.RegisterUpSmsView;

/**
 * <p>抽象类，UI界面的父类 </p>
 * @author wangzefeng
 */
public abstract class AddAccountActivity extends Activity implements View.OnClickListener, ILoginResultListener, IRegResultListener {
    /**
     * 管理注册、登录成功的帐号，控制登录中的自动完成文本框的下拉列表内容
     * 在activity中new，在各view中使用
     */
    DefaultLocalAccounts mUiAccounts;

    //是否提供邮箱注册，默认提供
    private boolean mIsNeedEmailRegister=true;
    
    //是否开启邮箱注册激活，默认开启
    private boolean mIsNeedActiveEmail = true;
    
    //是否开启上行短信注册，默认不开启
    private boolean mIsNeedUpSmsRegister=false;
    
    //是否单卡
    private boolean mIsSingleSimCard = false;

    //增加账号：注册/登录
    private int mAddAccountType;
    
    //增加帐号参数：手机注册类型
    private int mAddAccountMobileType;
    
    //增加帐号参数：邮箱注册类型
    private int mAddAccountEmailType;
    
    //增加账号参数：是否提供邮箱注册
    private int mAddAccountEmail;

    //账号输入框中初始化的用户
    private String mInitUser;

    /**
     * 调用服务时，传id/key的方案
     */
    private ClientAuthKey mAuthKey;

    /**
     * 将会在Activity中显示的页面9个页面
     * 某一时刻Activity只会显示其中的一个，其他都隐藏
     */
    private View mLoginLayout;
    
    private View mRegEmailLayout;
    
    private View mRegEmailActiveLayout;

    private View mRegLayout;
    
    private View mRegUpSmsLayout;

    private View mRegDownLayout;

    private View mRegDownCaptchaLayout;

    private View mFindPwdLayout;
    
    private View mFindPwdByMobileLayout;
    
    private View mFindPwdByMobileCaptchaLayout;
    
    private View mFindPwdByMobileSavePwdLayout;
    
    /**
     * 自定义的9个View
     */
    private LoginView mLoginView;//登录页面
    
    private RegisterEmailView mRegEmailView;//邮箱注册页面
    
    private RegisterEmailActiveView mRegEmailActiveView;//邮箱注册激活页面

    private RegisterUpSmsView mRegUpSmsView;//上行短信注册页面

    private RegisterDownSmsView mRegDownSmsView;//下行短信注册页面

    private RegisterDownSmsCaptchaView mRegDownSmsCaptchaView;//下行短信验证码提交页面
    
    private FindPwdByMobileView mFindPwdByMobileView;
    
    private FindPwdByMobileCaptchaView mFindPwdByMobileCaptchaView;
    
    private FindPwdByMobileSavePwdView mFindPwdByMobileSavePwdView;

    int mScreenW;
    
    private TextView title;

    //自定义Dialog，设置超时时间30秒后自动关闭
    private AccountCustomDialog mCustomDialog;
    
    //容器类
    IContainer mContainer = new IContainer() {

    	//注册成功监听事件
        @Override
        public IRegResultListener registerListener() {
            return AddAccountActivity.this;
        }

        //登录成功监听事件
        @Override
        public ILoginResultListener loginListener() {
            return AddAccountActivity.this;
        }

        @Override
        public void finish() {
            AddAccountActivity.this.finish();
        }

        //处理返回键
        @Override
        public void back() {
            // TODO 实现此处
        }

        //获取Looper
        @Override
        public Looper getLooper() {
            return getMainLooper();
        }

        //客户端标识/加密/认证信息，由用户中心服务器颁发
        @Override
        public ClientAuthKey getClientAuthKey() {
            return mAuthKey;
        }


        @Override
        public String getInitUser() {
            return mInitUser;
        }

        //下行短信注册界面
        @Override
        public View getRegDownSmsView() {
            return mRegDownSmsView;
        }

        //下行短信验证码界面
        @Override
        public View getRegDownSmsCaptchaView() {
            return mRegDownSmsCaptchaView;
        }

        //显示各个界面（按照登录、注册等界面类型）
        @Override
        public void showAddAccountsView(int viewType) {
            showView(viewType);
        }

        //注册、登录成功的帐号管理，自定义ui中使用
        @Override
        public DefaultLocalAccounts getUiAccounts() {
            return mUiAccounts;
        }

        //邮箱注册是否需要激活
		@Override
		public boolean getIsNeedActiveEmail() {
			// TODO Auto-generated method stub
			return mIsNeedActiveEmail;
		}
		
		//是否提供邮箱注册方式
		@Override
		public boolean getIsNeedEmailRegister() {
			// TODO Auto-generated method stub
			return mIsNeedEmailRegister;
		}

		//是否提供上行短信注册
		@Override
		public boolean getIsNeedUpSmsRegister() {
			// TODO Auto-generated method stub
			return mIsNeedUpSmsRegister;
		}

		//获取激活邮箱页面
		@Override
		public View getRegEmailActiveView() {
			// TODO Auto-generated method stub
			return mRegEmailActiveView;
		}

		@Override
		public boolean getIsSingleSimCard() {
			// TODO Auto-generated method stub
			return mIsSingleSimCard;
		}

		@Override
		public View getLoginView() {
			// TODO Auto-generated method stub
			return mLoginView;
		}

		@Override
		public View getFindPwdByMobileCaptchaView() {
			// TODO Auto-generated method stub
			return mFindPwdByMobileCaptchaView;
		}

		@Override
		public View getFindPwdByMobileSavePwdView() {
			// TODO Auto-generated method stub
			return mFindPwdByMobileSavePwdView;
		}

		@Override
		public View getFindPwdByMobileView() {
			// TODO Auto-generated method stub
			return mFindPwdByMobileView;
		}

    };
   

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handleSetContentView(savedInstanceState);
        initParam();
        //现默认的服务接续
        mUiAccounts = new DefaultLocalAccounts();
        mUiAccounts.setSpName(Constant.UI_SP_NAME); //指定spname
        initView();//初始化UI
    }

    /**
     * 业务方可重写该方法，自定义布局文件
     * @param savedInstanceState
     */
    protected void handleSetContentView(Bundle savedInstanceState){
    	setContentView(R.layout.qihoo_accounts_add_account_activity);//默认的登录注册界面
    }
    
    
   
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK) {
            clickBackBtn();
            return false;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.qihoo_accounts_top_back) {
            clickBackBtn();
        }
    }

    @Override
    public final void onLoginSuccess(UserTokenInfo info) {
        handleLoginSuccess(info);//业务方实现,处理登录成功之后的操作
    }

    @Override
    public final void onLoginError(int errorType, int errorCode, String errorMessage) {
    	//已做错误处理
    }

    @Override
    public void onRegisterSuccess(UserTokenInfo info) {
        handleRegisterSuccess(info);//业务方实现，处理注册成功之后的操作
    }

    @Override
    public void onRegisterError(int errorType, int errorCode, String errorMessage) {
    	//已做错误处理
    }

    private final void initParam() {
        Bundle initBundle = getInitParam();
        if(initBundle == null || initBundle.isEmpty()){
        	Intent intent = getIntent();
        	if(intent != null){
        		initBundle = getIntent().getExtras();
        	}
        }
        
        // 判断是否是单卡手机
        mIsSingleSimCard=AddAccountsUtils.isSingleSimCardExist(this);
        //账号行为类型(登录还是注册)
        mAddAccountType = initBundle.getInt(Constant.KEY_ADD_ACCOUNT_TYPE, Constant.VALUE_ADD_ACCOUNT_LOGIN);
        //账号输入框默认初始化的用户
        mInitUser = initBundle.getString(Constant.KEY_ADD_ACCOUNT_USER);
        //是否提供邮箱注册（默认提供）
        mAddAccountEmail=initBundle.getInt(Constant.KEY_ADD_ACCOUNT_EMAIL,Constant.VALUE_ADD_ACCOUNT_HAS_EMAIL);
        //邮箱注册类型（默认不激活）
        mAddAccountEmailType=initBundle.getInt(Constant.KEY_ADD_ACCOUNT_EMAIL_TYPE,Constant.VALUE_ADD_ACCOUNT_EMAIL_REGISTER_ACTIVE);
        //手机注册类型（默认下行）
        mAddAccountMobileType=initBundle.getInt(Constant.KEY_ADD_ACCOUNT_MOBILE_TYPE,Constant.VALUE_ADD_ACCOUNT_DOWN_SMS_REGISTER);
        
        if((mAddAccountEmail & Constant.VALUE_ADD_ACCOUNT_NO_EMAIL)!=0){
        	mIsNeedEmailRegister=false;
        }
        if((mAddAccountEmailType & Constant.VALUE_ADD_ACCOUNT_EMAIL_REGISTER)!=0){
        	mIsNeedActiveEmail=false;
        }
        if((mAddAccountMobileType & Constant.VALUE_ADD_ACCOUNT_UP_SMS_REGISTER)!=0){
        	if (!MultiSimUtil.isMultiSimLibExist(this)) {
    			throw new MultimSimSupportLibNotInstalledException();
    		}
        	mIsNeedUpSmsRegister=true;
        }
        
        //传递的from参数，由用户中心分配给业务
        String from = initBundle.getString(Constant.KEY_CLIENT_AUTH_FROM);
        //传递的签名密钥，由用户中心分配给业务
        String sigKey = initBundle.getString(Constant.KEY_CLIENT_AUTH_SIGN_KEY);
        //传递的url加密DES密钥
        String crpytKey = initBundle.getString(Constant.KEY_CLIENT_AUTH_CRYPT_KEY);
        //客户端标识/加密/认证信息，由用户中心服务器颁发
        mAuthKey = new ClientAuthKey(from, sigKey, crpytKey);
    }

    private final void initView() {
        mLoginLayout = findViewById(R.id.qihoo_accounts_login);//登录布局页面
        mRegLayout = findViewById(R.id.qihoo_accounts_register);//整个注册布局页面
        mFindPwdLayout = findViewById(R.id.qihoo_accounts_findpwd_view);//手机找回密码布局页面
        mRegUpSmsLayout=mRegLayout.findViewById(R.id.qihoo_accounts_register_up_sms_layout);//手机上行注册布局页面
        mRegDownLayout = mRegLayout.findViewById(R.id.qihoo_accounts_register_down_sms_layout);//手机下行注册布局页面
        mRegDownCaptchaLayout = mRegLayout.findViewById(R.id.qihoo_accounts_register_down_sms_captcha_layout);//手机注册验证码提交布局页面
        mRegEmailLayout = mRegLayout.findViewById(R.id.qihoo_accounts_register_email_layout);//邮箱注册布局页面
        mRegEmailActiveLayout = mRegLayout.findViewById(R.id.qihoo_accounts_register_email_active_layout);//邮箱注册激活布局页面       
        mFindPwdByMobileLayout=mFindPwdLayout.findViewById(R.id.qihoo_accounts_findpwd_step1_layout);//手机"找回密码"第一步 输入手机号
        mFindPwdByMobileCaptchaLayout=mFindPwdLayout.findViewById(R.id.qihoo_accounts_findpwd_step2_layout);//手机"找回密码"第二步 短信验证码
        mFindPwdByMobileSavePwdLayout=mFindPwdLayout.findViewById(R.id.qihoo_accounts_findpwd_step3_layout);//手机"找回密码"第三步 保存新密码
        
        mLoginView = (LoginView) mLoginLayout.findViewById(R.id.login_view);
        mLoginView.setContainer(mContainer);//activity调用，用来把acitivity中初始化的变量传递到view中
        
        mRegDownSmsView = (RegisterDownSmsView) mRegDownLayout.findViewById(R.id.register_down_sms_view);
        mRegDownSmsView.setContainer(mContainer);//手机下行注册view
        
        mRegDownSmsCaptchaView = (RegisterDownSmsCaptchaView) mRegDownCaptchaLayout.findViewById(R.id.register_down_sms_captcha_view);
        mRegDownSmsCaptchaView.setContainer(mContainer);//下行验证码view
        
        mRegEmailView = (RegisterEmailView) mRegEmailLayout.findViewById(R.id.register_email);//邮箱注册view
        mRegEmailView.setContainer(mContainer);//邮箱注册view
        
        mRegEmailActiveView = (RegisterEmailActiveView) mRegEmailActiveLayout.findViewById(R.id.register_email_active_view);//邮箱注册view
        mRegEmailActiveView.setContainer(mContainer);//邮箱注册激活view
        
        mRegUpSmsView =(RegisterUpSmsView)mRegUpSmsLayout.findViewById(R.id.register_up_sms_view);
        mRegUpSmsView.setContainer(mContainer);//手机上行短信注册view

        mFindPwdByMobileView=(FindPwdByMobileView)mFindPwdByMobileLayout.findViewById(R.id.findpwd_by_mobile_view);
        mFindPwdByMobileView.setContainer(mContainer);//手机找回密码第一步 输入手机号
        
        mFindPwdByMobileCaptchaView=(FindPwdByMobileCaptchaView)mFindPwdByMobileCaptchaLayout.findViewById(R.id.findpwd_by_mobile_captcha_view);
        mFindPwdByMobileCaptchaView.setContainer(mContainer);//手机找回密码第二步 短信验证码
        
        mFindPwdByMobileSavePwdView=(FindPwdByMobileSavePwdView)mFindPwdByMobileSavePwdLayout.findViewById(R.id.findpwd_by_mobile_savePwd);
        mFindPwdByMobileSavePwdView.setContainer(mContainer);//手机找回密码第三步 保存新密码
        // 根据用户的操作类型，判断显示登录页面还是注册页面
        if ((mAddAccountType & Constant.VALUE_ADD_ACCOUNT_LOGIN) != 0) {
            mContainer.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_LOGIN_VIEW);
        }else if ((mAddAccountType & Constant.VALUE_ADD_ACCOUNT_REGISTER) != 0) {
        	//只有在单卡并且业务方配置上行短信注册方式的情况
        	if(mContainer.getIsNeedUpSmsRegister()){
        		mContainer.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_UP_SMS_VIEW);
        	}else{
        		mContainer.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_DOWN_SMS_VIEW);
        	}
        }
        mLoginView.findViewById(R.id.qihoo_accounts_top_back).setOnClickListener(this);
        mRegLayout.findViewById(R.id.qihoo_accounts_top_back).setOnClickListener(this);
        mFindPwdLayout.findViewById(R.id.qihoo_accounts_top_back).setOnClickListener(this);
        title=(TextView)mFindPwdLayout.findViewById(R.id.qihoo_accounts_top_title);
        title.setText(R.string.qihoo_accounts_findpwd_by_mobile_title);//设置找回密码title
    }

    /**
     * 控制在Activity中显示哪个view
     * @param viewType
     */
    private void showView(int viewType) {
        mLoginLayout.setVisibility(View.GONE);
        mRegLayout.setVisibility(View.GONE);
        mRegDownLayout.setVisibility(View.GONE);
        mRegDownCaptchaLayout.setVisibility(View.GONE);
        mRegEmailLayout.setVisibility(View.GONE);
        mRegEmailActiveLayout.setVisibility(View.GONE);
        mRegUpSmsLayout.setVisibility(View.GONE);
        mFindPwdLayout.setVisibility(View.GONE);
        mFindPwdByMobileLayout.setVisibility(View.GONE);
        mFindPwdByMobileCaptchaLayout.setVisibility(View.GONE);
        mFindPwdByMobileSavePwdLayout.setVisibility(View.GONE);
        switch (viewType) {
            case AddAccountsUtils.VALUE_SHOW_LOGIN_VIEW:
                mLoginLayout.setVisibility(View.VISIBLE);
                break;

            case AddAccountsUtils.VALUE_SHOW_DOWN_SMS_VIEW:
                mRegLayout.setVisibility(View.VISIBLE);//注册布局页面可见
                mRegDownLayout.setVisibility(View.VISIBLE);//手机下行注册可见
                TextView mRegisterEmailTv=((TextView) mContainer.getRegDownSmsView().findViewById(R.id.register_email_button));
      		    //业务方设置不提供邮箱注册
                if(!mContainer.getIsNeedEmailRegister()){
                	mRegisterEmailTv.setVisibility(View.GONE);
                }
                break;

            case AddAccountsUtils.VALUE_SHOW_DOWN_SMS_CAPTCHA_VIEW:
                mRegLayout.setVisibility(View.VISIBLE);//注册布局页面可见
                mRegDownCaptchaLayout.setVisibility(View.VISIBLE);//手机下行短信验证码页面可见
                break;
                
            case AddAccountsUtils.VALUE_SHOW_EMAIL_VIEW:
            	mRegLayout.setVisibility(View.VISIBLE);//注册布局页面可见
                mRegEmailLayout.setVisibility(View.VISIBLE);//邮箱注册页面可见
                break;
                
            case AddAccountsUtils.VALUE_SHOW_EMAIL_ACTIVE_VIEW:
            	mRegLayout.setVisibility(View.VISIBLE);//注册布局页面可见
                mRegEmailActiveLayout.setVisibility(View.VISIBLE);//邮箱注册激活页面可见
                break;
                
            case AddAccountsUtils.VALUE_SHOW_UP_SMS_VIEW:
            	//只有在单卡并且业务方配置上行短信注册方式的情况
            	if(mIsSingleSimCard && mContainer.getIsNeedUpSmsRegister()){
            		mRegLayout.setVisibility(View.VISIBLE);//注册布局页面可见
                    mRegUpSmsLayout.setVisibility(View.VISIBLE);//上行短信注册页面可见
            	}else{
            		mContainer.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_DOWN_SMS_VIEW);
            	}
                break;
                
            case AddAccountsUtils.VALUE_SHOW_FINDPWD_MOBILE_VIEW:
            	//手机找回密码第一步：输入手机号
            	mFindPwdLayout.setVisibility(View.VISIBLE);
            	mFindPwdByMobileLayout.setVisibility(View.VISIBLE);
            	break;
            	
            case AddAccountsUtils.VALUE_SHOW_FINDPWD_MOBILE_CAPTCHA_VIEW:
            	//手机找回密码第二步：短信验证码
            	mFindPwdLayout.setVisibility(View.VISIBLE);
            	mFindPwdByMobileCaptchaLayout.setVisibility(View.VISIBLE);
            	break;
            	
            case AddAccountsUtils.VALUE_SHOW_FINDPWD_MOBILE_SAVEPWD_VIEW:
            	//手机找回密码第三步：保存新密码
            	mFindPwdLayout.setVisibility(View.VISIBLE);
            	mFindPwdByMobileSavePwdLayout.setVisibility(View.VISIBLE);
            	break;
            	
            default:
                break;
        }
    }

    @Override
    protected void onDestroy() {
        // 关闭dialog，否则可能导致泄漏
        if (mLoginView != null) {
            mLoginView.closeDialogsOnDestroy();
        }
        if (mRegDownSmsView != null) {
            mRegDownSmsView.closeDialogsOnDestroy();
        }
        if (mRegUpSmsView != null) {
            mRegUpSmsView.closeDialogsOnDestroy();
        }
        if (mRegEmailView != null) {
            mRegEmailView.closeDialogsOnDestroy();
        }
        if (mRegDownSmsCaptchaView != null) {
            mRegDownSmsCaptchaView.closeDialogsOnDestroy();
        }
        if (mRegEmailActiveView != null) {
        	mRegEmailActiveView.closeDialogsOnDestroy();
        }
        if (mFindPwdByMobileView != null) {
        	mFindPwdByMobileView.closeDialogsOnDestroy();
        }
        if (mFindPwdByMobileCaptchaView != null) {
        	mFindPwdByMobileCaptchaView.closeDialogsOnDestroy();
        }
        if (mFindPwdByMobileSavePwdView != null) {
        	mFindPwdByMobileSavePwdView.closeDialogsOnDestroy();
        }
        AddAccountsUtils.closeDialogsOnDestroy(mCustomDialog);

        super.onDestroy();
    }

    
    /**
     * 系统返回按钮和界面顶端返回按钮的响应
     */
    private void clickBackBtn() {
        if (mRegDownCaptchaLayout.getVisibility() == View.VISIBLE) {
            showView(AddAccountsUtils.VALUE_SHOW_DOWN_SMS_VIEW);
        }else if(mRegEmailActiveLayout.getVisibility()==View.VISIBLE && ((RegisterEmailActiveView)mContainer.getRegEmailActiveView()).IsLoginNeedEmailActive()){
        	((RegisterEmailActiveView)mContainer.getRegEmailActiveView()).setLoginNeedEmailActive(false);
        	showView(AddAccountsUtils.VALUE_SHOW_LOGIN_VIEW);
        }else if(mRegEmailActiveLayout.getVisibility()==View.VISIBLE && !((RegisterEmailActiveView)mContainer.getRegEmailActiveView()).IsLoginNeedEmailActive()){
        	showView(AddAccountsUtils.VALUE_SHOW_EMAIL_VIEW);
        }else if (mRegEmailLayout.getVisibility() == View.VISIBLE) {
            showView(AddAccountsUtils.VALUE_SHOW_DOWN_SMS_VIEW);
        }else if(mRegUpSmsLayout.getVisibility()==View.VISIBLE && ((mAddAccountType & Constant.VALUE_ADD_ACCOUNT_REGISTER) != 0)){
        	onFinished();
        }else if(mRegUpSmsLayout.getVisibility()==View.VISIBLE && ((mAddAccountType & Constant.VALUE_ADD_ACCOUNT_LOGIN) != 0)){
        	showView(AddAccountsUtils.VALUE_SHOW_LOGIN_VIEW);
        }else if(mRegDownLayout.getVisibility()==View.VISIBLE && mIsSingleSimCard && mContainer.getIsNeedUpSmsRegister()){
        	showView(AddAccountsUtils.VALUE_SHOW_UP_SMS_VIEW);
        }else if(mRegDownLayout.getVisibility()==View.VISIBLE && ((mAddAccountType & Constant.VALUE_ADD_ACCOUNT_REGISTER) != 0)){
        	onFinished();
        }else if(mRegDownLayout.getVisibility()==View.VISIBLE && ((mAddAccountType & Constant.VALUE_ADD_ACCOUNT_LOGIN) != 0)){
        	showView(AddAccountsUtils.VALUE_SHOW_LOGIN_VIEW);
        }else if (mRegLayout.getVisibility() == View.VISIBLE) {
            showView(AddAccountsUtils.VALUE_SHOW_LOGIN_VIEW);
        }else if(mFindPwdByMobileSavePwdLayout.getVisibility()==View.VISIBLE){
        	showView(AddAccountsUtils.VALUE_SHOW_FINDPWD_MOBILE_CAPTCHA_VIEW);
        }else if(mFindPwdByMobileCaptchaLayout.getVisibility()==View.VISIBLE){
        	showView(AddAccountsUtils.VALUE_SHOW_FINDPWD_MOBILE_VIEW);
        }else if(mFindPwdByMobileLayout.getVisibility()==View.VISIBLE){
        	showView(AddAccountsUtils.VALUE_SHOW_LOGIN_VIEW);
        }else if(mFindPwdLayout.getVisibility()==View.VISIBLE){
        	showView(AddAccountsUtils.VALUE_SHOW_LOGIN_VIEW);
        }else if (mLoginLayout.getVisibility() == View.VISIBLE) {
            onFinished();
        }
    }

    /**
     * 供业务调用展示dialog(正在doing...)
     * @param title
     * @param msg
     */
    protected void showCustomDialog(String title, String msg) {
        mCustomDialog = AddAccountsUtils.showCustomDialog(this, title, msg);
        if (!this.isFinishing()) {
            mCustomDialog.show();
        }
    }
    
    /**
     * 在自定义UI时或手机桌面需求，在登录、注册成功后可能会有些耗时操作，先不关闭登录注册成功时的Dialog
     * 可由业务控制手动关闭dialog，
     * 如果不调用该函数则Activity销毁时自动关闭Dialog
     */
    protected void closeDialogs() {
        if (mLoginView != null) {
            mLoginView.closeLoginDialog();
        }
        if (mRegDownSmsView != null) {
            mRegDownSmsView.closeRegDialog();
        }
        if (mRegUpSmsView != null) {
            mRegUpSmsView.closeRegDialog();
        }
        if (mRegEmailView != null) {
            mRegEmailView.closeRegDialog();
        }
        if (mRegDownSmsCaptchaView != null) {
            mRegDownSmsCaptchaView.closeCommitDialog();
        }
        AddAccountsUtils.closeDialogsOnCallback(this, mCustomDialog);
    }
    
    /**
     * 当Acitivity finish时调用
     */
    protected void onFinished() {
        finish();
    }

    /**
     * 业务可重写该方法，初始化参数，从而避免intent传参方式
     */
    protected Bundle getInitParam(){
    	return null;
    }
    
    /**
     * 业务方实现,处理登录成功之后的操作
     * @param info
     */
    public abstract void handleLoginSuccess(UserTokenInfo info);

    /**
     * 业务方实现,处理注册成功之后的操作
     * @param info
     */
    public abstract void handleRegisterSuccess(UserTokenInfo info);
    
}

