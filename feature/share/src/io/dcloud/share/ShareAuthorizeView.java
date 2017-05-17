package io.dcloud.share;

import io.dcloud.common.DHInterface.IReflectAble;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.ui.AdaFrameItem;
import io.dcloud.common.adapter.util.PlatformUtil;
import io.dcloud.common.util.JSUtil;

import java.lang.reflect.Method;

import android.os.Build;
import android.webkit.WebView;
import android.webkit.WebViewClient;

/**
 * <p>
 * Description:自定义授权控件
 * </p>
 * 
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-5-30 下午3:14:32 created.
 * 
 *       <pre>
 * <p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-30 下午3:14:32
 * </pre>
 */
public class ShareAuthorizeView extends AdaFrameItem implements IReflectAble{

	private String mCallbackId;
	private WebView mAuthorizeWebview;
	private WebViewClient mClient;
	private IWebview mWebview;

	/**
	 * Description: 构造函数
	 * 
	 * @param context
	 * 
	 *            <pre>
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-30 下午3:15:24
	 * </pre>
	 */
	public ShareAuthorizeView(IWebview pWebview, String pCallbackId) {
		super(pWebview.getContext());
		mWebview = pWebview;
		mCallbackId = pCallbackId;
		mAuthorizeWebview = new WebView(pWebview.getContext());
		removeUnSafeJavascriptInterface(mAuthorizeWebview);
		setMainView(mAuthorizeWebview);
	}
	
	private void removeUnSafeJavascriptInterface(WebView webview){
		try {
			if ((Build.VERSION.SDK_INT >= 11) && (Build.VERSION.SDK_INT < 17))
			  {
			    Class localClass = webview.getClass();
			    Class[] arrayOfClass = new Class[1];
			    arrayOfClass[0] = String.class;
			    Method localMethod = localClass.getMethod("removeJavascriptInterface", arrayOfClass);
			    WebView localWebView = webview;
			    Object[] arrayOfObject = new Object[1];
			    arrayOfObject[0] = "searchBoxJavaBridge_";
			    Object localObject = localMethod.invoke(localWebView, arrayOfObject);
			  }
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void load(ShareApiManager shareApiMgr,String pId) {
		String shareClassName = shareApiMgr.getShareClassName(pId);
		AbsWebviewClient webViewClient = (AbsWebviewClient)PlatformUtil.invokeMethod(shareClassName, "getWebviewClient", null, new Class[]{ShareAuthorizeView.class}, new Object[]{this});
		mAuthorizeWebview.setWebViewClient(webViewClient);
		mAuthorizeWebview.loadUrl(webViewClient.getInitUrl());
	}

	public void onloaded(){
		String _msg = "{evt:'load'}";
		JSUtil.execCallback(mWebview, mCallbackId, _msg, 1, true, true);
	}
	
	public void onauthenticated(String pServiceName){
		String _msg = "{type:'"+pServiceName+"'}";
		JSUtil.execCallback(mWebview, mCallbackId, _msg, 1, true, true);
	}
	
	public void onerror(String pCode){
		JSUtil.execCallback(mWebview, mCallbackId, pCode, 9, false, true);
	}
	
}
