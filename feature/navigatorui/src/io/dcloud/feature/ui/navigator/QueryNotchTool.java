package io.dcloud.feature.ui.navigator;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.view.DisplayCutout;
import android.view.View;
import android.view.WindowInsets;

import java.lang.reflect.Method;

import io.dcloud.common.adapter.util.MobilePhoneModel;

/**
 * 查询刘海屏工具类
 */
public class QueryNotchTool {
    /**
     * 检查当前手机是否为刘海屏手机
     * 暂时仅支持 小米 vivo oppo 华为
     * @param activity
     * @return
     */
    public static boolean hasNotchInScreen(Activity activity) {
        if(MobilePhoneModel.isAppointPhone(MobilePhoneModel.XIAOMI)) {// 小米手机
            return hasNotchInXiaomi(activity);
        } else if(MobilePhoneModel.isAppointPhone(MobilePhoneModel.VIVO)) {//vivo手机
            return hasNotchInVoio(activity);
        } else if(MobilePhoneModel.isAppointPhone(MobilePhoneModel.OPPO)) {//oppo手机
            return hasNotchInOppo(activity);
        } else if(MobilePhoneModel.isAppointPhone(MobilePhoneModel.HUAWEI)
                || MobilePhoneModel.isAppointPhone(MobilePhoneModel.HONOR)) {//华为手机
            return hasNotchInHuawei(activity);
        } else if(isAndroidP(activity) != null) {
            return true;
        }
        return false;
    }

    /**
     * 检查oppo手机是否为刘海屏
     * @param context
     * @return
     */
    public static boolean hasNotchInOppo(Context context){
        return context.getPackageManager().hasSystemFeature("com.oppo.feature.screen.heteromorphism");
    }

    public static final int NOTCH_IN_SCREEN_VOIO=0x00000020;//是否有凹槽
    public static final int ROUNDED_IN_SCREEN_VOIO=0x00000008;//是否有圆角

    /**
     * 检查voio手机是否为刘海屏
     * @param context
     * @return
     */
    public static boolean hasNotchInVoio(Context context){
        boolean hasNotch = false;
        try {
            ClassLoader cl = context.getClassLoader();
            Class FtFeature = cl.loadClass("com.util.FtFeature");
            Method get = FtFeature.getMethod("isFeatureSupport",int.class);
            hasNotch = (boolean) get.invoke(FtFeature, NOTCH_IN_SCREEN_VOIO);
        }catch (Exception e) {
            e.printStackTrace();
        }
        return hasNotch;
    }

    /**
     * 检查华为手机是否为刘海屏
     * @param context
     * @return
     */
    public static boolean hasNotchInHuawei(Context context) {
        boolean hasNotch = false;
        try {
            ClassLoader cl = context.getClassLoader();
            Class HwNotchSizeUtil = cl.loadClass("com.huawei.android.util.HwNotchSizeUtil");
            Method hasNotchInScreen = HwNotchSizeUtil.getMethod("hasNotchInScreen");
            if(hasNotchInScreen != null) {
                hasNotch = (boolean) hasNotchInScreen.invoke(HwNotchSizeUtil);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return hasNotch;
    }

    /**
     * 检查小米手机是否为刘海屏
     * @param context
     * @return
     */
    public static boolean hasNotchInXiaomi(Context context) {
        boolean hasNotch = false;
        try {
            Class<?> cls = Class.forName("android.os.SystemProperties");
            Method m = cls.getDeclaredMethod("getInt",String.class,int.class);
            int play = (int) m.invoke(null,"ro.miui.notch",0);
            hasNotch = play == 1;
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return hasNotch;
    }

    /**
     * Android P 刘海屏判断
     * @param activity
     * @return
     */
    @TargetApi(28)
    public static DisplayCutout isAndroidP(Activity activity){
        View decorView = activity.getWindow().getDecorView();
        if (decorView != null && android.os.Build.VERSION.SDK_INT >= 28){
            WindowInsets windowInsets = decorView.getRootWindowInsets();
            if (windowInsets != null)
                return windowInsets.getDisplayCutout();
        }
        return null;
    }


}
