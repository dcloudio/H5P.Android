package io.dcloud.adapter.messaging;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.IWebview;

/**
 * <p>Description:messaging的js扩展</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-1-8 下午3:56:16 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-8 下午3:56:16</pre>
 */
public class MessagingPluginImpl implements IFeature {

	@Override
	public String execute(IWebview pWebViewImpl, String pActionName,
			String[] pJsArgs) {
		if("sendMessage".equals(pActionName)){
			DHMessaging _messaging = DHMessaging.parseMessage(pWebViewImpl, pJsArgs[0],pJsArgs[1]);
		}else if("listenMessage".equals(pActionName)){
			new SMSListener(pWebViewImpl,pJsArgs[0]).listen();
		}
		return null;
	}

	@Override
	public void init(AbsMgr pFeatureMgr, String pFeatureName) {
		DHMessagCenter.initDHMessaging(pFeatureMgr.getContext());
	}

	@Override
	public void dispose(String pAppid) {
	}

}
