package io.dcloud.js.geolocation;

import android.os.Build;
import android.text.TextUtils;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.FeatureMessageDispatcher;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.PermissionUtil;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSUtil;

import static io.dcloud.common.util.ReflectUtils.getApplicationContext;

/**
 * <p>Description:定位接口类</p>
 *
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @version 1.0
 * @Date 2013-4-11 上午11:49:24 created.
 * <p>
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-11 上午11:49:24</pre>
 */
public class GeolocationFeatureImpl implements IFeature {

    private GeoOptDispatcher mGeoBroker;
    private boolean isPermissionGranted = false;
    @Override
    public String execute(final IWebview pWebViewImpl, final String pActionName,
                          final String[] pJsArgs) {
        if(FeatureMessageDispatcher.contains("record_address")){//辅助输入获取位置啥不进行流权限检测
            mGeoBroker.execute(pWebViewImpl, pActionName, pJsArgs);
        }else {
            isPermissionGranted = false;
            PermissionUtil.usePermission(pWebViewImpl.getActivity(), pWebViewImpl.obtainApp().isStreamApp(), PermissionUtil.PMS_LOCATION, new PermissionUtil.StreamPermissionRequest(pWebViewImpl.obtainApp()) {
                @Override
                public void onGranted(String streamPerName) {
                    if(!isPermissionGranted) {
                        isPermissionGranted = true;
                        mGeoBroker.execute(pWebViewImpl, pActionName, pJsArgs);
                    }
                    int targetSdkVersion = getApplicationContext().getApplicationInfo().targetSdkVersion;
                    // 系统版本>=29 && target>=29，才需要申请后台权限，否则系统默认处理
                    if (Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q && targetSdkVersion>=29 && isPermissionGranted) {
                        PermissionUtil.usePermission(pWebViewImpl.getActivity(), pWebViewImpl.obtainApp().isStreamApp(), "android.permission.ACCESS_BACKGROUND_LOCATION", new PermissionUtil.StreamPermissionRequest(pWebViewImpl.obtainApp()) {
                            @Override
                            public void onGranted(String streamPerName) {
                            }

                            @Override
                            public void onDenied(String streamPerName) {
                            }
                        });
                    }
                }

                @Override
                public void onDenied(String streamPerName) {
                    String _json = DOMException.toJSON(DOMException.CODE_GEOLOCATION_PERMISSION_ERROR, DOMException.MSG_GEOLOCATION_PERMISSION_ERROR);
                    JSUtil.execCallback(pWebViewImpl, pJsArgs[0], _json, JSUtil.ERROR, true, false);
                }
            });


        }
        return null;
    }

    @Override
    public void init(AbsMgr pFeatureMgr, String pFeatureName) {
        mGeoBroker = new GeoOptDispatcher(pFeatureMgr);
    }

    @Override
    public void dispose(String pAppid) {
        if (TextUtils.isEmpty(pAppid)) {
            mGeoBroker.onDestroy();
        }
    }

}
