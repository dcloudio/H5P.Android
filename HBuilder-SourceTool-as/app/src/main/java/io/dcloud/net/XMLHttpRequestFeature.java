package io.dcloud.net;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.IWebview;

/**
 * <p>Description:XHR入口</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-4-7 下午12:15:56 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-4-7 下午12:15:56</pre>
 */
public class XMLHttpRequestFeature implements IFeature{

	private XMLHttpRequestMgr mXHRMgr;
	@Override
	public String execute(IWebview pWebViewImpl, String pActionName,
			String[] pJsArgs) {
		return mXHRMgr.execute(pWebViewImpl, pActionName, pJsArgs);
	}

	@Override
	public void init(AbsMgr pFeatureMgr, String pFeatureName) {
		mXHRMgr = new XMLHttpRequestMgr();
	}

	@Override
	public void dispose(String pAppid) {
		
	}

}
