package io.dcloud.feature.barcode;

import android.text.TextUtils;

import org.json.JSONObject;

import java.util.LinkedHashMap;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IMgr;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.ui.AdaFrameView;
import io.dcloud.common.util.JSUtil;

public class BarcodeProxyMgr {
    private static BarcodeProxyMgr mInstance;

    private LinkedHashMap<String, BarcodeProxy> mBProxyCaches;
    private AbsMgr mFeatureMgr;

    public BarcodeProxyMgr() {
        mBProxyCaches = new LinkedHashMap<String, BarcodeProxy>();
    }

    public static BarcodeProxyMgr getBarcodeProxyMgr() {
        if(mInstance == null) {
            mInstance = new BarcodeProxyMgr();
        }
        return mInstance;
    }

    public void setFeatureMgr(AbsMgr pFeatureMgr) {
        mFeatureMgr = pFeatureMgr;
    }

    public String execute(IWebview pWebViewImpl, String pActionName, String[] pJsArgs) {
        String _ret = null;
        if(pActionName.equals("getBarcodeById")) {
            String id = pJsArgs[0];
            BarcodeProxy barcodeProxy = getBarcodeProxyById(id);
            if(barcodeProxy != null) {
                JSONObject json = barcodeProxy.getJsBarcode();
                _ret = JSUtil.wrapJsVar(json);
            }
        } else {
            String uuid = pJsArgs[0];
            BarcodeProxy barcodeProxy;
            if(!mBProxyCaches.containsKey(uuid)) {
                barcodeProxy = new BarcodeProxy();
                mBProxyCaches.put(uuid, barcodeProxy);
            } else {
                barcodeProxy = mBProxyCaches.get(uuid);
            }
            barcodeProxy.execute(pWebViewImpl, pActionName, pJsArgs);
        }
        return _ret;
    }

    public BarcodeProxy getBarcodeProxyById(String id) {
        for(String key : mBProxyCaches.keySet()) {
            BarcodeProxy bp = mBProxyCaches.get(key);
            if(!TextUtils.isEmpty(bp.mId) && bp.mId.equals(id)) {
                return bp;
            }
        }
        return null;
    }

    public BarcodeProxy getBarcodeProxyByUuid(String uuid) {
        if(mBProxyCaches.containsKey(uuid)) {
            return mBProxyCaches.get(uuid);
        }
        return null;
    }

    public void removeBarcodeProxy(String uuid) {
        BarcodeProxy proxy = mBProxyCaches.remove(uuid);
        if(proxy != null) {
            proxy.onDestroy();
        }
    }

    public void onDestroy() {
        for(String uuid : mBProxyCaches.keySet()) {
            BarcodeProxy proxy = mBProxyCaches.remove(uuid);
            if(proxy != null) {
                proxy.onDestroy();
            }
        }
        mBProxyCaches.clear();
    }

    public Object doForFeature(String actionType, Object args) {
        if(actionType.equals("appendToFrameView")) {
            Object[] pArgs = (Object[]) args;
            AdaFrameView frameView = (AdaFrameView) pArgs[0];
            String uuid = (String) pArgs[1];
            String appid = frameView.obtainApp().obtainAppId();
            BarcodeProxy proxy = getBarcodeProxyByUuid(uuid);
            if(proxy != null) {
                proxy.appendToFrameView(frameView);
            }
        }
        return null;
    }

    public IWebview findWebviewByUuid(IWebview webview, String webUuid) {
        if(mFeatureMgr != null) {
            Object object =  mFeatureMgr.processEvent(IMgr.MgrType.FeatureMgr, IMgr.MgrEvent.CALL_WAITER_DO_SOMETHING,new Object[]{webview,"ui","findWebview",new String[]{webview.obtainApp().obtainAppId(), webUuid}});
            if(object != null) {
                return (IWebview) object;
            }
        }
        return null;
    }
}
