package io.dcloud.adapter.messaging;

import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.net.Uri;

/**
 * <p>Description:需要发送的message对象</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-1-8 下午5:47:00 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-8 下午5:47:00</pre>
 */
class DHMessaging {
	String mCallbackId;
	/**
	 * message的类型（短信，邮件）
	 */
	int mType;
	/**短信*/
	public static final int TYPE_SMS = 1;
	/**彩信*/
	public static final int TYPE_MMS = 2;
	/**邮件*/
	public static final int TYPE_EMAIL = 3;
	/**
	 * 收件人
	 */
	String[] mTo;
	/**
	 * 抄送人
	 */
	String[] mCc;
	/**
	 * 密送人
	 */
	String[] mBcc;
	/**
	 * 主题
	 */
	String mSubject;
	/**
	 * 内容
	 */
	String mBody;
	/**
	 * webview
	 */
	IWebview mWebview;
	/**
	 * 附件集合
	 */
	ArrayList< Uri> mAttachments;
	/**
	 * 是否后台发送
	 */
	boolean mSilent = false;
	private DHMessaging(){
		
	}
	/**
	 * 
	 * Description:设置webview
	 * @param pWebview
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-9 下午5:09:23</pre>
	 */
	protected void setWebview(IWebview pWebview){
		mWebview = pWebview;
	}
	
	/**
	 * 
	 * Description:根据json字符获得message对象
	 * @param pJsonMessage
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-9 下午2:17:34</pre>
	 */
	protected static DHMessaging parseMessage(IWebview pWebViewImpl,String pCallid,String pJsonMessage){
		DHMessaging _messaging = new DHMessaging();
		_messaging.mCallbackId =pCallid;
		_messaging.setWebview(pWebViewImpl);
		try {
			JSONObject _jsonObj = new JSONObject(pJsonMessage);
			_messaging.mType = _jsonObj.getInt("type");
//			_messaging.mBodyType = _jsonObj.getInt("bodyType");
			if(!_jsonObj.isNull("to")){
				_messaging.mTo = JSUtil.jsonArrayToStringArr((JSONArray) _jsonObj.get("to"));
			}
			if(!_jsonObj.isNull("body")){
				_messaging.mBody = (String) _jsonObj.get("body");
			}
			if(!_jsonObj.isNull("silent")){
				_messaging.mSilent = PdrUtil.parseBoolean(JSONUtil.getString(_jsonObj, "silent"), _messaging.mSilent, false);
			}
			if(_messaging.mType == TYPE_SMS){//发送短信
				DHMessagCenter.sendMessage(_messaging);
			}else if(_messaging.mType == TYPE_MMS){//发送彩信
				if(!_jsonObj.isNull("attachment")){
					_messaging.mAttachments = new ArrayList<Uri>();
					JSONArray atts = (JSONArray)_jsonObj.get("attachment");
					IApp app = pWebViewImpl.obtainFrameView().obtainApp();
					String s = app.convert2AbsFullPath(pWebViewImpl.obtainFullUrl(),atts.getString(0));
					Uri r = Uri.parse(s.startsWith("file://") ? s : "file://" + s);
					_messaging.mAttachments.add(r);
				}
				DHMessagCenter.sendMMS(_messaging);
			}else if(_messaging.mType == TYPE_EMAIL){//发送邮件
				if(!_jsonObj.isNull("cc")){
					_messaging.mCc = JSUtil.jsonArrayToStringArr((JSONArray)_jsonObj.get("cc"));
				}
				if(!_jsonObj.isNull("bcc")){
					_messaging.mBcc = JSUtil.jsonArrayToStringArr((JSONArray) _jsonObj.get("bcc"));
				}
				if(!_jsonObj.isNull("subject")){
					_messaging.mSubject = _jsonObj.getString("subject");
				}
				if(!_jsonObj.isNull("attachment")){
					_messaging.mAttachments = new ArrayList<Uri>();
					JSONArray atts = (JSONArray)_jsonObj.get("attachment");
					IApp app = pWebViewImpl.obtainFrameView().obtainApp();
					for(int i=0; i<atts.length(); i++){
						String s = app.convert2AbsFullPath(pWebViewImpl.obtainFullUrl(),atts.getString(i));
						Uri r = Uri.parse(s.startsWith("file://") ? s : "file://" + s);
						_messaging.mAttachments.add(r);
					}
					
				}
				DHMessagCenter.sendMail(_messaging);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return _messaging;
	}
}
