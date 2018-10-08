package io.dcloud.js.map.amap.adapter;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.JSUtil;

import java.util.List;
import java.util.Locale;

import android.content.Intent;
import android.net.Uri;
import android.text.TextUtils;

import com.amap.api.maps.AMapUtils;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.GeocodeSearch.OnGeocodeSearchListener;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;



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
	public static void geocode(final IWebview webview, String address, final String coordType, String city, final String callBackId) {
		GeocodeSearch geocodeSearch = new GeocodeSearch(webview.getContext());
		geocodeSearch.setOnGeocodeSearchListener(new OnGeocodeSearchListener() {
			
			@Override
			public void onRegeocodeSearched(RegeocodeResult arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onGeocodeSearched(GeocodeResult result, int code) {
				// TODO Auto-generated method stub
				if(code == 1000) {//兼容低版本成功错误号
					code = 0;
				}
				if(code == 0) { //搜索成功
					if (result != null && result.getGeocodeAddressList() != null
							&& result.getGeocodeAddressList().size() > 0) {
						GeocodeAddress GeoAddress = result.getGeocodeAddressList().get(0);
						LatLonPoint point = GeoAddress.getLatLonPoint();
						String geoCode_F = "{" +
								"long:%f" +
								",lat:%f" +
								",addr:'%s'" +
								",type:'%s'" +
								"}";
						String geoCode = String.format(Locale.ENGLISH, geoCode_F,point.getLongitude(), point.getLatitude(), GeoAddress.getFormatAddress(),coordType);
						JSUtil.execCallback(webview, callBackId, geoCode, JSUtil.OK, true, false);
					} else {
						String error = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString("对不起，没有搜索到相关数据！"));
						JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
					}
				} else if(code == 27 || code == 1804){
					String error = DOMException.toJSON(DOMException.CODE_NETWORK_ERROR, DOMException.toString(code, "Maps高德地图", "网络错误", AMapLink.AMapErrorLink));
					JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
				} else if(code == 32 || code == 1001){
					String error = DOMException.toJSON(DOMException.CODE_NETWORK_ERROR, DOMException.toString(code, "Maps高德地图", "key验证无效！", AMapLink.AMapErrorLink));
					JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
				} else {
					String error = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString(code, "Maps高德地图", "未知错误", AMapLink.AMapErrorLink));
					JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
				}
			}
		});
		geocodeSearch.getFromLocationNameAsyn(new GeocodeQuery(address, city));
	}
	
	/**
	 * 反向地理编码
	 * @param point
	 * @param coordType
	 * @param city
	 * @param callBackId
	 */
	public static void reverseGeocode(final IWebview webview, final MapPoint point, final String coordType, String city, final String callBackId) {
		GeocodeSearch geocodeSearch = new GeocodeSearch(webview.getContext());
		geocodeSearch.setOnGeocodeSearchListener(new OnGeocodeSearchListener() {
			
			@Override
			public void onRegeocodeSearched(RegeocodeResult result, int rCode) {
				// TODO Auto-generated method stub
				if(rCode == 1000) {//兼容低版本成功错误码
					rCode = 0;
				}
				if (rCode == 0) {
					if (result != null && result.getRegeocodeAddress() != null
							&& result.getRegeocodeAddress().getFormatAddress() != null) {
						String geoCode_F = "{" +
								"long:%f" +
								",lat:%f" +
								",addr:'%s'" +
								",type:'%s'" +
								"}";
						RegeocodeAddress address = result.getRegeocodeAddress();
						String geoCode = String.format(Locale.ENGLISH, geoCode_F,point.getLongitude(), point.getLatitude(), address.getFormatAddress(),coordType);
						JSUtil.execCallback(webview, callBackId, geoCode, JSUtil.OK, true, false);
					} else {
						String error = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString("对不起，没有搜索到相关数据！"));
						JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
					}
				} else if (rCode == 27 || rCode == 1804) {
					String error = DOMException.toJSON(DOMException.CODE_NETWORK_ERROR, DOMException.toString(rCode, "Maps高德地图", "网络错误", AMapLink.AMapErrorLink));
					JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
				} else if(rCode == 32 || rCode == 1001){
					String error = DOMException.toJSON(DOMException.CODE_NETWORK_ERROR, DOMException.toString(rCode, "Maps高德地图", "key验证无效！", AMapLink.AMapErrorLink));
					JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
				} else {
					String error = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString(rCode, "Maps高德地图", "未知错误", AMapLink.AMapErrorLink));
					JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
				}
			}
			
			@Override
			public void onGeocodeSearched(GeocodeResult arg0, int arg1) {
				// TODO Auto-generated method stub
				
			}
		});
		String latLonType = geocodeSearch.AMAP;
		if (!TextUtils.isEmpty(coordType) && "wgs84".equals(coordType)) {
			latLonType = geocodeSearch.GPS;
		}
		geocodeSearch.getFromLocationAsyn(new RegeocodeQuery(point.getLatLngPoint(), 3000, latLonType));
	}
	
	/**
	 * 根据用户的起点和终点经纬度计算两点间距离，此距离为相对较短的距离，单位米。
	 * @param sPoint
	 * @param ePoint
	 * @param callBackId
	 */
	public static void calculateDistance(IWebview webview, MapPoint sPoint, MapPoint ePoint, String callBackId) {
		try {
			float d = AMapUtils.calculateLineDistance(sPoint.getLatLng(), ePoint.getLatLng());
			if (d == 0) {
				String error = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString("计算结果为0，请查看坐标是否正确"));
				JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
				return;
			}
			if (!TextUtils.isEmpty(callBackId)) {
				JSUtil.execCallback(webview, callBackId, d, JSUtil.OK, true);
			}
		}catch(Exception e) {
			String error = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString("数据信息异常"));
			JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
		}
	}
	
	/**
	 * 计算地图上矩形区域的面积，单位平方米
	 * @param sPoint
	 * @param ePoint
	 * @param callBackId
	 */
	public static void calculateArea(IWebview webview, MapPoint sPoint, MapPoint ePoint, String callBackId) {
		/*try {
			float d = AMapUtils.calculateArea(sPoint.getLatLng(), ePoint.getLatLng());
			if (d == 0) {
				String error = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, "计算结果为0，请查看坐标是否正确");
				JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
				return;
			}
			if (!TextUtils.isEmpty(callBackId)) {
				Logger.d("shutao", "calculateArea-----="+d);
				JSUtil.execCallback(webview, callBackId, d, JSUtil.OK, true);
			}
		}catch(Exception e) {
			String error = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, "数据信息异常");
			JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
		}*/
		String error = DOMException.toJSON(DOMException.CODE_BUSINESS_INTERNAL_ERROR, DOMException.toString("高德地图暂不支持面积计算"));
		JSUtil.execCallback(webview, callBackId, error, JSUtil.ERROR, true, false);
	}
}
