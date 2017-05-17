package io.dcloud.share.tencent;

import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.adapter.util.DeviceInfo;
import io.dcloud.common.util.PdrUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.tencent.weibo.sdk.android.api.util.BackGroudSeletor;
import com.tencent.weibo.sdk.android.api.util.Util;
import com.tencent.weibo.sdk.android.component.sso.WeiboToken;

public class WebAuthorize extends Activity {
    WebView webView;
    String _url;
    String _fileName;
    public static int WEBVIEWSTATE_1 = 0;
    int webview_state = 0;
    String path;
    Dialog _dialog;
    public static final int ALERT_DOWNLOAD = 0;
    public static final int ALERT_FAV = 1;
    public static final int PROGRESS_H = 3;
    public static final int ALERT_NETWORK = 4;
    private ProgressDialog dialog;
    private LinearLayout layout = null;
    private String redirectUri = null;
    private String clientId = null;
    private boolean isShow = false;

    Handler handle = new Handler() {
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 100:
                    WebAuthorize.this.showDialog(4);
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!Util.isNetworkAvailable(this)) {
            showDialog(4);
        } else {
            DisplayMetrics displaysMetrics = new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getMetrics(displaysMetrics);
            String pix = displaysMetrics.widthPixels + "x"
                    + displaysMetrics.heightPixels;
            BackGroudSeletor.setPix(pix);
            try {
               /* if (!PdrUtil.isEmpty(AndroidResources.getMetaValue("TENCENT_APPKEY"))) {
                    clientId = AndroidResources.getMetaValue("TENCENT_APPKEY").substring(1);
                }
                redirectUri = AndroidResources
                        .getMetaValue("TENCENT_REDIRECT_URI");*/
                clientId= getIntent().getStringExtra(TencentWeiboApiManager.KEY_APP_KEY);
                redirectUri=getIntent().getStringExtra(TencentWeiboApiManager.KEY_REDIRECT_URL);
                if ((this.clientId == null) || ("".equals(this.clientId))
                        || (this.redirectUri == null)
                        || ("".equals(this.redirectUri))) {
                    Toast.makeText(this, "请在配置文件中填写相应的信息", 0).show();
                }
                Log.d("redirectUri", this.redirectUri);
//				getWindow().setFlags(1024, 1024);
                requestWindowFeature(1);
                int state = (int) Math.random() * 1000 + 111;
                this.path = ("https://open.t.qq.com/cgi-bin/oauth2/authorize?client_id="
                        + this.clientId
                        + "&response_type=token&redirect_uri="
                        + this.redirectUri + "&state=" + state);
                initLayout();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void initLayout() {
        RelativeLayout.LayoutParams fillParams = new RelativeLayout.LayoutParams(
                -1, -1);
        RelativeLayout.LayoutParams fillWrapParams = new RelativeLayout.LayoutParams(
                -1, -2);
        RelativeLayout.LayoutParams wrapParams = new RelativeLayout.LayoutParams(
                -2, -2);

        this.dialog = new ProgressDialog(this);
        this.dialog.setProgressStyle(0);
        this.dialog.requestWindowFeature(1);
        this.dialog.setMessage("请稍后...");
        this.dialog.setIndeterminate(false);
        this.dialog.setCancelable(false);
        this.dialog.show();

        this.layout = new LinearLayout(this);
        this.layout.setLayoutParams(fillParams);
        this.layout.setOrientation(1);

        RelativeLayout cannelLayout = new RelativeLayout(this);
        cannelLayout.setLayoutParams(fillWrapParams);
        cannelLayout.setBackgroundDrawable(BackGroudSeletor.getdrawble(
                "up_bg2x", getApplication()));
        cannelLayout.setGravity(0);

        Button returnBtn = new Button(this);
        String[] pngArray = {"quxiao_btn2x", "quxiao_btn_hover"};
        returnBtn.setBackgroundDrawable(BackGroudSeletor.createBgByImageIds(
                pngArray, getApplication()));
        returnBtn.setText("取消");
        wrapParams.addRule(9, -1);
        wrapParams.addRule(15, -1);
        wrapParams.leftMargin = 10;
        wrapParams.topMargin = 10;
        wrapParams.bottomMargin = 10;

        returnBtn.setLayoutParams(wrapParams);
        returnBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                WebAuthorize.this.finish();
            }
        });
        cannelLayout.addView(returnBtn);

        TextView title = new TextView(this);
        title.setText("授权");
        title.setTextColor(-1);
        title.setTextSize(24.0F);
        RelativeLayout.LayoutParams titleParams = new RelativeLayout.LayoutParams(
                -2, -2);
        titleParams.addRule(13, -1);
        title.setLayoutParams(titleParams);
        cannelLayout.addView(title);

        this.layout.addView(cannelLayout);

        this.webView = new WebView(this);

