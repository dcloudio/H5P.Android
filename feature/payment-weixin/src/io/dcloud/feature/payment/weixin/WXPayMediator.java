package io.dcloud.feature.payment.weixin;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.tencent.mm.sdk.modelpay.PayReq;
import com.tencent.mm.sdk.openapi.IWXAPI;
import com.tencent.mm.sdk.openapi.WXAPIFactory;

import io.dcloud.ProcessMediator;
import io.dcloud.common.adapter.util.AndroidResources;

/**
 * Created by DCloud on 2017/2/28.
 */

public class WXPayMediator implements ProcessMediator.Logic{
    private static String APPID;
    private IWXAPI api;
    @Override
    public void exec(Context context, Intent intent) {
        APPID = AndroidResources.getMetaValue("WX_APPID");
        api = WXAPIFactory.createWXAPI(context, APPID);
        api.registerApp(APPID);
        Bundle pStatement = intent.getBundleExtra(ProcessMediator.REQ_DATA);
        PayReq req = new PayReq();
        req.fromBundle(pStatement);
        // 在支付之前，如果应用没有注册到微信，应该先调用IWXMsg.registerApp将应用注册到微信
        boolean ret = api.sendReq(req);
    }
}
