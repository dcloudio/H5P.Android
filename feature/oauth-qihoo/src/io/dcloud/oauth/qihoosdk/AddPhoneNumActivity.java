package io.dcloud.oauth.qihoosdk;

import io.dcloud.common.adapter.util.Logger;

import java.util.regex.Pattern;

import org.json.JSONObject;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.ResultReceiver;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.qihoo360.accounts.QihooR;
import com.qihoo360.accounts.R;

/**
 * create by caiyingyuan on 2015/09/08
 * 生活助手，绑定手机号码
 * */
public class AddPhoneNumActivity extends Activity implements View.OnClickListener {

    private static final String TAG = AddPhoneNumActivity.class.getSimpleName();
    private static final int HANDLER_GET_VERITY_CODE_RESULT = 1000;
    private static final int HANDLER_SET_PHONENUM_RESULT = 1001;
    public static final String BUNDLE_FINISH_CALLBACK = "finish_callback";

//    private SecondaryToolbar mToolbar;
    private Context mContext;
    private EditText mPhoneNum;
    private EditText mVerityCode;
    private Button mBtnVC;
    private Button mSubmit;

    private ResultReceiver mResultRcrv;
    private Pattern PhonePattern = Pattern.compile("[1]\\d{10}"); //手机号码格式正则表达式


    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            JSONObject obj = (JSONObject) msg.obj;
            int errorNo = obj.optInt("errno", LifeHelper.OnResult.CODE_UNKNOW);
            String errmsg = obj.optString("errmsg", null);
            String data = obj.optString("data", null);
            String exception = obj.optString("exception", null);
            if (errmsg == null && exception != null) {
                errmsg = exception;
            }

