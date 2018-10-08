package io.dcloud.feature.payment.weixin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;


import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelpay.PayReq;
import com.tencent.mm.opensdk.modelpay.PayResp;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import org.json.JSONObject;

import java.util.Set;

import io.dcloud.ProcessMediator;
import io.dcloud.common.DHInterface.FeatureMessageDispatcher;
import io.dcloud.common.DHInterface.FeatureMessageDispatcher.MessageListener;
import io.dcloud.common.DHInterface.IActivityHandler;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.PlatformUtil;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.feature.payment.AbsPaymentChannel;
import io.dcloud.feature.payment.PaymentResult;


public class WeiXinPay extends AbsPaymentChannel implements ISysEventListener{

	//https://open.weixin.qq.com/zh_CN/htmledition/res/dev/document/sdk/android/index.html
	private static final String ERR_MSG_AUTH_DENIED = "Authentication failed";
	private static final String ERR_MSG_COMM = "General errors";
	private static final String ERR_MSG_SENT_FAILED = "Unable to send";
	private static final String ERR_MSG_UNSUPPORT = "Unsupport error";
	private static final String ERR_MSG_USER_CANCEL = "User canceled";

	private static final String PNAME_MM = "com.tencent.mm";
	private static final String URL_MARKET_DETAILS = "market://search?q=pname:%s";

	private static String APPID;
	private IWXAPI api;

	private boolean isInstallingService = false;

	private String statement = null;
	@Override
	public void init(Context context) {
		super.init(context);
		id = "wxpay";
		description = "微信";
		serviceReady = PlatformUtil.isAppInstalled(mContext, PNAME_MM);
		APPID = AndroidResources.getMetaValue("WX_APPID");
		api = WXAPIFactory.createWXAPI(context, APPID);
		api.registerApp(APPID);
	}
	private boolean hasFullConfigData(){
		return !TextUtils.isEmpty(APPID);
	}
	//返回false时则继续
	private boolean hasGeneralError(){
		if(!hasFullConfigData()){
			mListener.onError(DOMException.CODE_BUSINESS_PARAMETER_HAS_NOT, DOMException.toString(DOMException.MSG_BUSINESS_PARAMETER_HAS_NOT));
			return true;
		}else if(!PlatformUtil.isAppInstalled(mContext, PNAME_MM)){
			mListener.onError(DOMException.CODE_CLIENT_UNINSTALLED, DOMException.toString(DOMException.MSG_CLIENT_UNINSTALLED));
			return true;
		}
		return false;
	}
	@Override
	protected void installService() {
		try {
			String url = String.format(URL_MARKET_DETAILS, PNAME_MM);
			PlatformUtil.openURL(mContext, url, null);
			isInstallingService = true;
			mWebview.obtainApp().registerSysEventListener(this, SysEventType.onResume);
		} catch (Exception e) {
			e.printStackTrace();
			mWebview.obtainApp().unregisterSysEventListener(this, SysEventType.onResume);
		}
	}

	@Override
	public boolean onExecute(SysEventType pEventType, Object pArgs) {
		if(pEventType == SysEventType.onResume){
			serviceReady = PlatformUtil.isAppInstalled(mContext, PNAME_MM);
			isInstallingService = false;
			mWebview.obtainApp().unregisterSysEventListener(this, SysEventType.onResume);
			if(serviceReady){//安装成功后
				if(!PdrUtil.isEmpty(statement)){//存在订单信息时，需要继续安装
					request(statement);
					statement = null;
				}
			}else{//安装不成功
				mListener.onError(DOMException.CODE_CLIENT_UNINSTALLED, DOMException.MSG_CLIENT_UNINSTALLED);
			}
		}
		return false;
	}

	private void startWXPayMediator(PayReq req){
		Intent intent = new Intent();
		intent.putExtra(ProcessMediator.LOGIC_CLASS,WXPayMediator.class.getName());
		Bundle bundle = new Bundle();
		req.toBundle(bundle);
		intent.putExtra(ProcessMediator.REQ_DATA,bundle);
		intent.setClassName(mWebview.getActivity(),ProcessMediator.class.getName());
		mWebview.getActivity().startActivityForResult(intent,ProcessMediator.CODE_REQUEST);
		mWebview.getActivity().overridePendingTransition(0,0);
		mWebview.obtainApp().registerSysEventListener(new ISysEventListener() {
			@Override
			public boolean onExecute(SysEventType pEventType, Object pArgs) {
				Object[] _args = (Object[])pArgs;
				int requestCode = (Integer)_args[0];
				int resultCode = (Integer)_args[1];
				Intent data = (Intent) _args[2];
				if(pEventType == SysEventType.onActivityResult && requestCode == ProcessMediator.CODE_REQUEST){
					Bundle bundle = data.getBundleExtra(ProcessMediator.RESULT_DATA);
					String s = bundle.getString(ProcessMediator.STYLE_DATA);
					if("BaseResp".equals(s)){
						PayResp payResp = new PayResp();
						payResp.fromBundle(bundle);
						sPayCallbackMessageListener.onReceiver(payResp);
					}else if("BaseReq".equals(s)){
						BaseReq baseReq = new PayReq();
						baseReq.fromBundle(bundle);
						sPayCallbackMessageListener.onReceiver(baseReq);
					}
				}
				return false;
			}
		},SysEventType.onActivityResult);
	}
	@Override
	protected void request(String pStatement) {
		if(hasGeneralError()){
			return;
		}
		if(!PlatformUtil.isAppInstalled(mContext, PNAME_MM)){//没有安装微信时候，执行支付错误回调
			if(isInstallingService){//正在安装微信支付服务，记录订单信息，等待安装完毕自动请求支付
				statement = pStatement;
			}else{
				mListener.onError(DOMException.CODE_CLIENT_UNINSTALLED, DOMException.MSG_CLIENT_UNINSTALLED);
			}
			return ;
		}

		PayInfoResult result = new PayInfoResult();
		result.parseFrom(pStatement);

		PayReq req = new PayReq();
		req.appId = result.appid;
		req.partnerId = result.partnerid;
		req.prepayId = result.prepayid;
		req.nonceStr = result.noncestr;
		req.timeStamp = result.timestamp;
		req.packageValue = result.result_package;
		req.sign = result.sign;
		if(mWebview.getActivity() instanceof IActivityHandler && ((IActivityHandler)mWebview.getActivity()).isMultiProcessMode()){//多进程模式
			startWXPayMediator(req);
			return;
		}
		// 在支付之前，如果应用没有注册到微信，应该先调用IWXMsg.registerApp将应用注册到微信
		boolean ret = api.sendReq(req);
		if(ret && hasWXPayEntryActivity(mWebview.getContext())){
			FeatureMessageDispatcher.registerListener(sPayCallbackMessageListener);
			Logger.d("wxpay","will pay");
		}else{
			onPayCallback(ret? BaseResp.ErrCode.ERR_OK:BaseResp.ErrCode.ERR_COMM, null);
		}
	}
	MessageListener sPayCallbackMessageListener = new MessageListener() {

		@Override
		public void onReceiver(Object msg) {
			if(msg instanceof BaseResp){
				executePayCallbackMsg((BaseResp)msg);
				FeatureMessageDispatcher.unregisterListener(sPayCallbackMessageListener);
			}
		}
	};

