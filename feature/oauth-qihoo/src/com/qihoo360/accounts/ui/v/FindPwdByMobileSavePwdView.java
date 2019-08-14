
package com.qihoo360.accounts.ui.v;

import java.util.ArrayList;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONObject;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.qihoo360.accounts.R;
import com.qihoo360.accounts.api.CoreConstant;
import com.qihoo360.accounts.api.auth.QucRpc;
import com.qihoo360.accounts.api.auth.i.IQucRpcListener;
import com.qihoo360.accounts.api.auth.model.UserTokenInfo;
import com.qihoo360.accounts.api.auth.p.model.RpcResponseInfo;
import com.qihoo360.accounts.base.common.ErrorCode;
import com.qihoo360.accounts.base.utils.MD5Util;
import com.qihoo360.accounts.ui.model.AddAccountsUtils;

/**
 * 手机"找回密码"页面 第三步：保存新密码
 * @author wangzefeng
 *
 */
public class FindPwdByMobileSavePwdView extends LinearLayout implements View.OnClickListener {

	private Context mContext;

    private IContainer mContainer;

    // 密码编辑框
    private EditText mPswText;

    private Button mDeletePswBtn;

    private Button mShowPswBtn;

    //找回密码默认显示输入密码
    private static Boolean mIsShowPsw = true;
    
    private String phoneNumber;
    
    private static String method="CommonAccount.findAccountPwd";
    
    private static String mAutoLogin="1";//找回密码是否自动登录
    
    private static String mSecType="data";//找回密码自动登录，获取用户密保信息
    
    private static String mResDataKey="user";//服务端接口返回user字段

    // 正在保存新密码对话框
    private AccountCustomDialog mSavingPwdDialog;

    private final AccountCustomDialog.ITimeoutListener mDialogTimeoutListener = new AccountCustomDialog.ITimeoutListener() {

        @Override
        public void onTimeout(Dialog dialog) {
        	dialog.dismiss();
            mSendSmsPending = false;
        }
    };
    
    //密码输入后回车键
  	private final OnKeyListener onFindPwdKey = new OnKeyListener() {
  		
  		public boolean onKey(View v, int keyCode, KeyEvent event) {

  			// TODO Auto-generated method stub
  			if (keyCode == KeyEvent.KEYCODE_ENTER) {
  				AddAccountsUtils.hideSoftInput(mContext, mPswText);// 隐藏键盘
  				mPswText.setSelection(mPswText.getText().toString().length());// EditText设置光标在内容的最尾端
  				doSavePwd();
  				return true;
  			}
  			return false;
  		}
  	};

    public FindPwdByMobileSavePwdView(Context context, AttributeSet attrs) {
 		super(context, attrs);
 		// TODO Auto-generated constructor stub
 	}

    public final void setContainer(IContainer container) {
        mContainer = container;
    }

    public String getPsw() {
        return mPswText.getText().toString();
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
        if(viewId==R.id.findpwd_by_mobile_savePwd_click){
        	doSavePwd();//保存新密码
        }else if (viewId == R.id.findpwd_by_mobile_savePwd_delete_password) {
            mPswText.setText(null);
            AddAccountsUtils.setViewFocus(mPswText);
            AddAccountsUtils.displaySoftInput(mContext, mPswText);
        }else if (viewId == R.id.findpwd_by_mobile_savePwd_show_password) {
            mIsShowPsw = !mIsShowPsw;
            OnPwdChange();
            // 修改 EditText 中的光标位置，把光标移动到文本后面
            mPswText.setSelection(mPswText.getText().toString().length());
        }
    }

