package com.qihoo360.accounts.ui.a;

import static com.qihoo360.accounts.base.env.BuildEnv.LOGE_ENABLED;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.qihoo360.accounts.QihooAccount;
import com.qihoo360.accounts.R;
import com.qihoo360.accounts.api.auth.RefreshUser;
import com.qihoo360.accounts.api.auth.i.IRefreshListener;
import com.qihoo360.accounts.api.auth.model.UserTokenInfo;
import com.qihoo360.accounts.api.auth.p.ClientAuthKey;
import com.qihoo360.accounts.base.common.Constant;
import com.qihoo360.accounts.ui.model.AddAccountsUtils;
import com.qihoo360.accounts.ui.v.AccountCustomDialog;

/**
 * sso登录页面
 * @author wangzefeng
 *
 */
public abstract class SelectAccountActivity extends Activity {

	private AccountsAdapter mAdapter;
	private RefreshUser mRefreshUser;
	private ClientAuthKey mAuthKey;
	private Context mContext;
	private boolean mLoginPending;
	//SSO登录类型：同步校验QT/非同步
    private int mSsoLoginType;
    //正在登录对话框
 	public AccountCustomDialog mLoginingDialog;
 	private boolean syncSso =false;
 	private String ssoTag ="1" ;
 	
	public static final String KEY_ACCOUNTS = "accounts";
    public static final int RESULT_CODE_PICK_OK = 1;
    public static final int RESULT_CODE_CANCEL = 2;
    public static final String KEY_SELECTED_ACCOUNT = "selected_account";
    public static final String TAG = "SelectAccountActivity";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.qihoo_accounts_select_account_activity);
		mContext = this;
		initParam();
		initAccountList();
		setupButtons();
		
	}
	
	private final void initParam() {
        Bundle initBundle = getInitParam();
        if(initBundle == null || initBundle.isEmpty()){
        	Intent intent = getIntent();
        	if(intent != null){
        		initBundle = getIntent().getExtras();
        	}
        }
        //SSO登录类型：同步检测QT or not, 默认同步检测
        mSsoLoginType = initBundle.getInt(Constant.KEY_SSO_LOGIN_TYPE,  Constant.VALUE_SSO_LOGIN_SYNC_QT);
        if((mSsoLoginType & Constant.VALUE_SSO_LOGIN_SYNC_QT) != 0){
        	syncSso=true;//同步登录方式
        	ssoTag="2";
        }else {
        	syncSso=false;//非同步登录方式
        	ssoTag="1";
		}
		// 传递的from参数，由用户中心分配给业务
		String from = initBundle.getString(Constant.KEY_CLIENT_AUTH_FROM);
		// 传递的签名密钥，由用户中心分配给业务
		String sigKey = initBundle.getString(Constant.KEY_CLIENT_AUTH_SIGN_KEY);
		// 传递的url加密DES密钥
		String crpytKey = initBundle.getString(Constant.KEY_CLIENT_AUTH_CRYPT_KEY);
		// 客户端标识/加密/认证信息，由用户中心服务器颁发
		mAuthKey = new ClientAuthKey(from, sigKey, crpytKey);
        
    }
	
	/**
     * 业务可重写该方法，初始化参数，从而避免intent传参方式
     */
    protected Bundle getInitParam(){
    	return null;
    }	
    
	private void initAccountList(){
		ArrayList<QihooAccount> qihooAccounts = getAccountToShow();
		if(qihooAccounts == null || qihooAccounts.isEmpty()){
			Intent intent = getIntent();
			Parcelable[] accounts = intent.getParcelableArrayExtra(KEY_ACCOUNTS);
			if (accounts != null) {
				qihooAccounts = new ArrayList<QihooAccount>();
				for (int i = 0; i < accounts.length; i++) {
					qihooAccounts.add((QihooAccount) accounts[i]);
				}
				
			}
		}
		setData(qihooAccounts);
	}
	
	private void setupButtons(){
		Button loginOtherBt = (Button) findViewById(R.id.login_other_bt);
		loginOtherBt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	handle2Login();	
            }

        });
		
		Button registerBt  = (Button) findViewById(R.id.register_bt);
		registerBt.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
            	handle2register();
            }

        });

	}


	private void setData(ArrayList<QihooAccount> qihooAccounts) {
		ListView list = (ListView) findViewById(R.id.select_account_list);
		//条数过多的时候， 设置listview的weight为1， 避免挤出登录button
		if(qihooAccounts != null && qihooAccounts.size() >= 3 ){
			list.setLayoutParams(new LinearLayout.LayoutParams(list.getLayoutParams().width, list.getLayoutParams().height, 1.0f));
		}
		list.setVisibility(View.VISIBLE);
		mAdapter = new AccountsAdapter(qihooAccounts);
		list.setAdapter(mAdapter);
		list.setOnItemClickListener(mAdapter);
	}
	
	protected ArrayList<QihooAccount> getAccountToShow() {
//		QihooAccount[] accounts= new QihooAccount[8];
//		accounts[0] = new QihooAccount("360安仔", "1", "q", "t", true, null);
//		accounts[1] = new QihooAccount("13264134814", "2", "q", "t", true, null);
//		accounts[2] = new QihooAccount("13264134814", "2", "q", "t", true, null);
//		accounts[3] = new QihooAccount("13264134814", "2", "q", "t", true, null);
//		accounts[4] = new QihooAccount("13264134814", "2", "q", "t", true, null);
//		accounts[5] = new QihooAccount("13264134814", "2", "q", "t", true, null);
//		accounts[6] = new QihooAccount("13264134814", "2", "q", "t", true, null);
//		accounts[7] = new QihooAccount("13264134814", "2", "q", "t", true, null);
//		return accounts;
		return null;
	}
	
	private void handleAccountSelectedPre(QihooAccount selectedAccount) {	
		checkQTVaild(selectedAccount);
		if(!syncSso){
			handleAccountSelected(selectedAccount);
			Intent intent = new Intent();
			intent.putExtra(KEY_SELECTED_ACCOUNT, selectedAccount);
			setResult(RESULT_CODE_PICK_OK, intent);
			finish();
		}
	}
	
	

	protected void handleAccountSelected(QihooAccount selectedAccount) {	
	}
	
	protected abstract void handle2register() ;
	
	protected abstract void handle2Login() ;
	
	private class AccountsAdapter extends BaseAdapter implements
			OnItemClickListener {

		public ArrayList<QihooAccount> mExistAccounts;

		public AccountsAdapter(ArrayList<QihooAccount> accounts) {
			mExistAccounts = accounts;
		}
		
		public void removeAccount(QihooAccount account) {
			mExistAccounts.remove(account);
			notifyDataSetChanged();
		}
		

		@Override
		public int getCount() {
			if (mExistAccounts != null) {
				return mExistAccounts.size();
			}
			return 0;
		}

		@Override
		public Object getItem(int position) {
			if (mExistAccounts != null && position < mExistAccounts.size()) {
				return mExistAccounts.get(position);
			}
			return null;
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			ViewHolder holder;
			if (convertView == null || convertView.getTag() == null) {
				convertView = View.inflate(getApplicationContext(),
						R.layout.qihoo_accounts_select_account_item, null);
				holder = new ViewHolder();
				holder.mUserName = (TextView) convertView
						.findViewById(R.id.select_item_username_textview);
				convertView.setTag(holder);
			} else {
				holder = (ViewHolder) convertView.getTag();
			}

			holder.mUserName.setText(mExistAccounts.get(position).getAccount());
			return convertView;
		}

		@Override
		public void onItemClick(AdapterView<?> listView, View itemView,
				int position, long id) {
			QihooAccount selectedAccount = mExistAccounts.get(position);
			handleAccountSelectedPre(selectedAccount);
		}
	}

	private static final class ViewHolder {
		TextView mUserName;
	}
	
	/**
	 * 校验QT回调
	 *
	 */
	private class RefreshListener implements IRefreshListener {
		private QihooAccount account;
		
		public RefreshListener(QihooAccount account) {
			this.account = account;
		}

		@Override
		public void onRefreshSuccess(UserTokenInfo info) {
			if (info != null) {
				if (LOGE_ENABLED) {
					Log.e(TAG, "[onRefreshSuccess] data:" + info.toString());
				}
				if(syncSso){
					mLoginPending = false;
					closeLoginDialog();
					handleAccountSelected(info.toQihooAccount());
					Intent intent = new Intent();
					intent.putExtra(KEY_SELECTED_ACCOUNT, info.toQihooAccount());
					setResult(RESULT_CODE_PICK_OK, intent);
					finish();
				}
			}
		}

		@Override
		public void onRefreshError(int errorType, int errorCode,
				String errorMessage) {
			if (LOGE_ENABLED) {
				Log.e(TAG, "[onRefreshError] errorType:" + errorType
						+ " errorCode:" + errorCode + " errorMessage:"
						+ errorMessage);
			}
			if(syncSso){
				mLoginPending = false;
				closeLoginDialog();
				AddAccountsUtils.showErrorToast(mContext,
						AddAccountsUtils.VALUE_DIALOG_LOGIN, errorType, errorCode,
						errorMessage);
			}
		}

		@Override
		public void onInvalidQT(String errorMessage) {
			if (LOGE_ENABLED) {
				Log.e(TAG, "[onInvalidQT] errorMessage:" + errorMessage);
			}
			if(syncSso){
				mLoginPending = false;
				closeLoginDialog();
				Toast.makeText(mContext,errorMessage,Toast.LENGTH_SHORT).show();
				if(mAdapter.getCount() <= 1){
					handle2Login();
				}else {
					mAdapter.removeAccount(this.account);
				}
			}
		}

	};
	
	/**
	 * 校验QT
	 * @param accounts
	 */
	private final void checkQTVaild(final QihooAccount accounts) {
		if(syncSso){
			if (mLoginPending) {
				// 正在登录，直接返回
				return;
			}
			mLoginPending = true;
			mLoginingDialog = AddAccountsUtils.showDoingDialog(mContext,
					AddAccountsUtils.VALUE_DIALOG_LOGIN);
		}
		if (accounts != null) {
			mRefreshUser = new RefreshUser(mContext, mAuthKey,
					getMainLooper(), new RefreshListener(accounts));
			mRefreshUser.setSsoTag(ssoTag);
			mRefreshUser.refresh(accounts.mAccount, accounts.mQ,
					accounts.mT, null, null);
		}
		return;
	}
	
	public final void closeLoginDialog() {
		AddAccountsUtils.closeDialogsOnCallback(mContext, mLoginingDialog);
	}
}
