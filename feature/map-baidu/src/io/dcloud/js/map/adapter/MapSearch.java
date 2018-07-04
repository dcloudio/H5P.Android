package io.dcloud.js.map.adapter;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.constant.DOMException;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.js.map.JsMapManager;
import io.dcloud.js.map.JsMapRoute;
import io.dcloud.js.map.MapJsUtil;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import android.view.View;

import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.model.LatLngBounds;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.RouteNode;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiBoundSearchOption;
import com.baidu.mapapi.search.poi.PoiCitySearchOption;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiIndoorResult;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.baidu.mapapi.search.route.BikingRouteResult;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption;
import com.baidu.mapapi.search.route.DrivingRoutePlanOption.DrivingPolicy;
import com.baidu.mapapi.search.route.DrivingRouteLine;
import com.baidu.mapapi.search.route.DrivingRouteResult;
import com.baidu.mapapi.search.route.IndoorRouteResult;
import com.baidu.mapapi.search.route.MassTransitRouteResult;
import com.baidu.mapapi.search.route.OnGetRoutePlanResultListener;
import com.baidu.mapapi.search.route.PlanNode;
import com.baidu.mapapi.search.route.RoutePlanSearch;
import com.baidu.mapapi.search.route.TransitRouteLine;
import com.baidu.mapapi.search.route.TransitRouteLine.TransitStep;
import com.baidu.mapapi.search.route.TransitRoutePlanOption;
import com.baidu.mapapi.search.route.TransitRoutePlanOption.TransitPolicy;
import com.baidu.mapapi.search.route.TransitRouteResult;
import com.baidu.mapapi.search.route.WalkingRouteLine;
import com.baidu.mapapi.search.route.WalkingRouteLine.WalkingStep;
import com.baidu.mapapi.search.route.WalkingRoutePlanOption;
import com.baidu.mapapi.search.route.WalkingRouteResult;


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
public class MapSearch implements OnGetRoutePlanResultListener, OnGetPoiSearchResultListener{
	
	/**
	 * 搜索对象
	 */
	private RoutePlanSearch mSearchHandler;
	
