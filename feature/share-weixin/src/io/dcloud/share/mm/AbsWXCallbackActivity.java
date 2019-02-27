package io.dcloud.share.mm;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import com.tencent.mm.opensdk.constants.ConstantsAPI;
import com.tencent.mm.opensdk.modelbase.BaseReq;
import com.tencent.mm.opensdk.modelbase.BaseResp;
import com.tencent.mm.opensdk.modelbiz.WXLaunchMiniProgram;
import com.tencent.mm.opensdk.modelmsg.ShowMessageFromWX;
import com.tencent.mm.opensdk.modelmsg.WXAppExtendObject;
import com.tencent.mm.opensdk.modelmsg.WXMediaMessage;
import com.tencent.mm.opensdk.openapi.IWXAPI;
import com.tencent.mm.opensdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.opensdk.openapi.WXAPIFactory;

import io.dcloud.ProcessMediator;
import io.dcloud.common.DHInterface.FeatureMessageDispatcher;
import io.dcloud.common.adapter.util.AndroidResources;
import io.dcloud.common.constant.IntentConst;
import io.dcloud.common.constant.StringConst;


public class AbsWXCallbackActivity extends Activity implements IWXAPIEventHandler {
	boolean isMultiProcess = false;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		String wx_appid = AndroidResources.getMetaValue("WX_APPID");
		String MultiProcessCount = AndroidResources.getMetaValue("MultiProcessCount");
		isMultiProcess = !TextUtils.isEmpty(MultiProcessCount);
		try {
			IWXAPI api = WXAPIFactory.createWXAPI(this, wx_appid, false);
			api.handleIntent(getIntent(), this);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	@Override
	// 微信发送请求到第三方应用时，会回调到该方法
	public void onReq(BaseReq req) {
        FeatureMessageDispatcher.dispatchMessage(req);
		if(req.getType() == ConstantsAPI.COMMAND_SHOWMESSAGE_FROM_WX) {
			goToMsg(req);
		}
		if(isMultiProcess){
			Intent n = new Intent();
			Bundle bundle = new Bundle();
			bundle.putString(ProcessMediator.STYLE_DATA,"BaseReq");
			req.toBundle(bundle);
			n.putExtra(ProcessMediator.RESULT_DATA,bundle);
			ProcessMediator.setResult(n);
		}
		finish();
	}

	private void goToMsg(BaseReq req) {
		ShowMessageFromWX.Req showReq = (ShowMessageFromWX.Req) req;
		WXMediaMessage wxMsg = showReq.message;
		WXAppExtendObject obj = (WXAppExtendObject) wxMsg.mediaObject;
		PackageManager packageManager = getPackageManager();
		Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
		intent.putExtra(StringConst.KEY_WX_SHOW_MESSAGE, obj.extInfo);
		intent.putExtra(IntentConst.STREAM_LAUNCHER, "miniProgram");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
		startActivity(intent);
	}

	private void goToMsg(BaseResp resp) {
		WXLaunchMiniProgram.Resp launchMiniProResp = (WXLaunchMiniProgram.Resp) resp;
		String extraData =launchMiniProResp.extMsg;
		if(TextUtils.isEmpty(extraData)) {
			return;
		}
		PackageManager packageManager = getPackageManager();
		Intent intent = packageManager.getLaunchIntentForPackage(getPackageName());
		intent.putExtra(StringConst.KEY_WX_SHOW_MESSAGE, extraData);
		intent.putExtra(IntentConst.STREAM_LAUNCHER, "miniProgram");
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_DOCUMENT);
		startActivity(intent);
	}
	
	// 第三方应用发送到微信的请求处理后的响应结果，会回调到该方法
	@Override
	public void onResp(BaseResp resp) {
        FeatureMessageDispatcher.dispatchMessage(resp);
		if(resp.getType() == ConstantsAPI.COMMAND_LAUNCH_WX_MINIPROGRAM) {
			goToMsg(resp);
		}
		if(isMultiProcess){
			Intent n = new Intent();
			Bundle bundle = new Bundle();
			bundle.putString(ProcessMediator.STYLE_DATA,"BaseResp");
			resp.toBundle(bundle);
			n.putExtra(ProcessMediator.RESULT_DATA,bundle);
			ProcessMediator.setResult(n);
		}
		finish();
	}
}