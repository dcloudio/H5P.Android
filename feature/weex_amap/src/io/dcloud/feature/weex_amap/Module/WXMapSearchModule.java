package io.dcloud.feature.weex_amap.Module;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.amap.api.services.core.AMapException;
import com.amap.api.services.core.LatLonPoint;
import com.amap.api.services.core.PoiItem;
import com.amap.api.services.geocoder.GeocodeResult;
import com.amap.api.services.geocoder.GeocodeSearch;
import com.amap.api.services.geocoder.RegeocodeAddress;
import com.amap.api.services.geocoder.RegeocodeQuery;
import com.amap.api.services.geocoder.RegeocodeResult;
import com.amap.api.services.poisearch.PoiResult;
import com.amap.api.services.poisearch.PoiSearch;
import com.taobao.weex.annotation.JSMethod;
import com.taobao.weex.bridge.JSCallback;
import com.taobao.weex.common.WXModule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import io.dcloud.common.DHInterface.ICallBack;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.feature.weex_amap.adapter.MapResourceUtils;

public class WXMapSearchModule extends WXModule {

    /**
     * 反向地理编码
     * @param data
     * @param callback
     */
    @JSMethod(uiThread = true)
    public void reverseGeocode(JSONObject data, final JSCallback callback) {
        final Map<String, Object> params = new HashMap<>();
        params.put("coordType", "gcj02");
        final LatLonPoint point = MapResourceUtils.createLatLonPoint(data.getJSONObject("point"));
        if(data != null && point != null) {
            int radius = 3000;
            final GeocodeSearch geocodeSearch = new GeocodeSearch(mWXSDKInstance.getContext());
            geocodeSearch.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
                @Override
                public void onRegeocodeSearched(RegeocodeResult result, int i) {
                    switch (i) {
                        case AMapException.CODE_AMAP_SUCCESS: {
                            if (result != null && result.getRegeocodeAddress() != null
                                    && result.getRegeocodeAddress().getFormatAddress() != null) {
                                RegeocodeAddress address = result.getRegeocodeAddress();
                                String ress = address.getFormatAddress();
                                params.put("type", "success");
                                params.put("address", ress);
                            } else {
                                params.put("type", "fail");
                                params.put("msg", "对不起，没有搜索到相关数据！");
                            }
                            break;
                        }
                        case AMapException.CODE_AMAP_CLIENT_UNKNOWHOST_EXCEPTION: {
                            params.put("type", "fail");
                            params.put("msg", "网络错误");
                            break;
                        }
                        case AMapException.CODE_AMAP_SIGNATURE_ERROR: {
                            params.put("type", "fail");
                            params.put("msg", "key验证无效！");
                            break;
                        }
                        default:{
                            params.put("type", "fail");
                            params.put("msg", "未知错误");
                            break;
                        }
                    }
                    if(callback != null) {
                        callback.invoke(params);
                    }
                }

                @Override
                public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {

                }
            });
            geocodeSearch.getFromLocationAsyn(new RegeocodeQuery(point, radius, geocodeSearch.AMAP));
        } else {
            params.put("type", "fail");
            params.put("msg", "参数有误");
            if(callback != null) {
                callback.invoke(params);
            }

        }
    }

    /**
     * 周边检索
     * @param data
     * @param callback
     */
    @JSMethod(uiThread = true)
    public void poiSearchNearBy(final JSONObject data, final JSCallback callback) {
        final LatLonPoint point = MapResourceUtils.createLatLonPoint(data.getJSONObject("point"));
        if(data != null && point != null) {
            RegeocodeQuery regeocodeQuery = new RegeocodeQuery(point, 200, GeocodeSearch.AMAP);
            getCityKey(regeocodeQuery, new ICallBack() {
                @Override
                public Object onCallBack(int pType, Object pArgs) {
                    if(pArgs != null) {
                        searchPOI(data, "", point, callback);
                    }
                    return null;
                }
            });
        }
    }

    private void searchPOI(JSONObject data, String city, LatLonPoint point, final JSCallback callback) {
        final Map<String, Object> params = new HashMap<>();
        int radius = 3000;
        if(data.containsKey("radius")) {
            radius = data.getIntValue("radius");
        }
        int pageNum = 0;
        if(data.containsKey("index")) {
            pageNum = data.getIntValue("index");
        }
        String keyCode = data.getString("key");
        PoiSearch.Query query = new PoiSearch.Query(keyCode, "", city);
        query.setPageSize(10);
        query.setDistanceSort(true);
        query.setPageNum(pageNum);
        PoiSearch poiSearch = new PoiSearch(mWXSDKInstance.getContext(), query);
        poiSearch.setBound(new PoiSearch.SearchBound(point, radius));
        poiSearch.setOnPoiSearchListener(new PoiSearch.OnPoiSearchListener() {
            @Override
            public void onPoiSearched(PoiResult result, int i) {
                switch (i) {
                    case AMapException.CODE_AMAP_SUCCESS: {
                        if (result != null && result.getQuery() != null) {
                            int currentNumber = result.getPois().size();
                            int pageNumber = result.getPageCount();
                            int pageIndex = result.getQuery().getPageNum();
                            ArrayList<PoiItem> pArray = result.getPois();
                            params.put("currentNumber", currentNumber);
                            params.put("pageNumber", pageNumber);
                            params.put("pageIndex", pageIndex);
                            JSONArray poiList = toPositionArray(pArray);
                            params.put("poiList", poiList);
                        }
                        break;
                    }
                    case AMapException.CODE_AMAP_CLIENT_UNKNOWHOST_EXCEPTION:{
                        params.put("type", "fail");
                        params.put("msg", "网络错误");
                        break;
                    }
                    case AMapException.CODE_AMAP_SIGNATURE_ERROR:{
                        params.put("type", "fail");
                        params.put("msg", "key验证无效！");
                        break;
                    }
                    default:{
                        params.put("type", "fail");
                        params.put("msg", "未知错误");
                        break;
                    }
                }
                if(callback != null) {
                    callback.invoke(params);
                }
            }

            @Override
            public void onPoiItemSearched(PoiItem poiItem, int i) {

            }
        });
        poiSearch.searchPOIAsyn();// 异步poi查询
    }

    private JSONArray toPositionArray(ArrayList<PoiItem> pArray) {
        JSONArray data = new JSONArray();
        if(pArray != null) {
            for(PoiItem poi: pArray) {
                JSONObject item = new JSONObject();
                if(poi.getLatLonPoint() != null) {
                    JSONObject location = new JSONObject();
                    location.put("latitude", poi.getLatLonPoint().getLatitude());
                    location.put("longitude", poi.getLatLonPoint().getLongitude());
                    item.put("location", location);
                }
                item.put("address", PdrUtil.makeQueryStringAllRegExp(poi.getSnippet()));
                item.put("city", PdrUtil.makeQueryStringAllRegExp(poi.getCityName()));
                item.put("name", PdrUtil.makeQueryStringAllRegExp(poi.getTitle()));
                item.put("phone", poi.getTel());
                item.put("postcode", poi.getPostcode());
                data.add(item);
            }
        }
        return data;
    }

    /**
     * 通过指定经纬度获取城市名称
     * @param regeocodeQuery
     * @return
     */
    private void getCityKey(RegeocodeQuery regeocodeQuery, final ICallBack callBack) {
        GeocodeSearch search = new GeocodeSearch(mWXSDKInstance.getContext());
        search.setOnGeocodeSearchListener(new GeocodeSearch.OnGeocodeSearchListener() {
            @Override
            public void onRegeocodeSearched(RegeocodeResult regeocodeResult, int i) {
                //Log.e("shutao", "onRegeocodeSearched-------"+regeocodeResult.getRegeocodeAddress().getCity());
                if(regeocodeResult != null && regeocodeResult.getRegeocodeAddress() != null) {
                    callBack.onCallBack(1, regeocodeResult.getRegeocodeAddress().getCity());
                } else {
                    callBack.onCallBack(-1, null);
                }
            }

            @Override
            public void onGeocodeSearched(GeocodeResult geocodeResult, int i) {
            }
        });
        search.getFromLocationAsyn(regeocodeQuery);
    }


}
