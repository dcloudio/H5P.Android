package io.dcloud.js.map.amap.adapter;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.CanvasHelper;
import io.dcloud.common.adapter.util.PlatformUtil;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.amap.api.maps.MapView;
import com.amap.api.maps.model.BitmapDescriptor;
import com.amap.api.maps.model.BitmapDescriptorFactory;
import com.amap.api.maps.model.LatLng;
import com.amap.api.maps.model.Marker;
import com.amap.api.maps.model.MarkerOptions;

//import com.amap.mapapi.core.GeoPoint;
//import com.amap.mapapi.core.OverlayItem;

/**
 * <p>Description:地图上的marker对象</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2012-10-31 下午3:19:54 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-10-31 下午3:19:54</pre>
 */
public class MapMarker {
	private String uuid;
	/**
	 * 标志图片
	 */
	private String mIcon;
	/**
	 * 标志标题
	 */
	private String mLabel;
	/**
	 * 点对象
	 */
	private MapPoint mMapPoint;
	/**
	 * 气泡图案
	 */
	private String mBubbleIcon;
	/**
	 * 气泡里面的文本描述
	 */
	private String mBubbleLabel;
	/**
	 * 地图上的marker
	 */
	private Marker mMarker;
	
	private IWebview mIWebview;
	/** 是否支持拖拽*/
	private boolean isDraggable = false;
	/** 是否默认弹出气泡*/
	private boolean isPop = false;
	/** 是否将覆盖物显示在最上层*/
	private boolean isToTop = false;
	/** 轮播图数组*/
	ArrayList<BitmapDescriptor> mIcons;
	/** 轮播时常*/
	private int mPeriod;
	
	private String mLoadImagePath;
	
	private String mLoadImageUrlData;

	public IWebview getWebview() {
		return this.mIWebview;
	}
	
	public Marker getMarker() {
		return mMarker;
	}
	/**
	 * Description: 构造函数 
	 * @param pFrameView
	 * @param pJsId 
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-8 下午3:48:42</pre>
	 */
	public MapMarker(MapPoint pMapPoint, IWebview webview) {
		mMapPoint = pMapPoint;
		mIWebview = webview;
	}
	
	public void setPop(boolean isPop) {
		this.isPop = isPop;
	}
	
	/**
	 * 生成marker 直接显示到map上
	 * @param mapView
	 */
	public void initMapMarker(MapView mapView) {
		mMarker = mapView.getMap().addMarker(getMapMarkOptions());
		if (isToTop) {
			bringToTop();
		}
	}
	
	/**
	 * @return the mIcon
	 */
	public String getIcon() {
		return mIcon;
	}
	/**
	 * @param mIcon the mIcon to set
	 */
	public void setIcon(String pIcon) {
		this.mIcon = pIcon;
		if (mMarker != null) {
			mMarker.setIcon(getIcon(mIcon));
		}
	}
	/**
	 * @return the mLabel
	 */
	public String getLabel() {
		return mLabel;
	}
	/**
	 * @param mLabel the mLabel to set
	 */
	public void setLabel(String pLabel) {
		this.mLabel = pLabel;
		if (mMarker != null) {
			mMarker.setIcon(getIcon(mIcon));
		}
	}
	
	
	/**
	 * @return the mMapPoint
	 */
	public MapPoint getMapPoint() {
		return mMapPoint;
	}
	/**
	 * @param mMapPoint the mMapPoint to set
	 */
	public void setMapPoint(MapPoint pMapPoint) {
		this.mMapPoint = pMapPoint;
		if (mMarker != null) {
			mMarker.setPosition(mMapPoint.getLatLng());
		}
	}
	
	/**
	 * @param pBubbleIcon the mBubbleIcon to set
	 */
	public void setBubbleIcon(String pBubbleIcon) {
		this.mBubbleIcon = pBubbleIcon;
	}

	public String getUuid() {
		return uuid;
	}

