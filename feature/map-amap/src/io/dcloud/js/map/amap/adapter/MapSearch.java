package io.dcloud.js.map.amap.adapter;

import android.os.Handler;
import android.os.Message;

import com.amap.api.maps.MapView;
import com.amap.api.maps.TextureMapView;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeAddress;
import com.amap.api.services.geocoder.GeocodeQuery;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.amap.api.services.poisearch.PoiSearch.OnPoiSearchListener;
import com.amap.api.services.poisearch.PoiSearch.SearchBound;
import com.amap.api.services.route.BusPath;
import com.amap.api.services.route.BusRouteResult;
import com.amap.api.services.route.DrivePath;
import com.amap.api.services.route.DriveRouteResult;
import com.amap.api.services.route.DriveStep;
import com.amap.api.services.route.RideRouteResult;
import com.amap.api.services.route.RouteSearch;
import com.amap.api.services.route.RouteSearch.BusRouteQuery;
import com.amap.api.services.route.RouteSearch.DriveRouteQuery;
import com.amap.api.services.route.RouteSearch.FromAndTo;
import com.amap.api.services.route.RouteSearch.OnRouteSearchListener;
import com.amap.api.services.route.RouteSearch.WalkRouteQuery;
import com.amap.api.services.route.WalkPath;
import com.amap.api.services.route.WalkRouteResult;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.js.map.amap.JsMapManager;
import io.dcloud.js.map.amap.JsMapRoute;
import io.dcloud.js.map.amap.MapJsUtil;


/**
 * <p>Description:管理地图上的检索功能</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2012-11-6 上午10:07:29 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-6 上午10:07:29</pre>
 */
public class MapSearch {
	
	/**
	 * 搜索对象
	 */
	private RouteSearch mSearchHandler;
	
	/**
	 * 页面对象
	 */
	private IWebview mIWebview;
	/**
	 * 对应js对象的id
	 */
	public String mCallbackId;
	/**
	 * 检索返回结果每页的容量
	 */
	private int mPageCapacity;
	/**
	 * 检索结果的页面
	 */
	private int mIndex = 0;
	
	private int busMode = RouteSearch.BusDefault;// 公交默认模式
	private int drivingMode = RouteSearch.DrivingDefault;// 驾车默认模式
	private int walkMode = RouteSearch.WalkDefault;// 步行默认模式


	private final static int BUS_MODE_SEARCH = 0;
	private final static int DRIVING_MODE_SEARCH = 1;
	private final static int WALK_MODE_SEARCH = 2;
	private final static int SEARCH_ACTION = 10000;

	private ArrayList<AMapSearchResultData> mCallResultDatas;

