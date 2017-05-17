/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.dcloud.feature.barcode.camera;

import io.dcloud.feature.barcode.decoding.IBarHandler;
import io.dcloud.feature.barcode.view.DetectorViewConfig;

import java.io.IOException;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.os.Handler;
import android.view.SurfaceHolder;

/**
 * This object wraps the Camera service object and expects to be the only one talking to it. The
 * implementation encapsulates the steps needed to take preview-sized images, which are used for
 * both preview and decoding.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CameraManager {

  private static final String TAG = CameraManager.class.getSimpleName();

  private static final int MIN_FRAME_WIDTH = 240;
  private static final int MIN_FRAME_HEIGHT = 240;
  private static final int MAX_FRAME_WIDTH = 640;
  private static final int MAX_FRAME_HEIGHT = 640;

  private static CameraManager cameraManager;

  public static int sScreenWidth,sScreenAllHeight;
  static final int SDK_INT; // Later we can use Build.VERSION.SDK_INT
  static {
    int sdkInt;
    try {
      sdkInt = Integer.parseInt(Build.VERSION.SDK);
    } catch (NumberFormatException nfe) {
      // Just to be safe
      sdkInt = 10000;
    }
    SDK_INT = sdkInt;
  }

  private final Context context;
  private final CameraConfigurationManager configManager;
  private Camera camera;
  private Rect framingRect;
  private Rect framingRectInPreview;
  private boolean initialized;
  private boolean previewing;
  private  boolean useOneShotPreviewCallback;
  /**
   * Preview frames are delivered here, which we pass on to the registered handler. Make sure to
   * clear the handler so it will only receive one message.
   */
  private final PreviewCallback previewCallback;
  /** Autofocus callbacks arrive here, and are dispatched to the Handler which requested them. */
  private final AutoFocusCallback autoFocusCallback;

  /**
   * Initializes this static object with the Context of the calling Activity.
   *
   * @param context The Activity which wants to use the camera.
   */
  public static void init(Context context) {
    if (cameraManager == null) {
      cameraManager = new CameraManager(context);
    }
  }

  /**
   * Gets the CameraManager singleton instance.
   *
   * @return A reference to the CameraManager singleton.
   */
  public static CameraManager get() {
    return cameraManager;
  }

  private CameraManager(Context context) {

    this.context = context;
    this.configManager = new CameraConfigurationManager(context);

    // Camera.setOneShotPreviewCallback() has a race condition in Cupcake, so we use the older
    // Camera.setPreviewCallback() on 1.5 and earlier. For Donut and later, we need to use
    // the more efficient one shot callback, as the older one can swamp the system and cause it
    // to run out of memory. We can't use SDK_INT because it was introduced in the Donut SDK.
    //useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > Build.VERSION_CODES.CUPCAKE;
    useOneShotPreviewCallback = Integer.parseInt(Build.VERSION.SDK) > 3; // 3 = Cupcake

    previewCallback = new PreviewCallback(configManager, useOneShotPreviewCallback);
    useOneShotPreviewCallback = false;//不使用oneshotpreview预览
    autoFocusCallback = new AutoFocusCallback();
  }
  public byte[] getLastBitmapData(){
	  if(previewCallback==null){
		  return null;
	  }
	  return previewCallback.getLastBitmapData();
  }
  public void clearLastBitmapData(){
	  if(previewCallback!=null){
		  previewCallback.setLastBitmapData(null);
	  }
  }
  public static Point getCR(int gatherViewWidth,int gatherViewHeight){
	  Point p = null;
	try {
		Camera camera;
		camera = Camera.open();
		Camera.Parameters parameters = camera.getParameters();
		//根据采集区域宽高获取适合的相机分辨率
		Point  sr = new Point(gatherViewWidth, gatherViewHeight);//就使用屏幕宽高，避免焦距调整
		p = CameraConfigurationManager.getCameraResolution(parameters, sr);
		camera.release();
	} catch (Exception e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	}
	return p;
  }
  /**
   * Opens the camera driver and initializes the hardware parameters.
   *
   * @param holder The surface object which the camera will draw preview frames into.
   * @throws IOException Indicates the camera driver failed to open.
   */
  public void openDriver(SurfaceHolder holder) throws IOException {
    if (camera == null) {
      camera = Camera.open();
      if (camera == null) {
        throw new IOException();
      }
      camera.setPreviewDisplay(holder);
      camera.setDisplayOrientation(90);
      if (!initialized) {
        initialized = true;
        configManager.initFromCameraParameters(camera);
      }
      configManager.setDesiredCameraParameters(camera);
    }
  }

  public void setFlashlight(boolean enable){
	  if(enable){
		  FlashlightManager.enableFlashlight();
	  }else{
		  FlashlightManager.disableFlashlight();
	  }
  }
  
  public Camera getCameraHandler(){
	  return camera;
  }
  /**
   * Closes the camera driver if still in use.
   */
  public void closeDriver() {
    if (camera != null) {
      FlashlightManager.disableFlashlight();
      camera.release();
      camera = null;
    }
  }

  /**
   * Asks the camera hardware to begin drawing preview frames to the screen.
   */
  public void startPreview() {
    if (camera != null && !previewing) {
      camera.startPreview();
      previewing = true;
    }
  }

  /**
   * Tells the camera to stop drawing preview frames.
   */
  public void stopPreview() {
    if (camera != null && previewing) {
      if (!useOneShotPreviewCallback) {
        camera.setPreviewCallback(null);
      }
      camera.stopPreview();
      previewCallback.setHandler(null,null, 0);
      autoFocusCallback.setHandler(null, 0);
      previewing = false;
    }
  }

  /**
   * A single preview frame will be returned to the handler supplied. The data will arrive as byte[]
   * in the message.obj field, with width and height encoded as message.arg1 and message.arg2,
   * respectively.
   *
   * @param handler The handler to send the message to.
   * @param message The what field of the message to be sent.
   */
  public void requestPreviewFrame(IBarHandler barHandler,Handler handler, int message) {
    if (camera != null && previewing) {
      previewCallback.setHandler(barHandler,handler, message);
      if (useOneShotPreviewCallback) {
        camera.setOneShotPreviewCallback(previewCallback);
      } else {
        camera.setPreviewCallback(previewCallback);
      }
    }
  }

  /**
   * Asks the camera hardware to perform an autofocus.
   *
   * @param handler The Handler to notify when the autofocus completes.
   * @param message The message to deliver.
   */
  public void requestAutoFocus(Handler handler, int message) {
    if (camera != null && previewing) {
      autoFocusCallback.setHandler(handler, message);
      //Log.d(TAG, "Requesting auto-focus callback");
      try {
		camera.autoFocus(autoFocusCallback);
	} catch (Exception e) {
		e.printStackTrace();
	}
    }
  }
  /**
   * 取消自动对焦
   * 
   * <br/>Create By: yanglei Email:yanglei@dcloud.io at 2013-6-6 上午10:57:32
   */
  public void removeAutoFocus(){
	  if(camera!=null){
		  camera.cancelAutoFocus();
	  }
  }

  /**
   * Like {@link #getFramingRect} but coordinates are in terms of the preview frame,
   * not UI / screen.
   */
  public Rect getFramingRectInPreview() {
    if (framingRectInPreview == null) {
    	Point cameraResolution = configManager.getCameraResolution();
        Rect detectorRect = DetectorViewConfig.getInstance().getDetectorRect();
        Rect surfaceViewRect = DetectorViewConfig.getInstance().surfaceViewRect;
        
    	{
//    		以下s表示像素值，c表示camera分辨率的值(w为x，h为y即，c_w = screenResolution.x,c_h=screenResolution.y)
//    		由成像透射到surfaceView的关系可得到公式:s_view_w / c_w = s_view_h / c_h;
    		float s = surfaceViewRect.width() / cameraResolution.y;//获取屏幕实际像素与预览分辨的比例，以方便之后回去预览分辨率中指定区域的数据
    		int cl = (detectorRect.top - DetectorViewConfig.detectorRectOffestTop) * cameraResolution.x / surfaceViewRect.height();
    		int cr = cl + (detectorRect.height() * cameraResolution.x / surfaceViewRect.height());
    		
    		int ct = (surfaceViewRect.right - detectorRect.right) * cameraResolution.y / surfaceViewRect.width();
    		int cb = ct + detectorRect.width() * cameraResolution.x / surfaceViewRect.height();
    		
    		detectorRect = new Rect(cl,ct,cr,cb);
    	}
    	
      framingRectInPreview = detectorRect;
    }
    return framingRectInPreview;
  }

  /**
   * Converts the result points from still resolution coordinates to screen coordinates.
   *
   * @param points The points returned by the Reader subclass through Result.getResultPoints().
   * @return An array of Points scaled to the size of the framing rect and offset appropriately
   *         so they can be drawn in screen coordinates.
   */
  /*
  public Point[] convertResultPoints(ResultPoint[] points) {
    Rect frame = getFramingRectInPreview();
    int count = points.length;
    Point[] output = new Point[count];
    for (int x = 0; x < count; x++) {
      output[x] = new Point();
      output[x].x = frame.left + (int) (points[x].getX() + 0.5f);
      output[x].y = frame.top + (int) (points[x].getY() + 0.5f);
    }
    return output;
  }
   */

  /**
   * A factory method to build the appropriate LuminanceSource object based on the format
   * of the preview buffers, as described by Camera.Parameters.
   *
   * @param data A preview frame.
   * @param width The width of the image.
   * @param height The height of the image.
   * @return A PlanarYUVLuminanceSource instance.
   */
  public PlanarYUVLuminanceSource buildLuminanceSource(byte[] data, int width, int height) {
    Rect rect = getFramingRectInPreview();
    int previewFormat = configManager.getPreviewFormat();
    String previewFormatString = configManager.getPreviewFormatString();
    switch (previewFormat) {
      // This is the standard Android format which all devices are REQUIRED to support.
      // In theory, it's the only one we should ever care about.
      case PixelFormat.YCbCr_420_SP:
      // This format has never been seen in the wild, but is compatible as we only care
      // about the Y channel, so allow it.
      case PixelFormat.YCbCr_422_SP:
        return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
            rect.width(), rect.height());
      default:
        // The Samsung Moment incorrectly uses this variant instead of the 'sp' version.
        // Fortunately, it too has all the Y data up front, so we can read it.
        if ("yuv420p".equals(previewFormatString)) {
          return new PlanarYUVLuminanceSource(data, width, height, rect.left, rect.top,
            rect.width(), rect.height());
        }
    }
    throw new IllegalArgumentException("Unsupported picture format: " +
        previewFormat + '/' + previewFormatString);
  }

    public AutoFocusCallback getAutoFocusCallback(){
        return  autoFocusCallback;
    }
}