    private void initView() {
    	mContext = getContext();
        mPswText = (EditText) findViewById(R.id.findpwd_by_mobile_savePwd_passwd_input);
        mPswText.setOnKeyListener(onFindPwdKey);
        findViewById(R.id.findpwd_by_mobile_savePwd_click).setOnClickListener(this);
        mShowPswBtn = (Button) findViewById(R.id.findpwd_by_mobile_savePwd_show_password);
        mShowPswBtn.setOnClickListener(this);
        mDeletePswBtn = (Button) findViewById(R.id.findpwd_by_mobile_savePwd_delete_password);
        mDeletePswBtn.setOnClickListener(this);

        OnPwdChange();
        RelativeLayout pswLayout = (RelativeLayout) findViewById(R.id.findpwd_by_mobile_savePwd_psw_layout);
        pswLayout.setOnTouchListener(new OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                AddAccountsUtils.setViewFocus(mPswText);
                AddAccountsUtils.displaySoftInput(mContext, mPswText);
                return false;
            }
        });
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

    private boolean mSendSmsPending;

    private final IQucRpcListener mFindPwdListener = new IQucRpcListener() {
		
		@Override
		public void onRpcSuccess(RpcResponseInfo rpcResInfo) {
			// TODO Auto-generated method stub
			mSendSmsPending = false;
			closeSavePwdDialog();
			handleSavePwdSuccess(rpcResInfo);
		}
		
		@Override
		public void onRpcError(int errorType, int errorCode, String errorMessage,
				RpcResponseInfo errorInfo) {
			// TODO Auto-generated method stub
			mSendSmsPending = false;
			closeSavePwdDialog();
			handleSavePwdError(errorType, errorCode, errorMessage);
		}
	};

    private final void doSavePwd() {
        AddAccountsUtils.hideSoftInput(mContext, mPswText);
        if (mSendSmsPending) {
            return;
        }
        phoneNumber=((FindPwdByMobileView)mContainer.getFindPwdByMobileView()).getPhone();
        String password = mPswText.getText().toString();
        String captcha=((FindPwdByMobileCaptchaView)mContainer.getFindPwdByMobileCaptchaView()).getCaptcha();
        if (!AddAccountsUtils.isPhoneNumberValid(mContext, phoneNumber)) {
            return;
        }
        if (!AddAccountsUtils.isPasswordValid(mContext, password)) {
            return;
        }
        if (!AddAccountsUtils.isCaptchaValid(mContext, captcha)) {
            return;
        }
        mSendSmsPending = true;
        mSavingPwdDialog = AddAccountsUtils.showDoingDialog(mContext, AddAccountsUtils.VALUE_DIALOG_SEND);
        mSavingPwdDialog.setTimeoutListener(mDialogTimeoutListener);
        QucRpc SendSmsCode = new QucRpc(mContext.getApplicationContext(),
				mContainer.getClientAuthKey(), mContainer.getLooper(),
				mFindPwdListener);
        ArrayList<NameValuePair> params=new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("account", phoneNumber));
        params.add(new BasicNameValuePair("smscode",captcha));
        params.add(new BasicNameValuePair("newpwd",MD5Util.getMD5code(password)));
        params.add(new BasicNameValuePair("autoLogin",mAutoLogin));//需要自动登录
        params.add(new BasicNameValuePair("sec_type", mSecType));
        SendSmsCode.request(method, params, null, null, null, mResDataKey);
    }

    private final void handleSavePwdSuccess(RpcResponseInfo rpcResInfo) {
        //TODO 确实是否弹出注册成功Dialog
    	UserTokenInfo userTokenInfo=handleFindPwdResult(phoneNumber, rpcResInfo);
    	if(userTokenInfo==null){
    		return;
    	}
        AddAccountsUtils.addAccount(mContainer, mContext, userTokenInfo);
        mContainer.loginListener().onLoginSuccess(userTokenInfo);
    }
    
    private final UserTokenInfo handleFindPwdResult(String account,RpcResponseInfo userinfo){
        UserTokenInfo info = new UserTokenInfo();
        JSONObject user=userinfo.getJsonObject();
        Map<String, String> cookies=userinfo.getCookies();
        String q = cookies != null && cookies.containsKey(CoreConstant.PARAM_Q) ? cookies.get(CoreConstant.PARAM_Q) : "";
        String t = cookies != null && cookies.containsKey(CoreConstant.PARAM_T) ? cookies.get(CoreConstant.PARAM_T) : "";
        if (TextUtils.isEmpty(q) || TextUtils.isEmpty(t)) {     
            q = user.optString("q");
            t = user.optString("t");
            if (TextUtils.isEmpty(q) || TextUtils.isEmpty(t)) {
            	handleSavePwdError(ErrorCode.ERR_TYPE_APP_ERROR, ErrorCode.ERR_CODE_BAD_SERVER_DATA,null);     
            	return null;
            }
        }
        info.u = account;//用于显示的账号
        info.qid = user.optString("qid");
        info.mUsername = user.optString("username");
        info.mLoginEmail = user.optString("loginemail");
        info.q = q;
        info.t = t;
        // 更多字段
        info.mNickname =  user.optString("nickname");
        info.mAvatorFlag = user.optInt("head_flag") != 0 ? true : false;
        info.mAvatorUrl = user.optString("head_pic");
        info.mSecPhoneZone =  user.optJSONObject("secmobile").optString("zone");
        info.mSecPhoneNumber =  user.optJSONObject("secmobile").optString("number");
        info.mSecEmail =  user.optString("secemail");
        
        info.orgInfo = user;
        
        return info;
    }

    private final void handleSavePwdError(int errorType, int errorCode, String errorMessage) {
    	AddAccountsUtils.showErrorToast(mContext, AddAccountsUtils.VALUE_DIALOG_SEND, errorType, errorCode, errorMessage);
    	if(errorCode==AddAccountsUtils.VALUE_SMS_CODE_ERROR){
    		mContainer.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_FINDPWD_MOBILE_CAPTCHA_VIEW);
    	}
    }

    public final void closeDialogsOnDestroy() {
        AddAccountsUtils.closeDialogsOnDestroy(mSavingPwdDialog);
    }

    public final void closeSavePwdDialog() {
        AddAccountsUtils.closeDialogsOnCallback(mContext, mSavingPwdDialog);
    }
}
