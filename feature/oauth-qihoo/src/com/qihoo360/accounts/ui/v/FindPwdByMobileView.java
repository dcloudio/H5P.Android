
package com.qihoo360.accounts.ui.v;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

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
import android.widget.TextView;

import com.qihoo360.accounts.R;
import com.qihoo360.accounts.api.auth.QucRpc;
import com.qihoo360.accounts.api.auth.i.IQucRpcListener;
import com.qihoo360.accounts.api.auth.p.model.RpcResponseInfo;
import com.qihoo360.accounts.base.utils.DeviceUtils;
import com.qihoo360.accounts.base.utils.InputChecker;
import com.qihoo360.accounts.ui.model.AddAccountsUtils;

/**
 * 手机"找回密码"页面 第一步：输入手机号
 * @author wangzefeng
 *
 */
public class FindPwdByMobileView extends LinearLayout implements View.OnClickListener {

    private Context mContext;

    private IContainer mContainer;

    // 手机号编辑框
    private EditText mPhoneText;

    private Button mDeletePhoneBtn;
    
    private static String method="CommonAccount.sendSmsCode";

    // 正在注册对话框
    private AccountCustomDialog mSmsCodeSendingDialog;

    private final AccountCustomDialog.ITimeoutListener mDialogTimeoutListener = new AccountCustomDialog.ITimeoutListener() {

        @Override
        public void onTimeout(Dialog dialog) {
        	dialog.dismiss();
            mSendSmsPending = false;
        }
    };
    
    //手机号输入后回车键
  	private final OnKeyListener onRegisterKey = new OnKeyListener() {
  		
  		public boolean onKey(View v, int keyCode, KeyEvent event) {

  			// TODO Auto-generated method stub
  			if (keyCode == KeyEvent.KEYCODE_ENTER) {
  				AddAccountsUtils.hideSoftInput(mContext, mPhoneText);// 隐藏键盘
  				mPhoneText.setSelection(mPhoneText.getText().toString().length());// EditText设置光标在内容的最尾端
  				doSendSmsCode();
  				return true;
  			}
  			return false;
  		}
  	};

