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

import io.dcloud.feature.barcode.decoding.IBarHandler;
import android.graphics.Point;
import android.hardware.Camera;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

final class PreviewCallback implements Camera.PreviewCallback {

  private static final String TAG = PreviewCallback.class.getSimpleName();

  private final CameraConfigurationManager configManager;
  private final boolean useOneShotPreviewCallback;
  private Handler previewHandler;
  private IBarHandler mBarHandler;
  /**摄像头预览到数据，回传数据时使用的message.what*/
  private int previewMessage;
  
  PreviewCallback(CameraConfigurationManager configManager, boolean useOneShotPreviewCallback) {
    this.configManager = configManager;
    this.useOneShotPreviewCallback = useOneShotPreviewCallback;
  }

  void setHandler(IBarHandler barHandler,Handler previewHandler, int previewMessage) {
	mBarHandler = barHandler;
    this.previewHandler = previewHandler;
    this.previewMessage = previewMessage;
  }

	public void onPreviewFrame(byte[] data, Camera camera) {
		if (mBarHandler != null && mBarHandler.isRunning()) {
			Point cameraResolution = configManager.getCameraResolution();
			if (!useOneShotPreviewCallback) {
				camera.setPreviewCallback(null);
			}
			if (previewHandler != null) {
				Message message = previewHandler.obtainMessage(previewMessage,
						cameraResolution.x, cameraResolution.y, data);
				message.sendToTarget();
				previewHandler = null;
			} else {
				Log.d(TAG, "Got preview callback, but no handler for it");
			}
			lastBitmapData=data;
		}
	}
	private byte[] lastBitmapData=null;
	public byte[] getLastBitmapData(){
		return lastBitmapData;
	}
	public void setLastBitmapData(byte[] data){
		this.lastBitmapData=data;
	}
}
