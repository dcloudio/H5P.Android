package io.dcloud.feature.contacts;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.util.PdrUtil;

/**
 * <p>Description:联系人接口</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-5-5 下午6:09:51 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-5 下午6:09:51</pre>
 */
public class ContactsFeatureImpl implements IFeature {

	private JsContactsMgr mContactsMgr;
	@Override
	public String execute(final IWebview pWebViewImpl,final String pActionName,
			final String[] pJsArgs) {
		new Thread(){
			public void run() {
				mContactsMgr.execute(pWebViewImpl, pActionName, pJsArgs);
			};
		}.start();
		return null;
	}

	@Override
	public void init(AbsMgr pFeatureMgr, String pFeatureName) {
		mContactsMgr = new JsContactsMgr(pFeatureMgr.getContext());
	}

	@Override
	public void dispose(String pAppid) {
		mContactsMgr.dispose(pAppid);
		if(PdrUtil.isEmpty(pAppid)){
			mContactsMgr = null;
		}
	}

}
