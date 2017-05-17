package io.dcloud.feature.apsXm;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.text.TextUtils;

import com.xiaomi.mipush.sdk.MiPushClient;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;
import com.xiaomi.mipush.sdk.PushMessageReceiver;

import java.util.List;

import io.dcloud.feature.aps.APSFeatureImpl;
import io.dcloud.feature.aps.AbsPushService;
import io.dcloud.feature.aps.PushMessage;
import io.dcloud.common.util.BaseInfo;
import io.dcloud.common.DHInterface.IReflectAble;

/**
 * @author vrmlpad
 */
public class XMMessageReceiver extends PushMessageReceiver implements IReflectAble{

    @Override
    public void onReceivePassThroughMessage(Context context, MiPushMessage message) {
        //"透传"推送消息"传输数据"不为空，则将传输数据添加到消息列表里。
        //如果需要并符合弹出通知栏规则，则弹出通知栏提示。
        if(!TextUtils.isEmpty(message.getContent())){
            String appid = BaseInfo.sDefaultBootApp;
            String data=message.getContent();
            PushMessage _pushMessage = new PushMessage(data,appid, getApplicationName(context));
            boolean needPush = AbsPushService.getAutoNotification(context, appid, XMPushService.ID);
            if (needPush && _pushMessage.needCreateNotifcation()) {
                APSFeatureImpl.sendCreateNotificationBroadcast(context, appid, _pushMessage);
            }else if (!APSFeatureImpl.execScript(context,"receive", _pushMessage.toJSON())) {// 添加receive执行队列
                APSFeatureImpl.addNeedExecReceiveMessage(context,_pushMessage);
            }
            APSFeatureImpl.addPushMessage(context,appid, _pushMessage);
        }
    }

    @Override
    public void onNotificationMessageClicked(Context context, MiPushMessage message) {
        //"通知栏提醒"推送消息,点击通知栏消息时将"传输数据添"加到消息列表里。
        String appid = BaseInfo.sDefaultBootApp;
        StringBuffer sb=new StringBuffer();
        sb.append("{");
        sb.append("\"title\":");
        sb.append(message.getTitle());
        sb.append(",");
        sb.append("\"content\":");
        sb.append(message.getDescription());
        sb.append(",");
        sb.append("\"payload\":");
        sb.append(message.getContent());
        sb.append("}");
        PushMessage _pushMessage = new PushMessage(sb.toString(),appid, getApplicationName(context));
        if(!APSFeatureImpl.execScript(context,"click",_pushMessage.toJSON())){
            APSFeatureImpl.addNeedExecMessage(context, _pushMessage);
        }

        //点击通知栏，如果应用在后台，则从后台切换到前台
        if(!moveTaskToFront(context)){
            //从后台切换到前台失败，则认为应用已关闭，启动应用
            startMySelf(context);
        }
    }

    @Override
    public void onNotificationMessageArrived(Context context, MiPushMessage message) { }

    @Override
    public void onCommandResult(Context context, MiPushCommandMessage message) {
        String command = message.getCommand();
        //注册请求结束
        if (MiPushClient.COMMAND_REGISTER.equals(command)) {
            XMPushService.IsRegisterPushing =false;
        }
    }

    @Override
    public void onReceiveRegisterResult(Context context, MiPushCommandMessage message) { }

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

    /**
     * App不在前台，则恢复App到前台
     * @param context
     * @return
     */
    private boolean moveTaskToFront(Context context){
        try {
            //获取ActivityManager
            ActivityManager mAm = (ActivityManager) context.getSystemService(context.ACTIVITY_SERVICE);
            //获得当前运行的task
            List<ActivityManager.RunningTaskInfo> taskList = mAm.getRunningTasks(100);
            for (ActivityManager.RunningTaskInfo rti : taskList) {
                //找到当前应用的task，并启动task的栈顶activity，达到程序切换到前台
                if(rti.topActivity.getPackageName().equals(context.getPackageName())) {
                    Intent  resultIntent = new Intent(context, Class.forName(rti.topActivity.getClassName()));
                    resultIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                    context.startActivity(resultIntent);
                    return true;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 启动自己
     * @param context
     */
    private void startMySelf(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo pi = packageManager.getPackageInfo(context.getPackageName(), 0);
            if(null != pi){
                Intent resolveIntent = new Intent(Intent.ACTION_MAIN, null);
                resolveIntent.addCategory(Intent.CATEGORY_LAUNCHER);
                resolveIntent.setPackage(pi.packageName);
                List<ResolveInfo> apps = packageManager.queryIntentActivities(resolveIntent, 0);
                if(null!=apps&&0<apps.size()){
                    ResolveInfo ri = apps.iterator().next();
                    if (ri != null ) {
                        String className = ri.activityInfo.name;
                        Intent intent = new Intent(Intent.ACTION_MAIN);
                        intent.addCategory(Intent.CATEGORY_LAUNCHER);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK );
                        ComponentName cn = new ComponentName(context.getPackageName(), className);
                        intent.setComponent(cn);
                        context.startActivity(intent);
                    }
                }
            }
        } catch (Exception e) {
            e.getMessage();
        }
    }
}
