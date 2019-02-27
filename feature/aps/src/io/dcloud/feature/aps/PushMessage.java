package io.dcloud.feature.aps;

import android.os.Bundle;
import android.text.TextUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.IReflectAble;
import io.dcloud.common.adapter.io.DHFile;
import io.dcloud.common.adapter.util.DeviceInfo;
import io.dcloud.common.adapter.util.PlatformUtil;
import io.dcloud.common.util.BaseInfo;
import io.dcloud.common.util.PdrUtil;

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
     * 消息的业务数据 JSONObject类型的对象
     */
    public JSONObject mPayloadJSON = null;
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
	/**显示图标*/
	public String mIconPath = null;

	public boolean mIsStreamApp = false;
	/**
	 * Description: 构造函数
	 *
	 * <pre>
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-14 下午5:45:42
	 * </pre>
	 * @param defaultAppid TODO
	 * @param defaultTitle TODO
	 */
	public PushMessage(String pJsonMsg, String defaultAppid,String defaultTitle) {
		init(pJsonMsg,null,defaultAppid,defaultTitle);
	}

	public PushMessage(String pJsonMsg, IApp app) {
		String defaultAppid = app != null ? app.obtainAppId() : null;
		String defaultTitle = app != null ? app.obtainAppName() : null;
		init(pJsonMsg,app,defaultAppid,defaultTitle);
	}

	private void init(String pJsonMsg, IApp app, String defaultAppid,String defaultTitle){
		mIsStreamApp = app != null ? app.isStreamApp() : mIsStreamApp;
		mUUID = getMessageUUID();
		parseJson(pJsonMsg, app,defaultAppid, defaultTitle);
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
	 * @param app TODO
	 *
	 * @throws JSONException
	 * <p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-21 下午4:57:46
	 * </pre>
	 */
	private void parseJson(String pJsonMsg, IApp app, String defaultAppid,String defaultTitle){
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
                if (!PdrUtil.isEmpty(_json.optJSONObject("payload"))) {
                    mPayloadJSON=_json.optJSONObject("payload");
                }else{
                    mPayload = _json.optString("payload");
                }

			}else{
				if(_json.has("Payload")){//兼容本地创建消息
                    if (!PdrUtil.isEmpty(_json.optJSONObject("Payload"))) {
                        mPayloadJSON=_json.optJSONObject("Payload");
                    }else{
                        mPayload = _json.optString("Payload");
                    }
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
            if (_option != null) {
                mIconPath = app != null ? app.convert2AbsFullPath(_option.optString("icon")) : convert2AbsFullPath(_option.optString("icon"), mMessageAppid);
            }
		}else{
			needCreateNotifcation = false;
			mContent = pJsonMsg;
			mPayload = pJsonMsg;
			mTitle = defaultTitle;
		}
	}
    public String convert2AbsFullPath(String pSrcPath,String mAppid) {
        try {//pSrcPath路径为真实存在路径则不进行修改，直接返回原值
            if (!PdrUtil.isEmpty(pSrcPath) && DHFile.isExist(pSrcPath)) return pSrcPath;
        } catch (IOException e) {
            e.printStackTrace();
        }
        int p;
        if (PdrUtil.isEmpty(pSrcPath)) {
            return pSrcPath;
        }
        if ((p = pSrcPath.indexOf("?")) > 0) {
            pSrcPath = pSrcPath.substring(0, p);
        }
        if (pSrcPath.startsWith(BaseInfo.REL_PUBLIC_DOCUMENTS_DIR + "/")) {    //		_documents/
            pSrcPath = BaseInfo.sDocumentFullPath + pSrcPath.substring((BaseInfo.REL_PUBLIC_DOCUMENTS_DIR + "/").length());
        } else if (pSrcPath.startsWith(BaseInfo.REL_PUBLIC_DOCUMENTS_DIR)) {    //		_documents
            pSrcPath = BaseInfo.sDocumentFullPath + pSrcPath.substring(BaseInfo.REL_PUBLIC_DOCUMENTS_DIR.length());
        } else if (pSrcPath.startsWith(BaseInfo.REL_PRIVATE_DOC_DIR + "/")) {    //		_doc/
            pSrcPath = BaseInfo.sBaseFsAppsPath + mAppid + "/" + BaseInfo.REAL_PRIVATE_DOC_DIR + pSrcPath.substring((BaseInfo.REL_PRIVATE_DOC_DIR + "/").length());
        } else if (pSrcPath.startsWith(BaseInfo.REL_PRIVATE_DOC_DIR)) {    //		_doc
            pSrcPath = BaseInfo.sBaseFsAppsPath + mAppid + "/" + BaseInfo.REAL_PRIVATE_DOC_DIR + pSrcPath.substring(BaseInfo.REL_PRIVATE_DOC_DIR.length());
        } else if (pSrcPath.startsWith(BaseInfo.REL_PUBLIC_DOWNLOADS_DIR + "/")) {        //		_downloads/
            pSrcPath = BaseInfo.sDownloadFullPath + pSrcPath.substring((BaseInfo.REL_PUBLIC_DOWNLOADS_DIR + "/").length());
        } else if (pSrcPath.startsWith(BaseInfo.REL_PUBLIC_DOWNLOADS_DIR)) {        //		_downloads
            pSrcPath = BaseInfo.sDownloadFullPath + pSrcPath.substring(BaseInfo.REL_PUBLIC_DOWNLOADS_DIR.length());
        } else if (pSrcPath.startsWith(BaseInfo.REL_PRIVATE_WWW_DIR + "/")) {        //		_www/
            pSrcPath = BaseInfo.sCacheFsAppsPath + mAppid + "/www/" + pSrcPath.substring((BaseInfo.REL_PRIVATE_WWW_DIR + "/").length());
            try {
                if(!DHFile.isExist(pSrcPath)){
                    pSrcPath = BaseInfo.sBaseResAppsPath + mAppid + "/" + BaseInfo.APP_WWW_FS_DIR + pSrcPath.substring((BaseInfo.REL_PRIVATE_WWW_DIR + "/").length());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (pSrcPath.startsWith(BaseInfo.REL_PRIVATE_WWW_DIR)) {
            pSrcPath = BaseInfo.sCacheFsAppsPath + mAppid + "/www/" + pSrcPath.substring(BaseInfo.REL_PRIVATE_WWW_DIR.length());
            try {
                if(!DHFile.isExist(pSrcPath)){
                    pSrcPath = BaseInfo.sBaseResAppsPath + mAppid + "/" + BaseInfo.APP_WWW_FS_DIR + pSrcPath.substring(BaseInfo.REL_PRIVATE_WWW_DIR.length());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (pSrcPath.startsWith("file://")) {
            pSrcPath = pSrcPath.substring("file://".length());
        } else if (pSrcPath.startsWith(DeviceInfo.sDeviceRootDir)) {
            return pSrcPath;
        }
        return pSrcPath;
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
		this.mIconPath = b.getString("icon");
		this.mIsStreamApp = b.getBoolean("isstreamapp");
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
		if (!PdrUtil.isEmpty(mPayloadJSON)) {
			b.putString("payload", this.mPayloadJSON.toString());
		}else{
			b.putString("payload", mPayload);
		}
		b.putString("icon", this.mIconPath);
		b.putBoolean("isstreamapp", this.mIsStreamApp);
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
            if (!PdrUtil.isEmpty(mPayloadJSON)) {
                json.put("payload", mPayloadJSON);
            }else{
                json.put("payload", mPayload);
            }
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return json.toString();
	}
}
