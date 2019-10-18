package io.dcloud.net;

import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import io.dcloud.common.DHInterface.IApp.ConfigProperty;
import io.dcloud.common.DHInterface.IReqListener;
import io.dcloud.common.DHInterface.IResponseListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.StringUtil;
import io.dcloud.common.util.net.NetWork;
import io.dcloud.common.util.net.RequestData;
import io.dcloud.common.util.net.UploadMgr;

/**
 * <p>Description:对应JS里的Upload对象</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-3-27 下午2:13:39 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-27 下午2:13:39</pre>
 */
public class JsUpload implements IReqListener,IResponseListener{
	/**
	 * js对象映射ID
	 */
	public String mUUID;
	/**
	 * 请求地址
	 */
	String mUrl;
	/**
	 * 请求状态
	 */
	int mState;
	/**
	 * 请求状态常量值
	 */
	private static final int STATE_UNKOWN = -1;
	private static final int STATE_INIT = 0;
	private static final int STATE_CONNECTING = 1;
	private static final int STATE_CONNECTED = 2;
	private static final int STATE_RECEIVING = 3;
	private static final int STATE_COMPLETED = 4;
	private static final int STATE_PAUSE = 5;
	/**
	 * 请求对象
	 */
	UploadNetWork mUploadNetWork;
	
	RequestData mRequestData;
	/**
	 * webview 对象句柄
	 */
	IWebview mWebview = null;
	
	boolean isStart = false;
	
