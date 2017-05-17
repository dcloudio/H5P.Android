package io.dcloud.feature.oauth;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.StandardFeature;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSUtil;

import org.json.JSONArray;

public class OAuthFeatureImpl extends StandardFeature {

	public void getServices(IWebview pWebViewImpl, 
			JSONArray pJsArgs) throws Exception{
		loadModules();
		String callbackId = pJsArgs.getString(0);
		JSUtil.execCallback(pWebViewImpl, callbackId, toModuleJSONArray(), JSUtil.OK, false);
	}
	
	public void addPhoneNumber(IWebview pWebViewImpl,JSONArray pJsArgs) throws Exception {
		String id = pJsArgs.getString(0);
		BaseOAuthService service = (BaseOAuthService)getBaseModuleById(id);
		if(service != null){
			service.addPhoneNumber(pWebViewImpl, pJsArgs);
		}else{
			String json = String.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_OAUTH_LOGIN,DOMException.MSG_OAUTH_LOGIN);
			JSUtil.execCallback(pWebViewImpl, pJsArgs.getString(1), json, JSUtil.ERROR,false);
		}
	}
	public void login(IWebview pWebViewImpl,JSONArray pJsArgs) throws Exception {
		String id = pJsArgs.getString(0);
		BaseOAuthService service = (BaseOAuthService)getBaseModuleById(id);
		if(service != null){
			service.login(pWebViewImpl, pJsArgs);
		}else{
			String json = String.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_OAUTH_LOGIN,DOMException.MSG_OAUTH_LOGIN);
			JSUtil.execCallback(pWebViewImpl, pJsArgs.getString(1), json, JSUtil.ERROR,false);
		}
	}
	public void logout(IWebview pWebViewImpl,JSONArray pJsArgs) throws Exception{
		String id = pJsArgs.getString(0);
		BaseOAuthService service = (BaseOAuthService)getBaseModuleById(id);
		if(service != null){
			service.logout(pWebViewImpl, pJsArgs);
		}else{
			String json = String.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_OAUTH_LOGOUT,DOMException.MSG_OAUTH_LOGOUT);
			JSUtil.execCallback(pWebViewImpl, pJsArgs.getString(1), json, JSUtil.ERROR,false);
		}
	}
	public void getUserInfo(IWebview pWebViewImpl,JSONArray pJsArgs) throws Exception{
		String id = pJsArgs.getString(0);
		BaseOAuthService service = (BaseOAuthService)getBaseModuleById(id);
		if(service != null){
			service.getUserInfo(pWebViewImpl, pJsArgs);
		}else{
			String json = String.format(DOMException.JSON_ERROR_INFO, DOMException.CODE_OAUTH_GET_USERINFO,DOMException.MSG_OAUTH_GET_USERINFO);
			JSUtil.execCallback(pWebViewImpl, pJsArgs.getString(1), json, JSUtil.ERROR,false);
		}
	}
}
