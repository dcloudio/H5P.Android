package com.qihoo360.accounts.ui.a;

import static com.qihoo360.accounts.base.env.BuildEnv.LOGE_ENABLED;

import java.lang.reflect.Method;
import java.util.HashMap;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.qihoo360.accounts.R;
import com.qihoo360.accounts.ui.model.AddAccountsUtils;

/**
 * webview界面 用于“找回密码”和“用户注册协议”等
 * 
 * @author wangzefeng
 * 
 */
public class WebViewActivity extends Activity implements View.OnClickListener {
	private static final String TAG = "ACCOUNT.WebViewActivity";
	
    public static final String KEY_URL = "url";
    public static final String KEY_TITILE = "title";
    public static final String KEY_COOKIE_Q = "Q";
    public static final String KEY_COOKIE_T = "T";
    public static final String KEY_QID = "qid";
    public static final int RESULT_CODE_OPT_OK = 1;
    public static final String SUCC_CB_URL = "qucsdk://";
    
    private String mTitle = "";
    private String mQ = "";
    private String mT = "";
    private String mQid = "";
    private String mUrl = "";

    private WebView webView;

    private TextView titleTv;

    private ImageView rotateImageView;

    private Button closeBtn;

    private static final String QIHOO_URL = ".360.cn";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // TODO Auto-generated method stub
        super.onCreate(savedInstanceState);
        setContentView(R.layout.qihoo_accounts_webview_activity);
        initView();
        initParam(getIntent());
    }

    private void initView() {
        webView = (WebView) findViewById(R.id.web_view);
        titleTv = (TextView) findViewById(R.id.qihoo_accounts_webview_top_title);
        rotateImageView = (ImageView) findViewById(R.id.webview_rotate_image);
        closeBtn = (Button) findViewById(R.id.webview_top_close);
        closeBtn.setOnClickListener(this);
        findViewById(R.id.webview_top_back).setOnClickListener(this);
    }

    private final void initParam(Intent intent) {
        if (intent == null) {
            return;
        }

        mTitle = intent.getStringExtra(KEY_TITILE);
        mUrl = intent.getStringExtra(KEY_URL);
        mQ = intent.getStringExtra(KEY_COOKIE_Q);
        mT = intent.getStringExtra(KEY_COOKIE_T);
        mQid = intent.getStringExtra(KEY_QID);

        titleTv.setText(mTitle);
        showWebView(mUrl, mQ, mT);
    }

    @Override
    public void onClick(View v) {
        // TODO Auto-generated method stub
        int viewId = v.getId();
        if (viewId == R.id.webview_top_back) {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                finish();
            }
        } else if (viewId == R.id.webview_top_close) {
            finish();
        }
    }

    @Override
    protected void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        finish();
    }

    /**
     * 显示WebView
     */
    @SuppressLint("SetJavaScriptEnabled")
	private void showWebView(String url, String Q, String T) {
        webView.requestFocusFromTouch();
        webView.getSettings().setJavaScriptEnabled(true);// 必不可少
        webView.getSettings().setSavePassword(false);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setSupportZoom(true); // 支持缩放
        webView.getSettings().setBuiltInZoomControls(true); // 显示放大缩小
        // webView.getSettings().setRenderPriority(RenderPriority.HIGH);//提高渲染的优先级
        // webView.setInitialScale(130); //初始化时缩放
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);// 解决缓存问题
        webView.getSettings().setDefaultTextEncodingName("utf-8");
        webView.setWebViewClient(new MyWebViewClient());
        setQTCookie(url, Q, T);// 通过设置QT同步登陆状态
        webView.loadUrl(url);
    }

    private void setQTCookie(String url, String Q, String T) {
        if (TextUtils.isEmpty(Q) || TextUtils.isEmpty(T)) {
            return;
        }

        CookieSyncManager.createInstance(this);
        CookieManager cookieManager = CookieManager.getInstance();
        cookieManager.setAcceptCookie(true);
        cookieManager.setCookie(url, "T=" + T + ";path=/; domain=360.cn; httponly");// cookies是在HttpClient中获得的cookie
        cookieManager.setCookie(url, "Q=" + Q + ";path=/;domain=360.cn");
        CookieSyncManager.getInstance().sync();
    }

    private HashMap<String, String> getQTCookie() {
        HashMap<String, String> ret = new HashMap<String, String>();
        CookieSyncManager.createInstance(this);
        CookieSyncManager.getInstance().sync();
        CookieManager cookieManager = CookieManager.getInstance();
        String cookie = cookieManager.getCookie(mUrl);
        if (!TextUtils.isEmpty(cookie)) {
            String cookies[] = cookie.split(";");
            for (String val : cookies) {
                val = val.trim();
                if (val.contains(KEY_COOKIE_T + "=")) {
                    ret.put(KEY_COOKIE_T, val.substring(2));
                }
                
                if (val.contains(KEY_COOKIE_Q + "=")){
                    ret.put(KEY_COOKIE_Q, val.substring(2));
                }
            }
        }

        return ret;
    }

    /**
     * 解决webview中当开启新的页面的时候用webview来进行处理而不是用系统自带的浏览器处理
     * 
     */
    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            if (url.startsWith(SUCC_CB_URL)) {//处理修改密码，绑定手机等操作成功的回调
            	Intent intent = new Intent();
            	try {
            		Uri aUri = Uri.parse(url);
                	String qid = aUri.getQueryParameter("qid");
                	if(qid.equals(mQid)){//仅当返回的qid和之前的qid相同时， 才会返回新的QT
                		HashMap<String, String> cookie = getQTCookie();
                        String q = TextUtils.isEmpty(cookie.get(KEY_COOKIE_Q)) ? "" : cookie.get(KEY_COOKIE_Q);
                        String t = TextUtils.isEmpty(cookie.get(KEY_COOKIE_T)) ? "" : cookie.get(KEY_COOKIE_T);
                        intent.putExtra(KEY_COOKIE_Q, q);
                        intent.putExtra(KEY_COOKIE_T, t);
                	}
				} catch (Throwable e) {
		            if (LOGE_ENABLED) {
		                Log.e(TAG, "[shouldOverrideUrlLoading] exception:" + e.toString());
		            }
				}
                setResult(RESULT_CODE_OPT_OK, intent);
                Toast.makeText(WebViewActivity.this, mTitle + getResources().getString(R.string.qihoo_accounts_dialog_opt_succ), Toast.LENGTH_LONG).show();
                finish();
            }else if(url.startsWith("http") || url.startsWith("https")){
                //非360的页面用浏览器打开
                if (url.contains(QIHOO_URL)) {
                    view.loadUrl(url);
                } else {
                    AddAccountsUtils.openBrowser(WebViewActivity.this, url);
                }
            }else{
                return false;
            }
            return true;
        }

        @Override
        public void onPageStarted(WebView view, String url, Bitmap favicon) {
            // TODO Auto-generated method stub
            super.onPageStarted(view, url, favicon);
            rotateImageView.setVisibility(View.VISIBLE);
            closeBtn.setVisibility(View.GONE);
            AddAccountsUtils.showWebviewRotate(WebViewActivity.this, rotateImageView);
        }

        /**
         * 载入页面完成的事件
         */
        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            rotateImageView.clearAnimation();
            rotateImageView.setVisibility(View.GONE);
            closeBtn.setVisibility(View.VISIBLE);
        }

    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        // TODO Auto-generated method stub
        if ((keyCode == KeyEvent.KEYCODE_BACK) && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}