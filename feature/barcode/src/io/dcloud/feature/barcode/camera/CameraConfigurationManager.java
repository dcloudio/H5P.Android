/*
 * Copyright (C) 2010 ZXing authors
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

import io.dcloud.common.adapter.util.Logger;
import io.dcloud.feature.barcode.view.DetectorViewConfig;

import java.util.regex.Pattern;

import android.content.Context;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.Build;
import android.util.Log;

final class CameraConfigurationManager {

  private static final String TAG = CameraConfigurationManager.class.getSimpleName();

  private static final int TEN_DESIRED_ZOOM = 27;
  private static final int DESIRED_SHARPNESS = 30;

  private static final Pattern COMMA_PATTERN = Pattern.compile(",");

  private final Context context;
//  /**相机控件预览尺寸*/
//  private Point screenResolution;
  
  /**自动计算出来的相机使用的分辨率x大，y小*/
  private Point cameraResolution;
  
  private int previewFormat;
  private String previewFormatString;

  CameraConfigurationManager(Context context) {
    this.context = context;
  }

  /**
   * Reads, one time, values from the camera that are needed by the app.
   */
  void initFromCameraParameters(Camera camera) {
    Camera.Parameters parameters = camera.getParameters();
    previewFormat = parameters.getPreviewFormat();
    previewFormatString = parameters.get("preview-format");
    Log.d(TAG, "Default preview format: " + previewFormat + '/' + previewFormatString);
//    screenResolution = new Point(CameraManager.sScreenWidth, CameraManager.sScreenAllHeight);//尽可能的使用最大值使得预览图像看起来更接近
//    Log.d(TAG, "Screen resolution: " + screenResolution);
    //获取使用屏幕宽高的预览尺寸
//    int w = CameraManager.sScreenWidth;
//    int h = CameraManager.sScreenAllHeight;
    
    Rect rect = DetectorViewConfig.getInstance().gatherRect;//使用探测区域大小，避免相机预览成像超出div
    int w = rect.width();
    int h = rect.height();
    cameraResolution = getCameraResolution(parameters, new Point(w,h));
//    screenResolution = new Point(cameraResolution.x,cameraResolution.y);
//    Log.d(TAG, "Screen resolution: " + screenResolution);
  }

  /**
   * Sets the camera up to take preview images which are used for both preview and decoding.
   * We detect the preview format here so that buildLuminanceSource() can build an appropriate
   * LuminanceSource subclass. In the future we may want to force YUV420SP as it's the smallest,
   * and the planar Y can be used for barcode scanning without a copy in some cases.
   */
  void setDesiredCameraParameters(Camera camera) {
    Camera.Parameters parameters = camera.getParameters();
    Log.d(TAG, "Setting preview size: " + cameraResolution);
    //需要考虑到是否调用了Camera.setDisplayOrientation 90 270 时候w<h;0 180时候w>h
    parameters.setPreviewSize(cameraResolution.x, cameraResolution.y);
    setFlash(parameters);
    setZoom(parameters);
//    setSharpness(parameters);
    camera.setParameters(parameters);
  }

  Point getCameraResolution() {
    return cameraResolution;
  }

  int getPreviewFormat() {
    return previewFormat;
  }

  String getPreviewFormatString() {
    return previewFormatString;
  }

  public static Point getCameraResolution(Camera.Parameters parameters, Point screenResolution) {

    String previewSizeValueString = parameters.get("preview-size-values");
    // saw this on Xperia
    if (previewSizeValueString == null) {
      previewSizeValueString = parameters.get("preview-size-value");
    }

    Point cameraResolution = null;

    if (previewSizeValueString != null) {
      Logger.i(TAG, "preview-size-values parameter: " + previewSizeValueString);
      cameraResolution = findBestPreviewSizeValue(previewSizeValueString, screenResolution);
    }

    if (cameraResolution == null) {
      // Ensure that the camera resolution is a multiple of 8, as the screen may not be.
      cameraResolution = new Point(
          (screenResolution.x >> 3) << 3,
          (screenResolution.y >> 3) << 3);
    }

    return cameraResolution;
  }

  //得到比采集值小的的一个最合适的分辨率,缺陷，可能使得探测区域很小
	private static Point findBestPreviewSizeValue0(
			CharSequence previewSizeValueString, Point screenResolution) {
		int bestW = 0;
		int bestH = 0;
		String[] list = COMMA_PATTERN.split(previewSizeValueString);
		// 从大到小遍历
		int bTs = 0;//0未确定，-1需要倒叙变为从大到小，1正序从大到小
		int offsetW = 0;
		for (int i = 0; i < list.length; i++) {
			String previewSize = null;
			if (bTs == 0 || bTs == 1) {
				previewSize = list[i];
			} else {
				previewSize = list[list.length - 1 - i];
			}
			previewSize = previewSize.trim();
			int dimPosition = previewSize.indexOf('x');
			if (dimPosition < 0) {
				Log.w(TAG, "Bad preview-size: " + previewSize);
				continue;
			}

			int newH = 0;
			int newW = 0;
			try {
				newW = Integer.parseInt(previewSize.substring(0, dimPosition));
				newH = Integer.parseInt(previewSize.substring(dimPosition + 1));
			} catch (NumberFormatException nfe) {
				Log.w(TAG, "Bad preview-size: " + previewSize);
				continue;
			}
			if(bTs != 0){
				// 需要获得正常的预览分辨率
				if (bestW > 0 && newW <= screenResolution.y && newH <= screenResolution.x) {
					bestW = newW;
					bestH = newH;
					break;
				}
			}else{
				if(bestW != 0 ){
					if(newW > bestW){//从小到大了(第二次比较才会出结果)在此需要调整顺序,需要倒叙变为从大到小
						bTs = -1;
						i = -1;//因为结束当前次循环会i++
						bestW = 0;
						bestH = 0;
						continue;
					}else{
						bTs = 1;
						i = -1;//因为结束当前次循环会i++
						bestW = 0;
						bestH = 0;
						continue;
					}
				}
			}
			bestW = newW;
			bestH = newH;
		}

		if (bestW > 0 && bestH > 0) {
			return new Point(bestW, bestH);
		}
		return null;
	}

	 //得到比采集值小的的一个最合适的分辨率
	private static Point findBestPreviewSizeValue(
			CharSequence previewSizeValueString, Point screenResolution) {
		int bestX = 0;
		int bestY = 0;
		int diff = Integer.MAX_VALUE;
		for (String previewSize : COMMA_PATTERN.split(previewSizeValueString)) {

			previewSize = previewSize.trim();
			int dimPosition = previewSize.indexOf('x');
			if (dimPosition < 0) {
				Log.w(TAG, "Bad preview-size: " + previewSize);
				continue;
			}

			int newX;
			int newY;
			try {
				newX = Integer.parseInt(previewSize.substring(0, dimPosition));
				newY = Integer.parseInt(previewSize.substring(dimPosition + 1));
				if(newX < newY) {
				    continue;
                }
			} catch (NumberFormatException nfe) {
				Log.w(TAG, "Bad preview-size: " + previewSize);
				continue;
			}

			int newDiff = Math.abs(newX - screenResolution.x)
					+ Math.abs(newY - screenResolution.y);
			if (newDiff == 0) {
				bestX = newX;
				bestY = newY;
				break;
			} else if (newDiff < diff) {
				bestX = newX;
				bestY = newY;
				diff = newDiff;
			}
		}

		if (bestX > 0 && bestY > 0) {
			return new Point(bestX, bestY);
		}
		return null;
	}
  private static int findBestMotZoomValue(CharSequence stringValues, int tenDesiredZoom) {
    int tenBestValue = 0;
    for (String stringValue : COMMA_PATTERN.split(stringValues)) {
      stringValue = stringValue.trim();
      double value;
      try {
        value = Double.parseDouble(stringValue);
      } catch (NumberFormatException nfe) {
        return tenDesiredZoom;
      }
      int tenValue = (int) (10.0 * value);
      if (Math.abs(tenDesiredZoom - value) < Math.abs(tenDesiredZoom - tenBestValue)) {
        tenBestValue = tenValue;
      }
    }
    return tenBestValue;
  }

  private void setFlash(Camera.Parameters parameters) {
    // FIXME: This is a hack to turn the flash off on the Samsung Galaxy.
    // And this is a hack-hack to work around a different value on the Behold II
    // Restrict Behold II check to Cupcake, per Samsung's advice
    //if (Build.MODEL.contains("Behold II") &&
    //    CameraManager.SDK_INT == Build.VERSION_CODES.CUPCAKE) {
    if (Build.MODEL.contains("Behold II") && CameraManager.SDK_INT == 3) { // 3 = Cupcake
      parameters.set("flash-value", 1);
    } else {
      parameters.set("flash-value", 2);
    }
    // This is the standard setting to turn the flash off that all devices should honor.
    parameters.set("flash-mode", "off");
  }

  private void setZoom(Camera.Parameters parameters) {

    String zoomSupportedString = parameters.get("zoom-supported");
    if (zoomSupportedString != null && !Boolean.parseBoolean(zoomSupportedString)) {
      return;
    }

    int tenDesiredZoom = TEN_DESIRED_ZOOM;

    String maxZoomString = parameters.get("max-zoom");
    if (maxZoomString != null) {
      try {
        int tenMaxZoom = (int) (10.0 * Double.parseDouble(maxZoomString));
        if (tenDesiredZoom > tenMaxZoom) {
          tenDesiredZoom = tenMaxZoom;
        }
      } catch (NumberFormatException nfe) {
        Log.w(TAG, "Bad max-zoom: " + maxZoomString);
      }
    }

    String takingPictureZoomMaxString = parameters.get("taking-picture-zoom-max");
    if (takingPictureZoomMaxString != null) {
      try {
        int tenMaxZoom = Integer.parseInt(takingPictureZoomMaxString);
        if (tenDesiredZoom > tenMaxZoom) {
          tenDesiredZoom = tenMaxZoom;
        }
      } catch (NumberFormatException nfe) {
        Log.w(TAG, "Bad taking-picture-zoom-max: " + takingPictureZoomMaxString);
      }
    }

    String motZoomValuesString = parameters.get("mot-zoom-values");
    if (motZoomValuesString != null) {
      tenDesiredZoom = findBestMotZoomValue(motZoomValuesString, tenDesiredZoom);
    }

    String motZoomStepString = parameters.get("mot-zoom-step");
    if (motZoomStepString != null) {
      try {
        double motZoomStep = Double.parseDouble(motZoomStepString.trim());
        int tenZoomStep = (int) (10.0 * motZoomStep);
        if (tenZoomStep > 1) {
          tenDesiredZoom -= tenDesiredZoom % tenZoomStep;
        }
      } catch (NumberFormatException nfe) {
        // continue
      }
    }

    // Set zoom. This helps encourage the user to pull back.
    // Some devices like the Behold have a zoom parameter
    if (maxZoomString != null || motZoomValuesString != null) {
      parameters.set("zoom", String.valueOf(tenDesiredZoom / 10.0));
    }

    // Most devices, like the Hero, appear to expose this zoom parameter.
    // It takes on values like "27" which appears to mean 2.7x zoom
    if (takingPictureZoomMaxString != null) {
      parameters.set("taking-picture-zoom", tenDesiredZoom);
    }
  }

  
  private void setSharpness(Camera.Parameters parameters) {

    int desiredSharpness = DESIRED_SHARPNESS;

    String maxSharpnessString = parameters.get("sharpness-max");
    if (maxSharpnessString != null) {
      try {
        int maxSharpness = Integer.parseInt(maxSharpnessString);
        if (desiredSharpness > maxSharpness) {
          desiredSharpness = maxSharpness;
        }
      } catch (NumberFormatException nfe) {
        Log.w(TAG, "Bad sharpness-max: " + maxSharpnessString);
      }
    }

    parameters.set("sharpness", desiredSharpness);
  }
   
}
