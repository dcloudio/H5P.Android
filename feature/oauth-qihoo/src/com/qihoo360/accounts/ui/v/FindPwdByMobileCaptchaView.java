package com.qihoo360.accounts.ui.v;

import java.util.ArrayList;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;

import android.app.Dialog;
import android.content.Context;
import android.text.Editable;
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
import com.qihoo360.accounts.api.auth.QucRpc;
import com.qihoo360.accounts.api.auth.i.IQucRpcListener;
import com.qihoo360.accounts.api.auth.p.model.RpcResponseInfo;
import com.qihoo360.accounts.base.utils.DeviceUtils;
import com.qihoo360.accounts.ui.model.AddAccountsUtils;

/**
 * 手机"找回密码"页面 第二步：输入短信验证码
 * @author wangzefeng
 *
 */
public class FindPwdByMobileCaptchaView extends LinearLayout implements View.OnClickListener {

	private Context mContext;

    private IContainer mContainer;

    // 验证码编辑框
    private EditText mCaptchaText;

    private Button mCaptchaDeleteBtn;
    
    //获取验证码
    private Button mCaptchaClickBtn;

    // 正在重新发送对话框
    private AccountCustomDialog mSendAgainDialog;
    
    private static String method="CommonAccount.sendSmsCode";

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
    
    public FindPwdByMobileCaptchaView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public final void setContainer(IContainer container) {
        mContainer = container;
    }

    public String getCaptcha() {
		// TODO Auto-generated method stub
    	return mCaptchaText.getText().toString();
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
        if (viewId == R.id.findpwd_by_mobile_captcha_delete) {
            mCaptchaText.setText(null);
            AddAccountsUtils.setViewFocus(mCaptchaText);
            AddAccountsUtils.displaySoftInput(mContext, mCaptchaText);
        } else if (viewId == R.id.findpwd_by_mobile_captcha_commit) {
            doCommandCommitCaptcha();
        } else if (viewId == R.id.findpwd_by_mobile_captcha_send_click) {
        	doCommandSendAgain();
        } 
    }

    private void initView() {
    	mContext = getContext();
        mCaptchaText = (EditText) findViewById(R.id.findpwd_by_mobile_captcha_text);
        mCaptchaText.setOnKeyListener(onSendSmsCodeKey);
        mCaptchaDeleteBtn = (Button) findViewById(R.id.findpwd_by_mobile_captcha_delete);
        mCaptchaClickBtn=(Button) findViewById(R.id.findpwd_by_mobile_captcha_send_click);
        mCaptchaDeleteBtn.setOnClickListener(this);
        findViewById(R.id.findpwd_by_mobile_captcha_commit).setOnClickListener(this);
        findViewById(R.id.findpwd_by_mobile_captcha_send_click).setOnClickListener(this);
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
    
    private void doCommandCommitCaptcha() {
        AddAccountsUtils.hideSoftInput(mContext, mCaptchaText);
        String captcha = mCaptchaText.getText().toString();
        if (!AddAccountsUtils.isSmsCodeValid(mContext, captcha)) {
            return;
        }
        mContainer.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_FINDPWD_MOBILE_SAVEPWD_VIEW);
    }

    private boolean mSendAgainPending;

    private final IQucRpcListener mSendSmsCodeListener = new IQucRpcListener() {
		
		@Override
		public void onRpcSuccess(RpcResponseInfo rpcResInfo) {
			// TODO Auto-generated method stub
			mSendAgainPending= false;
        	closeSendDialog();
        	AddAccountsUtils.getSmsContent(mContext, mCaptchaText);//自动填充短信验证码
        	AddAccountsUtils.startCodeTimer(mContext, mCaptchaClickBtn);//启动120s倒计时
		}
		
		@Override
		public void onRpcError(int errorType, int errorCode, String errorMessage,
				RpcResponseInfo errorInfo) {
			// TODO Auto-generated method stub
			mSendAgainPending = false;
	        closeSendDialog();
	        handleSendAgainError(errorType, errorCode, errorMessage);
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
        String phone = ((FindPwdByMobileView) mContainer.getFindPwdByMobileView()).getPhone();
        QucRpc SendSmsCode = new QucRpc(mContext.getApplicationContext(),
				mContainer.getClientAuthKey(), mContainer.getLooper(),
				mSendSmsCodeListener);
        ArrayList<NameValuePair> params=new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("account", phone));
        params.add(new BasicNameValuePair("condition", "1"));
        params.add(new BasicNameValuePair("mid", DeviceUtils.getDeviceId(mContext)));
        SendSmsCode.request(method, params, null, null);
    }

    private final void handleSendAgainError(int errorType, int errorCode, String errorMessage) {
    	AddAccountsUtils.showErrorToast(mContext, AddAccountsUtils.VALUE_DIALOG_SEND_AGIAN, errorType, errorCode, errorMessage);	
    }

    public final void closeDialogsOnDestroy() {
        AddAccountsUtils.closeDialogsOnDestroy(mSendAgainDialog);
    }

    private final void closeSendDialog() {
        AddAccountsUtils.closeDialogsOnCallback(mContext, mSendAgainDialog);
    }
}
