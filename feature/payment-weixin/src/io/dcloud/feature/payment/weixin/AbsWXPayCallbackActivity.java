package io.dcloud.feature.payment.weixin;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;

import com.tencent.mm.sdk.modelbase.BaseReq;
import com.tencent.mm.sdk.modelbase.BaseResp;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.IWXAPIEventHandler;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import io.dcloud.ProcessMediator;
import io.dcloud.common.DHInterface.FeatureMessageDispatcher;
import io.dcloud.common.DHInterface.IReflectAble;
import io.dcloud.common.adapter.util.AndroidResources;

public abstract class AbsWXPayCallbackActivity extends Activity implements IWXAPIEventHandler,IReflectAble{
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

	// 第三方应用发送到微信的请求处理后的响应结果，会回调到该方法
	@Override
	public void onResp(BaseResp resp) {
		FeatureMessageDispatcher.dispatchMessage(resp);
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
