package io.dcloud.net;

import android.text.TextUtils;
import android.util.Log;
import android.webkit.CookieManager;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.dcloud.common.DHInterface.IReqListener;
import io.dcloud.common.DHInterface.IReqListener.NetState;
import io.dcloud.common.DHInterface.IResponseListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.net.NetWork;
import io.dcloud.common.util.net.RequestData;
import io.dcloud.common.util.net.UploadMgr;
import io.dcloud.net.JsUpload.UploadFile;
import io.dcloud.net.JsUpload.UploadItem;
import io.dcloud.net.JsUpload.UploadString;

/**
 * <p>Description:http网络请求</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-3-25 下午6:07:31 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-25 下午6:07:31</pre>
 */
public class UploadNetWork extends NetWork {

	/**
	 * 请求模式常量
	 */
	public static String REQMETHOD_GET = "GET";
	public static String REQMETHOD_POST = "POST";
	
	private boolean mSupport = false;
	/**
	 * 请求响应码
	 */
	public int mStatus = 0;
	
	/**
	 * 已上传的文件大小
	 */
	long mUploadedSize;
	/**
	 * 上传文件总大小
	 */
	long mTotalSize;

	/**
	 * 上传单元
	 */
	LinkedHashMap<String,UploadItem> mUploadItems;
	/**
	 * 正在上传的文件的名称
	 */
	StringBuffer mUploadingFile = new StringBuffer();
	/**
	 * 上传文件大小
	 */
	long mContentLength;
	
	long RANGE_BUF = 100 * 1024;
	
	String responseHeaders;
	/**
	 * Description: 构造函数 
	 * @param pWorkType
	 * @param pRequestData
	 * @param pReqListener
	 * @param pResponseListener 
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-10 下午2:42:18</pre>
	 */
	public UploadNetWork(int pWorkType, RequestData pRequestData,
			IReqListener pReqListener, IResponseListener pResponseListener) {
		super(pWorkType, pRequestData, pReqListener, pResponseListener);
		mUploadItems = new LinkedHashMap<String,UploadItem>(4);
	}
	/**
	 * 
	 * Description:添加请求属性
	 * @param pKey
	 * @param pValue
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-28 上午11:51:04</pre>
	 */
	public boolean addParemeter(String pKey, String pValue){
		boolean _ret = false;
		if(!mUploadItems.containsKey(pKey)){
			mUploadItems.put(pKey, new UploadString(pValue));
			_ret = true;
		}
		return _ret;
	}
	/**
	 * 
	 * Description:添加上传文件
	 * @param pKey
	 * @param pFile
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-28 下午4:35:46</pre>
	 */
	public boolean addFile(String pKey, UploadFile pFile){
		boolean _ret = false;
		if(!mUploadItems.containsKey(pFile)){
			mUploadItems.put(pKey, pFile);
			_ret = true;
		}
		return _ret;
	}
	
