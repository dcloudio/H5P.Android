package io.dcloud.share.sina;

import io.dcloud.common.DHInterface.IReqListener;
import io.dcloud.common.DHInterface.IResponseListener;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.adapter.util.PlatformUtil;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.net.NetWork;
import io.dcloud.common.util.net.RequestData;
import io.dcloud.share.AbsWebviewClient;
import io.dcloud.share.ShareAuthorizeView;

import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.graphics.Bitmap;
import android.webkit.WebView;

/**
 * <p>Description:新浪分享控件特殊处理</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-5-31 下午5:07:44 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-31 下午5:07:44</pre>
 */
public class SinaWebviewClient extends AbsWebviewClient {
	
	private String APP_KEY;
	private String APP_SECRET;
	private String REDIRECT_URL="";
	private NetWork netWork;
	private static final String BASEURL = "https://api.weibo.com/oauth2/access_token";
	
	private boolean isloaded = false;
	private ShareAuthorizeView mAuthorizeView;
	
	public SinaWebviewClient(ShareAuthorizeView pView){
		mAuthorizeView = pView;
		initData();
	}
	public void initData(){
		APP_KEY = AndroidResources.getMetaValue("SINA_APPKEY").substring(1);
		APP_SECRET = AndroidResources.getMetaValue("SINA_SECRET");//
		REDIRECT_URL = AndroidResources.getMetaValue("SINA_REDIRECT_URI");
	}
	@Override
	public String getInitUrl(){
		return "https://api.weibo.com/oauth2/authorize?client_id="+ APP_KEY +
				"&response_type=code&redirect_uri="+REDIRECT_URL;
	}
	
	@Override
	public void onPageStarted(WebView view, String url, Bitmap favicon) {
		if (url.indexOf("code=") != -1) {
			int start = url.indexOf("code=");
			String code = url.substring(start);
			RequestData _request = new RequestData(BASEURL, "GET");
			_request.addParemeter("client_id", APP_KEY);
			_request.addParemeter("client_secret", APP_SECRET);
			_request.addParemeter("grant_type", "authorization_code");
			_request.addParemeter("redirect_uri", REDIRECT_URL);
			_request.addParemeter("code", code);
			NetWorkLtr _ltr = new NetWorkLtr();
			netWork = new NetWork(NetWork.WORK_COMMON, _request, _ltr, _ltr);
			netWork.startWork();
			return;
		}
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


	/**
	 * <p>Description:NetWork监听者实现</p>
	 *
	 * @version 1.0
	 * @author cuidengfeng Email:cuidengfeng@dcloud.io
	 * @Date 2013-5-31 下午4:06:41 created.
	 * 
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-31 下午4:06:41</pre>
	 */
	private class NetWorkLtr implements IReqListener, IResponseListener {

		@Override
		public void onResponseState(int pState, String pStatusText) {
		}

		@Override
		public void onNetStateChanged(NetState state,boolean isAbort) {
			if(state.equals(NetState.NET_HANDLE_END)){
				try {
					JSONObject _response = new JSONObject(netWork.getResponseText());
					String access_token = _response.optString("access_token");
					String remind_in = _response.optString("remind_in");
					String expires_in = _response.optString("expires_in");
					PlatformUtil.setBundleData(SinaWeiboApiManager.SINAWEIBO_ID, 
							StringConst.JSON_SHARE_ACCESSTOKEN, access_token);
					PlatformUtil.setBundleData(SinaWeiboApiManager.SINAWEIBO_ID, 
							StringConst.JSON_SHARE_EXPIRES_IN, expires_in);
					mAuthorizeView.onauthenticated(SinaWeiboApiManager.SINAWEIBO_ID);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public void onResponsing(InputStream os) {
		}

		@Override
		public int onReceiving(InputStream is) throws Exception {

			return 0;
		}

	}
}