	private PoiSearch mPoiSearch = null;
	
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
	private int mPageCapacity = 10;
	/**
	 * 检索结果的页面
	 */
	private int mIndex = 0;
	/**
	 * 公交路线搜索策略
	 */
	private TransitPolicy mTransitPolicy = TransitPolicy.EBUS_TIME_FIRST;
	/**
	 * 驾车路线搜索策略
	 */
	private DrivingPolicy mDrivingPolicy = DrivingPolicy.ECAR_TIME_FIRST;
	
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
		mSearchHandler = RoutePlanSearch.newInstance();
		mSearchHandler.setOnGetRoutePlanResultListener(this);
		// 初始化搜索模块，注册搜索事件监听
		mPoiSearch = PoiSearch.newInstance();
		mPoiSearch.setOnGetPoiSearchResultListener(this);
	}
	
	public void destroy() {
		mSearchHandler.destroy();
		mPoiSearch.destroy();
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
	 * 
	 * Description:根据城市搜索
	 * @param pCity
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-9 上午11:57:28</pre>
	 */
	public boolean poiSearchInCity(String pCity,String pKeyCode, String index){//onGetPoiResult
		mIndex = PdrUtil.parseInt(index, 0);
		return mPoiSearch.searchInCity(new PoiCitySearchOption().city(pCity).keyword(pKeyCode).pageNum(mIndex).pageCapacity(mPageCapacity));
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
	public boolean poiSearchNearBy(String pKeyCode,MapPoint pCenter,String pRadius,String index){//onGetPoiResult
		int _radius = PdrUtil.parseInt(pRadius, 0);
		mIndex = PdrUtil.parseInt(index, 0);
		return mPoiSearch.searchNearby(new PoiNearbySearchOption().keyword(pKeyCode).location(pCenter.getLatLng()).radius(_radius).pageNum(mIndex).pageCapacity(mPageCapacity));
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
	public boolean poiSearchInbounds(String pKeyCode,MapPoint ptLB,MapPoint ptRT,String index){//onGetPoiResult
		mIndex = PdrUtil.parseInt(index, 0);
		LatLngBounds bounds = new LatLngBounds.Builder().include(ptLB.getLatLng()).include(ptRT.getLatLng()).build();
		return mPoiSearch.searchInBound(new PoiBoundSearchOption().keyword(pKeyCode).pageNum(mIndex).bound(bounds).pageCapacity(mPageCapacity));
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
		if("TRANSIT_FEE_FIRST".equals(pPolicy)){
			mTransitPolicy = TransitPolicy.EBUS_NO_SUBWAY;
		}else if("TRANSIT_TIME_FIRST".equals(pPolicy)){
			mTransitPolicy = TransitPolicy.EBUS_TIME_FIRST;
		}else if("TRANSIT_TRANSFER_FIRST".equals(pPolicy)){
			mTransitPolicy = TransitPolicy.EBUS_TRANSFER_FIRST;
		}else if("TRANSIT_WALK_FIRST".equals(pPolicy)){
			mTransitPolicy = TransitPolicy.EBUS_WALK_FIRST;
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
		if("DRIVING_DIS_FIRST".equals(pPolicy)){
			mDrivingPolicy = DrivingPolicy.ECAR_DIS_FIRST;
		}else if("DRIVING_FEE_FIRST".equals(pPolicy)){
			mDrivingPolicy = DrivingPolicy.ECAR_FEE_FIRST;
		}else if("DRIVING_NO_EXPRESSWAY".equals(pPolicy)){
			mDrivingPolicy = DrivingPolicy.ECAR_AVOID_JAM;
		}else{
			_ret = false;
		}
		return _ret;
	}
	
	/**
	 * 
	 * Description:Description:用于公交路线搜索
	 * @param pStart
	 * @param pEnd
	 * @param city
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-22 下午12:05:01</pre>
	 */
	public void transitSearch(Object pStart,Object pEnd,String city){
		PlanNode sNode;
		if(pStart instanceof MapPoint){
			sNode = PlanNode.withLocation(((MapPoint)pStart).getLatLng());
		}else{
			sNode = PlanNode.withCityNameAndPlaceName(city, (String)pStart);
		}
		PlanNode eNode;
		if(pEnd instanceof MapPoint){
			eNode = PlanNode.withLocation(((MapPoint)pEnd).getLatLng());
		}else{
			eNode = PlanNode.withCityNameAndPlaceName(city, (String)pEnd);
		}
		mSearchHandler.transitSearch(new TransitRoutePlanOption().city(city).from(sNode).to(eNode).policy(mTransitPolicy));
	}
	/**
	 * 
	 * Description:用于驾车路线搜索（start,end为point点)
	 * @param pStart
	 * @param pStartCity
	 * @param pEnd
	 * @param pEndCity
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-18 下午5:18:54</pre>
	 */
	public void drivingSearch(Object pStart,String pStartCity,Object pEnd, String pEndCity){
		PlanNode sNode;
		if(pStart instanceof MapPoint){
			sNode = PlanNode.withLocation(((MapPoint)pStart).getLatLng());
		}else{
			sNode = PlanNode.withCityNameAndPlaceName(pStartCity, (String)pStart);
		}
		PlanNode eNode;
		if(pEnd instanceof MapPoint){
			eNode = PlanNode.withLocation(((MapPoint)pEnd).getLatLng());
		}else{
			eNode = PlanNode.withCityNameAndPlaceName(pEndCity, (String)pEnd);
		}
		mSearchHandler.drivingSearch(new DrivingRoutePlanOption().from(sNode).to(eNode).policy(mDrivingPolicy));
	}

	public void walkingSearch(Object pStart, String pStartCity, Object pEnd, String pEndCity) {
		PlanNode sNode;
		if(pStart instanceof MapPoint){
			sNode = PlanNode.withLocation(((MapPoint)pStart).getLatLng());
		}else{
			sNode = PlanNode.withCityNameAndPlaceName(pStartCity, (String)pStart);
		}
		PlanNode eNode;
		if(pEnd instanceof MapPoint){
			eNode = PlanNode.withLocation(((MapPoint)pEnd).getLatLng());
		}else{
			eNode = PlanNode.withCityNameAndPlaceName(pEndCity, (String)pEnd);
		}
		mSearchHandler.walkingSearch(new WalkingRoutePlanOption().from(sNode).to(eNode));
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
	private String newJS_Point_Obj(LatLng pPoint, String pName){
        if (pPoint == null) {
            return "var " + pName +";";
        }
		StringBuffer sb = new StringBuffer();
		double _lat = pPoint.latitude;
		double _lng = pPoint.longitude;
		MapJsUtil.newJsVar(sb, pName, "plus.maps.Point", _lng + "," + _lat);
		return sb.toString();
	}
	
	private StringBuffer newJS_SearchPoiResult_Obj( String pName, StringBuffer pRet){
		MapJsUtil.newJsVar(pRet, pName, "plus.maps.__SearchPoiResult__", null);
		return pRet;
	}
	
	private String newJS_Position_Obj(PoiInfo poi){
		StringBuffer sb = new StringBuffer();
		String ptName = "p";
		sb.append(newJS_Point_Obj(poi.location,ptName));
		String posName = "pos";
		MapJsUtil.newJsVar(sb, posName, "plus.maps.Position", ptName);
		MapJsUtil.assignJsVar(sb, posName, "address", poi.address);
		MapJsUtil.assignJsVar(sb, posName, "city", poi.city);
		MapJsUtil.assignJsVar(sb, posName, "name", poi.name);
		MapJsUtil.assignJsVar(sb, posName, "phone", poi.phoneNum);
		MapJsUtil.assignJsVar(sb, posName, "postcode", poi.postCode);
		return MapJsUtil.wrapJsEvalString(sb.toString(), posName);
	}
	private String newJS_Position_Obj(LatLng pt,String posName){
		StringBuffer sb = new StringBuffer();
		String ptName = "p";
		sb.append(newJS_Point_Obj(pt,ptName));
		MapJsUtil.newJsVar(sb, posName, "plus.maps.Position", ptName);
		return sb.toString();
	}
	
	private JSONArray toPositionArray(List<PoiInfo> list){
		JSONArray arr = new JSONArray();
		for(PoiInfo poi : list){
			arr.put(newJS_Position_Obj(poi));
		}
		return arr;
	}
	
	private JSONArray toRouteArray(WalkingRouteResult routes){//walkingSearch
		JSONArray arr = new JSONArray();
		for(WalkingRouteLine line : routes.getRouteLines()) {
			arr.put(newWalkingRouteObj(line));
		}
		return arr;
	}
	private JSONArray toRouteArray(DrivingRouteResult routes){//drivingSearch
		JSONArray arr = new JSONArray();
		for(DrivingRouteLine line : routes.getRouteLines()) {
			arr.put(newJSDrivingRouteObj(line));
		}
		return arr;
	}
	
	private JSONArray toRouteArray(TransitRouteResult routes){//transitSearch
		JSONArray arr = new JSONArray();
		for(TransitRouteLine line: routes.getRouteLines()){
			arr.put(newJSTransitRouteObj(line));
		}
		return arr;
	}
	
	private String newJSDrivingRouteObj(DrivingRouteLine routePlan){
		// 5+ js map 规范
//		startPoint：Point，只读属性，路线起点地理坐标点
//		endPoint：Point，只读属性，路线终点地理坐标点
//		pointCount：Point，只读属性，路线坐标点段数
//		pointList：Array，只读属性，路线的地理坐标点数组，数组中保存Point对象。
//		distance：Number，只读属性，路线从起始点到终点的距离，单位为米。
//		routeTip：DOMString，只读属性，线路提示信息，没有提示信息则返回空字符串。
		StringBuffer sb = new StringBuffer();
		String sptName = "sp";
		sb.append(newJS_Point_Obj(routePlan.getStarting().getLocation(),sptName));
		String eptName = "ep";
		sb.append(newJS_Point_Obj(routePlan.getTerminal().getLocation(),eptName));
		String routeName = "route";
		MapJsUtil.newJsVar(sb, routeName, "plus.maps.Route", sptName + "," + eptName + ",false");//不调用至native层
		MapJsUtil.assignJsVar(sb, routeName, "distance", routePlan.getDistance());
		//构造native层对象，与js层一一对应
		String uuid = "Route_" + routePlan.hashCode();
		MapJsUtil.assignJsVar(sb, routeName, "_UUID_", uuid);//修改uuid使native层保存对象于js层一一对应
		JsMapRoute routeNativeObj = new JsMapRoute(mIWebview);
		routeNativeObj.setRoute(routePlan);
		JsMapManager.getJsMapManager().putJsObject(uuid, routeNativeObj);
		JSONArray ptsArr = new JSONArray();
		if (routePlan.getWayPoints() != null) {
			for (RouteNode node : routePlan.getWayPoints()) {
				ptsArr.put(newJS_Point_Obj(node.getLocation(), null));
			}
			MapJsUtil.assignJsVar(sb, routeName, "pointCount", ptsArr.length());
			MapJsUtil.assignJsVar(sb, routeName, "pointList", ptsArr);
		}
		return MapJsUtil.wrapJsEvalString(sb.toString(),routeName);
	}
	
	private String newJSTransitRouteObj(TransitRouteLine routePlan){
		// 5+ js map 规范
//		startPoint：Point，只读属性，路线起点地理坐标点
//		endPoint：Point，只读属性，路线终点地理坐标点
//		pointCount：Point，只读属性，路线坐标点段数
//		pointList：Array，只读属性，路线的地理坐标点数组，数组中保存Point对象。
//		distance：Number，只读属性，路线从起始点到终点的距离，单位为米。
//		routeTip：DOMString，只读属性，线路提示信息，没有提示信息则返回空字符串。
		StringBuffer sb = new StringBuffer();
		String sptName = "sp";
		sb.append(newJS_Point_Obj(routePlan.getStarting().getLocation(),sptName));
		String eptName = "ep";
		sb.append(newJS_Point_Obj(routePlan.getTerminal().getLocation(),eptName));
		String routeName = "route";
		MapJsUtil.newJsVar(sb, routeName, "plus.maps.Route", sptName + "," + eptName + ",false");//不调用至native层
		MapJsUtil.assignJsVar(sb, routeName, "distance", routePlan.getDistance());
		//构造native层对象，与js层一一对应
		String uuid = "Route_" + routePlan.hashCode();
		MapJsUtil.assignJsVar(sb, routeName, "_UUID_", uuid);//修改uuid使native层保存对象于js层一一对应
		JsMapRoute routeNativeObj = new JsMapRoute(mIWebview);
		routeNativeObj.setRoute(routePlan);
		JsMapManager.getJsMapManager().putJsObject(uuid, routeNativeObj);
		JSONArray ptsArr = new JSONArray();
		for (TransitStep transit : routePlan.getAllStep()) {
			JSONArray array = new JSONArray();
			for (LatLng lng : transit.getWayPoints()) {
				array.put(newJS_Point_Obj(lng, null));
			}
			ptsArr.put(array);
		}
		MapJsUtil.assignJsVar(sb, routeName, "pointCount", ptsArr.length());
		MapJsUtil.assignJsVar(sb, routeName, "pointList", ptsArr);
		return MapJsUtil.wrapJsEvalString(sb.toString(),routeName);
	}
	
	private StringBuffer newJS_SearchRouteResult_Obj( String pName, StringBuffer pRet){
		MapJsUtil.newJsVar(pRet, pName, "plus.maps.__SearchRouteResult__", null);
		return pRet;
	}
	
	
	private String newWalkingRouteObj(WalkingRouteLine routePlan){
		// 5+ js map 规范
//		startPoint：Point，只读属性，路线起点地理坐标点
//		endPoint：Point，只读属性，路线终点地理坐标点
//		pointCount：Point，只读属性，路线坐标点段数
//		pointList：Array，只读属性，路线的地理坐标点数组，数组中保存Point对象。
//		distance：Number，只读属性，路线从起始点到终点的距离，单位为米。
//		routeTip：DOMString，只读属性，线路提示信息，没有提示信息则返回空字符串。
		StringBuffer sb = new StringBuffer();
		String sptName = "sp";
		sb.append(newJS_Point_Obj(routePlan.getStarting().getLocation(),sptName));
		String eptName = "ep";
		sb.append(newJS_Point_Obj(routePlan.getTerminal().getLocation(),eptName));
		String routeName = "route";
		MapJsUtil.newJsVar(sb, routeName, "plus.maps.Route", sptName + "," + eptName + ",false");//不调用至native层
		MapJsUtil.assignJsVar(sb, routeName, "distance", routePlan.getDistance());
		//构造native层对象，与js层一一对应
		String uuid = "Route_" + routePlan.hashCode();
		MapJsUtil.assignJsVar(sb, routeName, "_UUID_", uuid);//修改uuid使native层保存对象于js层一一对应
		JsMapRoute routeNativeObj = new JsMapRoute(mIWebview);
		routeNativeObj.setRoute(routePlan);
		JsMapManager.getJsMapManager().putJsObject(uuid, routeNativeObj);
		JSONArray ptsArr = new JSONArray();
		for (WalkingStep step : routePlan.getAllStep()) {
			JSONArray array = new JSONArray();
			for (LatLng lng : step.getWayPoints()) {
				array.put(newJS_Point_Obj(lng, null));
			}
			ptsArr.put(array);
		}
		MapJsUtil.assignJsVar(sb, routeName, "pointCount", ptsArr.length());
		MapJsUtil.assignJsVar(sb, routeName, "pointList", ptsArr);
		return MapJsUtil.wrapJsEvalString(sb.toString(),routeName);
	}
	
/*	MKSearchListener listener = new MKSearchListener(){

		@Override
		public void onGetAddrResult(MKAddrInfo arg0, int arg1) {
		}

		@Override
		public void onGetBusDetailResult(MKBusLineResult arg0, int arg1) {
		}

		
		@Override
		public void onGetPoiDetailSearchResult(int arg0, int arg1) {
		}
		
		*//**
		 * @param type 返回结果类型: 
		 * 当预设城市有搜索结果时，type为 MKSearch.TYPE_POI_LIST，在预设城市没有搜索结果，
		 * 但在其他城市找到时返回其他城市列表, type为 MKSearch.TYPE_CITY_LIST
		 * @param iError - 错误号，0表示正确返回
		 *//*
		@Override
		public void onGetPoiResult(MKPoiResult pMKPoiResult, int type, int iError) {
			//poiSearchInCity	poiSearchNearBy		poiSearchInbounds
			String spr = "spr";
			StringBuffer js = new StringBuffer();
			newJS_SearchPoiResult_Obj(spr, js);
			MapJsUtil.assignJsVar(js, spr, "__state__", iError);
			MapJsUtil.assignJsVar(js, spr, "__type__", POISEARCH_TYPE);
			int totalNumber = 0;
			int currentNumber = 0;
			int pageNumber = 0;
			int pageIndex = 0;
			ArrayList<MKPoiInfo> pArray = null;
			if(pMKPoiResult != null){
				totalNumber = pMKPoiResult.getAllPoi().size();
				currentNumber = pMKPoiResult.getCurrentNumPois();
				pageNumber = pMKPoiResult.getNumPages();
				pageIndex = pMKPoiResult.getPageIndex();
				pArray = pMKPoiResult.getAllPoi();
			}else{
				pArray = new ArrayList<MKPoiInfo>();
			}
			MapJsUtil.assignJsVar(js, spr, "totalNumber", totalNumber);
			MapJsUtil.assignJsVar(js, spr, "currentNumber",currentNumber );
			MapJsUtil.assignJsVar(js, spr, "pageNumber", pageNumber);
			MapJsUtil.assignJsVar(js, spr, "pageIndex", pageIndex);
			JSONArray poiList = toPositionArray(pArray);
			MapJsUtil.assignJsVar(js, spr, "poiList", poiList);
			onSearchComplete(POISEARCH_TYPE, MapJsUtil.wrapJsEvalString(js.toString(), spr));
		}
		
		@Override
		public void onGetShareUrlResult(MKShareUrlResult arg0, int arg1,
				int arg2) {
		}

		@Override
		public void onGetSuggestionResult(MKSuggestionResult arg0, int arg1) {
		}
		
		@Override
		public void onGetDrivingRouteResult(MKDrivingRouteResult result, int iError) {
			//drivingSearch
			route_callback_js(result.getStart().pt, 
					result.getEnd().pt,
					result.getNumPlan(),
					iError,
					ROUTESEARCH_TYPE, 
					toRouteArray(result));
			
		}
		
		private void route_callback_js(GeoPoint sPoint,GeoPoint ePoint,int routeNumber,int state,int type,JSONArray routeList){
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
		
		@Override
		public void onGetTransitRouteResult(MKTransitRouteResult pMKTransitRouteResult, int iError) {
			//transitSearch
			route_callback_js(pMKTransitRouteResult.getStart().pt, 
					pMKTransitRouteResult.getEnd().pt,
					pMKTransitRouteResult.getNumPlan(),
					iError,
					ROUTESEARCH_TYPE, 
					toRouteArray(pMKTransitRouteResult));
		}

		@Override
		public void onGetWalkingRouteResult(MKWalkingRouteResult result, int iError) {
			//walkingSearch
			route_callback_js(result.getStart().pt, 
					result.getEnd().pt,
					result.getNumPlan(),
					iError,
					ROUTESEARCH_TYPE, 
					toRouteArray(result));
		}
		
	};*/
	DHMapView mMapView = null;
	public void setMapView(DHMapView pMapView) {
		mMapView = pMapView;
	}

	private void route_callback_js(LatLng sPoint,LatLng ePoint,int routeNumber,int state,int type,JSONArray routeList){
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
	
	@Override
	public void onGetDrivingRouteResult(DrivingRouteResult result) {
		// TODO Auto-generated method stub
		if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
			route_error_callback_js(DOMException.CODE_PARAMETER_ERRORP, "对不起，没有搜索到相关数据！", ROUTESEARCH_TYPE);
		}
		if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
			route_error_callback_js(DOMException.CODE_PARAMETER_ERRORP, "起终点或途经点地址有岐义", ROUTESEARCH_TYPE);
            return;
		}
		if (result.error == SearchResult.ERRORNO.NO_ERROR) {
			int error = 0;
			DrivingRouteLine line = result.getRouteLines().get(0);
			route_callback_js(line.getStarting().getLocation(), line.getTerminal().getLocation(), result.getRouteLines().size(), 
					error, ROUTESEARCH_TYPE, toRouteArray(result));
        }
	}

    @Override
    public void onGetIndoorRouteResult(IndoorRouteResult indoorRouteResult) {

    }

    @Override
    public void onGetBikingRouteResult(BikingRouteResult bikingRouteResult) {

    }

    @Override
	public void onGetTransitRouteResult(TransitRouteResult result) {
		// TODO Auto-generated method stub
		if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
			route_error_callback_js(DOMException.CODE_PARAMETER_ERRORP, "对不起，没有搜索到相关数据！", ROUTESEARCH_TYPE);
		}
		if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
			route_error_callback_js(DOMException.CODE_PARAMETER_ERRORP, "起终点或途经点地址有岐义", ROUTESEARCH_TYPE);
            return;
		}
		if (result.error == SearchResult.ERRORNO.NO_ERROR) {
			int error = 0;
			TransitRouteLine line = result.getRouteLines().get(0);
			route_callback_js(line.getStarting().getLocation(), line.getTerminal().getLocation(), 
					result.getRouteLines().size(), error, ROUTESEARCH_TYPE, toRouteArray(result));
		}
	}

    @Override
    public void onGetMassTransitRouteResult(MassTransitRouteResult massTransitRouteResult) {

    }

    @Override
	public void onGetWalkingRouteResult(WalkingRouteResult result) {
		// TODO Auto-generated method stub
		if (result == null || result.error != SearchResult.ERRORNO.NO_ERROR) {
			route_error_callback_js(DOMException.CODE_PARAMETER_ERRORP, "对不起，没有搜索到相关数据！", ROUTESEARCH_TYPE);
		}
		if (result.error == SearchResult.ERRORNO.AMBIGUOUS_ROURE_ADDR) {
			route_error_callback_js(DOMException.CODE_PARAMETER_ERRORP, "起终点或途经点地址有岐义", ROUTESEARCH_TYPE);
            return;
		}
		if (result.error == SearchResult.ERRORNO.NO_ERROR) {
			int error = 0;
			WalkingRouteLine line = result.getRouteLines().get(0);
			route_callback_js(line.getStarting().getLocation(), line.getTerminal().getLocation(), 
					result.getRouteLines().size(), error, ROUTESEARCH_TYPE, toRouteArray(result));
		}
	}
	
	/**
	 * 线路搜索错误回调JS
	 * @param state code 
	 * @param msg 错误描述 暂时没有用到
	 */
	private void route_error_callback_js(int state, String msg, int type) {
		String srrJSName = "srr";//SearchRouteResult
		StringBuffer js = new StringBuffer();
		newJS_SearchRouteResult_Obj(srrJSName, js);
		MapJsUtil.assignJsVar(js, srrJSName, "__state__", state);
		MapJsUtil.assignJsVar(js, srrJSName, "__type__", type);
		MapJsUtil.assignJsVar(js, srrJSName, "__msg__", msg);
		onSearchComplete(ROUTESEARCH_TYPE, MapJsUtil.wrapJsEvalString(js.toString(), srrJSName));
	}

	@Override
	public void onGetPoiDetailResult(PoiDetailResult arg0) {
		// TODO Auto-generated method stub
		
	}

    @Override
    public void onGetPoiIndoorResult(PoiIndoorResult poiIndoorResult) {

    }

    @Override
	public void onGetPoiResult(PoiResult result) {
		// TODO Auto-generated method stub
		if (result == null
				|| result.error == SearchResult.ERRORNO.RESULT_NOT_FOUND) {
			route_error_callback_js(DOMException.CODE_PARAMETER_ERRORP, "对不起，没有搜索到相关数据！", POISEARCH_TYPE);
		}
		if (result.error == SearchResult.ERRORNO.NO_ERROR) { // 搜索成功
			int iError = 0;
			String spr = "spr";
			StringBuffer js = new StringBuffer();
			newJS_SearchPoiResult_Obj(spr, js);
			MapJsUtil.assignJsVar(js, spr, "__state__", iError);
			MapJsUtil.assignJsVar(js, spr, "__type__", POISEARCH_TYPE);
			int totalNumber = 0;
			int currentNumber = 0;
			int pageNumber = 0;
			int pageIndex = 0;
			List<PoiInfo> pArray = null;
			if(result != null){
				totalNumber = result.getTotalPoiNum();
				currentNumber = result.getCurrentPageCapacity();
				pageNumber = result.getTotalPageNum();
				pageIndex = result.getCurrentPageNum();
				pArray = result.getAllPoi();
			}else{
				pArray = new ArrayList<PoiInfo>();
			}
			MapJsUtil.assignJsVar(js, spr, "totalNumber", totalNumber);
			MapJsUtil.assignJsVar(js, spr, "currentNumber",currentNumber );
			MapJsUtil.assignJsVar(js, spr, "pageNumber", pageNumber);
			MapJsUtil.assignJsVar(js, spr, "pageIndex", pageIndex);
			JSONArray poiList = toPositionArray(pArray);
			MapJsUtil.assignJsVar(js, spr, "poiList", poiList);
			onSearchComplete(POISEARCH_TYPE, MapJsUtil.wrapJsEvalString(js.toString(), spr));
		}
	}
	
}
