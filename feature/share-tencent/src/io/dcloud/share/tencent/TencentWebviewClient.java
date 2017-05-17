package io.dcloud.share.tencent;

import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.share.AbsWebviewClient;
import io.dcloud.share.ShareAuthorizeView;
import android.graphics.Bitmap;
import android.net.http.SslError;
import android.webkit.SslErrorHandler;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * <p>Description:腾讯分享控件特殊处理</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-5-31 下午5:04:09 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-31 下午5:04:09</pre>
 */
public class TencentWebviewClient extends AbsWebviewClient {
	
	private String APP_KEY;
	private String APP_SECRET;
	private String REDIRECT_URL;
	
	private boolean isloaded = false;
	private ShareAuthorizeView mAuthorizeView;
	
	protected TencentWebviewClient(ShareAuthorizeView pView){
		mAuthorizeView = pView;
		initData();
	}
	
	public void initData(){
		APP_KEY = AndroidResources.getMetaValue("TENCENT_APPKEY").substring(1);;
		APP_SECRET = AndroidResources.getMetaValue("TENCENT_SECRET");//"0b3ac9d885c09a1f45c0c08304291546";
		REDIRECT_URL = AndroidResources.getMetaValue("TENCENT_REDIRECT_URI");
	}
	@Override
	public String getInitUrl(){
		int state = (int) Math.random() * 1000 + 111;
		return "https://open.t.qq.com/cgi-bin/oauth2/authorize?client_id="+ APP_KEY +
				"&response_type=code&redirect_uri="+REDIRECT_URL + "&state=" + state;
	}
	/**
	 * 回调方法，当页面开始加载时执行
	 */
	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		super.onPageStarted(view, url, favicon);
	}

	
	@Override
	public void onPageFinished(WebView view, String url) {
		if(!isloaded){
			isloaded = true;
			mAuthorizeView.onloaded();
		}
		super.onPageFinished(view, url);
	}

	/*
	 * TODO Android2.2及以上版本才能使用该方法
	 * 目前https://open.t.qq.com中存在http资源会引起sslerror，待网站修正后可去掉该方法
	 */
	public void onReceivedSslError(WebView view, SslErrorHandler handler,
			SslError error) {
		if ((null != view.getUrl())
				&& (view.getUrl().startsWith("https://open.t.qq.com"))) {
			handler.proceed();// 接受证书
		} else {
			handler.cancel(); // 默认的处理方式，WebView变成空白页
		}
	}
}