	public void setUuid(String uuid) {
		this.uuid = uuid;
	}

	/**
	 * @return the mBubbleIcon
	 */
	public String getBubbleIcon() {
		return mBubbleIcon;
	}

	/**
	 * @param pBubbleLabel the mBubbleLabel to set
	 */
	public void setBubbleLabel(String pBubbleLabel) {
		this.mBubbleLabel = pBubbleLabel;
		if (mMarker != null) {
			mMarker.setSnippet(mBubbleLabel);
		}
	}

	/**
	 * @return the mBubbleLabel
	 */
	public String getBubbleLabel() {
		return mBubbleLabel;
	}

	private MarkerOptions getMapMarkOptions() {
		MarkerOptions markerOptions = new MarkerOptions();
		if (mMapPoint != null) {
			LatLng ll = mMapPoint.getLatLng();
			markerOptions.position(ll);
		}
		if (mBubbleLabel != null) {
			markerOptions.snippet(mBubbleLabel);
		}
		if (!TextUtils.isEmpty(mLabel)) {
			markerOptions.title(mLabel);
		}
		if (mIcons != null) {
			markerOptions.icons(mIcons);
			if (mPeriod >0)
			markerOptions.period(mPeriod);
		} else if (mIcon != null) {
			markerOptions.icon(getIcon(mIcon));
		} else {
			markerOptions.icon(getDefaultMarkerIcon());
		}
		if (isDraggable) {
			markerOptions.draggable(isDraggable);
		}
		return markerOptions;
	}
	
	public BitmapDescriptor getIcon(String path) {
		BitmapDescriptor _res = null;
		if (!TextUtils.isEmpty(path)) {
			String iconPath = mIWebview.obtainFrameView().obtainApp().convert2AbsFullPath(mIWebview.obtainFullUrl(), path);
			if (TextUtils.isEmpty(mLabel)) {
				_res = BitmapDescriptorFactory.fromBitmap(CanvasHelper.getBitmap(iconPath));
			} else {
				LinearLayout layout = new LinearLayout(mIWebview.getActivity());
				layout.setOrientation(LinearLayout.VERTICAL);
				layout.setGravity(Gravity.CENTER_VERTICAL);
				ImageView imageView = new ImageView(mIWebview.getActivity());
				imageView.setImageBitmap(CanvasHelper.getBitmap(iconPath));
				TextView textView = new TextView(mIWebview.getActivity());
				textView.setTextColor(Color.BLACK);
				textView.setText(mLabel);
				textView.setTextSize(12);
				layout.addView(imageView);
				layout.addView(textView);
				_res = BitmapDescriptorFactory.fromView(layout);
			}
		}
		return _res;
	}
	
	public void hide() {
		if(mMarker != null) {
			mMarker.setVisible(false);
		}
	}
	
	public void show() {
		if(mMarker != null) {
			mMarker.setVisible(true);
		}
	}
	
	/**
	 * 设置当前marker在最上面
	 */
	public void bringToTop() {
		if (mMarker != null) {
			mMarker.setToTop();
		} else {
			isToTop = true;
		}
	}
	
	/**
	 * 设置标记是否可拖动
	 * @param draggable
	 */
	public void setDraggable(boolean draggable) {
		isDraggable = draggable;
		if (mMarker != null) {
			mMarker.setDraggable(draggable);
		}
	}
	
	/**
	 * 获取 marker 覆盖物是否可以拖拽
	 * @return
	 */
	public boolean isDraggable() {
		if (mMarker != null) {
			return mMarker.isDraggable();
		}
		return isDraggable;
	}
	