	/**
	 * 
	 * Description:将请求参数写入输入流集合
	 * @param content
	 * @param _length
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-28 上午11:57:50</pre>
	 */
	public long appendPostParemeter(String content,long _length){
		ByteArrayInputStream ret=null;
		try {
			ret = new ByteArrayInputStream(content.getBytes("utf-8"));
			return ret.available()+_length;
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return 0;
	}
    
	/**
	 * Description:初始化上传数据
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-29 下午4:00:25</pre>
	 */
	private void initUploadData() throws Exception{
		String end = "\r\n";
		String twoHyphens = "--";
		String boundary = mMainBoundry;
		mRequest = mRequestData.getHttpRequest();
		initHttpsURLConnectionVel();
		mRequest.setDoOutput(true);
		mRequest.setDoInput(true);
		mRequest.setRequestProperty("Connection", "Keep-Alive");
		mRequest.setRequestProperty("Charset", "UTF-8");
		mRequest.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
		mRequest.setUseCaches(false);
		initContentLength();
		DataOutputStream ds = new DataOutputStream(mRequest.getOutputStream());
		//设置所有应该上传的数据
		mReqListener.onNetStateChanged(NetState.NET_REQUEST_BEGIN,isAbort);
		//上传addFile数据或addData数据
		if(mUploadItems != null && mUploadItems.size() > 0) {
			Set<String> _sets = mUploadItems.keySet();
			mTotalSize = mContentLength;
			for(String pKey : _sets){
				UploadItem ui = mUploadItems.get(pKey);
				ds.writeBytes(twoHyphens + boundary + end);
				if(ui instanceof UploadFile) {
					mUploadingFile.append(pKey);
					ds.writeBytes("Content-Disposition: form-data; name=\""+pKey+"\"; filename=\""+((UploadFile) ui).mFilename+"\""+end);
					ds.writeBytes("Content-Type: "+((UploadFile) ui).mMimetype+end);
					ds.writeBytes(end);
					FileInputStream fStream = ((UploadFile) ui).mFileInputS;
					int bufferSize = 1024;
					byte[] buffer = new byte[bufferSize];
					int length = -1;
					while ((length = fStream.read(buffer)) != -1) {
						ds.write(buffer, 0, length);
					}
					ds.writeBytes(end);
					fStream.close();
				}else if(ui instanceof UploadString){
					StringBuilder sb = new StringBuilder();
					sb.append("Content-Disposition: form-data; name=\""+pKey+"\""+end);
					sb.append(end);
					sb.append(((UploadString) ui).mData);
					sb.append(end);
					ds.write(sb.toString().getBytes());
					sb.delete(0, sb.length());
				}
				mUploadedSize = ds.size();
				mReqListener.onNetStateChanged(NetState.NET_HANDLE_ING,isAbort);
			}
		}
		ds.writeBytes(twoHyphens + boundary + twoHyphens + end);
		mUploadedSize = ds.size();
		mReqListener.onNetStateChanged(NetState.NET_HANDLE_ING,isAbort);
		ds.flush();
		responseUpload();
		ds.close();
	}

	public void initContentLength() {
		mContentLength = 0;
		//上传addFile数据或addData数据
		if(mUploadItems != null && mUploadItems.size() > 0){
			Set<String> _sets = mUploadItems.keySet();
			for(String pKey : _sets){
				UploadItem ui = mUploadItems.get(pKey);
				if(ui instanceof UploadFile) {
					mUploadingFile.append(pKey);
					addCutoffLine(mMainBoundry);
					addFileInputStream(pKey, (UploadFile)ui);
				}else if(ui instanceof UploadString){
					addCutoffLine(mMainBoundry);
					addPropertyInputStream(pKey, ((UploadString)ui).mData);
				}
			}
		}
		//上传结尾
		mContentLength = appendPostParemeter("--"+mMainBoundry+"--\r\n",mContentLength);
	}
	/**
	 * 
	 * Description:连接网络
	 * @param uploadOperate 是否上传操作
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-28 下午5:58:28</pre>
	 */
	public void connet(boolean uploadOperate){
		try{
			if(uploadOperate){
				mReqListener.onNetStateChanged(NetState.NET_CONNECTED,isAbort);
			}
			initUploadData();
			responseHeaders = getResponseHeaders();
		} catch (Exception e) {
			Logger.d("upload is ERROR:" + e.getLocalizedMessage());
			e.printStackTrace();
			long _nextconnecttime = System.currentTimeMillis() + mRetryIntervalTime * (1<<mTimes)/2;
			if(mTimes < MAX_TIMES){
    			mTimes++;//尝试次数计数
				while(true) {
					if(System.currentTimeMillis() > _nextconnecttime){
						this.connet(uploadOperate);
						break;
					}
				}
			}
		} 
	}
	private static boolean isRightRequest(int status){
		return status >= 200 && status < 300;
	}
	/**
	 * 响应上传服务查询结果
	 */
//	private void responseQuery(){
//		int status = 0;
//		if(mHttpResponse != null){
//			status = mHttpResponse.getStatusLine().getStatusCode();
//		}
//		if(isRightRequest(status)){
//			try {
//				Header setcookieHeader = mHttpResponse.getLastHeader(IWebview.SET_COOKIE);
//				if(null!=setcookieHeader){
//					String set_cookie = setcookieHeader.getValue();
//					if(!PdrUtil.isEmpty(set_cookie)){
//						CookieManager.getInstance().setCookie(mRequestData.getUrl(), set_cookie);
//					}
//				}
//				InputStream _is = mHttpResponse.getEntity().getContent();
//				byte[] _buf = new byte[1024];
//				int _len;
//				ByteArrayOutputStream _baos = new ByteArrayOutputStream();
//				while (((_len = _is.read(_buf)) > 0)) {
//					_baos.write(_buf, 0, _len);
//				}
//				String _data = new String(_baos.toByteArray(), "utf-8");
//				Logger.d("data:"+_data);
//				boolean _support = false;
//				try {
//					JSONObject _json = new JSONObject(_data);
//					String _protocol =  _json.optString("protocol");
//					_support = _json.optBoolean("support");
//					String _boundary = _json.optString("boundary");
//					JSONArray _fileArr = _json.optJSONArray("file");
//					if(_fileArr != null) {
//						String _name;String _range;UploadItem ui;
//						for(int i=0;i<_fileArr.length();i++){
//							JSONObject _file =_fileArr.getJSONObject(i);
//							_name = _file.optString("name");
//							_range = _file.optString("range");
//							ui = mUploadItems.get(_name);
//							if(ui != null && ui instanceof UploadFile){
//								((UploadFile)ui).mRange = _range;
//							}
//						}
//					}
//					JSONArray _dataArr = _json.optJSONArray("data");
//
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//				mSupport = _support = false;
//				_baos.close();
//				_is.close();
//				mHttpPost.abort();
//				mHttpPost = null;
//			} catch (IllegalStateException e) {
//				Logger.e("uploadnetwork","responseQuery IllegalStateException url=" + mRequestData.getUrl() );
//			} catch (IOException e) {
//				Logger.e("uploadnetwork","responseQuery IOException url=" + mRequestData.getUrl() );
//			} catch (Exception e) {
//				Logger.e("uploadnetwork","responseQuery Exception url=" + mRequestData.getUrl() );
//			}
//		}
//		uploadContent();//开始执行上传逻辑
//
//	}
	/**
	 * Description:返回信息
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-28 下午7:16:43</pre>
	 */
	public void responseUpload(){
		try {
			if(mRequest != null){
				mStatus = mRequest.getResponseCode();
				//Logger.d("uploadnetwork","responseUpload mStatus= " + mStatus);
			}
			String setcookieHeader = mRequest.getHeaderField(IWebview.SET_COOKIE);
			if(!TextUtils.isEmpty(setcookieHeader)){
				CookieManager.getInstance().setCookie(mRequestData.getUrl(), setcookieHeader);
			}
			InputStream _is = mRequest.getInputStream();
			byte[] _buf = new byte[1024];
			int _len;
			ByteArrayOutputStream _baos = new ByteArrayOutputStream();
			while (((_len = _is.read(_buf)) > 0)) {
				_baos.write(_buf, 0, _len);
			}
			String _data = new String(_baos.toByteArray(), "utf-8");
			mResponseText = _data;
			try {
				JSONObject _json = new JSONObject(_data);
				String result = JSONUtil.getString(_json, "result");
				String code = JSONUtil.getString(_json, "code");
				String message = JSONUtil.getString(_json, "message");
			} catch (Exception e) {
				Logger.e("uploadnetwork","responseUpload JSONObject _data=" + _data + ";url=" + mRequestData.getUrl() );
			}
			if(isRightRequest(mStatus)){
				mReqListener.onNetStateChanged(NetState.NET_HANDLE_END,isAbort);
			}else{
				mReqListener.onNetStateChanged(NetState.NET_ERROR,isAbort);
			}
//				uploadContent();// 开始执行上传逻辑
			_baos.close();
			_is.close();
		}catch (Exception e) {
			Logger.e("uploadnetwork","responseUpload "+e.getLocalizedMessage()+";url=" + mRequestData.getUrl() );
			mResponseText = e.getMessage();
			mReqListener.onNetStateChanged(NetState.NET_ERROR,isAbort);
		}
	}
	/**
	 * 
	 * Description:添加上传文件流
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-28 下午4:48:12</pre>
	 */
	private void addFileInputStream(String pKey,UploadFile pFile){
		if(mSupport){//支持短线续传时，需上传range数据
			mContentLength = appendPostParemeter("Content-Disposition: attachments; name=\""+pKey+"\"; filename=\""+pFile.mFilename+"\"; range=\"0-777/777\"\r\n", mContentLength);
		}else{
			mContentLength = appendPostParemeter("Content-Disposition: form-data; name=\""+pKey+"\"; filename=\""+pFile.mFilename+"\"\r\n", mContentLength);
		}
		mContentLength = appendPostParemeter("Content-Type: "+pFile.mMimetype+"\r\n\r\n", mContentLength);
		mContentLength = appendPostParemeter("\r\n", mContentLength);
		mContentLength = mContentLength + pFile.mFileSize;
	}
	/**
	 * 
	 * Description:添加上传文本流
	 * @param pKey
	 * @param pValue
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-28 下午5:43:52</pre>
	 */
	private void addPropertyInputStream(String pKey,String pValue){
		mContentLength = appendPostParemeter("Content-Disposition: form-data; name=\""+pKey+"\"\r\n\r\n", mContentLength);
		mContentLength = appendPostParemeter(pValue +"\r\n", mContentLength);
	}
	/**
	 * 
	 * Description:添加分割线
	 * @param mainBoundry
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-28 下午5:48:42</pre>
	 */
	private void addCutoffLine(String mainBoundry){
		mContentLength = appendPostParemeter("--"+mainBoundry+"\r\n",mContentLength);
	}

	@Override
	public void run() {
//查询服务器是否支持断点续传
//		querySurpport();
		uploadContent();
	}
	/**
	 * 上传内容
	 */
	private void uploadContent(){
		mTimes = 1;//上传数据时候，重新计数，清除“上传服务支持情况查询请求”的计数
		connet(true);
		dispose();
	}
	/**
	 * 服务器上传服务支持情况查询
	 */
//	private void querySurpport(){
//		mHttpClient = createHttpClient();
//		mHttpPost = new HttpPost(mRequestData.getUrl());
//    	Vector<InputStream> _arr = new Vector<InputStream>();
//    	mContentLength = appendPostParemeter(_arr, "server=uploader&action=query&boundary="+mMainBoundry, mContentLength);
//    	Enumeration<InputStream> en = _arr.elements();
//		SequenceInputStream sis = new SequenceInputStream(en);
//		InputStreamEntity ise = new InputStreamEntity(sis, mContentLength);
////		ise.setContentType("binary/octet-stream");
////		ise.setChunked(true);
//		mHttpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");
////	     mHttpPost.addHeader("Content-Length",String.valueOf(mContentLength));
//		mRequestData.addHeader(mHttpPost);
//		mHttpPost.setEntity(ise);
//		connet(false);
//		responseQuery();
//	}
	@Override
	public void dispose() {
		mHeaderList = null;
		mRequest = null;
		mUploadedSize = 0;
		mTotalSize = 0;
		UploadMgr.getUploadMgr().removeNetWork(this);
	}
	
	public String getResponseHeaders() {
		try {
			Map<String, List<String>> headers = mRequest.getHeaderFields();
			Map<String, String> map = new HashMap<String, String>();
			for(Map.Entry<String,List<String>> entry : headers.entrySet()){
				String values = "";
				for(String value : entry.getValue()){
					values = values +"  "+ value;
				}
				if(!PdrUtil.isEmpty(entry.getKey())) {
					map.put(entry.getKey(), values);
				}
			}
			JSONObject jsonObject = new JSONObject(map);
			return jsonObject.toString();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return "{}";
	}

}