	/**
	 * 
	 * Description: 构造函数 
	 * @param pWebview
	 * @param pJsonObject 
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-28 上午11:04:17</pre>
	 */
	public JsUpload(IWebview pWebview,JSONObject pJsonObject){
		mWebview = pWebview;
		mUrl = pJsonObject.optString("url");
		mUUID = pJsonObject.optString("__UUID__");
		
		String _requestMethod = pJsonObject.optString("method","POST");
		mRequestData = new RequestData(mUrl, _requestMethod);
		mRequestData.unTrustedCAType = pWebview.obtainApp().obtainConfigProperty(ConfigProperty.CONFIG_UNTRUSTEDCA);
		mRequestData.addHeader(IWebview.USER_AGENT, pWebview.getWebviewProperty(IWebview.USER_AGENT));
//		String cookie = pWebview.getWebviewProperty(mUrl);
//		if(!PdrUtil.isEmpty(cookie)){
//			mRequestData.addHeader(IWebview.COOKIE, cookie);
//		}
		mUploadNetWork = new UploadNetWork(NetWork.WORK_UPLOAD,mRequestData, this,this);
		mUploadNetWork.mPriority = pJsonObject.optInt("priority");
		if(pJsonObject.has("timeout")){
			mRequestData.mTimeout =  pJsonObject.optInt("timeout") * 1000;
		}
		mUploadNetWork.MAX_TIMES = pJsonObject.optInt("retry");
		mUploadNetWork.setRetryIntervalTime(pJsonObject.optLong(StringConst.JSON_KEY_RETRY_INTERVAL_TIME) * 1000);
	}
	/**
	 * 
	 * Description:添加文件
	 * @param pPath
	 * @param pOption
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-28 下午1:52:22</pre>
	 */
	public boolean addFile(IWebview pWebViewImpl, String pPath, JSONObject pOption){
		boolean _ret = false;
		UploadFile _uploadFile = new UploadFile();
		try {
			// 适配AndroidQ，"content://"文件（如相册获取）
			if (pPath.startsWith("content://")) {
				Uri contentUri = Uri.parse(pPath);
				Cursor cursor = pWebViewImpl.getContext().getContentResolver().query(contentUri, null, null, null, null);
				if(cursor != null){
					InputStream inputStream = pWebViewImpl.getContext().getContentResolver().openInputStream(contentUri);
					cursor.moveToFirst();
					int size = cursor.getInt(cursor.getColumnIndex(MediaStore.Images.Media.SIZE));
					String name = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME));
					String type = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.MIME_TYPE));
					_uploadFile.mFileInputS = (FileInputStream) inputStream;
					_uploadFile.mFileSize = size;
					String _key = pOption.optString("key", name);
					_uploadFile.mFilename = pOption.optString("name", name);
					_uploadFile.mMimetype = pOption.optString("mime", type);
					_ret = mUploadNetWork.addFile(_key, _uploadFile);
					//TO 成功
					cursor.close();
				}
			} else {
				File _file = new File(pPath);
				_uploadFile.mFileInputS = new FileInputStream(_file);
				_uploadFile.mFileSize = _file.length();
				String _key = pOption.optString("key", _file.getName());
				_uploadFile.mFilename = pOption.optString("name", _file.getName());
				_uploadFile.mMimetype = pOption.optString("mime", PdrUtil.getMimeType(pPath));
				_ret = mUploadNetWork.addFile(_key, _uploadFile);
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return _ret;
	}
	/**
	 * 
	 * Description:添加内容
	 * @param pKey
	 * @param pValue
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-28 下午12:05:31</pre>
	 */
	public boolean addData(String pKey, String pValue){
		return mUploadNetWork.addParemeter(pKey, pValue);
	}
	
	/**
	 * 
	 * Description:获取上传对象json串
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-28 下午1:53:43</pre>
	 */
	public String toJsonUpload(){
		return StringUtil.format("{state:%d,status:%d,uploadedSize:%d,totalSize:%d,headers:%s}",
				mState,mUploadNetWork.mStatus,mUploadNetWork.mUploadedSize,mUploadNetWork.mTotalSize, mUploadNetWork.responseHeaders);
	}
	
	public void setRequestHeader(String key, String value) {
		mRequestData.addHeader(key, value);
	}

	@Override
	public void onNetStateChanged(NetState state,boolean isAbort) {
		if(state == NetState.NET_INIT){
			mState = STATE_INIT;
			String json = toJsonUpload();
			JSUtil.excUploadCallBack(mWebview,json, mUUID);
		}else if(state == NetState.NET_REQUEST_BEGIN){
			mState = STATE_CONNECTING;
			String json = toJsonUpload();
			JSUtil.excUploadCallBack(mWebview,json, mUUID);
		}else if(state == NetState.NET_CONNECTED){
			mState = STATE_CONNECTED;
			String json = toJsonUpload();
			JSUtil.excUploadCallBack(mWebview,json, mUUID);
		}else if(state == NetState.NET_HANDLE_ING){
			mState = STATE_RECEIVING;
			String json = toJsonUpload();
			JSUtil.excUploadCallBack(mWebview,json, mUUID);
		}else if(state == NetState.NET_HANDLE_END || state == NetState.NET_ERROR){
			mState = STATE_COMPLETED;
			UploadMgr.getUploadMgr().removeNetWork(mUploadNetWork);
			String json = "{state:%d,status:%d,filename:'%s',responseText:%s,headers:%s}";
			String rt = JSONUtil.toJSONableString(mUploadNetWork.getResponseText());
			JSUtil.excUploadCallBack(mWebview,StringUtil.format(json, mState,mUploadNetWork.mStatus,mUploadNetWork.mUploadingFile.toString(),rt,mUploadNetWork.responseHeaders),mUUID);
		}
//			try {
//				String filePath = mFullRootPath + mFileName;
//				if (!DHFile.isExist(filePath)) {
//					DHFile.createNewFile(filePath);
//				}
//				mFileOs = new FileOutputStream(filePath);
//			} catch (IOException e) {
//				e.printStackTrace();
//				SharedPreferences _sp = mWebview.getContext().obtainContext().getSharedPreferences(StringConst.SPNAME_DOWNLOAD, Context.MODE_PRIVATE);
//				Editor _ed = _sp.edit();
//				_ed.putLong(mUrl, mFileSize);
//				_ed.commit();
//			}
	}
	
	
	@Override
	public void onResponsing(InputStream os) {
	}

	@Override
	public int onReceiving(InputStream is) {
		return 0;
	}
	
	class UploadFile implements UploadItem{
		FileInputStream mFileInputS;
		long mFileSize;
		String mFilename;
		String mMimetype;
		String mRange;
	}
	public static class UploadString implements UploadItem{
		String mData;
		public UploadString(String value){
			this.mData = value;
		}
	}

	interface UploadItem{

	}

	@Override
	public void onResponseState(int pState, String pStatusText) {
	}
}
