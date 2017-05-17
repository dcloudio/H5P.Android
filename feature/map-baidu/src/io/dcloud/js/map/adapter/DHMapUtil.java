package io.dcloud.js.map.adapter;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSUtil;

import java.util.Locale;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.geocode.GeoCodeOption;
import com.baidu.mapapi.search.geocode.GeoCodeResult;
import com.baidu.mapapi.search.geocode.GeoCoder;
import com.baidu.mapapi.search.geocode.OnGetGeoCoderResultListener;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeOption;
import com.baidu.mapapi.search.geocode.ReverseGeoCodeResult;
import com.baidu.mapapi.utils.AreaUtil;
import com.baidu.mapapi.utils.CoordinateConverter;
import com.baidu.mapapi.utils.CoordinateConverter.CoordType;
import com.baidu.mapapi.utils.DistanceUtil;



/**
 * <p>Description:地图工具</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2012-11-8 下午3:09:24 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-8 下午3:09:24</pre>
 */
public class DHMapUtil {
	 
	/**
	 * 开启手机第三方地图
	 * @param pWebView 执行所在webview
	 * @param callbackId 回调callbackid
	 * @param points 点（[lat,lng]）的数组
	 * @param pDdes 描述信息
	 * <br/>Create By: yanglei Email:yanglei@dcloud.io at 2014-5-30 下午04:16:51
	 */
	public static void openSysMap(IWebview pWebView,String callbackId,String[][] points,String pDdes){
		try {
			Uri _uri;
			/**
			 * 当没有传递过来描述的时候URI不需要传递参数
			 */
			if(pDdes!=null){
				_uri = Uri.parse("geo:"+points[0][0]+ ","+points[0][1]+"?q="+pDdes); 
			}else{
				_uri = Uri.parse("geo:"+points[0][0]+ ","+points[0][1]);
			}
			Intent _intent = new Intent(Intent.ACTION_VIEW,_uri); 
			pWebView.getActivity().startActivity(_intent); 
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	

	/**
	 * 地理编码转换
	 * @param address
	 * @param coordType
	 * @param city
	 * @param callBackId
	 */
	public static void geocode(final IWebview webview, String address, String coordType, String city, final String callBackId) {

		GeoCoder geoCoder = GeoCoder.newInstance();
		geoCoder.geocode(new GeoCodeOption().address(address).city(city));
		geoCoder.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
			
			@Override
			public void onGetReverseGeoCodeResult(ReverseGeoCodeResult arg0) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onGetGeoCodeResult(GeoCodeResult geocode) {
				// TODO Auto-generated method stub
				if (geocode.error == SearchResult.ERRORNO.NO_ERROR) {
					String coordType = "bd09ll";
					String address = geocode.getAddress();
					LatLng latLng = geocode.getLocation();
					String geoCode_F = "{" +
							"long:%f" +
							",lat:%f" +
							",addr:'%s'" +
							",type:'%s'" +
							"}";
					String geoCode = String.format(Locale.ENGLISH, geoCode_F, latLng.longitude, latLng.latitude, address, coordType);
					JSUtil.execCallback(webview, callBackId, geoCode, JSUtil.OK, true, false);
				} else if (geocode.error == SearchResult.ERRORNO.NETWORK_ERROR) { // 网络错误
					String error = DOMException.toJSON(DOMException.CODE_NETWORK_ERROR, DOMException.toString(geocode.error.toString(),"Maps百度地图", DOMException.MSG_NETWORK_ERROR, BaiduErrorLink.BaiduLink));
					JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
				} else {
					String error = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString(geocode.error.toString(),"Maps百度地图", geocode.error.toString(), BaiduErrorLink.BaiduLink));
					JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
				}
			}
		});
	}
	
	

	/**
	 * 反向地理编码
	 * @param point
	 * @param coordType
	 * @param city
	 * @param callBackId
	 */
	public static void reverseGeocode(final IWebview webview, MapPoint point, String coordType, String city, final String callBackId) {
		LatLng lng = point.getLatLng();
		if (!TextUtils.isEmpty(coordType) && coordType.equals("bd09ll")) {
			lng = convertCoordinates(webview, point, coordType, null);
		}
		GeoCoder geoCoder = GeoCoder.newInstance();
		geoCoder.reverseGeoCode(new ReverseGeoCodeOption().location(lng));
		geoCoder.setOnGetGeoCodeResultListener(new OnGetGeoCoderResultListener() {
			
			@Override
			public void onGetReverseGeoCodeResult(ReverseGeoCodeResult reverseGeoCodeResult) {
				// TODO Auto-generated method stub
				if (reverseGeoCodeResult.error == SearchResult.ERRORNO.NO_ERROR) {
					String coordType = "bd09ll";
					String address = reverseGeoCodeResult.getAddress();
					LatLng latLng = reverseGeoCodeResult.getLocation();
					String geoCode_F = "{" +
							"long:%f" +
							",lat:%f" +
							",addr:'%s'" +
							",type:'%s'" +
							"}";
					String geoCode = String.format(Locale.ENGLISH, geoCode_F, latLng.longitude, latLng.latitude, address, coordType);
					JSUtil.execCallback(webview, callBackId, geoCode, JSUtil.OK, true, false);
				} else if (reverseGeoCodeResult.error == SearchResult.ERRORNO.NETWORK_ERROR) { // 网络错误
					String error = DOMException.toJSON(DOMException.CODE_NETWORK_ERROR, DOMException.toString(reverseGeoCodeResult.error.toString(),"Maps百度地图", DOMException.MSG_NETWORK_ERROR, BaiduErrorLink.BaiduLink));
					JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
				} else {
					String error = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString(reverseGeoCodeResult.error.toString(),"Maps百度地图", reverseGeoCodeResult.error.toString(), BaiduErrorLink.BaiduLink));
					JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
				}
			}
			
			@Override
			public void onGetGeoCodeResult(GeoCodeResult arg0) {
				// TODO Auto-generated method stub
				
			}
		});
	}
	/**
	 * 百度系坐标转换
	 * @param point
	 * @param coordType
	 * @param callBackId
	 * @return
	 */
	public static LatLng convertCoordinates(IWebview webview, MapPoint point, String coordType, String callBackId) {
		if (point == null) {
			return null;
		}
		CoordType type = CoordType.COMMON;
		if (!TextUtils.isEmpty(coordType) && "wgs84".equals(coordType)) {
			type = CoordType.GPS;
		}
		CoordinateConverter converter = new CoordinateConverter();
		converter.from(type);
		converter.coord(point.getLatLng());
		LatLng lng = converter.convert();
		if (!TextUtils.isEmpty(callBackId)) {
			if (lng == null) { // 为空表示转换失败 
				String error = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString("转换坐标失败"));
				JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
				return lng;
			}
			String coord = "{" +
					"long:%f" +
					",lat:%f" +
					",type:'%s'" +
					"}";
			// "bd09ll" 百度坐标系
			JSUtil.execCallback(webview, callBackId, String.format(Locale.ENGLISH, coord,lng.longitude, lng.latitude, "bd09ll"), JSUtil.OK, true, false);
		}
		return lng;
	}
	/**
	 * 测距工具
	 * @param sPoint
	 * @param ePoint
	 * @param callBackId
	 */
	public static void calculateDistance(IWebview webview, MapPoint sPoint, MapPoint ePoint, String callBackId) {
		try {
			double d = DistanceUtil.getDistance(sPoint.getLatLng(), ePoint.getLatLng());
			if (d == 0) {
				String error = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString("计算结果为0，请查看坐标是否正确"));
				JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
				return;
			}
			if (!TextUtils.isEmpty(callBackId)) {
				JSUtil.execCallback(webview, callBackId, d, JSUtil.OK, false);
			}
		} catch(Exception e) {
			String error = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString("数据信息异常"));
			JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
		}

	}
	/**
	 * 面积计算
	 * @param sPoint
	 * @param ePoint
	 * @param callBackId
	 */
	public static void calculateArea(IWebview webview, MapPoint sPoint, MapPoint ePoint, String callBackId) {
		try {
			double d = AreaUtil.calculateArea(sPoint.getLatLng(), ePoint.getLatLng());
			if (d == 0) {
				String error = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString("计算结果为0，请查看坐标是否正确"));
				JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
				return;
			}
			if (!TextUtils.isEmpty(callBackId)) {
				JSUtil.execCallback(webview, callBackId, d, JSUtil.OK, false);
			}
		}catch (Exception e) {
			String error = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString("数据信息异常"));
			JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
		}
	}
}
