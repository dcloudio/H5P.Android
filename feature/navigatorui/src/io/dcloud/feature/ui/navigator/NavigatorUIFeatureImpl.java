package io.dcloud.feature.ui.navigator;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Build;
import android.text.TextUtils;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.webkit.CookieManager;
import android.widget.Toast;

import com.nostra13.dcloudimageloader.core.ImageLoader;
import com.nostra13.dcloudimageloader.core.assist.FailReason;
import com.nostra13.dcloudimageloader.core.assist.ImageLoadingListener;

import org.json.JSONException;
import org.json.JSONObject;

import io.dcloud.RInformation;
import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.IApp.ConfigProperty;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.IMgr.MgrEvent;
import io.dcloud.common.DHInterface.IMgr.MgrType;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.io.DHFile;
import io.dcloud.common.adapter.util.DeviceInfo;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.MessageHandler;
import io.dcloud.common.adapter.util.PermissionUtil;
import io.dcloud.common.adapter.util.PlatformUtil;
import io.dcloud.common.adapter.util.SP;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.constant.DataInterface;
import io.dcloud.common.constant.IntentConst;
import io.dcloud.common.constant.StringConst;
import io.dcloud.common.util.BaseInfo;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.ShortCutUtil;
import io.dcloud.common.util.ShortcutCreateUtil;
import io.dcloud.common.util.TestUtil;

public class NavigatorUIFeatureImpl implements IFeature {
    private static final String TAG = NavigatorUIFeatureImpl.class.getSimpleName();

    //	private static final String STREAM_QIHOO_PACKAGE_NAME = "io.dcloud.appstream";
    private static final String STREAM_PACKAGE_NAME = "io.dcloud.appstream";
    private static final String LIGHT_APP_ACTIVITY_NAME = "StreamAppListFakeActivity";
    private static final String STREAM_ACTIVITY_NAME = "StreamAppMainActivity";

