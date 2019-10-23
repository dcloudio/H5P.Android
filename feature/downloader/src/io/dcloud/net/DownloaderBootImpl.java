package io.dcloud.net;

import io.dcloud.common.DHInterface.IBoot;
import android.content.Context;
import android.os.Bundle;

/**
 * <p>Description:(描述类的作用与功能)</p>
 * <p>Example:(如果是组件则写出它的基本用法示例，否则删除本项)</p>
 * <p>Note:(使用本类时需要注意的问题，无则删除本项)</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-5-18 上午11:45:38 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-5-18 上午11:45:38</pre>
 */
public class DownloaderBootImpl implements IBoot {

	@Override
	public boolean onExecute(SysEventType pEventType, Object pArgs) {

		return false;
	}

	@Override
	public void onStart(Context pContext, Bundle pSavedInstanceState, String[] pRuntimeArgs) {
	}

	@Override
	public void onStop() {
	}

	@Override
	public void onPause() {
		DownloadJSMgr.getInstance().dispose();
	}

	@Override
	public void onResume() {
		
	}

}
