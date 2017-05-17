package io.dcloud.share;

import io.dcloud.common.DHInterface.IReflectAble;
import io.dcloud.common.DHInterface.IWebview;

/**
 * <p>Description:分享接口</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-5-28 下午3:07:32 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-28 下午3:07:32</pre>
 */
public interface IFShareApi extends IReflectAble{

	 String mLink = "http://ask.dcloud.net.cn/article/287";
	 void initConfig();
	 String getId();
	 void send(IWebview pWebViewImpl, String pCallbackId, String pShareMsg);
	 void forbid(IWebview pWebViewImpl);
	 void authorize(IWebview pWebViewImpl, String pCallbackId,String options);
	 String getJsonObject(IWebview pWebViewImpl);
	 void dispose();
	 
	
}
