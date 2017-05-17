package io.dcloud.share.mm;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.tencent.mm.sdk.modelmsg.SendAuth;
import com.tencent.mm.sdk.modelmsg.SendMessageToWX;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import io.dcloud.ProcessMediator;
import io.dcloud.common.adapter.util.AndroidResources;

public class WeiXinMediator implements ProcessMediator.Logic {
	private String APPID;
	private IWXAPI api;
	@Override
	public void exec(Context context, Intent intent) {
		APPID = AndroidResources.getMetaValue("WX_APPID");
		api = WXAPIFactory.createWXAPI(context, APPID, true);
		api.registerApp(APPID);
		Bundle bundle = intent.getBundleExtra(ProcessMediator.REQ_DATA);
		SendMessageToWX.Req req = new SendMessageToWX.Req();
		req.fromBundle(bundle);
		// 在支付之前，如果应用没有注册到微信，应该先调用IWXMsg.registerApp将应用注册到微信
		boolean ret = api.sendReq(req);
	}
}