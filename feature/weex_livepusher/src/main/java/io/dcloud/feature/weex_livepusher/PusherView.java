package io.dcloud.feature.weex_livepusher;

import android.content.Context;
import android.widget.RelativeLayout;

//import com.seu.magicfilter.MagicEngine;
//import com.seu.magicfilter.camera.CameraEngine;
//import com.seu.magicfilter.filter.helper.MagicFilterType;
//import com.seu.magicfilter.utils.MagicParams;
//import com.seu.magicfilter.widget.MagicCameraView;
//import net.ossrs.yasea.rtmp.RtmpPublisher;

public class PusherView extends RelativeLayout /*implements RtmpPublisher.EventHandler*/ {
    public PusherView(Context context) {
        super(context);
    }
//    private MagicCameraView cameraView;
//    private List<String> events = new ArrayList<>();
//    private WXComponent component;
//    private MagicEngine.Builder builder;
//    MagicEngine magicEngine;
//    private String mSrc;
//    Context context;
//
//    public PusherView(Context context, WXComponent component) {
//        super(context);
//        this.context = context;
//        this.component = component;
//        builder = new MagicEngine.Builder();
//    }
//
//    public void init() {
//        cameraView = new MagicCameraView(context, null);
//        magicEngine = builder.build(cameraView);
//        magicEngine.setEventHandler(this);
//        CameraEngine.openCamera();
//        addView(cameraView, new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
//    }
//
//    public void onLayoutFinish() {
//        builder.setVideoHeight(cameraView.getHeight());
//        builder.setVideoWidth(cameraView.getWidth());
//    }
//
//    public void enableCamera(boolean enable) {
////        magicEngine.
//    }
//
//    /**
//     * 静音
//     *
//     * @param muted
//     */
//    public void setMuted(boolean muted) {
//        if (magicEngine == null) return;
//        magicEngine.setSilence(muted);
//    }
//
//    /**
//     * 切换摄像头
//     */
//    public void switchCamera() {
//        if (magicEngine != null) magicEngine.switchCamera();
//    }
//
//    /**
//     * 开启闪光灯
//     *
//     * @param islight
//     */
//    public void setlight(boolean islight) {
//        if (magicEngine != null) magicEngine.switchFlashlight();
//    }
//
//    /**
//     * 开启美颜
//     *
//     * @param level
//     */
//    public void setBeauty(int level) {
//        if (magicEngine != null) {
//            if (level == 1) {
//                magicEngine.setFilter(MagicFilterType.WHITECAT);
//            } else {
//                magicEngine.setFilter(MagicFilterType.NONE);
//            }
//        }
//    }
//
//    /**
//     * 开启美白
//     *
//     * @param whiteness
//     */
//    public void setWhiteness(int whiteness) {
//        if (magicEngine != null) magicEngine.setBeautyLevel(whiteness);
//    }
//
//    public void setSrc(String src) {
//        this.mSrc = src;
//        MagicParams.videoPath = this.mSrc;
//    }
//
//    /**
//     * 自动对焦
//     *
//     * @param autofocus
//     */
//    public void autoFocus(boolean autofocus) {
//        OnTouchListener listener = null;
//        if (autofocus) {
//            listener = new OnTouchListener() {
//                @Override
//                public boolean onTouch(View v, MotionEvent event) {
//                    magicEngine.focusOnTouch();
//                    return false;
//                }
//            };
//        }
//        if (cameraView != null)
//        cameraView.setOnTouchListener(listener);
//    }
//
//    public void setminBitrate(int minBitrate) {
//        // 不支持
//    }
//
//    public void setmaxBitrate(int maxBitrate) {
//        // 不支持
//    }
//
//    /**
//     * 设置宽高比
//     */
//    String aspect = "9:16";
//    public void setAspect(String type) {
//        this.aspect = type;
//        if (!aspect.equals("9:16") && !aspect.equals("3:4")) {
//            aspect = "9:16";
//        }
//    }
//
//    public void setMode(String mode) {
//        switch (mode) {
//            case "SD":
//                break;
//            case "HD":
//                break;
//            case "FHD":
//                break;
//            case "RTC":
//                break;
//        }
//    }
//
//    public void snapshot(final String path, final JSCallback callback) {
//        /**
//         * 路径转化
//         */
//        CameraEngine.takePicture(new Camera.ShutterCallback() {
//            @Override
//            public void onShutter() {
//
//            }
//        }, new Camera.PictureCallback() {
//            @Override
//            public void onPictureTaken(byte[] data, Camera camera) {
//
//            }
//        }, new Camera.PictureCallback() {
//            @Override
//            public void onPictureTaken(byte[] data, Camera camera) {
//                if (data != null) {
//                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
//                    if (bitmap != null) {
//                        bitmap = rotateBitmap(bitmap);
//                        try {
//                            File filePic = new File(path);
//                            if (!filePic.exists()) {
//                                filePic.getParentFile().mkdirs();
//                                filePic.createNewFile();
//                            }
//                            FileOutputStream fos = new FileOutputStream(filePic);
//                            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
//                            fos.flush();
//                            fos.close();
//                            Map<String, Object> values = new HashMap<>();
//                            values.put("width", bitmap.getWidth());
//                            values.put("height", bitmap.getHeight());
//                            values.put("tempImagePath", filePic.getAbsolutePath());
//                            callback.invoke(values);
//                        } catch (IOException ignored) {
//                        }
//                    } else {
//                        Map<String, Object> values = new HashMap<>();
//                        values.put("code", 2);
//                        values.put("message", "数据截图转换失败");
//                        callback.invoke(values);
//                    }
//                } else {
//                    Map<String, Object> values = new HashMap<>();
//                    values.put("code", 1);
//                    values.put("message", "无法获取截图");
//                    callback.invoke(values);
//                }
//            }
//        });
//    }
//
//    private int oritation = 0;
//
//    public Bitmap rotateBitmap(Bitmap bitmap) {
//        int id = CameraEngine.getCameraID();
//        if (id == 0) {
//            oritation -= 180;
//        }
//        if (oritation == 0 || null == bitmap) {
//            return bitmap;
//        }
//        Matrix matrix = new Matrix();
//        matrix.setRotate(oritation, ((float) bitmap.getWidth() / 2), ((float) bitmap.getHeight() / 2));
//        Bitmap bmp = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
//        bitmap.recycle();
//        return bmp;
//    }
//
//    public void addEvent(String type) {
//        if (events != null)
//            events.add(type);
//    }
//
//    public void fireEvent(String type, Map<String, Object> values) {
//        if (events.contains(type))
//            component.fireEvent(type, values);
//    }
//
//    public void destory() {
//        if (magicEngine != null) {
//            magicEngine.stopRecord();
//        }
//    }
//
//
//    private void statusChangeEvent(int code) {
//        Map<String, Object> values = new HashMap<>();
//        values.put("code", code);
//        fireEvent("statechange", values);
//    }
//
//    @Override
//    public void onRtmpConnecting(String msg) {
//        statusChangeEvent(1001);
//    }
//
//    @Override
//    public void onRtmpConnected(String msg) {
//        statusChangeEvent(1002);
//    }
//
//    @Override
//    public void onRtmpVideoStreaming(String msg) {
//
//    }
//
//    @Override
//    public void onRtmpAudioStreaming(String msg) {
//
//    }
//
//    @Override
//    public void onRtmpStopped(String msg) {
//        statusChangeEvent(10010);
//    }
//
//    @Override
//    public void onRtmpDisconnected(String msg) {
//        statusChangeEvent(1009);
//    }
//
//    @Override
//    public void onRtmpOutputFps(double fps) {
//
//    }
//
//    @Override
//    public void onRtmpDataInfo(int bitrate, long totalSize) {
//
//    }
//
//    @Override
//    public void onNetWorkError(Exception e, int tag) {
//        statusChangeEvent(tag);
////        Map<String,Object> values = new HashMap<>();
////        values.put("code",tag);
////        values.put("message",e.getMessage());
////        fireEvent("error",values);
//
//    }
}
