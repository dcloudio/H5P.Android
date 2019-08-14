package io.dcloud.js.map;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.ui.AdaUniWebView;
import io.dcloud.common.util.PdrUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class MapJsUtil {

	public static void  execCallback(IWebview pWebViewImpl, String pCallbackId, String pMessage) {
		if(pWebViewImpl!=null){
			String jsFrom = "var p=((window.__html5plus__&&__html5plus__.isReady)?__html5plus__:(navigator.plus&&navigator.plus.isReady)?navigator.plus:window.plus);";
			if(pWebViewImpl instanceof AdaUniWebView) {
				jsFrom = "var p = plus;";
			}
			pWebViewImpl.executeScript(jsFrom + "p.maps.__bridge__.execCallback('" + pCallbackId +"',"+ pMessage + ");");
		}
	}
	
	/**
	 * 封装new js对象的语句
	 * Description:
	 * @param pDestScript 最终需要的js脚本
	 * @param pJsVar 要创建的js变量名称
	 * @param pJsConstructor 要创建js对象的类型
	 * @param pJsConParams 要创建js类型的构造函数参数
	 */
	public static void newJsVar(StringBuffer pDestScript,String pJsVar,String pJsConstructor,String pJsConParams){
		StringBuffer _sb = pDestScript;
		boolean emptyJsVar = PdrUtil.isEmpty(pJsVar);
		if(!emptyJsVar){
			_sb.append("var ").append(pJsVar).append("=");
		}
		_sb.append("new ").append(pJsConstructor).append("(").append(pJsConParams == null ? "":pJsConParams).append(")");
		if(!emptyJsVar){
			_sb.append(";");
		}
	}
	
	public static String wrapJsEvalString(String pScript,String pJsVar){
		StringBuffer sb = new StringBuffer();
		sb.append("(function(){").append(pScript).append(";return ").append(pJsVar).append(";})()");
		return sb.toString();
	}
	
	/**
	 * 封装对js变量赋值的语句
	 * Description:
	 * @param pDestScript 最终需要的js脚本
	 * @param pJsVar 要被赋值的js变量名称
	 * @param pJsVarProperty 要被赋值的js变量的属性（当为null时则认为给pJsVar变量赋值）
	 * @param pJsAssignValue 要赋值的值
	 */
	public static void assignJsVar(StringBuffer pDestScript,String pJsVar,String pJsVarProperty,String pJsAssignValue,boolean isString) {
		StringBuffer _sb = pDestScript;
		_sb.append(pJsVar);
		if(!PdrUtil.isEmpty(pJsVarProperty)){//给js对象的pJsVarProperty属性赋值，否则认为给pJsVar变量自身赋值
			_sb.append(".").append(pJsVarProperty);
		}
		_sb.append("=");
		if(isString){
			_sb.append("'").append(pJsAssignValue).append("'").append(";");
		}else{
			_sb.append(pJsAssignValue).append(";");
		}
	}
	public static void assignJsVar(StringBuffer pDestScript,String pJsVar,String pJsVarProperty,String pJsAssignValue) {
		assignJsVar(pDestScript, pJsVar, pJsVarProperty, pJsAssignValue, true);
	}
	public static void assignJsVar(StringBuffer pDestScript,String pJsVar,String pJsVarProperty,double pJsAssignValue) {
		assignJsVar(pDestScript, pJsVar, pJsVarProperty, String.valueOf(pJsAssignValue),false);
	}
	
	public static void assignJsVar(StringBuffer pDestScript,String pJsVar,String pJsVarProperty,JSONArray pJsAssignValue) {
		StringBuffer sb = new StringBuffer("[");
		try {
			int len = pJsAssignValue.length();
			for(int i = 0; i < len; i++){
				String str  = pJsAssignValue.getString(i);
				sb.append(str);
				if(i != len -1){
					sb.append(",");
				}
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}
		sb.append("]");
		assignJsVar(pDestScript, pJsVar, pJsVarProperty, sb.toString(),false);
	}
	
	public static void assignJsVar(StringBuffer pDestScript,String pJsVar,String pJsVarProperty,JSONObject pJsAssignValue) {
		assignJsVar(pDestScript, pJsVar, pJsVarProperty, pJsAssignValue.toString(),false);
	}
}