            switch (msg.what) {
                case HANDLER_GET_VERITY_CODE_RESULT:
                    if(errorNo == LifeHelper.OnResult.CODE_OK){
                        mPhoneNum.setEnabled(false);
                        mBtnVC.setClickable(false);
                        mBtnVC.setEnabled(false);
//                        mBtnVC.setTextColor(Color.GRAY);
                        //mBtnVC.setBackgroundResource(QihooR.DRAWABLE_BTN_INPUT_PHONE_CANCEL_BG);
                        mVerityCode.setEnabled(true);
                        getCountDownTimer(mBtnVC, mContext);
                    }else{
                        mPhoneNum.setEnabled(true);
                    }
                    Toast.makeText(mContext, errorNo == LifeHelper.OnResult.CODE_OK ? getString(QihooR.STRING_LIFE_SEND_VERIFY_CODE_SUCCESS) : errmsg, Toast.LENGTH_SHORT).show();
                    break;
                case HANDLER_SET_PHONENUM_RESULT:
                    if (errorNo == LifeHelper.OnResult.CODE_OK) {
                        Toast.makeText(mContext, getString(QihooR.STRING_LIFE_ADD_PHONE_SUCCESS), Toast.LENGTH_SHORT).show();
                        onBack(true);
                    } else {
                        Toast.makeText(mContext,errmsg, Toast.LENGTH_SHORT).show();
                    }
                    break;
            }

        }
    };

    /**
     * @param isSuccess true/false；
     *                  true：resultCode返回0，表示绑定手机号码成功；
     *                  false:resultCode返回-1，表示绑定手机号码失败
     */
    private void onBack(boolean isSuccess) {
        if (mResultRcrv != null) {
            mResultRcrv.send(isSuccess ? LifeHelper.OnResult.CODE_OK : LifeHelper.OnResult.CODE_UNKNOW, null);
        }
        finish();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(QihooR.LAYOUT_ACTIVITY_ADD_PHONE_NUM);
        mContext = getApplicationContext();
//        mToolbar = (SecondaryToolbar) findViewById(QihooR.ID_TOOLBAR);
//        mToolbar.setLeftViewBackground(AppSkinHelper.getReformDrawable(this, QihooR.DRAWABLE_COMMON_TOOBAR_ICON_BACK_LAYER));
//        mToolbar.setRightViewVisibility(View.GONE);
//        mToolbar.setTitleViewText(getString(QihooR.STRING_LIFE_ADD_PHONE_TITLE));
//        mToolbar.setListener(new SecondaryToolbar.OnToolbarListener() {
//
//            @Override
//            public void onToolbarClick(int id) {
//                switch (id) {
//                    case QihooR.ID_BTN_LEFT:
//                        finish();
//                        break;
//                    default:
//                        break;
//                }
//            }
//        });
        mPhoneNum = (EditText) findViewById(QihooR.ID_INPUT_PHONE_NUM);
        mVerityCode = (EditText) findViewById(QihooR.ID_INPUT_VERIFY_CODE);
        mBtnVC = (Button)findViewById(QihooR.ID_BTN_VERIFY_CODE);
        mSubmit = (Button)findViewById(QihooR.ID_SUBMIT);

        mPhoneNum.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if(s.length() == 11){
                    mBtnVC.setEnabled(true);
                }else{
                    mBtnVC.setEnabled(false);
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });
        mVerityCode.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() > 0) {
                    mSubmit.setEnabled(true);
//                    mSubmit.setBackgroundResource(QihooR.DRAWABLE_NEW_UI_BTN_BG);
                }else{
                    mSubmit.setEnabled(false);
//                    mSubmit.setBackgroundResource(QihooR.DRAWABLE_BTN_INPUT_PHONE_CANCEL_BG);
                }

            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        mBtnVC.setOnClickListener(this);
        mSubmit.setOnClickListener(this);
        findViewById(QihooR.ID_CANCEL).setOnClickListener(this);
        setTitle(QihooR.STRING_LIFE_ADD_PHONE_TITLE);

        if (getIntent().getExtras() != null)
            mResultRcrv = getIntent().getParcelableExtra(BUNDLE_FINISH_CALLBACK);
    }

    @Override
    public void onClick(View v) {
        String phoneNum = mPhoneNum.getText().toString().trim();
        switch (v.getId()) {
            case QihooR.ID_BTN_VERIFY_CODE:
                if (phoneNum.isEmpty()) {
                    Toast.makeText(this, QihooR.STRING_LIFE_PHONE_EMPTY, Toast.LENGTH_SHORT).show();
                }else if (phoneNum.length() < 11 || !PhonePattern.matcher(phoneNum).matches()) {
                    Toast.makeText(this, QihooR.STRING_LIFE_INVALID_PHONE, Toast.LENGTH_SHORT).show();
                } else {
                    final ProgressDialog loadingDlg = new ProgressDialog(AddPhoneNumActivity.this, AlertDialog.THEME_HOLO_LIGHT);
                    loadingDlg.setMessage(getString(QihooR.STRING_LIFE_LOADING_GET_VALID));
                    loadingDlg.setCancelable(true);
                    loadingDlg.setCanceledOnTouchOutside(false);
                    loadingDlg.show();
                    LifeHelper.getInstance().getMobileVerityCode(this, phoneNum, new LifeHelper.OnResult() {
                        @Override
                        public void onResult(int resultCode, String resultMsg, JSONObject resultData) {
                            Logger.d(TAG, "getVerityCode---->" + " resultcode:" + resultCode + "  resultMsg:" + resultMsg + "  resultData:" + resultData);
                            loadingDlg.cancel();
                            Message msg = new Message();
                            msg.what = HANDLER_GET_VERITY_CODE_RESULT;
                            msg.obj = resultData;
                            mHandler.sendMessage(msg);
                        }
                    });
                }

                break;
            case QihooR.ID_CANCEL:
                onBack(false);
                break;
            case QihooR.ID_SUBMIT:
                String vc = mVerityCode.getText().toString().trim();
                if (vc.isEmpty()) {
                    Toast.makeText(this, QihooR.STRING_LIFE_VERIFY_CODE_EMPTY, Toast.LENGTH_SHORT).show();
                } else {
                    final ProgressDialog loadingDlg = ProgressDialog.show(AddPhoneNumActivity.this, "", getString(R.STRING_LIFE_BIND_PHONE_NUM));
                    LifeHelper.getInstance().bindPhoneNum(this, phoneNum, vc, new LifeHelper.OnResult() {
                        @Override
                        public void onResult(int resultCode, String resultMsg, JSONObject resultData) {
                            Logger.d(TAG, "bindPhoneNum---->" + " resultcode:" + resultCode + "  resultMsg:" + resultMsg + "  resultData:" + resultData);

                            loadingDlg.cancel();
                            Message msg = new Message();
                            msg.what = HANDLER_SET_PHONENUM_RESULT;
                            msg.obj = resultData;
                            mHandler.sendMessage(msg);
                        }
                    });
                }
                break;
        }
    }

    /**
     * 倒计时获取验证码间隔
     *
     * @return
     * */
    public void getCountDownTimer(final Button btn, final Context mContext) {
        // 倒计时
        CountDownTimer timer = new CountDownTimer(60000, 1000) {

            @Override
            public void onTick(long millisUntilFinished) {
                btn.setText((millisUntilFinished / 1000) + "S");
            }

            @Override
            public void onFinish() {
                mPhoneNum.setEnabled(true);
                btn.setClickable(true);
                btn.setEnabled(true);
                btn.setText(QihooR.STRING_LIFE_GET_VERIFY_CODE);
//                btn.setTextColor(0x66ccff);
//                btn.setBackgroundResource(QihooR.DRAWABLE_NEW_UI_BTN_BG);
            }
        };

        timer.start();
    }

    @Override
    public boolean onKeyDown(int arg0, KeyEvent arg1) {
        if (arg0 == KeyEvent.KEYCODE_BACK) {
            onBack(false);
            return true;
        } else {
            return super.onKeyDown(arg0, arg1);
        }
    }

//    @Override
//    protected boolean composeByFragment() {
//        return false;
//    }
//
//    @Override
//    protected String getPageField() {
//        return null;
//    }
}
