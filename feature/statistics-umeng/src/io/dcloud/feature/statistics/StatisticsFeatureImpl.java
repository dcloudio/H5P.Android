package io.dcloud.feature.statistics;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.IWebview;

/**
 * <p>Description:统计接口类</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-5-6 上午10:30:47 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-6 上午10:30:47</pre>
 */
public class StatisticsFeatureImpl implements IFeature {
	/**
	 * 友盟统计管理者
	 */
	private UmengStatisticsMgr mStatisticsMgr;
	/**
	 * 
	 * Description:接口处理分发
	 * @param pWebViewImpl
	 * @param pActionName
	 * @param pJsArgs
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-6 上午11:09:45</pre>
	 */
	@Override
	public String execute(IWebview pWebViewImpl, String pActionName,
			String[] pJsArgs) {
		return mStatisticsMgr.execute(pWebViewImpl, pActionName, pJsArgs);
	}
	/**
	 * 
	 * Description:初始化管理者
	 * @param pFeatureMgr
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-6 上午11:05:17</pre>
	 */
	@Override
	public void init(AbsMgr pFeatureMgr, String pFeatureName) {
		mStatisticsMgr = new UmengStatisticsMgr(pFeatureMgr.getContext());
	}

	@Override
	public void dispose(String pAppid) {
	}

}
