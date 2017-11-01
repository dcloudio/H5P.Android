package io.dcloud.net;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.IWebview;

/**
 * <p>Description:上传js接口</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-3-27 下午12:19:28 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-27 下午12:19:28</pre>
 */
public class UploadFeature implements IFeature {
	
	private JsUploadMgr mJsUploadMgr;
	@Override
	public String execute(IWebview pWebViewImpl, String pActionName,
			String[] pJsArgs) {
		mJsUploadMgr.execute(pWebViewImpl, pActionName, pJsArgs);
		return null;
	}

	@Override
	public void init(AbsMgr pFeatureMgr, String pFeatureName) {
		mJsUploadMgr = new JsUploadMgr();
	}

	@Override
	public void dispose(String pAppid) {
	}

}
