package io.dcloud.share;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.IWebview;

/**
 * <p>Description:分享接口</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-5-27 上午11:40:49 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-27 上午11:40:49</pre>
 */
public class ShareFeatureImpl implements IFeature {

	private ShareApiManager mShareApiManager;
	
	@Override
	public String execute(IWebview pWebViewImpl, String pActionName,
			String[] pJsArgs) {
		return mShareApiManager.execute(pWebViewImpl, pActionName, pJsArgs);
	}

	@Override
	public void init(AbsMgr pFeatureMgr, String pFeatureName) {
		mShareApiManager = new ShareApiManager(pFeatureMgr, pFeatureName);
	}

	@Override
	public void dispose(String pAppid) {
		if(null == pAppid){
			if(null!=mShareApiManager){
				mShareApiManager.dispose();
			}
			//mShareApiManager = null;
		}
	}

}
