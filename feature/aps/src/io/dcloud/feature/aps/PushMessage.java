package io.dcloud.feature.aps;

import io.dcloud.common.DHInterface.IReflectAble;
import io.dcloud.common.adapter.util.PlatformUtil;

import org.json.JSONException;
import org.json.JSONObject;

import android.os.Bundle;
import android.text.TextUtils;

/**
 * <p>
 * Description:push的消息对象
 * </p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-3-13 下午2:39:39 created.
 * 
 *       <pre>
 * <p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-13 下午2:39:39
 * </pre>
 */
public class PushMessage implements IReflectAble{
	private boolean needCreateNotifcation = true;
	/**
	 * Message对应的appid
	 */
	public String mMessageAppid = null;
	/**
	 * 对应JS层的消息对象ID
	 */
	public String mUUID = null;
	/**
	 * 消息内容content
	 */
	public String mContent = null;
	/**
	 * 消息的业务数据
	 */
	public String mPayload = null;
	/**
	 * 消息声音
	 */
	public String sound = "system";
	/**
	 * 消息显示的时间
	 */
	public long mWhen = 0;
	/**
	 * 消息的标题
	 */
	public String mTitle = null;
	/**
	 * 是否覆盖上一个
	 */
	protected boolean isCover = PlatformUtil.APS_COVER;
	/**
	 * 延迟发送时间
	 */
	protected long mDelay = 0;
	/**
	 * 通知的ID
	 */
	public int nID;

	/**
	 * Description: 构造函数
	 *
	 * <pre>
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-14 下午5:45:42
	 * </pre>
	 * @param defaultName TODO
	 * @param defaultTitle TODO
	 */
	public PushMessage(String pJsonMsg, String defaultAppid, String defaultTitle) {
		mUUID = getMessageUUID();
		parseJson(pJsonMsg, defaultAppid, defaultTitle);
		setNotificationID();
	}

	public PushMessage(Bundle bundle){
		parse(bundle);
	}
	
	public boolean needCreateNotifcation(){
		return needCreateNotifcation;//payload为空串时需要创建
	}
	/**
	 * 
	 * Description:获取消息的UUID
	 * 
	 * @return
	 *
	 *         <pre>
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-19 下午2:42:44
	 * </pre>
	 */
	public String getMessageUUID() {
		// return "androidPushMsg" + j++;
		return "androidPushMsg" + hashCode();
	}

	protected  static int mNotificationId = 1;

	/**
	 * 
	 * Description:设置通知的ID
	 *
	 * <pre>
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-19 下午3:24:48
	 * </pre>
	 */
	private void setNotificationID() {
		if (!isCover) {
			mNotificationId++;
		}
		nID = mNotificationId;
	}

	/**
	 * 
	 * Description:解析js层传入的message对象
	 * @param pJsonMsg
	 * @param defaultAppid TODO
	 * @param defaultTitle TODO
	 * 
	 * @throws JSONException
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-21 下午4:57:46
	 * </pre>
	 */
	private void parseJson(String pJsonMsg, String defaultAppid, String defaultTitle){
		//{"appid":"H5BCD03E4","title":"Hell5","content":"翘起","payload":"asdadf","options":{}}  正常json
		//{"Payload":"LocalMSG","message":"2015-12-17 19:53:14: 欢迎使用Html5 Plus创建本地消息！","__UUID__":null,"options":{"cover":false}} aps.js原因导致不正规
		JSONObject _json = null;
		try {
			_json = new JSONObject(pJsonMsg);//
		} catch (JSONException e) {
			e.printStackTrace();
		}
		if(_json != null){
			String t_appid = _json.optString("appid");
			//消息中具有三个节点才推到消息中心，其中有一个没有值均不创建，执行receive事件
			if(_json.has("content")){
				mContent = _json.optString("content");
			}else{
				if(_json.has("message")){//兼容本地创建消息
					mContent = _json.optString("message");
				}else{
					needCreateNotifcation = false;
					mContent = pJsonMsg;
				}
			}
			if(_json.has("payload")){
				mPayload = _json.optString("payload");
			}else{
				if(_json.has("Payload")){//兼容本地创建消息
					mPayload = _json.optString("Payload");
				}else{
					needCreateNotifcation = false;
					mPayload = pJsonMsg;
				}
			}
			JSONObject _option = _json.optJSONObject("options");
			if(_json.has("title")){
				mTitle = _json.optString("title");
			}else{
				if(_option != null && _option.has("title")){//兼容本地创建消息
					mTitle = _option.optString("title");
				}else{
					needCreateNotifcation = false;
					mTitle = defaultTitle;
				}
			}
			if (_option != null) {
				isCover = _option.optBoolean("cover");
				if("none".equals(_option.optString("sound"))){
					sound = "none";
				}
				mWhen = _option.optLong("when");
				mDelay = _option.optLong("delay");
				if(TextUtils.isEmpty(t_appid)) {
					//个推、手助推送下消息格式为{appid:'helloh5',payload:'',content:'',options:''}
					//本地创建消息格式为{"__UUID__":null,"message":"显示内容","Payload":"{'msg':'msg'}","options":{"appid":'HelloH5',"sound":"none","delay":1}}
					t_appid = _option.optString("appid");
				}
			}

			if(TextUtils.isEmpty(t_appid)) {
				t_appid = defaultAppid;
			}
			mMessageAppid = t_appid;
		}else{
			needCreateNotifcation = false;
			mContent = pJsonMsg;
			mPayload = pJsonMsg;
			mTitle = defaultTitle;
		}
	}

	public void parse(Bundle b){
		this.mTitle = b.getString("title");
		this.mContent = b.getString("content");
		this.nID = b.getInt("nId");
		this.mWhen = b.getLong("when");
		this.sound = b.getString("sound");
		this.mMessageAppid = b.getString("appid");
		this.mUUID = b.getString("uuid");
		this.mPayload = b.getString("payload");
	}
	
	public Bundle toBundle(){
		Bundle b = new Bundle();
		b.putString("title", this.mTitle);
		b.putString("content", this.mContent);
		b.putInt("nId", this.nID);
		b.putLong("when", this.mWhen);
		b.putString("sound", this.sound);
		b.putString("appid", this.mMessageAppid);
		b.putString("uuid", this.mUUID);
		b.putString("payload", this.mPayload);
		return b;
	}

	/**
	 * 
	 * Description:获取Message的json对象
	 * 
	 * @return
	 *
	 *         <pre>
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-18 下午9:12:21
	 * </pre>
	 */
	public String toJSON() {
		JSONObject json = new JSONObject();
		try {
			json.put("__UUID__", mUUID);
			json.put("title", mTitle);
			json.put("appid", mMessageAppid);
			json.put("content", mContent);
			json.put("payload", mPayload);
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json.toString();
	}
}
