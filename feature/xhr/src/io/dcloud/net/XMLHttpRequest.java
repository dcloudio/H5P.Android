package io.dcloud.net;

import io.dcloud.common.DHInterface.IReqListener;
import io.dcloud.common.DHInterface.IResponseListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.IApp.ConfigProperty;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.net.NetWork;
import io.dcloud.common.util.net.RequestData;

import java.io.InputStream;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>Description:XHR请求</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-4-7 下午3:26:36 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-7 下午3:26:36</pre>
 */
public class XMLHttpRequest implements IReqListener,IResponseListener{
	
	public String mUUID;
	private NetWork mNetWork;
	IWebview mWebview;
	private RequestData mRequestData;
	private int mState;
	private int mReadyState;
	// 网络请求错误号标记
	private int mErrorCode = -1;
	private String mStatusText;
	private String mCallbackId;
	
	private final static int RECEIVING = 3;
	private final static int LOADED = 4;
	
	private final static int ERROR_TIME_OUT_CODE = 0;
	private final static int ERROR_OTHER_CODE = 1;
	
	public XMLHttpRequest(String pUUID, String pUrl, String pReqmethod,IWebview pWebViewImpl){
		mUUID = pUUID;
		mRequestData = new RequestData(pUrl,pReqmethod);
		mRequestData.unTrustedCAType = pWebViewImpl.obtainApp().obtainConfigProperty(ConfigProperty.CONFIG_UNTRUSTEDCA);
		mRequestData.addHeader(IWebview.USER_AGENT, pWebViewImpl.getWebviewProperty(IWebview.USER_AGENT));
//		String cookie = pWebViewImpl.getWebviewProperty(pUrl);
//		if(!PdrUtil.isEmpty(cookie)){
//			mRequestData.addHeader(IWebview.COOKIE, cookie);
//		}
		mNetWork = new NetWork(NetWork.WORK_COMMON, mRequestData, this, this);
		mWebview = pWebViewImpl;
	}

	public NetWork getNetWork(){
		return mNetWork;
	}
	
	/**
	 * @return the mRequestData
	 */
	public RequestData getRequestData() {
		return mRequestData;
	}

	/**
	 * @param mCallbackId the mCallbackId to set
	 */
	public void setCallbackId(String pCallbackId) {
		mCallbackId = pCallbackId;
	}
	
	@Override
	public void onResponseState(int pState,String pStatusText) {
		mState = pState;
		mStatusText = pStatusText;
		Logger.d("xhr","onResponseState pState=" + pState + ";mCallbackId=" + mCallbackId);
	}

	@Override
	public void onNetStateChanged(NetState state, boolean isAbort) {
		if (isAbort) {
			mReadyState = LOADED;
			return;
		}
		switch(state) {
		case NET_HANDLE_END:
			mReadyState = LOADED;
			JSUtil.execCallback(mWebview, mCallbackId, toJSON(), JSUtil.OK, true);
			break;
		case NET_HANDLE_ING:
			mReadyState = RECEIVING;
			JSUtil.execCallback(mWebview, mCallbackId, toJSON(), JSUtil.OK, true);
			break;
		case NET_ERROR:
			mReadyState = LOADED;
			mErrorCode = ERROR_OTHER_CODE;
			JSUtil.execCallback(mWebview, mCallbackId, toJSON(), JSUtil.OK, true);
			break;
		case NET_TIMEOUT:
			mReadyState = LOADED;
			mErrorCode = ERROR_TIME_OUT_CODE;
			JSUtil.execCallback(mWebview, mCallbackId, toJSON(), JSUtil.OK, true);
			break; 
		}
	}

	@Override
	public void onResponsing(InputStream os) {
	}

	@Override
	public int onReceiving(InputStream is) {
		
		return 0;
	}
	
	public JSONObject toJSON(){
		JSONObject json = new JSONObject();
		String responseText = mNetWork.getResponseText();
		try {
			json.put("readyState", mReadyState);
			json.put("status", mState);
			json.put("statusText", mStatusText);
			json.put("responseText", responseText);
			JSONObject headers = headersToJSON(mNetWork.getHeadersAndValues());
			json.put("header", headers);
			// error>-1 证明是网络请求出现错误
			if (mErrorCode > -1) {
				json.put("error", mErrorCode);
			} else { // 无网络错误
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json;
	}
	/**
	 * 
	 * Description:格式化ResponseText
	 * @param pResponseText
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-9 下午4:20:10</pre>
	 */
	private String toJsResponseText(String pResponseText){
		return JSONUtil.toJSONableString(pResponseText);
	}
	/**
	 * 
	 * Description:获得responseHeader的json字符
	 * @param pHeaders
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-9 下午4:20:07</pre>
	 */
	private JSONObject headersToJSON(Map<String, String> pHeaders){
		JSONObject ret = new JSONObject();
		Iterator<String> _keys = pHeaders.keySet().iterator();
		String _key,_value;
		while(_keys.hasNext()){
			_key = _keys.next();
			_value = pHeaders.get(_key);
			try {
				ret.put(_key, _value);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		return ret;
	}
}
