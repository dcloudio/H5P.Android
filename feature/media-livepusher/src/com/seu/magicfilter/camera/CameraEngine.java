package com.seu.magicfilter.camera;

import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.CameraInfo;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.util.Log;

import com.seu.magicfilter.camera.utils.CameraUtils;

import java.io.IOException;
import java.util.List;

public class CameraEngine {
    private static final String TAG = "CameraEngine";
    private static Camera camera = null;
    private static int cameraID = 0;
    private static SurfaceTexture surfaceTexture;

    public static Camera getCamera() {
        return camera;
    }

    public static boolean openCamera() {
        if (camera == null) {
            try {
                camera = Camera.open(cameraID);
                setDefaultParameters();
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        }
        return false;
    }

    public static boolean openCamera(int id) {
        if (camera == null) {
            try {
                camera = Camera.open(id);
                cameraID = id;
                setDefaultParameters();
                return true;
            } catch (RuntimeException e) {
                return false;
            }
        }
        return false;
    }

    public static int getCameraID(){
        if (camera != null) {
            return cameraID;
        } else {
            return -1;
        }
    }

    public static void releaseCamera() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }

    public void resumeCamera() {
        openCamera();
    }

    public void setParameters(Parameters parameters) {
        camera.setParameters(parameters);
    }

    public Parameters getParameters() {
        if (camera != null)
            camera.getParameters();
        return null;
    }

    public static void switchCamera() {
        releaseCamera();
        cameraID = cameraID == 0 ? 1 : 0;
        openCamera(cameraID);
        startPreview(surfaceTexture);
    }

    public static void switchCamera2(int incameraID){
        releaseCamera();
        cameraID = incameraID;
        openCamera(cameraID);
        startPreview(surfaceTexture);
    }

    public static void switchFlashlight() {
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            String mode = parameters.getFlashMode();
            if (mode != null) {
                if (mode.equals(Parameters.FLASH_MODE_OFF)) {
                    List<String> modes = parameters.getSupportedFlashModes();
                    if (modes != null) {
                        if (modes.contains(Parameters.FLASH_MODE_TORCH)) {
                            parameters.setFlashMode(Parameters.FLASH_MODE_TORCH);
                        }
                    }
                } else {
                    List<String> modes = parameters.getSupportedFlashModes();
                    if (modes != null) {
                        if (modes.contains(Parameters.FLASH_MODE_OFF)) {
                            parameters.setFlashMode(Parameters.FLASH_MODE_OFF);
                        }
                    }
                }
                camera.setParameters(parameters);
            } else {
                Log.e(TAG, "The device does not support control of a flashlight!");
            }
        }
    }

    private static void setDefaultParameters() {
        Parameters parameters = camera.getParameters();
        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }

        Size previewSize = CameraUtils.getLargePreviewSize(camera);
        parameters.setPreviewSize(previewSize.width, previewSize.height);
//        parameters.setPreviewSize(MagicParams.WIDTH, MagicParams.HEIGHT);
//        parameters.setPictureSize(MagicParams.WIDTH, MagicParams.HEIGHT);
        Size pictureSize = CameraUtils.getLargePictureSize(camera);
        parameters.setPictureSize(pictureSize.width, pictureSize.height);
//        parameters.setPictureSize(MagicParams.WIDTH, MagicParams.HEIGHT);
//        parameters.setRotation(360);

        camera.cancelAutoFocus();
        camera.setParameters(parameters);
    }


    private static void setPerviewRotation(int rotation){
        Parameters parameters = camera.getParameters();
        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        parameters.setRotation(rotation);
        camera.cancelAutoFocus();
        camera.setParameters(parameters);
    }

    private static Size getPreviewSize() {
        return camera.getParameters().getPreviewSize();
    }

    private static Size getPictureSize() {
        return camera.getParameters().getPictureSize();
    }

    public static void startPreview(SurfaceTexture surfaceTexture) {
        if (camera != null)
            try {
                camera.setPreviewTexture(surfaceTexture);
                CameraEngine.surfaceTexture = surfaceTexture;
                camera.startPreview();
            } catch (IOException e) {
                e.printStackTrace();
            }
    }

    public static void startPreview() {
        if (camera != null)
            camera.startPreview();
    }

    public static void stopPreview() {
        if (camera != null)
            camera.stopPreview();
    }

    public static void setRotation(int rotation) {
        if (camera == null)
            return;
        Camera.Parameters params = camera.getParameters();
        params.setRotation(rotation);
        camera.setParameters(params);
    }

    public static void takePicture(Camera.ShutterCallback shutterCallback, Camera.PictureCallback rawCallback,
                                   Camera.PictureCallback jpegCallback) {
        camera.takePicture(shutterCallback, rawCallback, jpegCallback);
    }

    public static com.seu.magicfilter.camera.utils.CameraInfo getCameraInfo() {
        com.seu.magicfilter.camera.utils.CameraInfo info = new com.seu.magicfilter.camera.utils.CameraInfo();
        Size size = getPreviewSize();
        Log.d("getPreviewSize:", size.width + "::" + size.height);

        CameraInfo cameraInfo = new CameraInfo();
        Camera.getCameraInfo(cameraID, cameraInfo);

        info.orientation = cameraInfo.orientation;
        info.isFront = cameraID == 1 ? true : false;

        /*if (info.orientation == 90 || info.orientation == 270) {
            info.previewWidth = size.height;
            info.previewHeight = size.width;
        } else {
            info.previewWidth = size.width;
            info.previewHeight = size.height;
        }*/
        info.previewWidth = size.width;
        info.previewHeight = size.height;

        size = getPictureSize();

        Log.d("getPictureSize:", size.width + "::" + size.height);
        info.pictureWidth = size.width;
        info.pictureHeight = size.height;
        return info;
    }

    public static void focusOnTouch() {

        if (camera != null) {
            Parameters parameters = camera.getParameters();
            if (!parameters.getFocusMode().equals(Parameters.FOCUS_MODE_AUTO) &&
                    parameters.getSupportedFocusModes() != null &&
                    parameters.getSupportedFocusModes().contains(Parameters.FOCUS_MODE_AUTO)) {
                parameters.setFocusMode(Parameters.FOCUS_MODE_AUTO);
                camera.setParameters(parameters);
            }

            camera.cancelAutoFocus();
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    if (success) {
                        Log.e(TAG, "auto focus success");
                    } else {
                        Log.e(TAG, "auto focus failed");
                    }

                    //resume the continuous focus
                    Parameters parameters = camera.getParameters();
                    camera.cancelAutoFocus();
                    if (parameters.getFocusMode() != Parameters.FOCUS_MODE_CONTINUOUS_PICTURE &&
                            parameters.getSupportedFocusModes() != null &&
                            parameters.getSupportedFocusModes().contains(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
                        parameters.setFocusMode(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
                        camera.setParameters(parameters);
                    }
                }
            });
        }
    }
}