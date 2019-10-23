package io.dcloud.net;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.IWebview;

public class DownloaderFeatureImpl implements IFeature {

	@Override
	public String execute(IWebview pWebViewImpl, String pActionName,
			String[] pJsArgs) {
		return mDownloadMgr.execute(pWebViewImpl, pActionName, pJsArgs);
	}
	DownloadJSMgr mDownloadMgr = null;
	@Override
	public void init(AbsMgr pFeatureMgr, String pFeatureName) {
		mDownloadMgr = DownloadJSMgr.getInstance();
	}

	@Override
	public void dispose(String pAppid) {
		if(mDownloadMgr != null){
			mDownloadMgr.dispose();
		}
	}

}
