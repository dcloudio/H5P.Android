package io.dcloud.share;

import io.dcloud.common.DHInterface.IReflectAble;
import android.webkit.WebViewClient;

public abstract class  AbsWebviewClient extends WebViewClient implements IReflectAble{
	public abstract String getInitUrl();
}
