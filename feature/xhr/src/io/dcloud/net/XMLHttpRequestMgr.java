package io.dcloud.net;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.net.NetWorkLoop;
import io.dcloud.common.util.net.RequestData;

import java.util.ArrayList;
import java.util.HashMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * <p>Description:XHR管理类</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-4-7 下午2:36:47 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-7 下午2:36:47</pre>
 */
public class XMLHttpRequestMgr {
	
	private NetWorkLoop mNetWorkLoop;
	
	public HashMap<String, ArrayList<XMLHttpRequest>> mXMLHttpRequests = null;
	
	public XMLHttpRequestMgr(){
		mNetWorkLoop = new NetWorkLoop();
		mXMLHttpRequests = new HashMap<String, ArrayList<XMLHttpRequest>>();
		mNetWorkLoop.startThreadPool();
	}
	
	public String execute(IWebview pWebViewImpl, String pActionName,
			String pJsArgs[]) {
		String _appid = pWebViewImpl.obtainFrameView().obtainApp().obtainAppId();
		if("send".equals(pActionName)){
			XMLHttpRequest _xhr = findXMLHttpRequest(_appid, pJsArgs[0]);
			_xhr.setCallbackId(pJsArgs[1]);
			String _body = pJsArgs[2];
			RequestData _req = _xhr.getRequestData();
			_req.addBody(_body);
			try {
				JSONObject _requestHead = new JSONObject(pJsArgs[3]);
				JSONArray _names = _requestHead.names();
				String _name,_value;
				if(_names != null && _names.length()>0){
					for(int i=0; i<_names.length(); i++){
						_name = _names.optString(i);
						_value = _requestHead.optString(_name);
						_req.addHeader(_name, _value);
					}
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
			mNetWorkLoop.addNetWork(_xhr.getNetWork());
		}else if("open".equals(pActionName)){
			String _uuid = pJsArgs[0];
			String _method = pJsArgs[1];
			String _url = pJsArgs[2];
			String _username = pJsArgs[3];
			String _password = pJsArgs[4];
			XMLHttpRequest _xhr = new XMLHttpRequest(_uuid,_url,_method,pWebViewImpl);
			RequestData _reqData  = _xhr.getRequestData();
			int timeout = PdrUtil.parseInt(pJsArgs[5], _reqData.mTimeout);
			_reqData.mTimeout = timeout;
			_reqData.addHeader(_username, _password);
			pushXMLHttpRequest(_appid, _xhr);
		}else if("overrideMimeType".equals(pActionName)){
			XMLHttpRequest _xhr = findXMLHttpRequest(_appid, pJsArgs[0]);
			if(_xhr != null){
				_xhr.getRequestData().mOverrideMimeType = pJsArgs[1];
			}
		}else if("abort".equals(pActionName)){
			XMLHttpRequest _xhr = findXMLHttpRequest(_appid, pJsArgs[0]);
			mNetWorkLoop.removeNetWork(_xhr.getNetWork());
		}
		return null;
	}
	
	private void pushXMLHttpRequest(String appid,XMLHttpRequest pTask){
		ArrayList<XMLHttpRequest> arr = mXMLHttpRequests.get(appid);
		if(arr == null){
			arr = new ArrayList<XMLHttpRequest>();
			mXMLHttpRequests.put(appid, arr);
		}
		arr.add(pTask);
	}
	
	private XMLHttpRequest findXMLHttpRequest(String appid,String uuid){
		XMLHttpRequest _ret = null;
		ArrayList<XMLHttpRequest> arr = mXMLHttpRequests.get(appid);
		if(arr != null){
			int count = arr.size();
			for (int i = 0; i < count;i++) {
				XMLHttpRequest _xhr = arr.get(i);
				if(uuid.equals(_xhr.mUUID)){
					_ret = _xhr;
					break;
				}
			}
		}
		return _ret;
	}
	
	
	
}
