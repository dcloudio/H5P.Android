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

package io.dcloud.feature.barcode.decoding;

import io.dcloud.feature.barcode.camera.CameraManager;
import io.dcloud.feature.barcode.view.ViewfinderResultPointCallback;

import java.util.Vector;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.dcloud.zxing.BarcodeFormat;
import com.dcloud.zxing.Result;

/**
 * This class handles all the messaging which comprises the state machine for capture.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 */
public final class CaptureActivityHandler extends Handler {

  private static final String TAG = CaptureActivityHandler.class.getSimpleName();

  private final IBarHandler activity;
  private final DecodeThread decodeThread;
  private State state;

  private enum State {
    PREVIEW,
    SUCCESS,
    DONE
  }

  public CaptureActivityHandler(IBarHandler activity, Vector<BarcodeFormat> decodeFormats,
      String characterSet) {
    this.activity = activity;
    decodeThread = new DecodeThread(activity, decodeFormats, characterSet,
        new ViewfinderResultPointCallback(activity.getViewfinderView()));
    decodeThread.start();
    state = State.SUCCESS;
    resume();
  }
  /**
   * 相机预览、绘制探测区域
   * 
   * <br/>Create By: yanglei Email:yanglei@dcloud.io at 2013-6-5 下午04:32:58
   */
  public void resume(){
	  // Start ourselves capturing previews and decoding.
	    CameraManager.get().startPreview();
	    activity.drawViewfinder();
	    restartPreviewAndDecode();
  }
  @Override
  public void handleMessage(Message message) {
    switch (message.what) {
      case CODE_AUTO_FOCUS:
        Log.d(TAG, "Got auto-focus message");
        // When one auto focus pass finishes, start another. This is the closest thing to
        // continuous AF. It does seem to hunt a bit, but I'm not sure what else to do.
        if (state == State.PREVIEW) {
          CameraManager.get().requestAutoFocus(this, CODE_AUTO_FOCUS);
        }
        break;
      
      case CODE_DECODE_SUCCEEDED:
        Log.d(TAG, "Got decode succeeded message");
        state = State.SUCCESS;
        Bundle bundle = message.getData();
        Bitmap barcode = bundle == null ? null :
            (Bitmap) bundle.getParcelable(DecodeThread.BARCODE_BITMAP);
        activity.handleDecode((Result) message.obj, barcode);
        if(barcode != null){
        	barcode.recycle();
        	System.out.println("barcode.recycle");
        }
        break;
      case CODE_DECODE_FAILED:
        // We're decoding as fast as possible, so when one decode fails, start another.
    	Log.d(TAG, "CODE_DECODE_FAILED");
        state = State.PREVIEW;
        CameraManager.get().requestPreviewFrame(activity,decodeThread.getHandler(), CODE_DECODE);
        break;
    }
  }

  public void quitSynchronously() {
    state = State.DONE;
    CameraManager.get().stopPreview();
    Message quit = Message.obtain(decodeThread.getHandler(), CODE_QUIT);
    quit.sendToTarget();
    try {
      decodeThread.join();
    } catch (InterruptedException e) {
      // continue
    }

    // Be absolutely sure we don't send any queued up messages
    stopDecode();
  }
  /**启动扫描*/
  public void restartPreviewAndDecode() {
    if (state == State.SUCCESS) {
      state = State.PREVIEW;
      CameraManager.get().requestPreviewFrame(activity,decodeThread.getHandler(), CODE_DECODE);
      autoFocus();
    }
  }
  
  public void autoFocus(){
	  CameraManager.get().requestAutoFocus(this, CODE_AUTO_FOCUS);
  }
  /**停止扫描*/
  public void stopDecode(){
	  removeMessages(CODE_DECODE_SUCCEEDED);
	  removeMessages(CODE_DECODE_FAILED);
	  CameraManager.get().removeAutoFocus();
	  state = State.SUCCESS;
  }
  public static final int CODE_AUTO_FOCUS = 1000;
  public static final int CODE_DECODE_FAILED = 1001;
  public static final int CODE_DECODE_SUCCEEDED = 1002;
  public static final int CODE_QUIT = 1003;
  public static final int CODE_DECODE = 1004;
  
  public static Result decode(Bitmap map){
	  return DecodeHandler.decode(map);
  }
}
