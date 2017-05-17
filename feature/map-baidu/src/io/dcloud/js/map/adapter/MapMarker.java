package io.dcloud.js.map.adapter;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.CanvasHelper;
import io.dcloud.common.adapter.util.PlatformUtil;
import io.dcloud.js.map.MapJsUtil;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.InfoWindow;
import com.baidu.mapapi.map.InfoWindow.OnInfoWindowClickListener;
import com.baidu.mapapi.map.Marker;
import com.baidu.mapapi.map.MarkerOptions;

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
	private Marker mMapMarker;
	
	private PopViewLayout mViewLayout;
	private IWebview mIWebview;
	
	private DHMapView mDHMapView;
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
	
	int mMangTop = -10;
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
	
	public void initMapMarker(DHMapView dhMapView) {
		mMapMarker = (Marker) dhMapView.getBaiduMap().addOverlay(getMarkerOptions());
		if (isToTop) {
			bringToTop();
		}
		if (isPop) {
			showInfoWindow(dhMapView.getBaiduMap(), dhMapView.getWebview().getContext(), dhMapView.getWebview());
		}
	}
	
	public void setPop(boolean isPop) {
		this.isPop = isPop;
	}
	/**
	 * 显示弹窗
	 * @param mapView
	 * @param context
	 */
	public void showInfoWindow(final BaiduMap mapView, Context context,
			final IWebview webview) {
		if (mLoadImageUrlData != null) {
			InfoWindow infoWindow = new InfoWindow(
					base64ToBitmap(mLoadImageUrlData), mMapPoint.getLatLng(), mMangTop,
					new OnInfoWindowClickListener() {

						@Override
						public void onInfoWindowClick() {
							// TODO Auto-generated method stub
							mapView.hideInfoWindow();
							MapJsUtil.execCallback(webview, uuid, "{type:'bubbleclick'}");
						}
					});
			mapView.showInfoWindow(infoWindow);
		} else if (mLoadImagePath != null) {
			InfoWindow infoWindow = new InfoWindow(
					getMarkerIcon(mLoadImagePath, false), mMapPoint.getLatLng(), mMangTop,
					new OnInfoWindowClickListener() {

						@Override
						public void onInfoWindowClick() {
							// TODO Auto-generated method stub
							mapView.hideInfoWindow();
							MapJsUtil.execCallback(webview, uuid,
									"{type:'bubbleclick'}");
						}
					});
			mapView.showInfoWindow(infoWindow);
		} else if (!TextUtils.isEmpty(mBubbleLabel)) {
			mViewLayout = new PopViewLayout(context);
			mViewLayout.setBubbleLabel(mBubbleLabel);
			Drawable drawable = getPopIcon();
			if (drawable != null) {
				mViewLayout.setBubbleIcon(drawable);
			}

			InfoWindow infoWindow = new InfoWindow(
					BitmapDescriptorFactory.fromView(mViewLayout),
					mMapPoint.getLatLng(), mMangTop,
					new OnInfoWindowClickListener() {

						@Override
						public void onInfoWindowClick() {
							// TODO Auto-generated method stub
							mapView.hideInfoWindow();
							MapJsUtil.execCallback(webview, uuid,
									"{type:'bubbleclick'}");
						}
					});

			mapView.showInfoWindow(infoWindow);
		}
	}
	
	public MarkerOptions getMarkerOptions() {
		MarkerOptions markerOptions = new MarkerOptions();
		markerOptions.position(mMapPoint.getLatLng());
		if (mIcons != null) {
			markerOptions.icons(mIcons);
			if (mPeriod >0)
			markerOptions.period(mPeriod);
		} else {
			BitmapDescriptor descriptor = null;
			if (!TextUtils.isEmpty(mIcon)) {
				descriptor = getMarkerIcon(mIcon, true);
			} else {
				descriptor = getDefaultMarkerIcon();
			}
			if (descriptor != null) {
				markerOptions.icon(descriptor);
			}
		}
		if (isDraggable) {
			markerOptions.draggable(isDraggable);
		}
		return markerOptions;
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
		if (mMapMarker != null) {
			mMapMarker.setIcon(getMarkerIcon(mIcon, true));
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
		if (mMapMarker != null) {
			mMapMarker.setIcon(getMarkerIcon(mIcon, true));
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
		if (mMapMarker != null) {
			mMapMarker.setPosition(mMapPoint.getLatLng());
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
	}
	
	public void setBubble(String pBubbleLabel, String pBubbleIcon, boolean isPop) {
		this.mBubbleLabel = pBubbleLabel;
		this.mBubbleIcon = pBubbleIcon;
		this.isPop = isPop;
	}

	/**
	 * @return the mBubbleLabel
	 */
	public String getBubbleLabel() {
		return mBubbleLabel;
	}

	/**
	 * @return the mMapMarker
	 */
	public Marker getMarkerOverlay() {
		return mMapMarker;
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
	public BitmapDescriptor getMarkerIcon(String path, boolean isText){
		BitmapDescriptor _ret = null;
		if(path != null){
			String iconPath = mIWebview.obtainFrameView().obtainApp().convert2AbsFullPath(mIWebview.obtainFullUrl(), path);
			if (!TextUtils.isEmpty(mLabel) && isText) {
				LinearLayout layout = new LinearLayout(mIWebview.getActivity());
				layout.setOrientation(LinearLayout.VERTICAL);
				layout.setGravity(Gravity.CENTER_VERTICAL);
				ImageView imageView = new ImageView(mIWebview.getActivity());
				Bitmap bitmap = CanvasHelper.getBitmap(iconPath);
				imageView.setImageBitmap(bitmap);
				TextView textView = new TextView(mIWebview.getActivity());
				textView.setTextColor(Color.BLACK);
				textView.setText(mLabel);
				textView.setTextSize(12);
				layout.addView(imageView);
				layout.addView(textView);
				mMangTop = -bitmap.getHeight()-20;
				_ret = BitmapDescriptorFactory.fromView(layout);
				bitmap.recycle();
			} else {
				Bitmap bitmap = CanvasHelper.getBitmap(iconPath);
				_ret = BitmapDescriptorFactory.fromBitmap(bitmap);
				mMangTop = -bitmap.getHeight();
				bitmap.recycle();
			}
		}
		return _ret;
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
		if(mBubbleIcon != null){
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
	
	public void show() {
		if (mMapMarker != null)
		mMapMarker.setVisible(true);
	}
	public void hide() {
		if (mMapMarker != null)
		mMapMarker.setVisible(false);
	}
	/**
	 * 设置当前marker在最上面
	 */
	public void bringToTop() {
		if (mMapMarker != null) {
			mMapMarker.setToTop();
		} else {
			isToTop = true;
		}
	}

	/**
	 * 设置 Marker 覆盖物的图标
	 * @param jsonArray
	 * @param period
	 */
	public void setIcons(JSONArray jsonArray, int period) {
		// TODO Auto-generated method stub
		if (jsonArray != null) {
			mIcons = new ArrayList<BitmapDescriptor>();
			for (int i = 0 ; i<jsonArray.length(); i++) {
				try {
					mIcons.add(getMarkerIcon(jsonArray.getString(i), true));
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			if (period > 0) {
				mPeriod = period;
			}
			if (mIcons.size() > 0 && mMapMarker != null) {
				mMapMarker.setIcons(mIcons);
				if (mPeriod > 0) {
					mMapMarker.setPeriod(mPeriod);
				}
			}
		}
	}
	
	/**
	 * 设置 marker 是否允许拖拽，默认不可拖拽
	 * @param draggable
	 */
	public void setDraggable(boolean draggable) {
		isDraggable = draggable;
		if (mMapMarker != null) {
			mMapMarker.setDraggable(isDraggable);
		}
	}
	/**
	 * 获取 marker 覆盖物是否可以拖拽
	 * @return
	 */
	public boolean isDraggable() {
		if (mMapMarker != null) {
			return mMapMarker.isDraggable();
		}
		return isDraggable;
	}
	
	public void hideBubble() {
		if (mMapMarker != null) {
			mDHMapView.getBaiduMap().hideInfoWindow();
		}
	}
	
	public void loadImage(String path) {
		mLoadImagePath = path;
	}
	
	public void loadImageDataURL(String base64) {
		mLoadImageUrlData = base64;
	}
	
	/** 
	 * base64转为bitmap 
	 * @param base64Data 
	 * @return 
	 */  
	public static BitmapDescriptor base64ToBitmap(String base64Data) { 
		String [] stars = base64Data.split(",");
		String stat = stars[1];
	    byte[] bytes = Base64.decode(stat, Base64.NO_PADDING);  
	    Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);  
	    return BitmapDescriptorFactory.fromBitmap(bitmap);
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
				mMangTop = -bg0.getHeight()-20;
				return BitmapDescriptorFactory.fromView(layout);
			}
			return BitmapDescriptorFactory.fromBitmap(bg0);
		}
		return null;
	}
	
}
