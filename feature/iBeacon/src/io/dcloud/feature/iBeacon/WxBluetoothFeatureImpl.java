package io.dcloud.feature.iBeacon;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.StandardFeature;
import io.dcloud.common.adapter.util.PermissionUtil;
import io.dcloud.common.util.JSUtil;

public class WxBluetoothFeatureImpl extends StandardFeature {

    private final static String TAG = WxBluetoothFeatureImpl.class.getSimpleName();


    private IBeaconContainer iBeaconContainer = new IBeaconContainer();
    private boolean mBeaconScanFlag = false;//扫描标志

    private final static int MSG_BEACON_SCAN_UPDATE_NOTIFY = 1;
    private final static int MSG_BEACON_SCAN_RESTART_NOTIFY = 2;
    private long beaconScanUpdateMillis = 1000;
    private long beaconScanRestartMillis = 15000;
    private Handler handler = new Handler() {
        @TargetApi(18)
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case MSG_BEACON_SCAN_UPDATE_NOTIFY: {
                    processBeaconRecords();
                    startBeaconScanNotifyCheck();
                    break;
                }

                case MSG_BEACON_SCAN_RESTART_NOTIFY: {
                    if (mBeaconScanFlag) {
                        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
                        if (bluetoothAdapter != null && bluetoothAdapter.isEnabled()) {
                            bluetoothAdapter.stopLeScan(leScanCallback);
                            bluetoothAdapter.stopLeScan(leScanCallback);
                        }
                    }
                    startBeaconScanRestartNotify();
                    break;
                }
            }
        }
    };

    private SensorManager sm = null;
    private Sensor orientationSensor = null;
    private Sensor accelerometerSensor = null;
    private Sensor magneticSensor = null;

    private float[] accelerometerValues = null;
    private float[] magneticValues = null;


    private float[] rotateValues = new float[9];
    private float[] resultValues = new float[3];

    private double rotateDegree = 0;

    private IWebview bindWebview = null;
    private String bindCallbackID = null;
    private Map<String,IWebview> updateListener= null;

    private void startBeaconScanNotifyCheck() {
        handler.removeMessages(MSG_BEACON_SCAN_UPDATE_NOTIFY);
        handler.sendEmptyMessageDelayed(MSG_BEACON_SCAN_UPDATE_NOTIFY, beaconScanUpdateMillis);
    }

    private void stopBeaconScanNotifyCheck() {
        handler.removeMessages(MSG_BEACON_SCAN_UPDATE_NOTIFY);
    }

    private void startBeaconScanRestartNotify() {
        handler.removeMessages(MSG_BEACON_SCAN_RESTART_NOTIFY);
        handler.sendEmptyMessageDelayed(MSG_BEACON_SCAN_RESTART_NOTIFY, beaconScanRestartMillis);
    }

    private void stopBeaconScanRestartNotify() {
        handler.removeMessages(MSG_BEACON_SCAN_RESTART_NOTIFY);
    }

    public WxBluetoothFeatureImpl() {

    }

    @Override
    public void init(AbsMgr absMgr, String s) {
        super.init(absMgr, s);
        startBeaconScanNotifyCheck();

        sm = (SensorManager)mApplicationContext.getSystemService(Context.SENSOR_SERVICE);
        orientationSensor = sm.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        accelerometerSensor = sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        magneticSensor = sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);

        sm.registerListener(sensorEventListener, accelerometerSensor, SensorManager.SENSOR_DELAY_GAME);
        sm.registerListener(sensorEventListener, magneticSensor, SensorManager.SENSOR_DELAY_GAME);

        updateListener = new HashMap<String, IWebview>();
    }

    @TargetApi(18)
    @Override
    public void dispose(String s) {
        super.dispose(s);
        stopBeaconScanNotifyCheck();
        sm.unregisterListener(sensorEventListener,accelerometerSensor);
        sm.unregisterListener(sensorEventListener,magneticSensor);
        if (mBeaconScanFlag) { //如果当前为false的时候减少了一次创建
            BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
            if (bluetoothAdapter != null) {
                bluetoothAdapter.stopLeScan(leScanCallback);
                mBeaconScanFlag = false;
            }
        }
        if (null != bindWebview) {
            bindWebview.getContext().unregisterReceiver(bluetoothStatuReceiver);
        }
    }

    @Override
    public void onStart(Context context, Bundle bundle, String[] strings) {
        super.onStart(context, bundle, strings);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    public void openBluetoothAdapter(IWebview pWebview, JSONArray array) {
        String CallBackID = array.optString(0);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        int code;

        if (bluetoothAdapter == null) {
            code = 10001;
        } else {
            code = bluetoothAdapter.enable() ? 0 : 10001;
        }

        JSONObject jsonResult = new JSONObject();
        try {
            jsonResult.put("code", code);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JSUtil.execCallback(pWebview, CallBackID, jsonResult, (code == 0) ? JSUtil.OK : JSUtil.ERROR, false);
    }

    public void closeBluetoothAdapter(IWebview pWebview, JSONArray array) {
        String CallBackID = array.optString(0);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        int code;

        if (bluetoothAdapter == null) {
            code = 10001;
        } else {
            code = bluetoothAdapter.disable() ? 0 : 10001;
        }

        JSONObject jsonResult = new JSONObject();
        try {
            jsonResult.put("code", code);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JSUtil.execCallback(pWebview, CallBackID, jsonResult, (code == 0) ? JSUtil.OK : JSUtil.ERROR, false);
    }

    public void getBluetoothAdapterState(IWebview pWebview, JSONArray array) {
        /*
        String CallBackID = array.optString(0);

        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        boolean discovering = false;
        boolean available = false;
        String errMsg;

        if (bluetoothAdapter != null) {
            available = true;
            discovering = bluetoothAdapter.isDiscovering();
            errMsg = "ok";
        } else {
            errMsg = "bluetooth device invalid!";
        }

        JSONObject jsonResult = new JSONObject();
        try {
            jsonResult.put("discovering", discovering);
            jsonResult.put("available", available);
//            jsonResult.put("errMsg", errMsg);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JSUtil.execCallback(pWebview, CallBackID, jsonResult, JSUtil.OK, false);
        */
    }
    public static final boolean isGpsEnable(final Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        if (gps || network) {
            return true;
        }
        return false;
    }
    @TargetApi(18)
    public void startBeaconDiscovery(final IWebview pWebview, JSONArray array) {
        String CallBackID = array.optString(0);
        JSONArray uuids = array.optJSONArray(1);

        PermissionUtil.useSystemPermission(pWebview.getActivity(), "android.permission.ACCESS_COARSE_LOCATION", new PermissionUtil.Request() {
            @Override
            public void onGranted(String streamPerName) {

            }

            @Override
            public void onDenied(String streamPerName) {

            }
        });
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        bluetoothAdapter.enable();
        int code = 0;
        String message = "";

        if (bluetoothAdapter == null) {
            code = 11000;
            message = "Bluetooth is not supported on this hardware platform";
            mBeaconScanFlag = false;
        } else if (!bluetoothAdapter.isEnabled()) {
            code = 11001;
            message = "Bluetooth is off";
            mBeaconScanFlag = false;
        } else if(mBeaconScanFlag) {//已经扫描时，不重启扫描
            JSUtil.execCallback(pWebview, CallBackID, "{code:11003,message:'already start'}", JSUtil.ERROR, true,false);
            return;
        } else if (!isGpsEnable(pWebview.getContext())){
            code = 11002;
            message = "location service unavailable";
            mBeaconScanFlag = false;
        }else {
            bluetoothAdapter.stopLeScan(leScanCallback);
            bluetoothAdapter.startLeScan(leScanCallback);
            code = 0;
            mBeaconScanFlag = true;
        }
        JSONObject jsonResult = new JSONObject();
        try {
            jsonResult.put("code", code);
            jsonResult.put("message",message);
        } catch (Exception e) {
            e.printStackTrace();
        }
        IBeaconContainer.setWxBeaconUUID(uuids);
        JSUtil.execCallback(pWebview, CallBackID, jsonResult, (code == 0) ? JSUtil.OK : JSUtil.ERROR, false);
    }
    public void stopBeaconDiscovery(IWebview pWebview, JSONArray array) {
        String CallBackID = array.optString(0);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        int code;
        String message = "success";

        if (bluetoothAdapter == null) {
            code = 11000;
            message = "Bluetooth is not supported on this hardware platform";
        } else if (!bluetoothAdapter.isEnabled()) {
            code = 11001;
            message = "Bluetooth is off";
        } else if (!isGpsEnable(pWebview.getContext())){
            code = 11002;
            message = "location service unavailable";
        } else {
            bluetoothAdapter.stopLeScan(leScanCallback);
            code = 0;
        }
        mBeaconScanFlag = false;
        JSONObject jsonResult = new JSONObject();
        try {
            jsonResult.put("code", code);
            jsonResult.put("message",message);
        } catch (Exception e) {
            e.printStackTrace();
        }

        JSUtil.execCallback(pWebview, CallBackID, jsonResult, (code == 0) ? JSUtil.OK : JSUtil.ERROR, false);
    }

    @TargetApi(18)
    private void stopDiscovery() {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        int code;

        if (bluetoothAdapter == null) {
            code = 10001;
        } else if (!bluetoothAdapter.isEnabled()) {
            code = 10000;
        } else {
            bluetoothAdapter.stopLeScan(leScanCallback);
            code = 0;
        }
        mBeaconScanFlag = false;
    }

    public void getBeacons(IWebview pWebview, JSONArray array) {
        String CallBackID = array.optString(0);
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        int code;
        JSONArray jsonBeacons = null;
        String message = "success";
        if (bluetoothAdapter == null) {
            code = 11000;
            message = "Bluetooth is not supported on this hardware platform";
        } else if (!bluetoothAdapter.isEnabled()) {
            code = 11001;
            message = "Bluetooth is off";
        } else {
            code = 0;
            jsonBeacons = getBeaconsJSON();
        }

        JSONObject jsonResult = new JSONObject();
        try {
            jsonResult.put("code", code);
            if (jsonBeacons != null) {
                jsonResult.put("beacons", jsonBeacons);
            } else {
                jsonResult.put("message",message);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        JSUtil.execCallback(pWebview, CallBackID, jsonResult, (code == 0) ? JSUtil.OK : JSUtil.ERROR, false);
    }

    /**
     * 做回调用
     * @param pWebview
     * @param array
     */
    public void onBeaconUpdate(IWebview pWebview, JSONArray array) {
        String CallBackID = array.optString(0);
//        mScanCallbackID = CallBackID;
//        mWebview = pWebview;

        //监听多少，通知多少
        updateListener.put(CallBackID,pWebview);
    }

    /**
     * 监听蓝牙状态
     * @param pWebview
     * @param array
     */
    public void onBeaconServiceChange(IWebview pWebview, JSONArray array) {
        bindCallbackID = array.optString(0);
        bindWebview = pWebview;
        IntentFilter stateChangeFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        IntentFilter connectedFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter disConnectedFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        pWebview.getContext().registerReceiver(bluetoothStatuReceiver, stateChangeFilter);
        pWebview.getContext().registerReceiver(bluetoothStatuReceiver, connectedFilter);
        pWebview.getContext().registerReceiver(bluetoothStatuReceiver, disConnectedFilter);
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {
        @Override
        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
            iBeaconContainer.update(device, rssi, scanRecord);
            Log.d(TAG, "onLeScan: " + iBeaconContainer.getBeacons().size());
        }
    };

    private void processBeaconRecords() {
        if (!mBeaconScanFlag)
            return;
        JSONObject jsonResult = new JSONObject();
        try {
            jsonResult.put("code", 0);
            JSONArray jsonBeacons = getBeaconsJSON();
            jsonResult.put("beacons", jsonBeacons);
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (String callbackid:updateListener.keySet()) {
            JSUtil.execCallback(updateListener.get(callbackid), callbackid, jsonResult, JSUtil.OK, true);
        }
    }

    private java.text.DecimalFormat df = new java.text.DecimalFormat("0.000000");
    private java.text.DecimalFormat df_heading = new java.text.DecimalFormat("0.0000");
    private JSONArray getBeaconsJSON() {
        //JSONObject jsonResult = new JSONObject();
        JSONArray jsonBeacons = new JSONArray();

        Map<String, IBeaconRecord> iBeaconRecordMap = iBeaconContainer.getBeacons();
        if (!iBeaconRecordMap.isEmpty()) {

            List<IBeaconRecord> iBeaconRecordList = new ArrayList<>(iBeaconRecordMap.values());
            Collections.sort(iBeaconRecordList, new Comparator<IBeaconRecord>() {
                @Override
                public int compare(IBeaconRecord o1, IBeaconRecord o2) {
                    return o1.getRssi() - o2.getRssi();
                }
            });

            try {
                for (IBeaconRecord record : iBeaconRecordList) {
                    JSONObject jsonBeacon = new JSONObject();

                    jsonBeacon.put("uuid", record.getUuid());
                    jsonBeacon.put("major", String.valueOf(record.getMajor()));
                    jsonBeacon.put("minor", String.valueOf(record.getMinor()));
                    jsonBeacon.put("rssi", String.valueOf(record.getRssi()));
                    jsonBeacon.put("accuracy", df.format(record.getAccuracy()));
                    jsonBeacon.put("heading", df_heading.format(rotateDegree));
                    //jsonBeacon.put("proximity", "1");
                    jsonBeacons.put(jsonBeacon);
                }
                //jsonResult.put("beacons", jsonBeacons);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return jsonBeacons;
    }

    private SensorEventListener sensorEventListener = new SensorEventListener() {

        private long lastRotateUpdateMillis = 0;

        @Override
        public void onSensorChanged(SensorEvent event) {

            int sensorType = event.sensor.getType();
            switch (sensorType) {
                case Sensor.TYPE_ACCELEROMETER: {
                    accelerometerValues = event.values;
                    break;
                }

                case Sensor.TYPE_MAGNETIC_FIELD: {
                    magneticValues = event.values;
                    break;
                }

                default:
                    return;

            }

            if (accelerometerValues == null || magneticValues == null) {
                return;
            }

            SensorManager.getRotationMatrix(rotateValues, null, accelerometerValues, magneticValues);
            SensorManager.getOrientation(rotateValues, resultValues);

            double rotate = Math.toDegrees(resultValues[0]);
            rotate = (rotate + 360.0) % 360.0;
            rotateDegree = rotate;

            long currentTimeMillis = Calendar.getInstance().getTimeInMillis();
            if (currentTimeMillis -  lastRotateUpdateMillis >= 250) {
                processBeaconRecords();
                lastRotateUpdateMillis = currentTimeMillis;
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {

        }
    };

    private BroadcastReceiver bluetoothStatuReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == BluetoothAdapter.ACTION_STATE_CHANGED){
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                if (blueState == BluetoothAdapter.STATE_ON) {
                    JSUtil.execCallback(bindWebview,bindCallbackID,String.format("{discovering:%b,available:true}",mBeaconScanFlag),JSUtil.OK,true,true);
                } else if (blueState == BluetoothAdapter.STATE_OFF) {
                    stopDiscovery();
                    JSUtil.execCallback(bindWebview,bindCallbackID,String.format("{discovering:%b,available:false}",mBeaconScanFlag),JSUtil.OK,true,true);
                }
            }
        }
    };
}