	private Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);
			switch (msg.what) {
				case SEARCH_ACTION:
					AMapSearchResultData data = (AMapSearchResultData) msg.obj;
					if (data.pStart instanceof LatLonPoint && data.pEnd instanceof LatLonPoint) {
						switch (data.type) {
							case BUS_MODE_SEARCH:
								transitSearch(data.pStart, data.pEnd, data.endCity);
								break;
							case DRIVING_MODE_SEARCH:
								drivingSearch(data.pStart, data.startCity, data.pEnd, data.endCity);
								break;
							case WALK_MODE_SEARCH:
								walkingSearch(data.pStart, data.startCity, data.pEnd, data.endCity);
								break;
						}
						data = null;
					}
					break;
			}
		}
	};

	/**
	 * Description: 构造函数 
	 * @param pIWebview
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-8 下午3:50:14</pre>
	 */
	public MapSearch(IWebview pIWebview) {
		mIWebview = pIWebview;
		mSearchHandler = new RouteSearch(pIWebview.getActivity());
		mSearchHandler.setRouteSearchListener(mRouteSearchListener);
	}

	/**
	 * @return the pageCapacity
	 */
	public int getPageCapacity() {
		return mPageCapacity;
	}
	/**
	 * @param pageCapacity the pageCapacity to set
	 */
	public void setPageCapacity(String pageCapacity) {
		mPageCapacity = PdrUtil.parseInt(pageCapacity,10);
	}

	/**
	 * @param pTransitPolicy the mBusPolicy to set
	 */
	public void setTransitPolicy(int pTransitPolicy) {
		this.busMode = pTransitPolicy;
	}

	/**
	 * @param pDrivingPolicy the mDrivingPolicy to set
	 */
	public void setDrivingPolicy(int pDrivingPolicy) {
		this.drivingMode = pDrivingPolicy;
	}
	
	public void setWalkPolicy(int walkPolicy) {
		this.walkMode = walkPolicy;
	}

	/**
	 * 
	 * Description:根据城市搜索
	 * @param pCity
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-9 上午11:57:28</pre>
	 */
	public void poiSearchInCity(String pCity, String pKeyCode, String index) {
		PoiSearch.Query query = new PoiSearch.Query(pKeyCode, "", pCity);
		mIndex = PdrUtil.parseInt(index, 0);
		query.setPageNum(mIndex);
		query.setPageSize(mPageCapacity);
		PoiSearch poiSearch = new PoiSearch(mIWebview.getContext(), query);
		poiSearch.setOnPoiSearchListener(mPoiSearchListener);
		poiSearch.searchPOIAsyn();// 异步poi查询
	}
	/**
	 * 
	 * Description:根据中心点搜索周边
	 * @param pCenter
	 * @param pRadius
	 * @param pKeyCode
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-9 上午11:57:05</pre>
	 */
	public void poiSearchNearBy(String pKeyCode, MapPoint pCenter, String pRadius, String index) {
		int _radius = PdrUtil.parseInt(pRadius, 0);
		RegeocodeQuery regeocodeQuery = new RegeocodeQuery(pCenter.getLatLngPoint(), _radius, GeocodeSearch.AMAP);
		String city = getCityKey(regeocodeQuery);
		PoiSearch.Query query = new PoiSearch.Query(pKeyCode, "", city);
		query.setPageSize(mPageCapacity);
		mIndex = PdrUtil.parseInt(index, 0);
		query.setPageNum(mIndex);
		PoiSearch poiSearch = new PoiSearch(mIWebview.getContext(), query);
		poiSearch.setBound(new SearchBound(pCenter.getLatLngPoint(), _radius));
		poiSearch.setOnPoiSearchListener(mPoiSearchListener);
		poiSearch.searchPOIAsyn();// 异步poi查询
	}
	/**
	 * 
	 * Description:根据范围和检索词发起范围检索
	 * @param pKeyCode
	 * @param ptLB
	 * @param ptRT
	 * @param index
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-16 下午4:04:45</pre>
	 */
	public void poiSearchInbounds(String pKeyCode, MapPoint ptLB, MapPoint ptRT, String index) {
		RegeocodeQuery regeocodeQuery = new RegeocodeQuery(ptLB.getLatLngPoint(), 200, GeocodeSearch.AMAP);
		String city = getCityKey(regeocodeQuery);
		PoiSearch.Query query = new PoiSearch.Query(pKeyCode, "", city);
		query.setPageSize(mPageCapacity);
		mIndex = PdrUtil.parseInt(index, 0);
		query.setPageNum(mIndex);
		PoiSearch poiSearch = new PoiSearch(mIWebview.getContext(), query);
		poiSearch.setBound(new SearchBound(ptLB.getLatLngPoint(), ptRT.getLatLngPoint()));
		poiSearch.setOnPoiSearchListener(mPoiSearchListener);
		poiSearch.searchPOIAsyn();// 异步poi查询
	}

	/**
	 * 
	 * Description:用于公交路线搜索策略。
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-16 下午4:07:19</pre>
	 */
	public boolean setTransitPolicy(String pPolicy){
		boolean _ret = true;
		// 选择车票花销最少优先
		if("TRANSIT_FEE_FIRST".equals(pPolicy)){
			busMode = RouteSearch.BusSaveMoney; // 公交最经济模式
		} 
		// 时间优先
		else if("TRANSIT_TIME_FIRST".equals(pPolicy)){
			busMode = RouteSearch.BusDefault; // 最快捷模式
		}
		// 最少换乘优先
		else if("TRANSIT_TRANSFER_FIRST".equals(pPolicy)){
			busMode = RouteSearch.BusLeaseChange; // 最少换乘
		}
		// 最少步行距离优先
		else if("TRANSIT_WALK_FIRST".equals(pPolicy)){
			busMode = RouteSearch.BusLeaseWalk; //最少步行
		}else{
			_ret = false;
		}
		return _ret;
	}
	/**
	 * 
	 * Description:设置驾车线路
	 * @param pPolicy
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-22 上午11:40:14</pre>
	 */
	public boolean setDrivingPolicy(String pPolicy){
		boolean _ret = true;
		// 最短距离优先
		if("DRIVING_DIS_FIRST".equals(pPolicy)){
			drivingMode = RouteSearch.DrivingShortDistance; // 距离优先
		}
		// 最少费用优先
		else if("DRIVING_FEE_FIRST".equals(pPolicy)){
			drivingMode = RouteSearch.DrivingSaveMoney; // 费用优先(不走收费路的最快道路)
		}
		// 无高速公路线路
		else if("DRIVING_NO_EXPRESSWAY".equals(pPolicy)){
			drivingMode = RouteSearch.DrivingNoHighWay; // 不走高速
		}else{
			_ret = false;
		}
		return _ret;
	}
	
	
	/**
	 * 
	 * 公交路径规划
	 * @param pStart
	 * @param pEnd
	 * @param city
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-22 下午12:05:01</pre>
	 */
	public void transitSearch(Object pStart,Object pEnd,String city) {
		AMapSearchResultData data = new AMapSearchResultData(BUS_MODE_SEARCH, pStart, null, pEnd, city);
		LatLonPoint lpS = getLatLonPoint(pStart, city, data, 1);
		LatLonPoint lpE = getLatLonPoint(pEnd, city, data, 2);
		if (lpS == null || lpE == null) {
			return;
		}
		RouteSearch.FromAndTo fromAndTo = new FromAndTo(lpS, lpE);
		// 第一个参数表示路径规划的起点和终点，第二个参数表示公交查询模式，第三个参数表示公交查询城市区号，第四个参数表示是否计算夜班车，0表示不计算
		BusRouteQuery busRouteQuery = new BusRouteQuery(fromAndTo, busMode, city, 0);
		mSearchHandler.calculateBusRouteAsyn(busRouteQuery); // 结果会回调到 onBusRouteSearched
	}

	/**
	 * 计算驾车路径
	 * @param pStart point
	 * @param pEnd point
	 */
	public void drivingSearch(Object pStart, String startCity, Object pEnd, String endCity) {
		AMapSearchResultData data = new AMapSearchResultData(DRIVING_MODE_SEARCH, pStart, startCity, pEnd, endCity);
		LatLonPoint lpS = getLatLonPoint(pStart, startCity, data, 1);
		LatLonPoint lpE = getLatLonPoint(pEnd, endCity, data, 2);
		if (lpS == null || lpE == null) {
			//route_error_callback_js(DOMException.CODE_PARAMETER_ERRORP, DOMException.MSG_PARAMETER_ERROR);
			return;
		}
		RouteSearch.FromAndTo fromAndTo = new FromAndTo(lpS, lpE);
		// 第一个参数表示路径规划的起点和终点，第二个参数表示驾车模式 可选，第三个参数表示途经点 可选，第四个参数表示避让区域 可选，第五个参数表示避让道路 可选
		DriveRouteQuery driveRouteQuery = new DriveRouteQuery(fromAndTo, drivingMode, null, null, "");
		mSearchHandler.calculateDriveRouteAsyn(driveRouteQuery); // 结果会回调到 onDriveRouteSearched
	}

