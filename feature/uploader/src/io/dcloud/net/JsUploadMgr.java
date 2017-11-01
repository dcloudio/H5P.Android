package io.dcloud.net;

import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.net.UploadMgr;

/**
 * <p>Description:js上传管理者，分发管理JS过来的上传请求</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-3-27 下午2:03:03 created.
 *
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-27 下午2:03:03</pre>
 */
public class JsUploadMgr {
	/**
	 * 应用上传管理集合
	 */
	public HashMap<String, ArrayList<JsUpload>> mAppsUploadTasks = null;
	private UploadMgr mUploadMgr;

	JsUploadMgr(){
		mAppsUploadTasks = new HashMap<String, ArrayList<JsUpload>>();
		mUploadMgr = UploadMgr.getUploadMgr();
	}
	/**
	 *
	 * Description:处理JS过来的上传逻辑
	 * @param pWebViewImpl
	 * @param pActionName
	 * @param pJsArgs
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-27 下午2:06:28</pre>
	 */
	public String execute(IWebview pWebViewImpl, String pActionName,
						  String pJsArgs[]) {
		String _ret = null;
		String _appid = pWebViewImpl.obtainFrameView().obtainApp().obtainAppId();
		if("start".equals(pActionName)||"resume".equals(pActionName)){
			JsUpload _jsUpload = findUploadTask(_appid, pJsArgs[0]);
			if(_jsUpload != null && !_jsUpload.isStart){
				mUploadMgr.start(_jsUpload.mUploadNetWork);
				_jsUpload.isStart = true;
			}

			String requestHeader = pJsArgs[1];
			if (!TextUtils.isEmpty(requestHeader)) {
				try {
					JSONObject jsonObject = new JSONObject(requestHeader);
					Iterator<String> keys = jsonObject.keys();
					while (keys.hasNext()) {
						String key = keys.next();
						String value = jsonObject.getString(key);
						_jsUpload.setRequestHeader(key, value);
					}
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}else if("pause".equals(pActionName)){
			JsUpload _jsUpload = findUploadTask(_appid, pJsArgs[0]);
			if(_jsUpload != null && _jsUpload.isStart){
				mUploadMgr.abort(_jsUpload.mUploadNetWork);
				_jsUpload.isStart = false;
			}
		}else if("abort".equals(pActionName)){
			JsUpload _jsUpload = findUploadTask(_appid, pJsArgs[0]);
			if(_jsUpload != null){
				mUploadMgr.abort(_jsUpload.mUploadNetWork);
				ArrayList<JsUpload> _arr = mAppsUploadTasks.get(_appid);
				_arr.remove(_jsUpload);
			}
		}else if("createUpload".equals(pActionName)){
			JSONObject _json;
			try {
				_json = new JSONObject(pJsArgs[0]);
				JsUpload _jsUpload = createUploadTask(pWebViewImpl, _json);
				pushUploadTask(_appid, _jsUpload);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}else if("enumerate".equals(pActionName)){
			ArrayList<JsUpload> _arr = mAppsUploadTasks.get(_appid);
			JSONArray _json = enumerate(pJsArgs[0],_arr);
			JSUtil.execCallback(pWebViewImpl,pJsArgs[0],_json,JSUtil.OK,false);
		}else if("clear".equals(pActionName)){
			ArrayList<JsUpload> _arr = mAppsUploadTasks.get(_appid);
			int state =  Integer.parseInt(pJsArgs[0]);
			if(_arr != null){
				int count = _arr.size();
				for (int i = count -1; i >= 0;i--) {
					JsUpload _jsUpload = _arr.get(i);
					if(_jsUpload != null && state == _jsUpload.mState) {
						UploadMgr.getUploadMgr().abort(_jsUpload.mUploadNetWork);
						_arr.remove(i);
					}
				}
			}
		}else if("startAll".equals(pActionName)){
			ArrayList<JsUpload> _arr = mAppsUploadTasks.get(_appid);
			if(_arr != null){
				for(int i=0;i<_arr.size();i++){
					mUploadMgr.start(_arr.get(i).mUploadNetWork);
				}
			}
		}else if("addFile".equals(pActionName)){
			JsUpload _upload = findUploadTask(_appid, pJsArgs[0]);
			JSONObject _jsonObject;
			try {
				_jsonObject = new JSONObject(pJsArgs[2]);
				String filePath = pWebViewImpl.obtainFrameView().obtainApp().convert2AbsFullPath(pWebViewImpl.obtainFullUrl(),pJsArgs[1]);
				_upload.addFile(filePath, _jsonObject);
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}else if("addData".equals(pActionName)){
			JsUpload _upload = findUploadTask(_appid, pJsArgs[0]);
			_upload.addData(pJsArgs[1], pJsArgs[2]);
		}
		return _ret;
	}

	/**
	 * Description:枚举download对象
	 * @pCallbackId
	 * @pArr
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-8 下午2:30:37</pre>
	 */
	private JSONArray enumerate(String pCallbackId, ArrayList<JsUpload> pArr) {
		JSONArray array = new JSONArray();
		if(pArr!=null && !pArr.isEmpty()){
			int count = pArr.size();
			for(int i=0;i<count;i++){
				JsUpload _jsUpload = pArr.get(i);
				try {
					JSONObject jsonObject = new JSONObject(_jsUpload.toJsonUpload());
					array.put(jsonObject);
				} catch (JSONException e) {
					e.printStackTrace();
				}
			}
		}
		return array;

	}
	/**
	 *
	 * Description:添加上传对象
	 * @param appid
	 * @param pTask
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-28 下午2:28:24</pre>
	 */
	private void pushUploadTask(String appid,JsUpload pTask){
		ArrayList<JsUpload> arr = mAppsUploadTasks.get(appid);
		if(arr == null){
			arr = new ArrayList<JsUpload>();
			mAppsUploadTasks.put(appid, arr);
		}
		arr.add(pTask);
	}
	/**
	 *
	 * Description:创建上传对象
	 * @param pWebViewImpl webview对象
	 * @param pJsonUpload jsUpload对象
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-28 下午2:26:02</pre>
	 */
	private JsUpload createUploadTask(IWebview pWebViewImpl, JSONObject pJsonUpload){
		JsUpload _ret = new JsUpload(pWebViewImpl,pJsonUpload);
		return _ret;
	}
	/**
	 *
	 * Description:查找上传对象
	 * @param appid 当前应用appid
	 * @param uuid	上传对象uuid
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-28 下午2:26:34</pre>
	 */
	private JsUpload findUploadTask(String appid,String uuid){
		JsUpload _ret = null;
		ArrayList<JsUpload> arr = mAppsUploadTasks.get(appid);
		if(arr != null){
			int count = arr.size();
			for (int i = 0; i < count;i++) {
				JsUpload _uploadTask = arr.get(i);
				if(uuid.equals(_uploadTask.mUUID)){
					_ret = _uploadTask;
					break;
				}
			}
		}
		return _ret;
	}
}
