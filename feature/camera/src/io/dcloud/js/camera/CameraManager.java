package io.dcloud.js.camera;

import io.dcloud.common.adapter.util.DeviceInfo;
import io.dcloud.common.util.JSONUtil;
import io.dcloud.common.util.PdrUtil;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Size;

/**
 * <p>Description:摄像头相关功能管理</p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-1-11 下午4:58:43 created.
 * 
 * <pre><p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-11 下午4:58:43</pre>
 */
class CameraManager {
	/**
	 * 管理者对象
	 */
	private static CameraManager mCameraManager;
	/**
	 * 摄像头对象
	 */
	
	/**
	 * 拍照摄像字段
	 */
	protected static int IMAGE_CAPTURE = 1;
	protected static int VIDEO_CAPTURE = 2;
	
	public static final int CAMERA_MAJOR = 1;
	public static final int CAMERA_MINOR = 2;
	List<Size> support_videoSizes = null;
	List<Integer> support_picFormats = null;
	List<Size> support_picSize = null;
	/**
	 * 
	 * Description: 构造函数  
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-11 下午5:02:14</pre>
	 */
	CameraManager(int pIndex){

		try {
			Camera mCamera = null;
			if(pIndex == CAMERA_MINOR && DeviceInfo.sDeviceSdkVer >= 9){//使用前置
                for(int i=0;i < Camera.getNumberOfCameras();i++){
                    CameraInfo _info = new CameraInfo();
                    Camera.getCameraInfo(i, _info);
                    if(_info.facing == CameraInfo.CAMERA_FACING_FRONT){
                        mCamera = Camera.open(i);
                        break;
                    }
                }
            }
			if(mCamera == null){
                mCamera = Camera.open();
            }
			if(DeviceInfo.sDeviceSdkVer >= 11){
                support_videoSizes = mCamera.getParameters().getSupportedVideoSizes();
            }
			support_picSize = mCamera.getParameters().getSupportedPictureSizes();
			if(DeviceInfo.sDeviceSdkVer >= 8){
                support_picFormats = mCamera.getParameters().getSupportedPictureFormats();
            }
			mCamera.release();//释放camera资源
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	/**
	 *
	 * Description:获得js摄像头对象
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-13 下午1:34:23</pre>
	 */
	protected String getJsCamera(){
		String _json = "(function(){"+
				"return{"+
						"supportedImageResolutions : %s,"+
						"supportedVideoResolutions : %s,"+
						"supportedImageFormats : %s,"+
						"supportedVideoFormats : %s"+
			        "};"+
				"})();";
		String[] _formats = supportedImageVideoFormats();
		_json = String.format(_json, supportedImageResolutions(),supportedVideoResolutions(),
				_formats[0],_formats[1]);
		return _json;
	}
	/**
	 *
	 * Description:获得摄像头管理者
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-11 下午5:00:16</pre>
	 */
	protected static CameraManager getCameraManager(String pIndex){
		int _index = PdrUtil.parseInt(pIndex,CAMERA_MAJOR);
		if(mCameraManager == null){
			mCameraManager = new CameraManager(_index);
		}
		return mCameraManager;
	}
	/**
	 * 
	 * Description:枚举设备支持的摄像分辨率
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-11 下午5:09:40</pre>
	 */
	private String supportedVideoResolutions(){
		String _result = "[]";
		if(support_videoSizes != null && DeviceInfo.sDeviceSdkVer >= 11){
			List<Size> _list = support_videoSizes;
			_result = getListSizeToString(_list);
		}
		return _result;
	}
	
	/**
	 * 
	 * Description:枚举设备支持的拍照分辨率
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-11 下午5:43:01</pre>
	 */
	private String supportedImageResolutions(){
		String _result = "[]";
		if(support_picSize != null){
			List<Size> _list = support_picSize;
			_result = getListSizeToString(_list);
		}
		return _result;
	}
	/**
	 * 
	 * Description:枚举设备支持的拍照/摄像格式
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-11 下午5:45:43</pre>
	 */
	private String[] supportedImageVideoFormats(){
		String[] _result = null;
		if(support_picFormats != null){
			List<Integer> _list = support_picFormats;
			_result = getListIntegerToString(_list);
		}
		if(_result == null) {
			_result = new String[2];
			_result[0] = "['jpg']";
			_result[1] = "['mp4']";
		}
		return _result;
	}
	/**
	 * 
	 * Description:将Integer的集合转换为字符串
	 * @param pList
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-11 下午5:47:33</pre>
	 */
	private String[] getListIntegerToString(List<Integer> pList){
		String[] _result = new String[2];
		_result[0] = "['jpg']";
		_result[1] = "['mp4']";
//		int _length = pList.size();
//		if(pList != null &&  _length> 1) { //如果length==1,表示只支持单一尺寸，按规范返回null
//			StringBuffer _imgSb = new StringBuffer();
//			_imgSb.append("[");
//			for(int i = 0; i < _length; i++) {
//				if(pList.get(i) == ImageFormat.JPEG){
//					_imgSb.append("jpg,");
//				}else{
//					
//				}
//			}
//			_imgSb.append("]");
//			_result[0] = _imgSb.toString();
////			_result[1] = _videoSb.toString();
//		}
		return _result;
	}
	
	/**
	 * 
	 * Description:将Size的集合转换为字符串
	 * @param pList
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-11 下午5:41:59</pre>
	 */
	private String getListSizeToString(List<Size> pList){
		String _result = "[]";
		int _length = pList.size();
		if(pList != null &&  _length> 1) { //如果length==1,表示只支持单一尺寸，按规范返回null
			StringBuffer sb = new StringBuffer();
			sb.append("[");
			for(int i = 0; i < _length; i++) {
				sb.append("'"+pList.get(i).width + "*" + pList.get(i).height+"'");
				if(i != _length-1){
					sb.append(",");
				}
			}
			sb.append("]");
			_result = sb.toString();
		}
		return _result;
	}
	
	

	/**
	 * Description:解析js的option对象
	 * @param pJson
	 * @return
	 *
	 * <pre><p>ModifiedLog:</p>
	 * Log ID: 1.0 (Log编号 依次递增)
	 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-14 下午3:31:19</pre>
	 */
	static CameraOption parseOption(String pJson,boolean isCaptureImage) {
		CameraOption _Option = new CameraOption();
		if(pJson != null){
			JSONObject json = null;
			try {
				json = new JSONObject(pJson);
			} catch (JSONException e) {
			}
			_Option.resolution = JSONUtil.getString(json, "resolution");
			String filename = JSONUtil.getString(json, "filename");
			String format = JSONUtil.getString(json, "format");
			format = isCaptureImage ? "jpg":"mp4";
			_Option.format = format;
			filename = PdrUtil.getDefaultPrivateDocPath(filename, format);
			_Option.filename = filename;
			_Option.index = JSONUtil.getInt(json, "index");
		}
		return _Option;
	}
	
	
	static class CameraOption{
		String filename;
		String resolution;
		String format;
		int index;
		/**
		 * @return the filename
		 */
		public String getFilename() {
			return filename;
		}
		/**
		 * @return the resolution
		 */
		public String getResolution() {
			return resolution;
		}
		/**
		 * @return the format
		 */
		public String getFormat() {
			return format;
		}
		/**
		 * @return the index
		 */
		public int getIndex() {
			return index;
		}
		
	}
}