/*	private void initSearchData(){
		mSearchHandler.goToPoiPage(mIndex);
		mSearchHandler.setPoiPageCapacity(mPageCapacity);
		mSearchHandler.setDrivingPolicy(this.mDrivingPolicy);
	}*/
	/**
	 * 步行默认模式
	 * @param pStart point
	 * @param pEnd point
	 */
	public void walkingSearch(Object pStart, String startCity, Object pEnd, String endCity) {
		AMapSearchResultData data = new AMapSearchResultData(WALK_MODE_SEARCH, pStart, startCity, pEnd, endCity);
		LatLonPoint lpS = getLatLonPoint(pStart, startCity, data, 1);
		LatLonPoint lpE = getLatLonPoint(pEnd, endCity, data, 2);
		if (lpS == null || lpE == null) {
			return;
		}
		RouteSearch.FromAndTo fromAndTo = new FromAndTo(lpS, lpE);
		// 此类定义了步行路径的起终点和计算路径的模式
		WalkRouteQuery walkRouteQuery = new WalkRouteQuery(fromAndTo, walkMode);
		mSearchHandler.calculateWalkRouteAsyn(walkRouteQuery); // 结果会回调到 onWalkRouteSearched
	}

	/**
	 * 根据当前对象类型获取LatLonPoint
	 * @param lonPoint
	 * @param city
	 * @param data
	 * @param pointType 坐标点类型 1 表示start Point 2 end Point
	 * @return
	 */
	private LatLonPoint getLatLonPoint(Object lonPoint, String city, AMapSearchResultData data, int pointType) {
		if(lonPoint instanceof LatLonPoint) {
			return (LatLonPoint) lonPoint;
		} else if (lonPoint instanceof MapPoint) {
			if(pointType == 1){
				data.pStart = ((MapPoint)lonPoint).getLatLngPoint();
			} else {
				data.pEnd = ((MapPoint)lonPoint).getLatLngPoint();
			}
			return ((MapPoint)lonPoint).getLatLngPoint();
		} else {
			GeocodeQuery queryS = new GeocodeQuery((String) lonPoint, city);
			getGeocodeLatLon(queryS, data, pointType);
			return null;
		}
	}

	/**
	 * 通过关键字获取指定坐标 默认选择第一个参数
	 * @param query
	 * @return
	 */
	private void getGeocodeLatLon(GeocodeQuery query, final AMapSearchResultData data, final int pointType) {
		GeocodeSearch search = new GeocodeSearch(mIWebview.getContext());
		search.getFromLocationNameAsyn(query);
		search.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
			@Override
			public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) { }
			@Override
			public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {
				if(i == 1000) {
					List<GeocodeAddress> List = geocodeResult.getGeocodeAddressList();
					if (List != null) {
						GeocodeAddress address = List.get(0);
						if(pointType == 1) {
							data.pStart = address.getLatLonPoint();
						} else {
							data.pEnd = address.getLatLonPoint();
						}
						Message message = new Message();
						message.what = SEARCH_ACTION;
						message.obj = data;
						mHandler.sendMessage(message);
					}
				} else {
					route_error_callback_js(DOMException.CODE_PARAMETER_ERRORP, DOMException.MSG_PARAMETER_ERROR+"(原错误号"+i+", 相关SDK官网查询具体错误原因)");
				}
				Logger.e("shutao", "onGeocodeSearched"+geocodeResult.getGeocodeAddressList().size() + "   code="+i);
			}
		});
	}
	
	/**
	 * 通过指定经纬度获取城市名称
	 * @param regeocodeQuery
	 * @return
	 */
	private String getCityKey(RegeocodeQuery regeocodeQuery) {
		GeocodeSearch search = new GeocodeSearch(mIWebview.getContext());
		try {
			RegeocodeAddress address = search.getFromLocation(regeocodeQuery);
            if (!PdrUtil.isEmpty(address)) {
                return address.getCity();
            }
        } catch (AMapException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
	
	private static final int POISEARCH_TYPE = 0;
	private static final int ROUTESEARCH_TYPE = 1;
	/**
	 * 
	 * @param searchType [onPoiSearchComplete|onRouteSearchComplete]
	 * @param pScript
	 */
	private void onSearchComplete(int searchType,String pScript){
		if(searchType == POISEARCH_TYPE){
			MapJsUtil.execCallback(mIWebview, mCallbackId,pScript);
		}else if(searchType == ROUTESEARCH_TYPE){
			MapJsUtil.execCallback(mIWebview, mCallbackId,pScript);
		}
	}
	
	private StringBuffer newJS_SearchPoiResult_Obj( String pName, StringBuffer pRet){
		MapJsUtil.newJsVar(pRet, pName, "plus.maps.__SearchPoiResult__", null);
		return pRet;
	}
	
	private String newJS_Position_Obj(PoiItem poi){
		StringBuffer sb = new StringBuffer();
		String ptName = "p";
		sb.append(newJS_Point_Obj(getMapPoint(poi.getLatLonPoint()),ptName));
		String posName = "pos";
		MapJsUtil.newJsVar(sb, posName, "plus.maps.Position", ptName);
		MapJsUtil.assignJsVar(sb, posName, "address", PdrUtil.makeQueryStringAllRegExp(poi.getSnippet()));
		MapJsUtil.assignJsVar(sb, posName, "city", PdrUtil.makeQueryStringAllRegExp(poi.getCityName()));
		MapJsUtil.assignJsVar(sb, posName, "name", PdrUtil.makeQueryStringAllRegExp(poi.getTitle()));
		MapJsUtil.assignJsVar(sb, posName, "phone", poi.getTel());
		MapJsUtil.assignJsVar(sb, posName, "postcode", poi.getPostcode());
		return MapJsUtil.wrapJsEvalString(sb.toString(), posName);
	}
	
	private String newJS_Position_Obj(MapPoint pt,String posName){
		StringBuffer sb = new StringBuffer();
		String ptName = "p";
		sb.append(newJS_Point_Obj(pt,ptName));
		MapJsUtil.newJsVar(sb, posName, "plus.maps.Position", ptName);
		return sb.toString();
	}
	
	private JSONArray toPositionArray(ArrayList<PoiItem> list){
		JSONArray arr = new JSONArray();
		for(PoiItem poi : list){
			arr.put(newJS_Position_Obj(poi));
		}
		return arr;
	}

	private StringBuffer newJS_SearchRouteResult_Obj( String pName, StringBuffer pRet){
		MapJsUtil.newJsVar(pRet, pName, "plus.maps.__SearchRouteResult__", null);
		return pRet;
	}
	
	/**
	 * 
	 * Description: 创建CMap.Route对象
	 * @param pStart
	 * @param pEnd
	 * @param pIndex
	 * @param pRet
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-12-3 下午4:52:36</pre>
	 */
	/*private String newJS_Route_Obj(GeoPoint pStart, GeoPoint pEnd, int pIndex, StringBuffer pRet){
		String _jsStartPoint = newJS_Point_Obj(pStart,"start_"+pIndex);
		String _jsEndPoint = newJS_Point_Obj(pStart,"end_"+pIndex);
		//构造Route对象
		String _route = "_route" + pIndex;
		MapJsUtil.newJsVar(pRet, _route, "plus.maps.Route", _jsStartPoint + "," +_jsEndPoint);
		return _route;
	}*/
	
	/**
	 * 
	 * Description:创建CMap.Point对象
	 * @param pPoint
	 * @param pName
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-12-4 上午10:13:02</pre>
	 */
	private String newJS_Point_Obj(MapPoint pPoint, String pName){
		StringBuffer sb = new StringBuffer();
		float _lat = (float) (pPoint.getLatitude());
		float _lng = (float) (pPoint.getLongitude());
		// 注意此处 HBuilder的经纬度是 经度在前  纬度在后 !!!!!!!!!!!!
		MapJsUtil.newJsVar(sb, pName, "plus.maps.Point", _lng + "," + _lat);
		return sb.toString();
	}
	
	/**
	 * 对步行搜索方案数据进行json封装
	 * @param list 步行方案list
	 * @param startp 开始坐标
	 * @param endp 结束坐标
	 * @return
	 */
	private JSONArray toWalkRouteArray(List<WalkPath> list, MapPoint startp, MapPoint endp) {
		JSONArray arr = new JSONArray();
		for (WalkPath walkPath : list) {
			// 步行路径规划的一个方案
			arr.put(newJSWalkRouteObject(walkPath, startp, endp));
		}
		return arr;
	}
	
	private String newJSWalkRouteObject(WalkPath walkPath, MapPoint startp, MapPoint endp) {
		// TODO Auto-generated method stub
		// 5+ js map 规范
		// startPoint：Point，只读属性，路线起点地理坐标点
		// endPoint：Point，只读属性，路线终点地理坐标点
		// pointCount：Point，只读属性，路线坐标点段数
		// pointList：Array，只读属性，路线的地理坐标点数组，数组中保存Point对象。
		// distance：Number，只读属性，路线从起始点到终点的距离，单位为米。
		// routeTip：DOMString，只读属性，线路提示信息，没有提示信息则返回空字符串。
		StringBuffer sb = new StringBuffer();
		String sptName = "sp";
		sb.append(newJS_Point_Obj(startp, sptName));
		String eptName = "ep";
		sb.append(newJS_Point_Obj(endp, eptName));
		String routeName = "route";
		MapJsUtil.newJsVar(sb, routeName, "plus.maps.Route", sptName + "," + eptName + ",false");// 不调用至native层
		MapJsUtil.assignJsVar(sb, routeName, "pointCount", walkPath.getSteps().get(0).getPolyline().size());
		MapJsUtil.assignJsVar(sb, routeName, "distance", walkPath.getSteps().get(0).getDistance());
		MapJsUtil.assignJsVar(sb, routeName, "routeTip", walkPath.getSteps().get(0).getInstruction());
		String uuid = "Route_" + walkPath.hashCode();
		// 构造native层对象，与js层一一对应
		MapJsUtil.assignJsVar(sb, routeName, "_UUID_", uuid);// 修改uuid使native层保存对象于js层一一对应
		JsMapRoute routeNativeObj = new JsMapRoute(mIWebview);
		routeNativeObj.setRoute(walkPath);
		routeNativeObj.setPoint(startp, endp); // 添加起始坐标 必须填写
		JsMapManager.getJsMapManager().putJsObject(uuid, routeNativeObj);

		/*JSONArray ptsArr = new JSONArray();
		for (ArrayList<GeoPoint> geoArr : route.getArrayPoints()) {
			for (GeoPoint p : geoArr) {
				ptsArr.put(newJS_Point_Obj(p, null));
			}
		}
		MapJsUtil.assignJsVar(sb, routeName, "pointList", ptsArr);*/
		return MapJsUtil.wrapJsEvalString(sb.toString(), routeName);
	}

	/**
	 * 对驾车搜索路线进行json封装
	 * @param list 驾车方案list
	 * @param startp 开始坐标
	 * @param endp 结束坐标
	 * @return
	 */
	private JSONArray toDriveRouteArray(List<DrivePath> list, MapPoint startp, MapPoint endp) {
		// TODO Auto-generated method stub
		JSONArray arr = new JSONArray();
		for (DrivePath dp : list) {
			arr.put(newJSDriveRouteObject(dp, startp, endp));
		}
		return arr;
	}
	
	private String newJSDriveRouteObject(DrivePath dp, MapPoint startp, MapPoint endp) {
		// 5+ js map 规范
		// startPoint：Point，只读属性，路线起点地理坐标点
		// endPoint：Point，只读属性，路线终点地理坐标点
		// pointCount：Point，只读属性，路线坐标点段数
		// pointList：Array，只读属性，路线的地理坐标点数组，数组中保存Point对象。
		// distance：Number，只读属性，路线从起始点到终点的距离，单位为米。
		// routeTip：DOMString，只读属性，线路提示信息，没有提示信息则返回空字符串。
		StringBuffer sb = new StringBuffer();
		String sptName = "sp";
		sb.append(newJS_Point_Obj(startp, sptName));
		String eptName = "ep";
		sb.append(newJS_Point_Obj(endp, eptName));
		String routeName = "route";
		MapJsUtil.newJsVar(sb, routeName, "plus.maps.Route", sptName + "," + eptName + ",false");// 不调用至native层
		MapJsUtil.assignJsVar(sb, routeName, "pointCount", dp.getSteps().get(0).getPolyline().size());
		MapJsUtil.assignJsVar(sb, routeName, "distance", dp.getSteps().get(0).getDistance());
		MapJsUtil.assignJsVar(sb, routeName, "routeTip", dp.getSteps().get(0).getInstruction());
		String uuid = "Route_" + dp.hashCode();
		// 构造native层对象，与js层一一对应
		MapJsUtil.assignJsVar(sb, routeName, "_UUID_", uuid);// 修改uuid使native层保存对象于js层一一对应
		JsMapRoute routeNativeObj = new JsMapRoute(mIWebview);
		routeNativeObj.setRoute(dp);
		routeNativeObj.setPoint(startp, endp); // 添加起始坐标 必须填写
		JsMapManager.getJsMapManager().putJsObject(uuid, routeNativeObj);

		JSONArray ptsArr = new JSONArray();
		for (DriveStep ds : dp.getSteps()) {
			for (LatLonPoint p : ds.getPolyline()) {
				ptsArr.put(newJS_Point_Obj(getMapPoint(p), null));
			}
		}
		MapJsUtil.assignJsVar(sb, routeName, "pointList", ptsArr);
		return MapJsUtil.wrapJsEvalString(sb.toString(), routeName);
	}
	
	
	/**
	 * 对公交路线进行json封装
	 * @param list
	 * @return
	 */
	private JSONArray toBusRouteArray(List<BusPath> list, MapPoint startp, MapPoint endp) {
		JSONArray arr = new JSONArray();
		for (BusPath bh : list) {
			arr.put(newJSBusRouteObject(bh, startp, endp));
		}
		return arr;
	}
	
	private String newJSBusRouteObject(BusPath path, MapPoint startp, MapPoint endp) {
		// 5+ js map 规范
		// startPoint：Point，只读属性，路线起点地理坐标点
		// endPoint：Point，只读属性，路线终点地理坐标点
		// pointCount：Point，只读属性，路线坐标点段数
		// pointList：Array，只读属性，路线的地理坐标点数组，数组中保存Point对象。
		// distance：Number，只读属性，路线从起始点到终点的距离，单位为米。
		// routeTip：DOMString，只读属性，线路提示信息，没有提示信息则返回空字符串。
		StringBuffer sb = new StringBuffer();
		String sptName = "sp";
		sb.append(newJS_Point_Obj(startp, sptName));
		String eptName = "ep";	
		sb.append(newJS_Point_Obj(endp, eptName));
		String routeName = "route";
		MapJsUtil.newJsVar(sb, routeName, "plus.maps.Route", sptName + "," + eptName + ",false");//不调用至native层
		MapJsUtil.assignJsVar(sb, routeName, "distance", path.getDistance());
		MapJsUtil.assignJsVar(sb, routeName, "routeTip", "");
		//构造native层对象，与js层一一对应
		String uuid = "Route_" + path.hashCode();
		MapJsUtil.assignJsVar(sb, routeName, "_UUID_", uuid);//修改uuid使native层保存对象于js层一一对应
		JsMapRoute routeNativeObj = new JsMapRoute(mIWebview);
		routeNativeObj.setRoute(path);
		routeNativeObj.setPoint(startp, endp); // 添加起始坐标 必须填写
		JsMapManager.getJsMapManager().putJsObject(uuid, routeNativeObj);
		//JSONArray ptsArr = new JSONArray();
		/*for(BusStep bs : path.getSteps()) {
			// 公交换乘路径规划的一个换乘段的公交信息
			RouteBusLineItem busLineItem = bs.getBusLine();
			// 公交换乘路径规划的一个换乘段的步行信息
			RouteBusWalkItem busWalkItem = bs.getWalk();
			
			
		}
		
		int lineLen = path.getSteps().size();//公交
		
		for(int l = 0; l < lineLen; l++){
			for(GeoPoint p : busPath.getLine(l).getPoints()){
				ptsArr.put(newJS_Point_Obj(p,null));
			}
		}
		
		int routeLen = busPath.getNumRoute();//公交中需要的步行
		for(int r = 0; r < routeLen; r++){
			for(GeoPoint p : busPath.getRoute(r).getArrayPoints().get(0)){
				ptsArr.put(newJS_Point_Obj(p,null));
			}
		}
		
		MapJsUtil.assignJsVar(sb, routeName, "pointCount", ptsArr.length());
		MapJsUtil.assignJsVar(sb, routeName, "pointList", ptsArr);*/
		return MapJsUtil.wrapJsEvalString(sb.toString(),routeName);
	}

	TextureMapView mMapView = null;
	public void setMapView(TextureMapView pMapView) {
		mMapView = pMapView;
	}

	OnRouteSearchListener mRouteSearchListener = new OnRouteSearchListener() {
		
		@Override
		public void onWalkRouteSearched(WalkRouteResult walkRouteResult, int iError) {
			// TODO Auto-generated method stub
            if(iError == 1000) { //兼容新版错误号
                iError = 0;
            }
			// 步行结果回调
			if (iError == 0) {
				if (walkRouteResult != null && walkRouteResult.getPaths() != null
						&& walkRouteResult.getPaths().size() > 0) {
					List<WalkPath> list = walkRouteResult.getPaths();
					MapPoint startp = getMapPoint(walkRouteResult.getStartPos());
					MapPoint endp = getMapPoint(walkRouteResult.getTargetPos());
					route_callback_js(startp, endp, list.size(), iError, toWalkRouteArray(list, startp, endp));
				} else {
					//对不起，没有搜索到相关数据！
					route_error_callback_js(DOMException.CODE_PARAMETER_ERRORP, "对不起，没有搜索到相关数据！");
				}
			} else if (iError == 27 || iError == 1804) {
				route_error_callback_js(iError, DOMException.MSG_NETWORK_ERROR);
			} else if (iError == 32 || iError == 1001) {
				route_error_callback_js(iError, "签名错误");
			} else {
				route_error_callback_js(iError, DOMException.MSG_UNKNOWN_ERROR);
			}
		}

		@Override
		public void onRideRouteSearched(RideRouteResult rideRouteResult, int i) {

		}

		@Override
		public void onDriveRouteSearched(DriveRouteResult driveRouteResult, int iError) {
			// TODO Auto-generated method stub
            if(iError == 1000) { //兼容新版错误号
                iError = 0;
            }
			// 驾车结果回调
			if (iError == 0) {
				if (driveRouteResult != null && driveRouteResult.getPaths() != null
						&& driveRouteResult.getPaths().size() > 0) {
					List<DrivePath> list = driveRouteResult.getPaths();
					MapPoint startp = getMapPoint(driveRouteResult.getStartPos());
					MapPoint endp = getMapPoint(driveRouteResult.getTargetPos());
					route_callback_js(startp, endp, list.size(), iError, toDriveRouteArray(list, startp, endp));
				} else {
					//对不起，没有搜索到相关数据！
					route_error_callback_js(DOMException.CODE_PARAMETER_ERRORP, "对不起，没有搜索到相关数据！");
				}
			} else if (iError == 27 || iError == 1804) {
				route_error_callback_js(iError, DOMException.MSG_NETWORK_ERROR);
			} else if (iError == 32 || iError == 1001) {
				route_error_callback_js(iError, "签名错误");
			} else {
				route_error_callback_js(iError, DOMException.MSG_UNKNOWN_ERROR);
			}
		}

		@Override
		public void onBusRouteSearched(BusRouteResult busRouteResult, int iError) {
			// TODO Auto-generated method stub
            if(iError == 1000) { //兼容新版错误号
                iError = 0;
            }
			// 公交路线回调
			if (iError == 0) {
				if (busRouteResult != null && busRouteResult.getPaths() != null
						&& busRouteResult.getPaths().size() > 0) {
					List<BusPath> list = busRouteResult.getPaths();
					MapPoint startp = getMapPoint(busRouteResult.getStartPos());
					MapPoint endp = getMapPoint(busRouteResult.getTargetPos());
					route_callback_js(startp, endp, list.size(), iError, toBusRouteArray(list, startp, endp));
				} else {
					//对不起，没有搜索到相关数据！
					route_error_callback_js(DOMException.CODE_PARAMETER_ERRORP, "对不起，没有搜索到相关数据！");
				}
			} else if (iError == 27 || iError == 1804) {
				route_error_callback_js(iError, DOMException.MSG_NETWORK_ERROR);
			} else if (iError == 32 || iError == 1001) {
				route_error_callback_js(iError, "签名错误");
			} else {
				route_error_callback_js(iError, DOMException.MSG_UNKNOWN_ERROR);
			}
		}
		
		/**
		 * 封装JS代码 回调
		 * @param sPoint 开始位置
		 * @param ePoint 结束位置
		 * @param routeNumber 搜索结果个数
		 * @param state 搜索结果状态 通常为 0
		 * @param routeList route数组JS代码
		 */
		private void route_callback_js(MapPoint sPoint, MapPoint ePoint, int routeNumber, int state, JSONArray routeList){
			String srrJSName = "srr";//SearchRouteResult
			StringBuffer js = new StringBuffer();
			newJS_SearchRouteResult_Obj(srrJSName, js);
			MapJsUtil.assignJsVar(js, srrJSName, "__state__", state);
			MapJsUtil.assignJsVar(js, srrJSName, "__type__", ROUTESEARCH_TYPE);
			MapJsUtil.assignJsVar(js, srrJSName, "startPosition", MapJsUtil.wrapJsEvalString(newJS_Position_Obj(sPoint,"startPosition"),"startPosition"),false);
			MapJsUtil.assignJsVar(js, srrJSName, "endPosition", MapJsUtil.wrapJsEvalString(newJS_Position_Obj(ePoint,"endPosition"),"endPosition"),false);
			MapJsUtil.assignJsVar(js, srrJSName, "routeNumber", routeNumber);
			MapJsUtil.assignJsVar(js, srrJSName, "routeList", routeList);
			onSearchComplete(ROUTESEARCH_TYPE, MapJsUtil.wrapJsEvalString(js.toString(), srrJSName));
		}
	};
	
	/**
	 * 线路搜索错误回调JS
	 * @param state code 
	 * @param msg 错误描述 暂时没有用到
	 */
	private void route_error_callback_js(int state, String msg) {
		String srrJSName = "srr";//SearchRouteResult
		StringBuffer js = new StringBuffer();
		newJS_SearchRouteResult_Obj(srrJSName, js);
		MapJsUtil.assignJsVar(js, srrJSName, "__state__", state);
		MapJsUtil.assignJsVar(js, srrJSName, "__type__", ROUTESEARCH_TYPE);
		onSearchComplete(ROUTESEARCH_TYPE, MapJsUtil.wrapJsEvalString(js.toString(), srrJSName));
	}
	
	OnPoiSearchListener mPoiSearchListener = new OnPoiSearchListener() {
		
		@Override
		public void onPoiSearched(PoiResult poiResult, int code) {
			// TODO Auto-generated method stub
			// poi搜索结果回调
			// totalNumber: POI检索总结果数
			// currentNumber: 当前页的POI检索结果数
			// pageNumber: 本次POI检索的总页数
			// pageIndex: 获取当前页的索引
			// poiList: 本次POI检索结果数组

			if(code == 1000) { //兼容新版错误号
				code = 0;
			}
			String spr = "spr";
			StringBuffer js = new StringBuffer();
			newJS_SearchPoiResult_Obj(spr, js);
			MapJsUtil.assignJsVar(js, spr, "__state__", code);
			MapJsUtil.assignJsVar(js, spr, "__type__", POISEARCH_TYPE);
			if (code == 0) {
				//int totalNumber = 0;
				int currentNumber = 0;
				int pageNumber = 0;
				int pageIndex = 0;
				ArrayList<PoiItem> pArray = null;
				if(poiResult != null){
					//totalNumber = ;
					currentNumber = poiResult.getPois().size();
					pageNumber = poiResult.getPageCount();
					pageIndex = poiResult.getQuery().getPageNum();
					pArray = poiResult.getPois();
				}else{
					pArray = new ArrayList<PoiItem>();
				}
				//MapJsUtil.assignJsVar(js, spr, "totalNumber", totalNumber);
				MapJsUtil.assignJsVar(js, spr, "currentNumber",currentNumber );
				MapJsUtil.assignJsVar(js, spr, "pageNumber", pageNumber);
				MapJsUtil.assignJsVar(js, spr, "pageIndex", pageIndex);
				JSONArray poiList = toPositionArray(pArray);
				MapJsUtil.assignJsVar(js, spr, "poiList", poiList);
				onSearchComplete(POISEARCH_TYPE, MapJsUtil.wrapJsEvalString(js.toString(), spr));
			} else if (code == 27 || code == 1804) {
				MapJsUtil.assignJsVar(js, spr, "errorMsg", DOMException.MSG_NETWORK_ERROR);
				onSearchComplete(POISEARCH_TYPE, MapJsUtil.wrapJsEvalString(js.toString(), spr));
			} else if (code == 32 || code == 1001) {
				MapJsUtil.assignJsVar(js, spr, "errorMsg", "签名错误");
				onSearchComplete(POISEARCH_TYPE, MapJsUtil.wrapJsEvalString(js.toString(), spr));
			} else {
				MapJsUtil.assignJsVar(js, spr, "errorMsg", DOMException.MSG_UNKNOWN_ERROR);
				onSearchComplete(POISEARCH_TYPE, MapJsUtil.wrapJsEvalString(js.toString(), spr));
			}
		}

		@Override
		public void onPoiItemSearched(PoiItem poiItem, int i) {

		}

	};
	
	/**
	 * 通过LatLonPoint 转换成 MapPoint
	 * @param point
	 * @return
	 */
	private MapPoint getMapPoint(LatLonPoint point) {
		MapPoint mPoint = new MapPoint(point.getLatitude()+"", point.getLongitude()+"");
		return mPoint;
	}
}