	/**
	 * 设置 Marker 覆盖物的图标
	 * @param jsonArray
	 * @param period
	 */
	public void setIcons(JSONArray jsonArray, int period) {
		if (jsonArray != null) {
			mIcons = new ArrayList<BitmapDescriptor>();
			for (int i = 0 ; i<jsonArray.length(); i++) {
				try {
					mIcons.add(getIcon(jsonArray.getString(i)));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (period > 0) {
				mPeriod = period;
			}
			if (mIcons.size() > 0 && mMarker != null) {
				mMarker.setIcons(mIcons);
				if (mPeriod > 0) {
					mMarker.setPeriod(mPeriod);
				}
			}
		}
	}
	
	/**
	 * 
	 * Description:获取图片
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-10-31 下午3:31:51</pre>
	 */
	public Drawable getPopIcon(){
		Drawable _ret = null;
		if(mBubbleIcon != null) {
			String iconPath = mIWebview.obtainFrameView().obtainApp().convert2AbsFullPath(mIWebview.obtainFullUrl(), mBubbleIcon);
			_ret = CanvasHelper.getDrawable(iconPath);
			if(_ret != null){
				int width = _ret.getIntrinsicWidth();
				int height = _ret.getIntrinsicHeight();
				_ret.setBounds(-width/2, -height, width/2, 0);
			}
		}
		return _ret;
	}
	public void setBubble(String label, String icon, boolean isPop) {
		// TODO Auto-generated method stub
		mBubbleLabel = label;
		mBubbleIcon = icon;
		this.isPop = isPop;
	}
	
	public void hideBubble() {
		if (mMarker != null) {
			mMarker.hideInfoWindow();
		}
	}
	
	public void checkPop() {
		if (isPop) {
			mMarker.showInfoWindow();
		}
	}
	
	public void loadImage(String path) {
		mLoadImagePath = path;
	}
	
	public void loadImageDataURL(String base64) {
		mLoadImageUrlData = base64;
	}
	
	public String getLoadImage() {
		return mLoadImagePath;
	}
	public String getLoadImageDataURL() {
		return mLoadImageUrlData;
	}
	
	public void loadImageBitmap(ImageView imageView) {
		String iconPath = mIWebview.obtainFrameView().obtainApp().convert2AbsFullPath(mIWebview.obtainFullUrl(), mLoadImagePath);
		Bitmap ret = CanvasHelper.getBitmap(iconPath);
		imageView.setImageBitmap(ret);
	    imageView.setBackgroundColor(Color.TRANSPARENT);
	}
	
	/** 
	 * base64转为bitmap 
	 * @param base64Data 
	 * @return 
	 */  
	public void base64ToBitmap(ImageView imageView) { 
		String [] stars = mLoadImageUrlData.split(",");
		String stat = stars[1];
	    byte[] bytes = Base64.decode(stat, Base64.NO_PADDING);  
	    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length); 
	    imageView.setImageBitmap(bitmap);
	    imageView.setBackgroundColor(Color.TRANSPARENT);
	}
	
	/**
	 * 获取marker默认图标
	 * @return
	 */
	private BitmapDescriptor getDefaultMarkerIcon() {
		Bitmap bg0 = BitmapFactory.decodeResourceStream(mIWebview.getActivity().getResources(), null,
				PlatformUtil.getResInputStream("res/point.png"),
				null, null);
		if (bg0 != null) {
			if (!TextUtils.isEmpty(mLabel)) {
				LinearLayout layout = new LinearLayout(mIWebview.getActivity());
				layout.setOrientation(LinearLayout.VERTICAL);
				layout.setGravity(Gravity.CENTER_VERTICAL);
				ImageView imageView = new ImageView(mIWebview.getActivity());
				imageView.setImageBitmap(bg0);
				TextView textView = new TextView(mIWebview.getActivity());
				textView.setTextColor(Color.BLACK);
				textView.setText(mLabel);
				textView.setTextSize(12);
				layout.addView(imageView);
				layout.addView(textView);
				return BitmapDescriptorFactory.fromView(layout);
			}
			return BitmapDescriptorFactory.fromBitmap(bg0);
		}
		return null;
	}
}