	void executePayCallbackMsg(BaseResp resp) {
		if(resp != null){
			int code = resp.errCode;
			StringBuffer sb = new StringBuffer("{");
			Bundle bundle = new Bundle();
			resp.toBundle(bundle);
			if(bundle != null){
				Set<String> sets = bundle.keySet();
				if(sets != null){
					int size = sets.size();
					String[] keys = new String[size];
					sets.toArray(keys);
					for(int i = 0; i < size ; i++){
						String key = keys[i];
						sb.append("'").append(key).append("':").append("'").append(bundle.get(key)).append("'");
						if(i != size -1){
							sb.append(",");
						}
					}
				}
			}
			sb.append("}");
			onPayCallback(code, sb.toString());
		}
	}

	void onPayCallback(int code, String rawData){
		boolean suc = false;
		String errorMsg = null;
		if(code == BaseResp.ErrCode.ERR_OK){
			suc = true;
		}else if(code == BaseResp.ErrCode.ERR_AUTH_DENIED){
			errorMsg = ERR_MSG_AUTH_DENIED;
		}else if(code == BaseResp.ErrCode.ERR_COMM){
			errorMsg = ERR_MSG_COMM;
		}else if(code == BaseResp.ErrCode.ERR_SENT_FAILED){
			errorMsg = ERR_MSG_SENT_FAILED;
		}else if(code == BaseResp.ErrCode.ERR_UNSUPPORT){
			errorMsg = ERR_MSG_UNSUPPORT;
		}else if(code == BaseResp.ErrCode.ERR_USER_CANCEL){
			errorMsg = ERR_MSG_USER_CANCEL;
		}
		if(suc){
			Logger.d("wxpay","pay success");
			PaymentResult pr = new PaymentResult(this);
			pr.rawDataJson = rawData;
			mListener.onSuccess(pr);
		}else {
			Logger.d("wxpay","pay failed code=" + code);
			mListener.onError(DOMException.CODE_BUSINESS_INTERNAL_ERROR,DOMException.toString(code, getFullDescription(),errorMsg,null));
		}
	}
	private static boolean hasWXPayEntryActivity(Context context){
		String clsName = context.getPackageName() + ".wxapi.WXPayEntryActivity";
		try {
			Class.forName(clsName);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}

	private static class PayInfoResult {

		private static final String KEY_RETCODE = "retcode";
		private static final String KEY_RETMSG = "retmsg";
		private static final String KEY_APPID = "appid";
		private static final String KEY_NONCESTR = "noncestr";
		private static final String KEY_PACKAGE = "package";
		private static final String KEY_PARTNERID = "partnerid";
		private static final String KEY_TIMESTAMP = "timestamp";
		private static final String KEY_SIGN = "sign";
		private static final String KEY_PREPAYID = "prepayid";

		public int retcode;
		public String retmsg;
		public String appid;
		public String noncestr;
		public String result_package;
		public String partnerid;
		public String prepayid;
		public String timestamp;
		public String sign;

		public void parseFrom(String content) {
			try {
				JSONObject json = new JSONObject(content);
//				retcode = Integer.valueOf(json.optString(KEY_RETCODE));
//				retmsg = json.optString(KEY_RETMSG);
				appid = json.optString(KEY_APPID);
				noncestr = json.optString(KEY_NONCESTR);
				result_package = json.optString(KEY_PACKAGE,"Sign=WXPay");
				partnerid = json.optString(KEY_PARTNERID);
				prepayid = json.optString(KEY_PREPAYID);
				timestamp = json.optString(KEY_TIMESTAMP);
				sign = json.optString(KEY_SIGN);
			} catch (Exception e) {
				retcode = -1000;
			}
		}
	}

}
