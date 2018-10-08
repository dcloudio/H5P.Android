package io.dcloud.feature.barcode;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.IWaiter;
import io.dcloud.common.DHInterface.IWebview;

public class BarcodeFeatureImpl implements IFeature ,IWaiter{

	BarcodeProxyMgr mBProxyMgr;

	@Override
	public String execute(IWebview pWebViewImpl, String pActionName,
			String[] pJsArgs) {
		return mBProxyMgr.execute(pWebViewImpl, pActionName, pJsArgs);
	}

	@Override
	public void init(AbsMgr pFeatureMgr, String pFeatureName) {
		mBProxyMgr = BarcodeProxyMgr.getBarcodeProxyMgr();
		mBProxyMgr.setFeatureMgr(pFeatureMgr);
	}

	@Override
	public void dispose(String pAppid) {
		mBProxyMgr.onDestroy();
	}

	@Override
	public Object doForFeature(String actionType, Object args) {
		return mBProxyMgr.doForFeature(actionType, args);
	}
}
