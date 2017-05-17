package io.dcloud.share.qq;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.tencent.connect.common.Constants;
import com.tencent.connect.share.QQShare;
import com.tencent.tauth.IUiListener;
import com.tencent.tauth.Tencent;
import com.tencent.tauth.UiError;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;

import io.dcloud.RInformation;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.ISysEventListener.SysEventType;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.io.DHFile;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.PlatformUtil;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.share.IFShareApi;

/**
 * QQ分享
 * @author shutao
 */
public class QQApiManager implements IFShareApi {
	private static final String PACKAGENAME = "com.tencent.mobileqq";
	
	private String APPID;
	private Tencent mTencent;
	private String QQ_SHARE_ID = "qq";
	private String QQ_SHARE_DES = "QQ";
	private MyIUiListener myIUiListener;

	@Override
	public void initConfig() {
		// TODO Auto-generated method stub
		APPID = AndroidResources.getMetaValue("QQ_APPID");
	}

	@Override
	public String getId() {
		// TODO Auto-generated method stub
		return QQ_SHARE_ID;
	}
	
	/**
	 * 供原生代码分享调用
	 * @param context
	 * @param pShareMsg
	 */
	public void send(Activity context, String pShareMsg) {
		initConfig();
		if (mTencent == null) {
			mTencent = Tencent.createInstance(APPID, context);
		}
		Bundle params = new Bundle();
		try {
			JSONObject _msg = new JSONObject(pShareMsg);
			String _content = _msg.optString("content");
			String href = _msg.getString("href");
			String thumbs = _msg.getString("thumbs");
			String title = _msg.getString("title");

			if (!TextUtils.isEmpty(_content)) {
				params.putString(QQShare.SHARE_TO_QQ_SUMMARY, _content);
			}
			params.putString(QQShare.SHARE_TO_QQ_TITLE, title);
			params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
		    params.putString(QQShare.SHARE_TO_QQ_TARGET_URL,  href); // 链接
		    params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, thumbs);
		    mTencent.shareToQQ(context, params, new IUiListener() {
				
				@Override
				public void onError(UiError arg0) {
					// TODO Auto-generated method stub
				}
				
				@Override
				public void onComplete(Object arg0) {
					// TODO Auto-generated method stub
					
				}
				
				@Override
				public void onCancel() {
					// TODO Auto-generated method stub
					
				}
			});
		} catch(Exception e){
			e.printStackTrace();
		}
	}

	@Override
	public void send(IWebview pWebViewImpl, String pCallbackId, String pShareMsg) {
		// TODO Auto-generated method stub
		if (mTencent == null) {
               mTencent = Tencent.createInstance(APPID, pWebViewImpl.getActivity());
		}
		pWebViewImpl.obtainApp().registerSysEventListener(new ISysEventListener() {
			
			@Override
			public boolean onExecute(SysEventType pEventType, Object pArgs) {
				// TODO Auto-generated method stub
				Object[] _args = (Object[]) pArgs;
				int requestCode = (Integer) _args[0];
				int resultCode = (Integer) _args[1];
				Intent data = (Intent) _args[2];
				if (requestCode == Constants.REQUEST_QQ_SHARE) {
		        	if (resultCode == Constants.ACTIVITY_OK) {
		        		Tencent.handleResultData(data, myIUiListener);
		        	}
		        }
				return false;
			}
		}, SysEventType.onActivityResult);
		Bundle params = new Bundle();
		try {
			JSONObject _msg = new JSONObject(pShareMsg);
			String _content = _msg.optString("content");
			String _title = _msg.optString("title");
			String href = JSONUtil.getString(_msg, "href");
			JSONArray _thumbs = _msg.optJSONArray("thumbs");
			JSONArray _pictures = _msg.optJSONArray("pictures");
			String imgPath = _pictures != null ? _pictures.optString(0, null) : null;
			String thumbImgPath = _thumbs != null ? _thumbs.optString(0, null) : null;
			//JSONObject extraInfo = JSONUtil.getJSONObject(_msg, "extra");
			if (!TextUtils.isEmpty(_title)) {
				params.putString(QQShare.SHARE_TO_QQ_TITLE, _title); 
			}
			if (!TextUtils.isEmpty(_content)) {
				params.putString(QQShare.SHARE_TO_QQ_SUMMARY, _content);
			}
		    if(!PdrUtil.isEmpty(href)) {
		    	// 有链接 默认模式
				params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_DEFAULT);
			    params.putString(QQShare.SHARE_TO_QQ_TARGET_URL,  href); // 链接
		    	if (!TextUtils.isEmpty(imgPath)) {
		    		setBundleImgUrl(pWebViewImpl, imgPath, params, true);
		    	} else if (!TextUtils.isEmpty(thumbImgPath)) {
		    		setBundleImgUrl(pWebViewImpl, thumbImgPath, params, true);
		    	}
		    } else {
		    	//无连接 表示图片模式
			    if (!TextUtils.isEmpty(imgPath)) {
			    	setBundleImgUrl(pWebViewImpl, imgPath, params, false);
			    	params.putInt(QQShare.SHARE_TO_QQ_KEY_TYPE, QQShare.SHARE_TO_QQ_TYPE_IMAGE);
			    } else {
			    	callBackError(pWebViewImpl, pCallbackId, DOMException.toString("非纯图片分享必须传递链接地址！"), DOMException.CODE_PARAMETER_ERRORP);
			    	return;
			    }
		    }
		    myIUiListener = new MyIUiListener(pWebViewImpl, pCallbackId);
		    mTencent.shareToQQ(pWebViewImpl.getActivity(), params, myIUiListener);

		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			callBackError(pWebViewImpl, pCallbackId, DOMException.toString(DOMException.MSG_UNKNOWN_ERROR), DOMException.CODE_UNKNOWN_ERROR);
		}
	}
	
	/**
	 * 判断是否为网络地址 ，不是补全本地的图片地址
	 * 并对bundle补全图片参数 
	 * @param pWebViewImpl
	 * @param imgPath
	 * @param defaultImg TODO
	 * @return
	 */
	private void setBundleImgUrl(IWebview pWebViewImpl, String imgPath, Bundle params, boolean defaultImg) {
		if (PdrUtil.isNetPath(imgPath)) { // 网络图片
			params.putString(QQShare.SHARE_TO_QQ_IMAGE_URL, imgPath);
		} else {
			imgPath = pWebViewImpl.obtainFrameView().obtainApp().convert2LocalFullPath(pWebViewImpl.obtainFullUrl(), imgPath);
			if(!DHFile.exists(imgPath) && defaultImg){//判断要分享文件是否存在，如果不存在则使用默认图标
				InputStream is = pWebViewImpl.getActivity().getResources().openRawResource(RInformation.DRAWABLE_ICON);
				imgPath = pWebViewImpl.obtainFrameView().obtainApp().obtainAppTempPath() + System.currentTimeMillis();//临时目录，当应用退出的时候会删除
				try {
					DHFile.writeFile(is, imgPath);
					is.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			params.putString(QQShare.SHARE_TO_QQ_IMAGE_LOCAL_URL, imgPath);
		}
	}

	class MyIUiListener implements IUiListener {
		String pCallbackId;
		IWebview pWebViewImpl;
		public MyIUiListener(IWebview pWebViewImpl, String CallBackId) {
			this.pCallbackId = CallBackId;
			this.pWebViewImpl = pWebViewImpl;
		}
		
		@Override
		public void onCancel() {
			// TODO Auto-generated method stub
			callBackError(pWebViewImpl, pCallbackId, DOMException.toString(DOMException.MSG_USER_CANCEL), DOMException.CODE_USER_CANCEL);
		}

		@Override
		public void onComplete(Object arg0) {
			// TODO Auto-generated method stub
			JSUtil.execCallback(pWebViewImpl, pCallbackId, arg0.toString(), JSUtil.OK, false, false);
		}

		@Override
		public void onError(UiError arg0) {
			// TODO Auto-generated method stub
			callBackError(pWebViewImpl, pCallbackId, DOMException.toString(arg0.errorCode, "ShareQQ分享", arg0.errorMessage, mLink), DOMException.CODE_BUSINESS_INTERNAL_ERROR);
		}
		
	}

	@Override
	public void forbid(IWebview pWebViewImpl) {
		// TODO Auto-generated method stub
		/*if (mTencent == null) {
			mTencent = Tencent.createInstance(APPID, pWebViewImpl.getActivity());
		}*/
	}

	public static final String AUTHORIZE_TEMPLATE = "{authenticated:%s,accessToken:'%s'}";
    public static final String KEY_APPID = "appid";
    private static final String TAG = "WeiXinApiManager";
	@Override
	public void authorize(IWebview pWebViewImpl, String pCallbackId,String options) {
		// TODO Auto-generated method stub
        JSONObject jsonOptions=JSONUtil.createJSONObject(options);
        if(jsonOptions != null){
            APPID = jsonOptions.optString(KEY_APPID, APPID);
            Logger.e(TAG, "authorize: appId"+APPID );
        }
        if(PdrUtil.isEmpty(APPID)){
            String msg = String.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_BUSINESS_PARAMETER_HAS_NOT, DOMException.toString(DOMException.MSG_BUSINESS_PARAMETER_HAS_NOT));
            JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.ERROR, true, false);
            return;
        }
			// QQ分享目前不需要授权。所以这里直接返回授权成功回调
		String msg = String.format(AUTHORIZE_TEMPLATE, "true","");
		JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.OK, true, false);
	}

	@Override
	public String getJsonObject(IWebview pWebViewImpl) {
		// TODO Auto-generated method stub
		String _json = null;
		try {
			JSONObject _weiXinObj = new JSONObject();
			_weiXinObj.put(StringConst.JSON_SHARE_ID, QQ_SHARE_ID);
			_weiXinObj.put(StringConst.JSON_SHARE_DESCRIPTION, QQ_SHARE_DES);
			_weiXinObj.put(StringConst.JSON_SHARE_AUTHENTICATED,false);
			_weiXinObj.put(StringConst.JSON_SHARE_ACCESSTOKEN, "");
			_weiXinObj.put(StringConst.JSON_SHARE_NATIVECLIENT, PlatformUtil.hasAppInstalled(pWebViewImpl.getContext(), PACKAGENAME));
			_json = _weiXinObj.toString();
		} catch (JSONException e) {
			e.printStackTrace();
		}
		return _json;
	}
	
	/**
	 * 错误JS回调
	 * @param pWebViewImpl 回调页面
	 * @param pCallbackId 回调ID
	 * @param errorMsg 错误信息
	 * @param errorCode 错误号
	 */
	public void callBackError(IWebview pWebViewImpl, String pCallbackId, String errorMsg, int errorCode) {
		String msg = DOMException.toJSON(errorCode, errorMsg);
		JSUtil.execCallback(pWebViewImpl, pCallbackId, msg, JSUtil.ERROR, true, false);
	}

	@Override
	public void dispose() {
		mTencent = null;
		myIUiListener = null;
	}
}