        if (Build.VERSION.SDK_INT >= 11) {
            Class[] name = {String.class};
            Object[] rmMethodName = {"searchBoxJavaBridge_"};
            try {
                Method rji = this.webView.getClass().getDeclaredMethod(
                        "removeJavascriptInterface", name);
                rji.invoke(this.webView, rmMethodName);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        LinearLayout.LayoutParams wvParams = new LinearLayout.LayoutParams(-2,
                -2);
        this.webView.setLayoutParams(wvParams);
        WebSettings webSettings = this.webView.getSettings();
        this.webView.setVerticalScrollBarEnabled(false);
        webSettings.setJavaScriptEnabled(true);
        webSettings.setUseWideViewPort(true);
        webSettings.setLoadWithOverviewMode(false);
        this.webView.loadUrl(this.path);
        this.webView.setWebChromeClient(new WebChromeClient() {

            public void onProgressChanged(WebView view, int newProgress) {
                super.onProgressChanged(view, newProgress);
                Log.d("newProgress", newProgress + "..");
            }
        });
        this.webView.setWebViewClient(new WebViewClient() {
            public void onPageFinished(WebView view, String url) {
                Log.d("backurl", "page finished:" + url);
                if ((url.indexOf("access_token") != -1)
                        && (!WebAuthorize.this.isShow)) {
                    WebAuthorize.this.jumpResultParser(url);
                }
                if ((WebAuthorize.this.dialog != null)
                        && (WebAuthorize.this.dialog.isShowing()))
                    WebAuthorize.this.dialog.cancel();
            }

            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if ((url.indexOf("access_token") != -1)
                        && (!WebAuthorize.this.isShow)) {
//					WebAuthorize.this.jumpResultParser(url);
                }
                return false;
            }

            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                Log.d("backurl", "page start:" + url);
                if ((url.indexOf("access_token") != -1)
                        && (!WebAuthorize.this.isShow)) {
//					WebAuthorize.this.jumpResultParser(url);
                }
                if ((WebAuthorize.this.dialog != null)
                        && (WebAuthorize.this.dialog.isShowing()))
                    WebAuthorize.this.dialog.cancel();
            }
        });
        this.layout.addView(this.webView, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        setContentView(this.layout);
    }

    public void jumpResultParser(String result) {
        String resultParam = result.split("#")[1];
        String[] params = resultParam.split("&");
        String accessToken = params[0].split("=")[1];
        String expiresIn = params[1].split("=")[1];
        String openid = params[2].split("=")[1];
        String openkey = params[3].split("=")[1];
        String refreshToken = params[4].split("=")[1];
        String state = params[5].split("=")[1];
        String name = params[6].split("=")[1];
        String nick = params[7].split("=")[1];
        Context context = DeviceInfo.sApplicationContext;
        Intent i = new Intent();
        i.putExtra("com.tencent.sso.PACKAGE_NAME", getPackageName());
        if ((accessToken != null) && (!"".equals(accessToken))) {
            Util.saveSharePersistent(context, "ACCESS_TOKEN", accessToken);
            Util.saveSharePersistent(context, "EXPIRES_IN", expiresIn);
            Util.saveSharePersistent(context, "OPEN_ID", openid);
            Util.saveSharePersistent(context, "OPEN_KEY", openkey);
            Util.saveSharePersistent(context, "REFRESH_TOKEN", refreshToken);
            Util.saveSharePersistent(context, "NAME", name);
            Util.saveSharePersistent(context, "NICK", nick);
            Util.saveSharePersistent(context, "CLIENT_ID", this.clientId);
            Util.saveSharePersistent(context, "AUTHORIZETIME",
                    String.valueOf(System.currentTimeMillis() / 1000L));
            this.isShow = true;
            i.putExtra("ACCESS_TOKEN", accessToken);
            i.putExtra("EXPIRES_IN", expiresIn);
//			byte[] tokenBytes = getTokenBytes(accessToken, Long.parseLong(expiresIn), refreshToken, openid, "", "");
//			i.putExtra("com.tencent.sso.ACCESS_TOKEN", tokenBytes);
        } else {
        }
        setResult(0, i);
        finish();
    }

    private static byte[] getTokenBytes(String accessToken, long expiresIn, String refreshToken, String openID, String omasToken, String omasKey) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(baos);
        try {
            dos.writeUTF(accessToken);
            dos.writeLong(expiresIn);
            dos.writeUTF(refreshToken);
            dos.writeUTF(openID);
            dos.writeUTF(omasToken);
            dos.writeUTF(omasKey);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return baos.toByteArray();
    }

    private WeiboToken revert(byte[] data) {
        WeiboToken token = new WeiboToken();
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        DataInputStream dis = new DataInputStream(bais);
        try {
            token.accessToken = dis.readUTF();
            token.expiresIn = dis.readLong();
            token.refreshToken = dis.readUTF();
            token.openID = dis.readUTF();
            token.omasToken = dis.readUTF();
            token.omasKey = dis.readUTF();
            WeiboToken localWeiboToken1 = token;
            return localWeiboToken1;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (bais != null)
                try {
                    bais.close();
                } catch (IOException localIOException4) {
                }
            if (dis != null)
                try {
                    dis.close();
                } catch (IOException localIOException5) {
                }
        }
        return null;
    }

    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case 3:
                this._dialog = new ProgressDialog(this);
                ((ProgressDialog) this._dialog).setMessage("加载中...");
                break;
            case 4:
                AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
                builder2.setTitle("网络连接异常，是否重新连接？");
                builder2.setPositiveButton("是",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                if (Util.isNetworkAvailable(WebAuthorize.this)) {
                                    WebAuthorize.this.webView
                                            .loadUrl(WebAuthorize.this.path);
                                } else {
                                    Message msg = Message.obtain();
                                    msg.what = 100;
                                    WebAuthorize.this.handle.sendMessage(msg);
                                }
                            }
                        });
                builder2.setNegativeButton("否",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                WebAuthorize.this.finish();
                            }
                        });
                this._dialog = builder2.create();
        }

        return this._dialog;
    }
}
