package io.dcloud.feature.apsGt;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import com.igexin.sdk.GTIntentService;
import com.igexin.sdk.message.GTCmdMessage;
import com.igexin.sdk.message.GTNotificationMessage;
import com.igexin.sdk.message.GTTransmitMessage;

import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.util.BaseInfo;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.feature.aps.APSFeatureImpl;
import io.dcloud.feature.aps.AbsPushService;
import io.dcloud.feature.aps.PushMessage;


/**
 * 继承 GTIntentService 接收来自个推的消息, 所有消息在线程中回调, 如果注册了该服务, 则务必要在 AndroidManifest中声明, 否则无法接受消息<br>
 * onReceiveMessageData 处理透传消息<br>
 * onReceiveClientId 接收 cid <br>
 * onReceiveOnlineState cid 离线上线通知 <br>
 * onReceiveCommandResult 各种事件处理回执 <br>
 */
public class GTNormalIntentService extends GTIntentService {
    public static final String TAG = GTNormalIntentService.class.getSimpleName();

    public GTNormalIntentService() {

    }

    public void onNotificationMessageClicked(Context context, GTNotificationMessage gtNotificationMessage) {
    }

    public void onNotificationMessageArrived(Context context, GTNotificationMessage gtNotificationMessage) {
    }

    @Override
    public void onReceiveServicePid(Context context, int pid) {
    }

    //处理透传消息
    @Override
    public void onReceiveMessageData(Context context, GTTransmitMessage msg) {
        Logger.e(TAG, "onReceiveMessageData -> " + "msg = " + msg);
        byte[] dataBase = msg.getPayload();
        if (dataBase != null) {
            String data = new String(dataBase);
            if (!PdrUtil.isEmpty(data)) {
                Logger.e(TAG, "onReceiveMessageData -> " + "msg data= " + data);
                String appid = BaseInfo.sDefaultBootApp;
                PushMessage _pushMessage = new PushMessage(data, appid, getApplicationName(context));
                boolean needPush = AbsPushService.getAutoNotification(context, appid, GTPushService.ID);
                if (needPush && _pushMessage.needCreateNotifcation()) {
                    APSFeatureImpl.sendCreateNotificationBroadcast(context, appid, _pushMessage);
                } else if (!APSFeatureImpl.execScript(context, "receive", _pushMessage.toJSON())) {// 添加receive执行队列
                    APSFeatureImpl.addNeedExecReceiveMessage(context, _pushMessage);
                }
                APSFeatureImpl.addPushMessage(context, appid, _pushMessage);
            }
        }
    }


    public String getApplicationName(Context context) {
        PackageManager packageManager = null;
        ApplicationInfo applicationInfo = null;
        try {
            packageManager = context.getApplicationContext().getPackageManager();
            applicationInfo = packageManager.getApplicationInfo(context.getPackageName(), 0);
        } catch (PackageManager.NameNotFoundException e) {
            applicationInfo = null;
        }
        String applicationName = (String) packageManager.getApplicationLabel(applicationInfo);
        return applicationName;
    }

    //onReceiveClientId 接收 cid
    @Override
    public void onReceiveClientId(Context context, String clientid) {
        Logger.e(TAG, "onReceiveClientId -> " + "clientid = " + clientid);
        SharedPreferences _sp = context.getSharedPreferences(AbsPushService.CLIENTID + GTPushService.ID, Context.MODE_PRIVATE);
        SharedPreferences.Editor ed = _sp.edit();
        ed.putString(AbsPushService.PUSH_CLIENT_ID_NAME, clientid);
        ed.commit();
    }

    //离线上线通知
    @Override
    public void onReceiveOnlineState(Context context, boolean online) {
        Logger.e(TAG, "onReceiveOnlineState -> " + "online = " + online);

    }

    //各种事件处理回执
    @Override
    public void onReceiveCommandResult(Context context, GTCmdMessage cmdMessage) {
        Logger.e(TAG, "onReceiveCommandResult -> " + "cmdMessage = " + cmdMessage);

    }
}