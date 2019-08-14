
package com.qihoo360.accounts.ui.v;

import android.app.Dialog;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import com.qihoo360.accounts.R;
import com.qihoo360.accounts.api.auth.SendActiveEmail;
import com.qihoo360.accounts.api.auth.i.ISendActiveEmailListener;
import com.qihoo360.accounts.base.common.ErrorCode;
import com.qihoo360.accounts.ui.model.AddAccountsUtils;

/**
 * 邮箱注册激活页面
 * @author wangzefeng
 *
 */
public class RegisterEmailActiveView extends LinearLayout implements View.OnClickListener {

//    private static final String TAG = "ACCOUNT.RegisterDownSmsView";
	
	//是否是在登录场景中需要激活邮箱
    private static boolean mLoginNeedEmailActive = false;

    private Context mContext;
    
    private String mailUrl;

    private IContainer mContainer;

    private Button mEmailSubmitBtn;

    // 激活后弹窗
    private Dialog mRegActiveDialog;

    private AccountCustomDialog mActiveEmailSendingDialog;
    
    private boolean mSendingPending;
    
    private final ISendActiveEmailListener mSendActiveEmailListener = new ISendActiveEmailListener() {
		@Override
		public void onSendActiveEmail() {
			// TODO Auto-generated method stub
			mSendingPending=false;
			closeSendingDialog();
			doCommandActiveEmail();//进入收信箱激活
		}

		@Override
		public void onSendActiveEmailError(int errorType, int errorCode,
				String errorMessage) {
			// TODO Auto-generated method stub
			mSendingPending=false;
			closeSendingDialog();
			AddAccountsUtils.showErrorToast(mContext, AddAccountsUtils.VALUE_DIALOG_SEND, errorType, errorCode, errorMessage);
		}
    };
    
    private final AccountCustomDialog.ITimeoutListener mDialogTimeoutListener = new AccountCustomDialog.ITimeoutListener() {

        @Override
        public void onTimeout(Dialog dialog) {
        	dialog.dismiss();
        	mSendingPending = false;
        	
        }
    };
    
    public RegisterEmailActiveView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public final void setContainer(IContainer container) {
        mContainer = container;
    }
    
    public final boolean IsLoginNeedEmailActive() {
        return mLoginNeedEmailActive;
    }
    
    public final void setLoginNeedEmailActive(boolean mNeedEmailActive) {
        mLoginNeedEmailActive=mNeedEmailActive;
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        initView();
    }

    @Override
    public void onClick(View v) {
        int viewId = v.getId();
        if (viewId == R.id.register_email_submit) {
        	doCommandActiveEmail();
        }else if(viewId==R.id.add_accounts_dialog_error_title_icon){
        	closeActiveDialog();//关闭对话框
        }else if (viewId == R.id.add_accounts_dialog_error_cancel_btn) {
        	closeActiveDialog();//重发激活链接
        	doCommandSendActiveEmailAgain();
        }else if(viewId==R.id.add_accounts_dialog_error_ok_btn){
        	closeActiveDialog();//立即登录逻辑
        	mContainer.showAddAccountsView(AddAccountsUtils.VALUE_SHOW_LOGIN_VIEW);//登录界面      
        	//自动填充用户名和密码
        	((LoginView)mContainer.getLoginView()).setAccount(AddAccountsUtils.getEmailName(mContext));
        	((LoginView)mContainer.getLoginView()).setPsw(AddAccountsUtils.getEmailPwd(mContext));
        	AddAccountsUtils.setEmailName(mContext,"");
            AddAccountsUtils.setEmailPwd(mContext,"");
        	((LoginView)mContainer.getLoginView()).doCommandLogin();      	
        }
    }
    
    private void initView() {
    	mContext = getContext();
        mEmailSubmitBtn = (Button) findViewById(R.id.register_email_submit);
        mEmailSubmitBtn.setOnClickListener(this);
    }

    private void doCommandActiveEmail() {
    	mailUrl=AddAccountsUtils.getEmailUrl(mContext);
    	AddAccountsUtils.openEmailUrl(mContext, mailUrl);
    	mRegActiveDialog = AddAccountsUtils.showErrorDialog(mContext, this, AddAccountsUtils.VALUE_DIALOG_EMAIL_ACTIVE, ErrorCode.ERR_TYPE_APP_ERROR, ErrorCode.ERR_CODE_EAMIL_ACTIVE,"");
    }

    private final void doCommandSendActiveEmailAgain() {
    	 if (mSendingPending) {
             // 正在注册，直接返回
             return;
         }
    	mSendingPending = true;
    	mActiveEmailSendingDialog = AddAccountsUtils.showDoingDialog(mContext, AddAccountsUtils.VALUE_DIALOG_SEND);
    	mActiveEmailSendingDialog.setTimeoutListener(mDialogTimeoutListener);
        SendActiveEmail sendActiveEmail = new SendActiveEmail(mContext.getApplicationContext(), mContainer.getClientAuthKey(), mContainer.getLooper(), mSendActiveEmailListener);
        //重新发送激活邮件
        sendActiveEmail.request(AddAccountsUtils.getEmailName(mContext),"");   
    }
    
    private final void closeActiveDialog() {
        AddAccountsUtils.closeDialogsOnCallback(mContext, mRegActiveDialog);
    }
    private final void closeSendingDialog() {
        AddAccountsUtils.closeDialogsOnCallback(mContext, mActiveEmailSendingDialog);
    }
    
    public final void closeDialogsOnDestroy() {
        AddAccountsUtils.closeDialogsOnDestroy(mRegActiveDialog);
        AddAccountsUtils.closeDialogsOnDestroy(mActiveEmailSendingDialog);
    }
}
