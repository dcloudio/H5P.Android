
package com.qihoo360.accounts.ui.v;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.view.View;

/**
 * 自定义Dialog，设置超时时间30秒后自动关闭
 *
 * @author wangzefeng
 *
 */
public class AccountCustomDialog extends Dialog {
    private int mTimeout = 30 * 1000;

    private final Context mContext;

    private View mContentView = null;

    private ITimeoutListener mListener = null;

    public AccountCustomDialog(Context context, int theme) {
        super(context, theme);
        mContext = context;
    }

    @Override
    public void setContentView(View view) {
        super.setContentView(view);
        mContentView = view;
        mContentView.postDelayed(mRunnable, mTimeout);
    }

    public void setTimeoutListener(ITimeoutListener listener) {
        mListener = listener;
    }

    public void setTimeout(int timeout) {
        mTimeout = timeout;
    }

    public void removeTimeoutDetecter() {
        mContentView.removeCallbacks(mRunnable);
    }

    private final Runnable mRunnable = new Runnable() {

        @Override
        public void run() {
            if (!((Activity) mContext).isFinishing() && AccountCustomDialog.this.isShowing()) {
                AccountCustomDialog.this.dismiss();
            }
            if (mListener != null) {
                mListener.onTimeout(AccountCustomDialog.this);
            }
        }
    };

    public static interface ITimeoutListener {
        void onTimeout(Dialog dialog);
    }
}
