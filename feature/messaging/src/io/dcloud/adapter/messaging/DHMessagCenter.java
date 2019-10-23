package io.dcloud.adapter.messaging;

import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telephony.SmsManager;

/**
 * <p>
 * Description:Messaging实际处理
 * </p>
 * 
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-1-8 下午4:10:43 created.
 * 
 *       <pre>
 * <p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-8 下午4:10:43
 * </pre>
 */
class DHMessagCenter {
	
	private static Context mContext;
	/**
	 * 
	 * Description:初始化消息中心（设置上下文）
	 * @param pContext
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-9 下午5:41:45</pre>
	 */
	protected static void initDHMessaging(Context pContext){
		mContext = pContext;
	}
	/**
	 * 
	 * Description:发送邮件
	 * @param pMessaging
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-8 下午6:05:36</pre>
	 */
	protected static void sendMail(DHMessaging pMessaging) {
		try {
			if(pMessaging == null){
				return;
			}
			Intent mEmailIntent = new Intent();
			Uri uri = Uri.parse("mailto:");
	    	mEmailIntent.setData(uri);
			mEmailIntent.putExtra(Intent.EXTRA_EMAIL, pMessaging.mTo);
			if(pMessaging.mCc != null && pMessaging.mCc.length > 0){
				mEmailIntent.putExtra(Intent.EXTRA_CC, pMessaging.mCc);
			}
			if(pMessaging.mBcc != null && pMessaging.mBcc.length > 0){
				mEmailIntent.putExtra(Intent.EXTRA_BCC, pMessaging.mBcc);
			}
			
			mEmailIntent.putExtra(Intent.EXTRA_SUBJECT, pMessaging.mSubject);
			mEmailIntent.putExtra(Intent.EXTRA_TEXT,pMessaging.mBody);
			
			int attachMentsSize = pMessaging.mAttachments != null ? pMessaging.mAttachments.size()  : 0;
			  if( attachMentsSize > 1 ){
				  mEmailIntent.setAction(Intent.ACTION_SEND_MULTIPLE);
		        	mEmailIntent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, pMessaging.mAttachments);
		        	mEmailIntent.setType("application/octet-stream");
		        	mEmailIntent.setType("message/rfc822");
		        }else if( attachMentsSize == 1 ){
				 	mEmailIntent.putExtra(Intent.EXTRA_STREAM, pMessaging.mAttachments.get(0));
		        	mEmailIntent.setType(PdrUtil.getMimeType(pMessaging.mAttachments.get(0).getPath()));
				  	mEmailIntent.setType("message/rfc882");
		        	mEmailIntent.setAction(Intent.ACTION_SEND);
		        }else{
		        	mEmailIntent.setAction(Intent.ACTION_SENDTO);
		        	//mEmailIntent.setType("plain/text");
		        }
			  
			//TODO 发送附件 图片
			pMessaging.mWebview.getActivity().startActivity(mEmailIntent);
			JSUtil.execCallback(pMessaging.mWebview, pMessaging.mCallbackId,"", JSUtil.OK, false, false);
		} catch (Exception e) {
			e.printStackTrace();
			String _json = DOMException.toJSON(DOMException.CODE_MESSAGING_ERROR, e.getLocalizedMessage());
			JSUtil.execCallback(pMessaging.mWebview, pMessaging.mCallbackId,_json ,JSUtil.ERROR, true, false);
		}
	}
	
	/**
	 * 
	 * Description:发送短信
	 * @param pMessaging
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-8 下午5:59:34</pre>
	 */
	protected static void sendMessage(DHMessaging pMessaging) {
		try {
			if(pMessaging != null){
				String _phoneCode = null;
				if(pMessaging.mTo !=null && pMessaging.mTo.length>0){
					StringBuffer _sb = new StringBuffer();
					for(String _s : pMessaging.mTo){
						_sb.append(_s+";");
					}
					_phoneCode = _sb.toString();
				}
				if(pMessaging.mSilent){
					SmsManager smsManager = SmsManager.getDefault();
					smsManager.sendTextMessage(_phoneCode , null, pMessaging.mBody, null, null);
				}else{
					Uri _uri = Uri.parse("sms:"+_phoneCode);
					Intent _intent = new Intent(Intent.ACTION_VIEW, _uri);
					_intent.putExtra("address", _phoneCode);
					_intent.putExtra("sms_body", pMessaging.mBody);
					pMessaging.mWebview.getActivity().startActivity(_intent);
				}
			}
			JSUtil.execCallback(pMessaging.mWebview, pMessaging.mCallbackId,"", JSUtil.OK, false, false);
		} catch (Exception e) {
			e.printStackTrace();
			String _json = DOMException.toJSON(DOMException.CODE_MESSAGING_ERROR, e.getLocalizedMessage());
			JSUtil.execCallback(pMessaging.mWebview, pMessaging.mCallbackId,_json ,JSUtil.ERROR, true, false);
		}
		
	}
	public static void sendMMS(DHMessaging _messaging) {
		try {
			Intent sendIntent = new Intent(Intent.ACTION_SEND);
			Uri uri = Uri.parse("mms:");
			sendIntent.setData(uri);
	//		sendIntent.setPackage("com.android.mms");
	//	    sendIntent.setType("image/jpeg");
		    sendIntent.setType("*/*");
		    if(_messaging.mAttachments != null && _messaging.mAttachments.size() > 0){
		    	sendIntent.putExtra(Intent.EXTRA_STREAM,_messaging.mAttachments.get(0) );
		    }
		    sendIntent.putExtra(Intent.EXTRA_TEXT, _messaging.mBody);
		    _messaging.mWebview.getActivity().startActivity(sendIntent);
		    JSUtil.execCallback(_messaging.mWebview, _messaging.mCallbackId,"", JSUtil.OK, false, false);
		} catch (Exception e) {
			e.printStackTrace();
			String _json = DOMException.toJSON(DOMException.CODE_MESSAGING_ERROR, e.getLocalizedMessage());
			JSUtil.execCallback(_messaging.mWebview, _messaging.mCallbackId,_json ,JSUtil.ERROR, true, false);
		}
	}
}
