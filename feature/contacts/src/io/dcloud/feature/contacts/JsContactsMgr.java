package io.dcloud.feature.contacts;

import android.Manifest;
import android.content.Context;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.PermissionUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;

/**
 * <p>Description:联系人管理类</p>
 *
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @version 1.0
 * @Date 2013-5-5 下午6:13:52 created.
 *
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-5 下午6:13:52</pre>
 */
public class JsContactsMgr {

    private ContactAccessor mContactAccessor;

    private static final String UNKNOWN_ERROR = "0";
    private static final String INVALID_ARGUMENT_ERROR = "1";
    private static final String TIMEOUT_ERROR = "2";
    private static final String PENDING_OPERATION_ERROR = "3";
    private static final String IO_ERROR = "4";
    private static final String NOT_SUPPORTED_ERROR = "5";
    private static final String PERMISSION_DENIED_ERROR = "20";

    public JsContactsMgr(Context pContext) {
        mContactAccessor = new ContactAccessorImpl(pContext);
    }

    public String execute(final IWebview pWebViewImpl, String pActionName,
                          final String[] pJsArgs) {
        if (android.os.Build.VERSION.RELEASE.startsWith("1.")) {
            JSUtil.execCallback(pWebViewImpl, pJsArgs[0], NOT_SUPPORTED_ERROR, JSUtil.ERROR, false, false);
        }
        mContactAccessor.mApp = pWebViewImpl.getActivity();
        mContactAccessor.mView = pWebViewImpl;
        if (pActionName.equals("getAddressBook")) {
            PermissionUtil.usePermission(pWebViewImpl.getActivity(), pWebViewImpl.obtainApp().isStreamApp(), Manifest.permission.READ_CONTACTS, new PermissionUtil.Request() {
                @Override
                public void onGranted(String streamPerName) {
                    JSUtil.execCallback(pWebViewImpl, pJsArgs[0], "", JSUtil.OK, false, false);
                }

                @Override
                public void onDenied(String streamPerName) {
                    JSUtil.execCallback(pWebViewImpl, pJsArgs[0], PERMISSION_DENIED_ERROR, JSUtil.ERROR, false, false);
                }
            });
        } else if (pActionName.equals("search")) {
            JSONArray fieldArray = null;
            JSONObject jsonObject = null;
            try {
                try {
                    if (PdrUtil.isEmpty(pJsArgs[1])) {
                        fieldArray = new JSONArray();
                    } else if (pJsArgs[1].indexOf('[') >= 0) {
                        fieldArray = new JSONArray(pJsArgs[1]);
                    } else {
                        fieldArray = new JSONArray();
                        fieldArray.put(0, pJsArgs[1]);
                    }

                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (!PdrUtil.isEmpty(pJsArgs[2])) {
                    jsonObject = new JSONObject(pJsArgs[2]);
                }
                fieldArray = mContactAccessor.search(fieldArray, jsonObject);
                JSUtil.execCallback(pWebViewImpl, pJsArgs[0], fieldArray.toString(), JSUtil.OK, true, false);
            } catch (JSONException e) {
                e.printStackTrace();
                JSUtil.execCallback(pWebViewImpl, pJsArgs[0], INVALID_ARGUMENT_ERROR, JSUtil.ERROR, false, false);
            }
        } else if (pActionName.equals("save")) {
            PermissionUtil.usePermission(pWebViewImpl.getActivity(), pWebViewImpl.obtainApp().isStreamApp(), Manifest.permission.WRITE_CONTACTS, new PermissionUtil.Request() {
                @Override
                public void onGranted(String streamPerName) {
                    JSONObject jsonObject;
                    try {
                        jsonObject = new JSONObject(pJsArgs[1]);//pWebViewImpl.obtainFrameView().obtainApp().convert2AbsFullPath("_www/ui/icon.png")
                        boolean ret = mContactAccessor.save(jsonObject);
                        if (ret) {
                            JSUtil.execCallback(pWebViewImpl, pJsArgs[0], jsonObject.toString(), JSUtil.OK, true, false);
                        } else {
                            JSUtil.execCallback(pWebViewImpl, pJsArgs[0], INVALID_ARGUMENT_ERROR, JSUtil.ERROR, false, false);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        JSUtil.execCallback(pWebViewImpl, pJsArgs[0], INVALID_ARGUMENT_ERROR, JSUtil.ERROR, false, false);
                    }
                }

                @Override
                public void onDenied(String streamPerName) {
                    JSUtil.execCallback(pWebViewImpl, pJsArgs[0], PERMISSION_DENIED_ERROR, JSUtil.ERROR, false, false);
                }
            });

        } else if (pActionName.equals("remove")) {
            PermissionUtil.usePermission(pWebViewImpl.getActivity(), pWebViewImpl.obtainApp().isStreamApp(), Manifest.permission.WRITE_CONTACTS, new PermissionUtil.Request() {
                @Override
                public void onGranted(String streamPerName) {
                    if (mContactAccessor.remove(pJsArgs[1])) {
                        JSUtil.execCallback(pWebViewImpl, pJsArgs[0], "", JSUtil.OK, false, false);
                    } else {
                        JSUtil.execCallback(pWebViewImpl, pJsArgs[0], UNKNOWN_ERROR, JSUtil.ERROR, false, false);
                    }
                }

                @Override
                public void onDenied(String streamPerName) {
                    JSUtil.execCallback(pWebViewImpl, pJsArgs[0], PERMISSION_DENIED_ERROR, JSUtil.ERROR, false, false);
                }
            });
        }
        return null;
    }

    void dispose(String pAppid) {

    }
}
