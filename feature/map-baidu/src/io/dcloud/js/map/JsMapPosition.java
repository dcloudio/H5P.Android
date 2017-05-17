package io.dcloud.js.map;

import io.dcloud.common.DHInterface.IWebview;

import org.json.JSONArray;


/**
 * <p>Description:对应js的position对象
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2012-11-6 下午4:05:25 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-6 下午4:05:25</pre>
 */
class JsMapPosition extends JsMapObject{
	
	/**
	 * JS对应的ID
	 */
	private String mJsId;
	/**
	 * 位置点的经纬度坐标
	 */
	private JsMapPoint mMapPoint;
	/**
	 * 位置点的地址信息
	 */
	private String address;
	/**
	 * 位置点的城市信息
	 */
	private String city;
	/**
	 * 位置点的名称
	 */
	private String name;
	/**
	 * 位置点的电话信息
	 */
	private String phone;
	/**
	 * 位置点的邮编信息
	 */
	private String postcode;
	
	/**
	 * Description: 构造函数 
	 * @param pFrameView
	 * @param pJsId 
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2012-11-8 下午3:51:48</pre>
	 */
	public JsMapPosition(IWebview pWebview,JsMapPoint pMapPoint,String pJsId) {
		super(pWebview);
		mMapPoint = pMapPoint;
		mJsId = pJsId;
	}
	
	/**
	 * @return the mJsId
	 */
	public String getmJsId() {
		return mJsId;
	}

	/**
	 * @return the mapPoint
	 */
	public JsMapPoint getMapPoint() {
		return mMapPoint;
	}
	/**
	 * @param mapPoint the mapPoint to set
	 */
	public void setMapPoint(JsMapPoint mapPoint) {
		this.mMapPoint = mapPoint;
	}
	/**
	 * @return the address
	 */
	public String getAddress() {
		return address;
	}
	/**
	 * @param address the address to set
	 */
	public void setAddress(String address) {
		this.address = address;
	}
	/**
	 * @return the city
	 */
	public String getCity() {
		return city;
	}
	/**
	 * @param city the city to set
	 */
	public void setCity(String city) {
		this.city = city;
	}
	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}
	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	/**
	 * @return the phone
	 */
	public String getPhone() {
		return phone;
	}
	/**
	 * @param phone the phone to set
	 */
	public void setPhone(String phone) {
		this.phone = phone;
	}
	/**
	 * @return the postcode
	 */
	public String getPostcode() {
		return postcode;
	}
	/**
	 * @param postcode the postcode to set
	 */
	public void setPostcode(String postcode) {
		this.postcode = postcode;
	}

	@Override
	protected void createObject(JSONArray pJsArgs) {
	}

	@Override
	protected void updateObject(String pStrEvent, JSONArray pJsArgs) {
	}
	
}