    AbsMgr mFeatureMgr;
    @Override
    public String execute(final IWebview pWebViewImpl, String pActionName,
                          final String[] pJsArgs) {
        String _ret = null;
        final IApp _app = pWebViewImpl.obtainApp();
        final String _appid = _app.obtainAppId();
        switch (pActionName) {
            case "closeSplashscreen":{
                Logger.d(Logger.MAIN_TAG, "appid=" + _appid + " closeSplashscreen");
                TestUtil.print(TestUtil.START_STREAM_APP,"closeSplashscreen appid=" + _appid);
                Logger.i("download_manager", "javascript webapp task begin success appid=" + _appid + " closeSplashscreen");
                mFeatureMgr.processEvent(MgrType.WindowMgr, MgrEvent.CLOSE_SPLASH_SCREEN, pWebViewImpl.obtainFrameView());
            }
            break;
            case "setFullscreen":{
                String fullscreen = pJsArgs[0];
                if (_app != null) {
                    _app.setFullScreen(PdrUtil.parseBoolean(String.valueOf(fullscreen), false, false));
                }
            }
            break;
            case "isFullScreen":{
                if (_app != null) {
                    _ret = JSUtil.wrapJsVar(_app.isFullScreen());
                }
            }
            break;
            case "setUserAgent":{
                String value = pJsArgs[0];
                String hasH5Plus = pJsArgs[1];
                // 1，更新全局ua
                _app.setConfigProperty(ConfigProperty.CONFIG_USER_AGENT, value);
                _app.setConfigProperty(ConfigProperty.CONFIG_funSetUA, true+"");
                _app.setConfigProperty(ConfigProperty.CONFIG_H5PLUS, hasH5Plus);
                // 2，设置webview的ua
                pWebViewImpl.setWebviewProperty(IWebview.USER_AGENT, value);
            }
            break;
            case "getUserAgent":{
                _ret = pWebViewImpl.getWebviewProperty(IWebview.USER_AGENT);
            }
            break;
            case "setCookie":{
                pWebViewImpl.setWebviewProperty(pJsArgs[0], pJsArgs[1]);
            }
            break;
            case "getCookie":{
                _ret = pWebViewImpl.getWebviewProperty(pJsArgs[0]);
            }
            break;
            case "removeAllCookie":{ // 不支持制定url删除cookie
                try {
                    CookieManager.getInstance().removeAllCookie();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            break;
            case "removeSessionCookie":{
                try {
                    CookieManager.getInstance().removeSessionCookie();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            break;
            case "removeCookie":{

            }
            break;
            case "setLogs":{
                String open = pJsArgs[0];
                Logger.setOpen(PdrUtil.parseBoolean(String.valueOf(open),
                        false, false));
            }
            break;
            case "isLogs":{// 返回日志开关状态
                _ret = JSUtil.wrapJsVar(Logger.isOpen());
            }
            break;
            case "createShortcut":{
                PermissionUtil.usePermission(pWebViewImpl.getActivity(), _app.isStreamApp(), true,PermissionUtil.PMS_SHORTCUT , new PermissionUtil.StreamPermissionRequest(_app) {
                    @Override
                    public void onGranted(String streamPerName) {
                        checkPermissionAndCreateShortcut(pWebViewImpl, pJsArgs, _app, _appid);
                    }
                    @Override
                    public void onDenied(String streamPerName) {

                    }
                });
            }
            break;
            case "getStatusbarHeight":{
                DeviceInfo.updateStatusBarHeight(pWebViewImpl.getActivity());
                _ret = JSUtil.wrapJsVar(DeviceInfo.sStatusBarHeight/pWebViewImpl.getScale());
            }
            break;
            case "isImmersedStatusbar":{
                boolean isImmersed = Boolean.valueOf(_app.obtainConfigProperty(StringConst.JSONKEY_STATUSBAR_IMMERSED));
                _ret = JSUtil.wrapJsVar(_app.obtainStatusBarMgr().checkImmersedStatusBar(pWebViewImpl.getActivity(), isImmersed));
            }
            break;
            case "hasShortcut":{
                String options = pJsArgs[0];
                final String callbackID = pJsArgs[1];

                JSONObject jsonObject = null;
                String name = pWebViewImpl.obtainApp().obtainAppName();
                try {
                    jsonObject = new JSONObject(options);
                    name = jsonObject.optString("name", name);
                } catch (JSONException e) {
                    e.printStackTrace();
                }

                handleHasShortcutCallback(pWebViewImpl.getContext(), pWebViewImpl,
                        callbackID, name);
            }
            break;
            case "updateSplashscreen":{
                String options = pJsArgs[0];
                try {
                    JSONObject jsonObject = new JSONObject(options);
                    SharedPreferences pdrSharedPre = PlatformUtil.getOrCreateBundle(SP.N_BASE);
                    SharedPreferences.Editor editor = pdrSharedPre.edit();
                    String appid = _app.obtainAppId();
                    String updateImg = jsonObject.optString("image", null);
                    if (!TextUtils.isEmpty(updateImg)) {
                        String convert2AbsFullPath = _app.convert2AbsFullPath(pWebViewImpl.obtainFullUrl(), updateImg);
                        if (PdrUtil.isDeviceRootDir(convert2AbsFullPath)) { // SD卡路径 直接替换路径图片文件即可
                            String sIconPath = StringConst.STREAMAPP_KEY_ROOTPATH + "splash/" + _app.obtainAppId() + ".png";
                            DHFile.copyFile(convert2AbsFullPath, sIconPath, true, false);
                        }
                        // 如果不是流应用也不是360插件， 那就进行本地保存
                        if (!BaseInfo.isStreamApp(pWebViewImpl.getActivity()) && !BaseInfo.isForQihooHelper(pWebViewImpl.getActivity())) {
                            editor.putString(SP.UPDATE_SPLASH_IMG_PATH, convert2AbsFullPath);
                        }
                    }
                    if(!jsonObject.isNull("autoclose")){
                        editor.putBoolean(appid + SP.UPDATE_SPLASH_AUTOCLOSE, jsonObject.optBoolean("autoclose"));
                    }
                    if (!jsonObject.isNull("delay")) {
                        editor.putInt(appid + SP.UPDATE_SPLASH_DELAY, jsonObject.optInt("delay"));
                    }
                    if(BaseInfo.isWap2AppAppid(appid)){
                        //此时支持
                        //autoclose、autoclose_w2a和delay、delay_w2a属性的支持
                        if(!jsonObject.isNull("autoclose_w2a")){
                            editor.putBoolean(appid + SP.UPDATE_SPLASH_AUTOCLOSE_W2A, jsonObject.optBoolean("autoclose_w2a"));
                        }
                        if (!jsonObject.isNull("delay_w2a")) {
                            editor.putInt(appid + SP.UPDATE_SPLASH_DELAY_W2A, jsonObject.optInt("delay_w2a"));
                        }
                    }
                    editor.commit();
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            break;
            case "requestPermission":{
                String permissionName = pJsArgs[0];
                final String callbackId = pJsArgs[1];
                final int f_requestCode = callbackId.hashCode();
                final String f_permissionName = PermissionUtil.convertNativePermission(permissionName);
                _app.registerSysEventListener(new io.dcloud.common.DHInterface.ISysEventListener(){
                    public boolean onExecute(io.dcloud.common.DHInterface.ISysEventListener.SysEventType pEventType,Object pArgs){
                        Object[] pars = (Object[])pArgs;
                        int requestCode = (Integer)pars[0];
                        String[] permissions = (String[]) pars[1];
                        int[] grantResults = (int[]) pars[2];
                        if(io.dcloud.common.DHInterface.ISysEventListener.SysEventType.onRequestPermissionsResult == pEventType && requestCode == f_requestCode){
                            _app.unregisterSysEventListener(this,io.dcloud.common.DHInterface.ISysEventListener.SysEventType.onRequestPermissionsResult);
                            int code;
                            if(grantResults.length > 0){
                                code = grantResults[0];
                            }else{
                                code = pWebViewImpl.obtainApp().checkSelfPermission(f_permissionName,pWebViewImpl.obtainApp().obtainAppName() );
                            }
                            String ret = PermissionUtil.convert5PlusValue(code);
                            JSUtil.execCallback(pWebViewImpl,callbackId,String.format("{result:'%s'}",ret),JSUtil.OK,true,false);
                        }
                        return true;
                    }
                }, io.dcloud.common.DHInterface.ISysEventListener.SysEventType.onRequestPermissionsResult);
                _app.requestPermissions(new String[]{f_permissionName},f_requestCode);
            }
            break;
            case "checkPermission":{
                _ret = JSUtil.wrapJsVar(PermissionUtil.checkPermission(pWebViewImpl, pJsArgs));
            }
            break;
            case "isBackground":{
                boolean isBackground = _app.obtainAppStatus() == _app.STATUS_UN_ACTIVIE;
                _ret = JSUtil.wrapJsVar(isBackground);
            }
            break;
            case "setStatusBarBackground":{
                String colorStr = pJsArgs[0];
                if(!TextUtils.isEmpty(colorStr)){
                    int color;
                    try{
                        color =  Color.parseColor(colorStr);
                    }catch (Exception e){
                        color = PdrUtil.stringToColor(colorStr);
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        _app.setConfigProperty(StringConst.JSONKEY_STATUSBAR_BC, pJsArgs[0]);
                        _app.obtainStatusBarMgr().setStatusBarColor(_app.getActivity(), color);
                    }
                }
            }
            break;
            case "getStatusBarBackground":{
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    int color = _app.getActivity().getWindow().getStatusBarColor();
                    _ret = JSUtil.wrapJsVar(PdrUtil.toHexFromColor(color));
                }
            }
            break;
            case "hasSplashscreen":{
                boolean hasClose = !_app.obtainWebAppRootView().didCloseSplash();
                _ret = JSUtil.wrapJsVar(hasClose);
            }
            break;
            case "hideSystemNavigation":{
                if(_app != null) {
                    Window window = _app.getActivity().getWindow();
                    int flags = window.getDecorView().getSystemUiVisibility();
                    window.getDecorView().setSystemUiVisibility(flags | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
            }
            break;
            case "setStatusBarStyle":{
                String statusBarMode = pJsArgs[0];
                _app.setConfigProperty(StringConst.JSONKEY_STATUSBAR_MODE, statusBarMode);
                _app.obtainStatusBarMgr().setStatusBarMode(_app.getActivity(), statusBarMode);
            }
            break;
            case "getStatusBarStyle":{
                String statusBarMode = _app.obtainConfigProperty(StringConst.JSONKEY_STATUSBAR_MODE);
                _ret = JSUtil.wrapJsVar(statusBarMode);
            }
            break;
            case "hasNotchInScreen":{
                _ret = JSUtil.wrapJsVar(QueryNotchTool.hasNotchInScreen(pWebViewImpl.getActivity()));
            }
            break;
            case "getOrientation":{
                int angle = pWebViewImpl.getActivity().getWindowManager().getDefaultDisplay().getRotation();
                int orientation = 0;
                switch (angle) {
                    case Surface.ROTATION_90:
                        orientation = 90;
                        break;
                    case Surface.ROTATION_180:
                        orientation = 180;
                        break;
                    case Surface.ROTATION_270:
                        orientation = -90;
                        break;
                }
                _ret = JSUtil.wrapJsVar(orientation);
            }
            break;
        }
//        if(pActionName.equals("closeSplashscreen")){
//        }else if(pActionName.equals("setFullscreen")){
//        }else if(pActionName.equals("isFullScreen")){
//        }else if(pActionName.equals("setUserAgent")){
//        }else if(pActionName.equals("getUserAgent")){
//        }else if(pActionName.equals("setCookie")){
//        }else if(pActionName.equals("getCookie")){
//        }else if(pActionName.equals("removeAllCookie")){
//        }else if(pActionName.equals("removeSessionCookie")){
//        }else if(pActionName.equals("removeCookie")){
//        }else if(pActionName.equals("setLogs")){//
//        }else if(pActionName.equals("isLogs")){
//        }else if(pActionName.equals("createShortcut")){
//        } else if(pActionName.equals("getStatusbarHeight")) {
//        } else if(pActionName.equals("isImmersedStatusbar")) {
//        } else if (pActionName.equals("hasShortcut")) {
//        } else if (pActionName.equals("updateSplashscreen")) {
//        } else if (pActionName.equals("requestPermission")) {
//        } else if (pActionName.equals("checkPermission")) {
//        } else if ("isBackground".equals(pActionName)) {
//        } else if("setStatusBarBackground".equals(pActionName)) {
//        } else if("getStatusBarBackground".equals(pActionName)) {
//        } else if("hasSplashscreen".equals(pActionName)) {
//        } else if("setStatusBarStyle".equals(pActionName)) {
//        } else if("getStatusBarStyle".equals(pActionName)) {
//        }
        return _ret;
    }

    private boolean checkPermissionAndCreateShortcut(IWebview pWebViewImpl, String[] pJsArgs, IApp _app, String _appid) {
        String options = pJsArgs[0];
        final String callbackID = pJsArgs[1];

        JSONObject jsonObject = null;
        String name = "";
        String icon = "";
        String className = "";
        boolean isForce = true;
        boolean check = true;
        String toast = String.format("\"%s\"已创建桌面图标",_app.obtainAppName());

        JSONObject extraJSON = null;
        try {
            jsonObject = new JSONObject(options);
            isForce = jsonObject.optBoolean("force", isForce);
            if (isForce) {
                toast = toast + "，如有重复请手动删除";
            }
            name = jsonObject.optString("name");
            icon = jsonObject.optString("icon");
            className = jsonObject.optString("classname");
            toast = jsonObject.has("toast") ? jsonObject.optString("toast") : toast;
            extraJSON = jsonObject.optJSONObject("extra");
            check = jsonObject.optBoolean("check", check);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        //360手机浏览器流应用插件模式下5+API创建快捷方式Toast提示文字
        if(BaseInfo.isForQihooBrowser( pWebViewImpl.getActivity())&&!TextUtils.isEmpty(toast)){
            toast="已创建桌面图标，如未成功请检查权限或桌面设置。";
        }
        Bitmap iconBitmap = null;
        try {
            if(!TextUtils.isEmpty(icon)){//获取指定路径的图标
                String convert2AbsFullPath = _app.convert2AbsFullPath(pWebViewImpl.obtainFullUrl(), icon);
                iconBitmap = BitmapFactory.decodeFile(convert2AbsFullPath);
            }
            if(iconBitmap == null){
                try {
                    iconBitmap = getDefaultBitmap(_app);//获取应用默认icon
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            if(iconBitmap == null){
                boolean streamApp = pWebViewImpl.obtainApp().isStreamApp();
                if(streamApp){//流应用时imageloader加载图标
                    final IWebview f_webview = pWebViewImpl;
                    final boolean isFe = isForce;
                    final boolean isCheck = check;
                    final String f_name = name;
                    final String f_className = className;
                    final String f_toast = toast;
                    final JSONObject f_extraJSON = extraJSON;
                    final String url = DataInterface.getIconImageUrl(_appid, _app.getActivity().getResources().getDisplayMetrics().widthPixels + "");
                    ImageLoader.getInstance().loadImage(url, new ImageLoadingListener() {
                        @Override
                        public void onLoadingStarted(String arg0, View arg1) {}
                        @Override
                        public void onLoadingFailed(String arg0, View arg1, FailReason arg2) {//加载失败使用android系统图标
                            if(StringConst.canChangeHost(arg0)) {
                                arg0 = StringConst.changeHost(arg0);
                                ImageLoader.getInstance().loadImage(arg0, this);
                            }else {
                                createShortCut(f_webview, f_name, null, f_className, f_toast, f_extraJSON, isFe, isCheck, callbackID);
                            }
                        }
                        @Override
                        public void onLoadingComplete(String arg0, View arg1, Bitmap arg2) {
                            createShortCut(f_webview, f_name, arg2, f_className, f_toast, f_extraJSON, isFe, isCheck, callbackID);
                        }
                        @Override
                        public void onLoadingCancelled(String arg0,View arg1) {

                        }
                    });
                    return true;
                }else{
                    if (iconBitmap == null) {//默认程序图标
                        iconBitmap = BitmapFactory.decodeResource(
                                pWebViewImpl.getContext().getResources(), RInformation.DRAWABLE_ICON);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        createShortCut(pWebViewImpl, name, iconBitmap, className, toast, extraJSON, isForce, check, callbackID);
//			InvokeExecutorHelper.QihooInnerStatisticUtil.invoke("doEvent",
//					new Class[] { String.class, String.class }, _appid,
//					"event_add_shortcut");
        return false;
    }

    private void handleHasShortcutCallback(Context context, IWebview webview, String callbackID, String name) {
        String success_json = "";
        String result = ShortCutUtil.requestShortCut(context, name);
        if (ShortCutUtil.SHORT_CUT_EXISTING.equals(result)){
            success_json = String.format(DOMException.JSON_SHORTCUT_RESULT_INFO,
                    "existing");
        }else if(ShortCutUtil.SHORT_CUT_NONE.equals(result)) {
            success_json = String.format(DOMException.JSON_SHORTCUT_RESULT_INFO,
                    "none");
        }else if (ShortCutUtil.NOPERMISSIONS.equals(result)) {
            success_json = String.format(DOMException.JSON_SHORTCUT_RESULT_INFO,
                    ShortCutUtil.NOPERMISSIONS);
        } else {
            success_json = String.format(DOMException.JSON_SHORTCUT_RESULT_INFO,
                    ShortCutUtil.UNKNOWN);
        }
        try {
            JSUtil.execCallback(webview, callbackID, new JSONObject(
                    success_json), JSUtil.OK, false);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    private void handleCallback(final Context context,final IWebview webview,final String callbackID, final String name) {
        Runnable runnable=new Runnable() {
            @Override
            public void run() {
                String sure = "false";
                //500毫秒之后查找是否创建成功（华为6.0不查，且返回false）防止创建的广播接收者还没有创建完成而返回false
                String result=ShortCutUtil.requestShortCutForCommit(context, name);
                if(ShortCutUtil.SHORT_CUT_EXISTING.equals(result)){
                    sure = "true";
                }
                String success_json = String.format(DOMException.JSON_SHORTCUT_SUCCESS_INFO, sure);
                try {
                    JSUtil.execCallback(webview, callbackID, new JSONObject(success_json), JSUtil.OK, false);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        };
        MessageHandler.postDelayed(runnable,500);
    }

    @Override
    public void init(AbsMgr pFeatureMgr, String pFeatureName) {
        mFeatureMgr = pFeatureMgr;
    }

    @Override
    public void dispose(String pAppid) {

    }

    private Bitmap getDefaultBitmap(IApp app) {
        String iconFilePath = getIconPath(app);
        if (iconFilePath != null) {
            return BitmapFactory.decodeFile(iconFilePath);
        }
        return null;
    }

    private String getIconPath(IApp app) {
        String iconFilePath = "";
        Intent intent = app.obtainWebAppIntent();
        if (intent != null) {
            iconFilePath = intent
                    .getStringExtra(IntentConst.WEBAPP_ACTIVITY_APPICON);
        }
        return iconFilePath;
    }

    private void createShortCut( IWebview pWebViewImpl, String name,
                                 Bitmap icon, String className, String toast, JSONObject extraJSON, boolean isForce, boolean isCheck, String callbackID) {
        IApp _app = pWebViewImpl.obtainApp();
        final String appid = _app.obtainAppId();
        final Activity context = pWebViewImpl.getActivity();
        SharedPreferences pdrSharedPre = PlatformUtil.getOrCreateBundle(SP.N_BASE);
        if (PdrUtil.isEmpty(name)) {
            name = _app.obtainAppName();
        }

		/*if(!AppPermissionUtil.checkShortcutOps(_app, _app.getActivity(), _app.obtainAppId(), name)) {
            return;
		}
		// 判断魅族快捷方式权限
		if (Build.BRAND.equals(MobilePhoneModel.MEIZU) && !AppPermissionUtil.isFlymeShortcutallowAllow(context, ShortCutUtil.getHeadShortCutIntent(name))) {
			AppPermissionUtil.showShortCutOpsDialog(_app, _app.getActivity(), appid, pdrSharedPre);
			return;
		}
		// 判断华为快捷方式权限
		if (Build.MANUFACTURER.equalsIgnoreCase(MobilePhoneModel.HUAWEI) && !AppPermissionUtil.isEmuiShortcutallowAllow()) {
			AppPermissionUtil.showShortCutOpsDialog(_app, _app.getActivity(), appid, pdrSharedPre);
			return;
		}
		// 检测如果当前手机为特殊对待的手机，并且未提示过创建快捷方式设置 拦截创建
		if (MobilePhoneModel.isSpecialPhone(context) && ShortCutUtil.showSettingsDialog(_app, null, icon)) {
			return;
		}*/
        // 判断当前手机是否支持创建快捷方式
		/*if (ShortcutCreateUtil.isDisableShort(pWebViewImpl.getActivity())) {
			return;
		}*/
        boolean created = pdrSharedPre.getBoolean(appid + SP.K_CREATED_SHORTCUT, false);
        if(TextUtils.isEmpty(className)){
            Intent intent = pWebViewImpl.obtainApp().obtainWebAppIntent();
            if(intent != null){
                className = intent.getStringExtra(IntentConst.WEBAPP_SHORT_CUT_CLASS_NAME);
            }
        }
        if (ShortcutCreateUtil.isDuplicateLauncher(context)) {
            if (ShortCutUtil.createShortcutToDeskTop(context, appid, name, icon, className, extraJSON, true)){
                if(!TextUtils.isEmpty(toast) && ShortcutCreateUtil.needToast(context)){
                    Toast.makeText(context.getApplicationContext(), toast, Toast.LENGTH_LONG).show();
                }
                ShortCutUtil.commitShortcut(_app, TestUtil.PointTime.S_TYPE_11, 1);
            }
        } else {
            if (!ShortCutUtil.hasShortcut(context, name)) {
                //是否强制创建快捷方式
                if (isForce) {
                    if (!TextUtils.isEmpty(toast)) {
                        if(ShortcutCreateUtil.needToast(context)){
                            Toast.makeText(context.getApplicationContext(), toast, Toast.LENGTH_LONG).show();
                        }
                    }
                    ShortCutUtil.createShortcutToDeskTop(context, appid, name, icon, className, extraJSON, true);
                    ShortCutUtil.commitShortcut(_app, TestUtil.PointTime.S_TYPE_11, 1);
                } else {
                    if (created) {
                        return;
                    }
                    if (ShortCutUtil.createShortcutToDeskTop(context, appid, name, icon, className, extraJSON, true)){
                        if(!TextUtils.isEmpty(toast) && ShortcutCreateUtil.needToast(context)){
                            Toast.makeText(context.getApplicationContext(), toast, Toast.LENGTH_LONG).show();
                        }
                        ShortCutUtil.commitShortcut(_app, TestUtil.PointTime.S_TYPE_11, 1);
                    }
                }
            }
        }
        handleCallback(pWebViewImpl.getContext(), pWebViewImpl, callbackID, name);

    }

}