    public FindPwdByMobileView(Context context, AttributeSet attrs) {
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

    public String getPhone() {
        return mPhoneText.getText().toString();
    }
    
    public void setPhone(String phone) {
        mPhoneText.setText(phone);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initView();
        checkDownSmsPhone();
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if(viewId==R.id.findpwd_by_mobile_next){
        	doSendSmsCode();//下一步
        }else if (viewId == R.id.findpwd_by_mobile_delete_tel) {
            mPhoneText.setText(null);
            AddAccountsUtils.setViewFocus(mPhoneText);
            AddAccountsUtils.displaySoftInput(mContext, mPhoneText);
        }else if(viewId==R.id.findpwd_by_other_button){
        	String account=((LoginView)mContainer.getLoginView()).getAccount().trim();
        	if(TextUtils.isEmpty(account)){
        		account=mPhoneText.getText().toString().trim();
        	}
        	AddAccountsUtils.toFindPwdWebView(mContext, account);
		} 
    }

    private void initView() {
    	mContext = getContext();
    	//手机找回密码第一步
    	/*title=(TextView)findViewById(R.id.qihoo_accounts_top_title);
		title.setText(R.string.qihoo_accounts_findpwd_by_mobile_title);*/
        mPhoneText = (EditText) findViewById(R.id.findpwd_by_mobile_text);//输入手机号
        mPhoneText.setOnKeyListener(onRegisterKey);
        mDeletePhoneBtn = (Button) findViewById(R.id.findpwd_by_mobile_delete_tel);//删除手机号
        mDeletePhoneBtn.setOnClickListener(this);
        findViewById(R.id.findpwd_by_mobile_next).setOnClickListener(this);//下一步
        findViewById(R.id.findpwd_by_other_button).setOnClickListener(this);
        RelativeLayout phoneLayout = (RelativeLayout) findViewById(R.id.qihoo_accounts_findpwd_by_mobile_layout);
        phoneLayout.setOnTouchListener(new OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                AddAccountsUtils.setViewFocus(mPhoneText);
                AddAccountsUtils.displaySoftInput(mContext, mPhoneText);
                return false;
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

    private boolean mSendSmsPending;

    private final IQucRpcListener mSendSmsCodeListener = new IQucRpcListener() {
		
		@Override
		public void onRpcSuccess(RpcResponseInfo rpcResInfo) {
			// TODO Auto-generated method stub
			mSendSmsPending = false;
			closeSendSmsCodeDialog();
			handleSendSmsCodeSuccess(rpcResInfo);
		}
		
		@Override
		public void onRpcError(int errorType, int errorCode, String errorMessage,
				RpcResponseInfo errorInfo) {
			// TODO Auto-generated method stub
			mSendSmsPending = false;
			closeSendSmsCodeDialog();
			handleSendSmsCodeError(errorType, errorCode, errorMessage);
		}
	};

    private final void doSendSmsCode() {
        AddAccountsUtils.hideSoftInput(mContext, mPhoneText);
        if (mSendSmsPending) {
            return;
        }
        String phoneNumber = mPhoneText.getText().toString();
        if (!AddAccountsUtils.isPhoneNumberValid(mContext, phoneNumber)) {
            return;
        }
        mSendSmsPending = true;
        mSmsCodeSendingDialog = AddAccountsUtils.showDoingDialog(mContext, AddAccountsUtils.VALUE_DIALOG_SEND);
        mSmsCodeSendingDialog.setTimeoutListener(mDialogTimeoutListener);
        QucRpc SendSmsCode = new QucRpc(mContext.getApplicationContext(),
				mContainer.getClientAuthKey(), mContainer.getLooper(),
				mSendSmsCodeListener);
        ArrayList<NameValuePair> params=new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("account", phoneNumber));
        params.add(new BasicNameValuePair("condition", "1"));
        params.add(new BasicNameValuePair("mid", DeviceUtils.getDeviceId(mContext)));
        SendSmsCode.request(method, params, null, null);
    }

    private final void handleSendSmsCodeSuccess(RpcResponseInfo info) {
        //TODO 确实是否弹出注册成功Dialog
    	 View captchaView = mContainer.getFindPwdByMobileCaptchaView();
         ((TextView) captchaView.findViewById(R.id.findpwd_by_mobile_captcha_phone)).setText(mPhoneText.getText());
         EditText mCaptchaText=(EditText)captchaView.findViewById(R.id.findpwd_by_mobile_captcha_text);
         Button mCaptchaClickBtn=(Button)captchaView.findViewById(R.id.findpwd_by_mobile_captcha_send_click);
         AddAccountsUtils.getSmsContent(mContext, mCaptchaText);
         AddAccountsUtils.startCodeTimer(mContext, mCaptchaClickBtn);
         mContainer.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_FINDPWD_MOBILE_CAPTCHA_VIEW);//下一步
    }

    private final void handleSendSmsCodeError(int errorType, int errorCode, String errorMessage) {
    	if(errorCode==AddAccountsUtils.VALUE_FINDPWD_MOBILE_NOT_BIND || errorCode==AddAccountsUtils.VALUE_FINDPWD_MOBILE_NOT_EXIST){
    		errorMessage=getResources().getString(R.string.qihoo_accounts_findpwd_valid_phone);
    	}
    	AddAccountsUtils.showErrorToast(mContext, AddAccountsUtils.VALUE_DIALOG_COMMIT, errorType, errorCode, errorMessage);
    }

    public final void closeDialogsOnDestroy() {
        AddAccountsUtils.closeDialogsOnDestroy(mSmsCodeSendingDialog);
    }

    public final void closeSendSmsCodeDialog() {
        AddAccountsUtils.closeDialogsOnCallback(mContext, mSmsCodeSendingDialog);
    }
}
