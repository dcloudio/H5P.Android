package io.dcloud.feature.device;

import android.Manifest;
import android.app.Activity;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.Vibrator;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.Window;
import android.view.WindowManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IApp;
import io.dcloud.common.DHInterface.IFeature;
import io.dcloud.common.DHInterface.ISysEventListener;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.DeviceInfo;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.adapter.util.PermissionUtil;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.StringUtil;
import io.dcloud.common.util.TelephonyUtil;

/**
 *
 * <p>
 * Description:系统的功能的处理
 * </p>
 *
 * @version 1.0
 * @author cuidengfeng Email:cuidengfeng@dcloud.io
 * @Date 2013-3-11 下午2:54:58 created.
 *
 * <pre>
 * <p>ModifiedLog:</p>
 * Log ID: 1.0 (Log编号 依次递增)
 * Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-3-11 下午2:54:58
 * </pre>
 */
public class DeviceFeatureImpl implements IFeature, ISysEventListener {
    private SensorManager mSensorManager;
    private WakeLock wakeLock = null;
    private Sensor mSensor;
    private boolean mRegister = false;

    private Context mContext;

    @Override
    public String execute(final IWebview pWebViewImpl, String pActionName, final String[] pJsArgs) {
        switch(pActionName) {
            case "getCurrentType": {
                return DeviceInfo.getNetWorkType();
            }
            case "unlockOrientation": {
                pWebViewImpl.obtainApp().setRequestedOrientation(null);
                break;
            }
            case "lockOrientation": {
                String type = pJsArgs[0];
                pWebViewImpl.obtainApp().setRequestedOrientation(type);
                break;
            }
            case "dial": {
                final boolean cf = PdrUtil.parseBoolean(pJsArgs[1], true, false);
                PermissionUtil.usePermission(pWebViewImpl.getActivity(), false, PermissionUtil.PMS_PHONE, new PermissionUtil.StreamPermissionRequest(pWebViewImpl.obtainApp()) {

                    @Override
                    public void onGranted(String streamPerName) {
                        dial(pWebViewImpl, pJsArgs[0], cf);
                    }

                    @Override
                    public void onDenied(String streamPerName) {

                    }
                });

                break;
            }
            case "beep": {
                ToneGenerator toneGenerator = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, ToneGenerator.MAX_VOLUME);
                int _times = 1;
                try {
                    _times = Integer.parseInt(pJsArgs[0]);
                    if (_times <= 0) {
                        _times = 1;
                    }
                } catch (NumberFormatException e1) {
                    e1.printStackTrace();
                }
                for (int i = 0; i < _times; i++) {
                    toneGenerator.startTone(ToneGenerator.TONE_CDMA_ONE_MIN_BEEP);
                    try {
                        Thread.sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            }
            case "setWakelock": {
                boolean lock = false;
                lock = PdrUtil.parseBoolean(pJsArgs[0], lock, false);
                if (lock) {//屏幕常亮
                    wakeLock.acquire();
                } else {//屏幕伴随系统
                    wakeLock.release();
                }
                break;
            }
            case "isWakelock": {
                boolean held = wakeLock.isHeld();
                return JSUtil.wrapJsVar(String.valueOf(held), false);
            }
            case "__isWakelockNative__" : {
                return wakeLock.isHeld() + "";
            }
            case "vibrate": {
                long _milliseconds = 500;
                try {
                    _milliseconds = Long.parseLong(pJsArgs[0]);
                    if (_milliseconds <= 0) {
                        _milliseconds = 500;
                    }
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                Vibrator _vibrator = (Vibrator) mContext.getSystemService(Context.VIBRATOR_SERVICE);
                _vibrator.vibrate(_milliseconds);
                break;
            }
            case "setVolume": {
                float f = Float.parseFloat(pJsArgs[0]);
                AudioManager am = (AudioManager) pWebViewImpl.getContext().getSystemService(Context.AUDIO_SERVICE);
                int index = convertVolume(f);
                am.setStreamVolume(AudioManager.STREAM_ALARM, index, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                am.setStreamVolume(AudioManager.STREAM_DTMF, index, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                am.setStreamVolume(AudioManager.STREAM_MUSIC, index, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                am.setStreamVolume(AudioManager.STREAM_NOTIFICATION, index, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                am.setStreamVolume(AudioManager.STREAM_RING, index, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                am.setStreamVolume(AudioManager.STREAM_SYSTEM, index, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                am.setStreamVolume(AudioManager.STREAM_VOICE_CALL, index, AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE);
                break;
            }
            case "getVolume": {
                AudioManager am = (AudioManager) pWebViewImpl.getContext().getSystemService(Context.AUDIO_SERVICE);
                float i = am.getStreamVolume(AudioManager.STREAM_MUSIC);
                return JSUtil.wrapJsVar(String.valueOf(i / maxVolume), false);
            }
            case "s.resolutionHeight": {
                IApp app = pWebViewImpl.obtainApp();
                float scale = pWebViewImpl.getScale();
                float sScreenAllHeight = app.getInt(IApp.SCREEN_ALL_HEIGHT) / scale;
                return JSUtil.wrapJsVar(String.valueOf(sScreenAllHeight), false);
            }
            case "s.resolutionWidth": {
                IApp app = pWebViewImpl.obtainApp();
                float scale = pWebViewImpl.getScale();
                float sScreenWidth = app.getInt(IApp.SCREEN_WIDTH);
                return JSUtil.wrapJsVar(String.valueOf(sScreenWidth / scale), false);
            }
            case "d.resolutionHeight": {
                IApp app = pWebViewImpl.obtainApp();
                float scale = pWebViewImpl.getScale();
                int sScreenHeight = app.getInt(IApp.SCREEN_HEIGHT);
                return JSUtil.wrapJsVar(String.valueOf(sScreenHeight / scale), false);
            }
            case "d.resolutionWidth": {
                IApp app = pWebViewImpl.obtainApp();
                float scale = pWebViewImpl.getScale();
                int sScreenWidth = app.getInt(IApp.SCREEN_WIDTH);
                return JSUtil.wrapJsVar(String.valueOf(sScreenWidth / scale), false);
            }
            case "setBrightness": {
                final float f = Float.parseFloat(pJsArgs[0]);
                pWebViewImpl.obtainWindowView().post(new Runnable() {
                    @Override
                    public void run() {
                        setScreenBrightness(pWebViewImpl, f);
                    }
                });
                break;
            }
            case "getBrightness": {
                float i = getScreenBrightness(pWebViewImpl.getActivity());
                return JSUtil.wrapJsVar(String.valueOf(i / MAX_BRIGHTNESS), false);
            }
            case "getCurrentAPN" : {
                String type = DeviceInfo.getCurrentAPN();
                if (TextUtils.isEmpty(type)) {
                    return null;
                }
                String returnValue = "{name:" + type + "}";
                JSONObject valueObject;
                try {
                    valueObject = new JSONObject(returnValue);
                    return JSUtil.wrapJsVar(valueObject);
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                return null;
            }
            case "getInfo":
                final String callbackId = pJsArgs[0];
                PermissionUtil.usePermission(pWebViewImpl.getActivity(), false, Manifest.permission.READ_PHONE_STATE, new PermissionUtil.Request() {
                    @Override
                    public void onGranted(String streamPerName) {
                        DeviceInfo.updateIMEI();
                        DeviceInfo.getUpdateIMSI();
                        String uuid = TextUtils.isEmpty(DeviceInfo.sIMEI) ? TelephonyUtil.getIMEI(pWebViewImpl.getContext(),false, true) : DeviceInfo.sIMEI;
                        String object = StringUtil.format("{'imei':'%s','imsi':['%s'],'uuid':'%s'}",DeviceInfo.sIMEI,DeviceInfo.sIMSI, uuid);
                        JSUtil.execCallback(pWebViewImpl,callbackId,object,JSUtil.OK,true,false);
                    }

			        @Override
			        public void onDenied(String streamPerName) {
                        String json = "{'imei':'','imsi':[],'uuid':'"+ TelephonyUtil.getIMEI(pWebViewImpl.getContext(),false, true)+"'}";
                        JSUtil.execCallback(pWebViewImpl,callbackId,json,JSUtil.ERROR,true,false);
			        }
		        });
                break;
        }
        return null;
    }

    static final int MAX_BRIGHTNESS = 255;

    /**
     * 获得当前屏幕亮度值 0--255
     */
    private int getScreenBrightness(Activity actvity) {
        float screenBrightness = 125;
        Window localWindow = actvity.getWindow();
        WindowManager.LayoutParams localLayoutParams = localWindow.getAttributes();
        screenBrightness = localLayoutParams.screenBrightness * 255;
        if (screenBrightness < 0) {//先使用当前activity的亮度，
            try {
                if (Build.VERSION.SDK_INT > 17) {
                    screenBrightness = Settings.Global.getInt(actvity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
                } else {
                    screenBrightness = Settings.System.getInt(actvity.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);// == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC;
                }
            } catch (Exception localException) {
            }
        }
        if (screenBrightness < 0) {
            screenBrightness = 125;
        }
        try {
            screenBrightness = Settings.System.getInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS);
        } catch (Exception localException) {
        }
        return (int) screenBrightness;
    }

    /**
     * 保存当前的屏幕亮度值，并使之生效
     */
    private void setScreenBrightness(final IWebview webview, final float f) {
        PermissionUtil.useSystemPermission(webview.getActivity(), "android.permission.WRITE_SETTINGS", new PermissionUtil.Request() {
            @Override
            public void onGranted(String streamPerName) {
                doScreenBrightness(webview, f);
            }

            @Override
            public void onDenied(String streamPerName) {
                if (Build.VERSION.SDK_INT >= 23) {
                    boolean type = false;
                    try {
                        Class clazz = Class.forName("android.provider.Settings$System");
                        Method method = clazz.getDeclaredMethod("canWrite",Context.class);
                        type = (Boolean) method.invoke(null,webview.getContext());
                    } catch (Exception e) {
                    }
                    if (type) {
                        doScreenBrightness(webview, f);
                    } else {
                        Intent intent = new Intent("android.settings.action.MANAGE_WRITE_SETTINGS");
                        intent.setData(Uri.parse("package:" + webview.getActivity().getPackageName()));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        webview.getActivity().startActivityForResult(intent, 0);
                    }
                } else {
                    doScreenBrightness(webview, f);
                }
            }
        });
    }

    private void doScreenBrightness(IWebview webview, float f) {
        Window localWindow = webview.getActivity().getWindow();
        WindowManager.LayoutParams localLayoutParams = localWindow.getAttributes();
        localLayoutParams.screenBrightness = f;
        localWindow.setAttributes(localLayoutParams);
        Settings.System.putInt(mContext.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, (int) (f * MAX_BRIGHTNESS));
    }

    int maxVolume = -1;

    /**
	 *
     * @param f
     * @return
     */
    private int convertVolume(float f) {
        if (f > 1 || f < 0) return 0;
        int i = (int) (f * maxVolume);
        return i;
    }

    /**
	 *
     * Description:打电话
     *
	 * @param pPhoneCode
	 *
	 *            <pre>
     *                   <p>ModifiedLog:</p>
     *                   Log ID: 1.0 (Log编号 依次递增)
     *                   Modified By: cuidengfeng Email:cuidengfeng@dcloud.io at 2013-1-8 下午5:59:23
     *                   </pre>
     */
    protected void dial(IWebview pWebview, String pPhoneCode, boolean confirm) {
        Uri _uri = Uri.parse("tel:" + pPhoneCode);
        String action = Intent.ACTION_DIAL;
        if (!confirm) {
            action = Intent.ACTION_CALL;
        }
        Intent _intent = new Intent(action, _uri);
        pWebview.getActivity().startActivity(_intent);
    }

    @Override
    public void init(AbsMgr pFeatureMgr, String pFeatureName) {
        mSensorManager = (SensorManager) pFeatureMgr.getContext()
                .getSystemService(Context.SENSOR_SERVICE);
        mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        mContext = pFeatureMgr.getContext();
        PowerManager powerManager = (PowerManager) mContext
                .getSystemService(Service.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK,
                "My Lock");
        wakeLock.setReferenceCounted(false); // 是否需计算锁的数量,在此不需要计数

        AudioManager am = (AudioManager) pFeatureMgr.getContext().getSystemService(Context.AUDIO_SERVICE);
        maxVolume = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
    }

    @Override
    public boolean onExecute(SysEventType pEventType, Object pArgs) {
        if (pEventType == SysEventType.onResume) {
            mSensorManager.registerListener(mListener, mSensor,
                    SensorManager.SENSOR_DELAY_FASTEST);
        } else if (pEventType == SysEventType.onStop) {
            mSensorManager.unregisterListener(mListener);
        }
        return false;
    }

    private final SensorEventListener mListener = new SensorEventListener() {

        private final float[] mScale = new float[]{2, 2.5f, 0.5f}; // accel
        private float[] mPrev = new float[3];
        private long mLastGestureTime;

        public void onSensorChanged(SensorEvent event) {
            boolean show = false;
            float[] diff = new float[3];

            for (int i = 0; i < 3; i++) {
                diff[i] = Math.round(mScale[i] * (event.values[i] - mPrev[i])
                        * 0.45f);
                if (Math.abs(diff[i]) > 0) {
                    show = true;
                }
                mPrev[i] = event.values[i];
            }

            if (show) {
                // only shows if we think the delta is big enough, in an attempt
                // to detect "serious" moves left/right or up/down
                Logger.i("sensorChanged " + event.sensor.getName() + " ("
                        + event.values[0] + ", " + event.values[1] + ", "
                        + event.values[2] + ")" + " diff(" + diff[0] + " "
                        + diff[1] + " " + diff[2] + ")");
            }

            long now = android.os.SystemClock.uptimeMillis();
            if (now - mLastGestureTime > 1000) {
                mLastGestureTime = 0;

                float x = diff[0];
                float y = diff[1];
                boolean gestX = Math.abs(x) > 3;
                boolean gestY = Math.abs(y) > 3;

                if ((gestX || gestY) && !(gestX && gestY)) {
                    if (gestX) {
                        if (x < 0) {
                            Logger.i("test", "<<<<<<<< LEFT <<<<<<<<<<<<");
                        } else {
                            Logger.i("test", ">>>>>>>>> RITE >>>>>>>>>>>");
                        }
                    } else {
                        if (y < -2) {
                            Logger.i("test", "<<<<<<<< UP <<<<<<<<<<<<");
                        } else {
                            Logger.i("test", ">>>>>>>>> DOWN >>>>>>>>>>>");
                        }
                    }
                    mLastGestureTime = now;
                }
            }
        }

        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    @Override
    public void dispose(String pAppid) {
    }
}
