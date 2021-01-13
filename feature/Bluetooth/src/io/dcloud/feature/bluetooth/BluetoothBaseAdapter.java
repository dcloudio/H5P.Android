package io.dcloud.feature.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.adapter.util.Logger;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.StringUtil;
import io.dcloud.feature.bluetooth.connect.BLEConnectionWorker;
import io.dcloud.feature.bluetooth.connect.ICommonBleCallback;

public class BluetoothBaseAdapter {
    public static final UUID DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    boolean allowDuplicatesDevice = false;
    boolean isInit = false;
    boolean isSearchBTDevice = false;
    public static final String _JS_FUNCTION = "{code:%d,message:'%s'}";
    String STATUS_ACTION = "io.dcloud.bluetooth.sendsearch";
    private Map<String, HashMap<String, IWebview>> callbacks=new HashMap<>();
    private static String CALLBACK_ADAPTER_STATUS_CHANGED = "callback_adapter_status_changed";
    static String CALLBACK_DEVICE_FOUND = "callback_device_found";
    private static String CALLBACK_BLECHARACTERISTIC_VALUE_CHANGE = "callback_blecharacteristicvaluechange";
    private static String CALLBACK_CONNECTION_STATUS_CHANGED = "callback_connection_status_change";
    private HashMap<String, BLEConnectionWorker> mWorkerHashMap = new HashMap<>();
    private ICommonBleCallback mICommonBleCallback = new ICommonBleCallback() {
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, boolean isRead) {
            execJsCallback(CALLBACK_BLECHARACTERISTIC_VALUE_CHANGE,
                    String.format("{deviceId:'%s',serviceId:'%s',characteristicId:'%s',value:'%s'}",
                            gatt.getDevice().getAddress().toUpperCase(),
                            characteristic.getService().getUuid().toString().toUpperCase(),
                            characteristic.getUuid().toString().toUpperCase(),
                            bytesToHexString(characteristic.getValue())));
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            String deviceid = gatt.getDevice().getAddress();
            boolean connected=newState == BluetoothProfile.STATE_CONNECTED;
            execJsCallback(CALLBACK_CONNECTION_STATUS_CHANGED, StringUtil.format("{deviceId:'%s',connected:%b}",
                    deviceid, connected));
            if(!connected){
                //断开连接，清掉缓存
                BLEConnectionWorker bleConnectionWorker= mWorkerHashMap.get(deviceid);
                if (bleConnectionWorker != null) {
                    bleConnectionWorker.dispose(null);
                }
                mWorkerHashMap.remove(deviceid);
            }
        }

        @Override
        public void onWorkerDispose(String deviceId) {
            mWorkerHashMap.remove(deviceId);
        }
    };

    public void openBluetoothAdapter(IWebview pwebview, JSONArray args) {
        Log.i("console", "openBluetoothAdapter" + args);
        JSONObject param = args.optJSONObject(1);
        if (param != null) {
            BluetoothFeature.isPrintLog = param.optBoolean("debug", false);
            Log.i("console", "打开调试模式:" + BluetoothFeature.isPrintLog);
        }
        String callbackid = args.optString(0);
        isInit = true;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (Build.VERSION.SDK_INT < 18 || !pwebview.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10009, "system not support"), JSUtil.ERROR, true, false);
            return;
        } else if (adapter == null) {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10010, "unsupport"), JSUtil.ERROR, true, false);
            return;
        } else if (!adapter.isEnabled()) {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10001, "not available"), JSUtil.ERROR, true, false);
            return;
        }
        adapter.enable();
        JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 0, "ok"), JSUtil.OK, true, false);
    }

    public void dispose(String pAppid) {
        HashMap<String, IWebview> adapters = getStringIWebviewHashMap(CALLBACK_ADAPTER_STATUS_CHANGED);
        for (String key : adapters.keySet()) {
            IWebview iWebview = adapters.get(key);
            if (null != iWebview) {
                iWebview.getContext().unregisterReceiver(bluetoothStatuReceiver);
            }
        }
        callbacks.clear();
        isInit = false;
        if (mWorkerHashMap.size() > 0) {
            for (String key : mWorkerHashMap.keySet()) {
                BLEConnectionWorker worker = mWorkerHashMap.get(key);
                if (worker != null) {
                    worker.dispose(pAppid);
                }
            }
            mWorkerHashMap.clear();
        }
    }

    public void closeBluetoothAdapter(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (Build.VERSION.SDK_INT < 18 || !pwebview.getContext().getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10009, "system not support"), JSUtil.ERROR, true, false);
            return;
        } else if (adapter == null) {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10010, "unsupport"), JSUtil.ERROR, true, false);
            return;
        }
        isInit = false;
        if (callbacks != null) {
//            callbacks.clear();
        }
        if (mWorkerHashMap.size() > 0) {
            for (String key : mWorkerHashMap.keySet()) {
                BLEConnectionWorker worker = mWorkerHashMap.get(key);
                if (worker != null) {
                    worker.dispose(null);
                }
            }
            mWorkerHashMap.clear();
        }
        JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 0, "ok"), JSUtil.OK, true, false);
    }


    public void getBluetoothAdapterState(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (isInit) {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format("{discovering:%b,available:%2b}", isSearchBTDevice, adapter.isEnabled()), JSUtil.OK, true, false);
        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10000, "not init"), JSUtil.ERROR, true, false);
        }
    }

    public void onBluetoothDeviceFound(IWebview pwebview, JSONArray args) {
        HashMap<String, IWebview> adapter = getStringIWebviewHashMap(CALLBACK_DEVICE_FOUND);
        adapter.put(args.optString(0), pwebview);
        callbacks.put(CALLBACK_DEVICE_FOUND, adapter);

    }

    public void getBluetoothDevices(IWebview pwebview, JSONArray args) {

    }

    public void startBluetoothDevicesDiscovery(IWebview pwebview, JSONArray args) {

    }

    public void stopBluetoothDevicesDiscovery(IWebview pwebview, JSONArray args) {

    }

    public void onBluetoothAdapterStateChange(IWebview pwebview, JSONArray args) {
        HashMap<String, IWebview> adapter = getStringIWebviewHashMap(CALLBACK_ADAPTER_STATUS_CHANGED);
        adapter.put(args.optString(0), pwebview);
        callbacks.put(CALLBACK_ADAPTER_STATUS_CHANGED, adapter);
        Log.i("console", "[APP]onBluetoothAdapterStateChange" + args);
        IntentFilter stateChangeFilter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        IntentFilter connectedFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED);
        IntentFilter disConnectedFilter = new IntentFilter(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        IntentFilter statusFilter = new IntentFilter(STATUS_ACTION);
        pwebview.getContext().registerReceiver(bluetoothStatuReceiver, statusFilter);
        pwebview.getContext().registerReceiver(bluetoothStatuReceiver, stateChangeFilter);
        pwebview.getContext().registerReceiver(bluetoothStatuReceiver, connectedFilter);
        pwebview.getContext().registerReceiver(bluetoothStatuReceiver, disConnectedFilter);
    }

    private HashMap<String, IWebview> getStringIWebviewHashMap(String key) {
        if (null == callbacks) {
            callbacks = new HashMap<>();
        }
        HashMap<String, IWebview> adapter = callbacks.get(key);
        if (null == adapter) {
            adapter = new HashMap<>();
        }
        return adapter;
    }

    public void getConnectedBluetoothDevices(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
        JSONArray services = args.optJSONObject(1).optJSONArray("services");
        if (isInit) {
            BluetoothManager bluetoothManager = (BluetoothManager) pwebview.getContext().getSystemService(Context.BLUETOOTH_SERVICE);
            if (bluetoothManager != null) {
                List<BluetoothDevice> devices = bluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
                JSONArray array = new JSONArray();
                for (BluetoothDevice device : devices) {

                    JSONObject object = new JSONObject();
                    try {
                        object.put("name", device.getName());
                        object.put("deviceId", device.getAddress());
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    //TODO  是否要加入uuid信息
                    array.put(object);
                }
                JSUtil.execCallback(pwebview, callbackid, String.format("{devices:%s}", array.toString()), JSUtil.OK, true, false);
            }
        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10000, "not init"), JSUtil.ERROR, true, false);
        }
    }

    public void createBLEConnection(IWebview pwebview, JSONArray args) {
        Log.i("console", "createBLEConnection" + args);

        String callbackid = args.optString(0);
        JSONObject param = args.optJSONObject(1);
        String deviceid = param.optString("deviceId");
        String timeout = param.optString("timeout");
        checkNull(pwebview, callbackid, deviceid);
        BLEConnectionWorker lastWorker = mWorkerHashMap.get(deviceid);
        if (lastWorker != null) {
            if (lastWorker.isConnected()) {
                JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, -1, "already connect"), JSUtil.ERROR, true, false);
                return;
            }else{
                //
            }
        }
        if (isInit) {
            BLEConnectionWorker bleConnectionWorker = new BLEConnectionWorker(pwebview, deviceid);
            boolean ret = bleConnectionWorker.createBLEConnection(callbackid, deviceid, timeout, mICommonBleCallback);
            if (ret) {
                mWorkerHashMap.put(deviceid, bleConnectionWorker);
            }
        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10000, "not init"), JSUtil.ERROR, true, false);
        }

    }

    public void closeBLEConnection(IWebview pwebview, JSONArray args) {
        String deviceid = args.optJSONObject(1).optString("deviceId");
        String callbackid = args.optString(0);

        checkNull(pwebview, callbackid, deviceid);
        if (isInit) {
            BLEConnectionWorker lastWorker = mWorkerHashMap.get(deviceid);
            if (lastWorker != null) {
                lastWorker.closeBLEConnection();
                mWorkerHashMap.remove(deviceid);
                JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 0, "ok"), JSUtil.OK, true, false);
            } else {
                JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10006, "no connection"), JSUtil.OK, true, false);
            }
        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10000, "not init"), JSUtil.ERROR, true, false);
        }
    }

    public void getBLEDeviceCharacteristics(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
        JSONObject param = args.optJSONObject(1);
        String deviceid = param.optString("deviceId");
        String serviceId = param.optString("serviceId");
        checkNull(pwebview, callbackid, deviceid, serviceId);
        if (isInit) {
            BLEConnectionWorker lastWorker = mWorkerHashMap.get(deviceid);
            if (null != lastWorker && lastWorker.isConnected()) {
                lastWorker.getBLEDeviceCharacteristics(pwebview, callbackid, serviceId);
            } else {
                JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10006, "no connection"), JSUtil.ERROR, true, false);
            }
        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10000, "not init"), JSUtil.ERROR, true, false);
        }

    }


    public void getBLEDeviceServices(IWebview pwebview, JSONArray args) {
        String deviceid = args.optJSONObject(1).optString("deviceId");
        String callbackid = args.optString(0);
        checkNull(pwebview, callbackid, deviceid);
        if (isInit) {
            BLEConnectionWorker lastWorker = mWorkerHashMap.get(deviceid);
            if (null != lastWorker && lastWorker.isConnected()) {
                lastWorker.getBLEDeviceServices(pwebview, callbackid);
            } else {
                JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION,
                        10006, "no connection"), JSUtil.ERROR, true, false);
            }
        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10000, "not init"), JSUtil.ERROR, true, false);
        }
    }

    public void notifyBLECharacteristicValueChange(IWebview pwebview, JSONArray args) {
        Log.i("console", "notifyBLECharacteristicValueChange" + args);
        String callbackid = args.optString(0);
        JSONObject param = args.optJSONObject(1);
        String serviceId = param.optString("serviceId");
        String deviceid = param.optString("deviceId");
        String characteristicId = param.optString("characteristicId");
        boolean state = param.optBoolean("state", true);
        checkNull(pwebview, callbackid, serviceId, deviceid, characteristicId);
        if (isInit) {
            BLEConnectionWorker lastWorker = mWorkerHashMap.get(deviceid);
            if (lastWorker != null) {
                lastWorker.notifyBLECharacteristicValueChange(callbackid, param, pwebview);
            } else {
                JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10006, "no connection"), JSUtil.ERROR, true, false);
            }
        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10000, "not init"), JSUtil.ERROR, true, false);
        }
    }


    public void onBLECharacteristicValueChange(IWebview pwebview, JSONArray args) {
        HashMap<String, IWebview> adapter = getStringIWebviewHashMap(CALLBACK_BLECHARACTERISTIC_VALUE_CHANGE);
        adapter.put(args.optString(0), pwebview);
        callbacks.put(CALLBACK_BLECHARACTERISTIC_VALUE_CHANGE, adapter);
    }

    public void onBLEConnectionStateChange(IWebview pwebview, JSONArray args) {
        HashMap<String, IWebview> adapter = getStringIWebviewHashMap(CALLBACK_CONNECTION_STATUS_CHANGED);
        adapter.put(args.optString(0), pwebview);
        callbacks.put(CALLBACK_CONNECTION_STATUS_CHANGED, adapter);
    }

    public void readBLECharacteristicValue(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
        JSONObject param = args.optJSONObject(1);
        String serviceId = param.optString("serviceId");
        String deviceid = param.optString("deviceId");
        String characteristicId = param.optString("characteristicId");
        checkNull(pwebview, callbackid, serviceId, deviceid, characteristicId);
        if (isInit) {
            BLEConnectionWorker lastWorker = mWorkerHashMap.get(deviceid);
            if (lastWorker != null) {
                lastWorker.readBLECharacteristicValue(callbackid, param, pwebview);
            } else {
                JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10006, "no connection"), JSUtil.ERROR, true, false);
            }
        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10000, "not init"), JSUtil.ERROR, true, false);
        }
    }


    public static byte[] hexToByte(String str) {
        byte[] bytes = new byte[str.length() / 2];
        for (int i = 0; i < str.length() / 2; i++) {
            String subStr = str.substring(i * 2, i * 2 + 2);
            bytes[i] = (byte) Integer.parseInt(subStr, 16);
        }
        return bytes;
    }

    public static String bytesToHexString(byte[] bArray) {
        StringBuilder sb = new StringBuilder(bArray.length);
        String sTemp;
        for (byte b : bArray) {
            sTemp = Integer.toHexString(0xFF & b);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }
//    public static interface ILogUploadNotify{
//        void onLogUploadNotify();
//    }
//    public static ILogUploadNotify sILogUploadNotify;

    public static final String LogUploadNotify_action = "com.lifesense.uploadlog";
    public void getBLEDeviceRSSI(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
        JSONObject param = args.optJSONObject(1);
        String deviceid = param.optString("deviceId");
        if (deviceid.equals("0000")) {
            Intent intent = new Intent(LogUploadNotify_action);
            // 将要广播的数据添加到Intent对象中
            pwebview.getContext().sendBroadcast(intent);
            Log.i("console", "[APP]发送日志上传广播");


            //触发日志上传  临时方案

            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 0, "sILogUploadNotify 为空"), JSUtil.OK, true, false);

        }else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10009, "接口功能暂未实现"), JSUtil.ERROR, true, false);
        }


    }

    public void setBLEMTU(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
        JSONObject param = args.optJSONObject(1);
        String deviceid = param.optString("deviceId");

        BLEConnectionWorker lastWorker = mWorkerHashMap.get(deviceid);
        if (lastWorker != null) {
            lastWorker.setBLEMTU(callbackid, param, pwebview);
        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10006, "no connection"), JSUtil.ERROR, true, false);
        }

    }
    public void writeBLECharacteristicValue(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
        JSONObject param = args.optJSONObject(1);
        String serviceId = param.optString("serviceId");
        String deviceid = param.optString("deviceId");
        String characteristicId = param.optString("characteristicId");
        String valueOpt = param.optString("value");
        checkNull(pwebview, callbackid, serviceId, deviceid, characteristicId, valueOpt);
        if (isInit) {
            BLEConnectionWorker lastWorker = mWorkerHashMap.get(deviceid);
            if (lastWorker != null) {
                lastWorker.writeBLECharacteristicValue(callbackid, param, pwebview);
            } else {
                JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10006, "no connection"), JSUtil.ERROR, true, false);
            }
        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10000, "not init"), JSUtil.ERROR, true, false);
        }
    }

    void checkNull(IWebview pwebview, String callbackid, String... param) {
        for (String p : param) {
            if (PdrUtil.isEmpty(p)) {
                JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10013, "invalid data,please check parameters"), JSUtil.ERROR, true, false);
                return;
            }
        }
    }

    protected void execJsCallback(String type, String msg) {
        if (callbacks == null) return;
        HashMap<String, IWebview> jscallback = callbacks.get(type);
//        Log.i("console", "[APP]execJsCallback:" + type + msg);
        if (jscallback != null) {
            for (String key : jscallback.keySet()) {
                JSUtil.execCallback(jscallback.get(key), key, msg, JSUtil.OK, true, true);
            }
        }
    }


    private BroadcastReceiver bluetoothStatuReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            Log.i("console", "[APP]bluetoothStatusReceiver:" + action);

            if (action != null && action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);
                if (state == BluetoothAdapter.STATE_OFF || state == BluetoothAdapter.STATE_ON) {
                    execJsCallback(CALLBACK_ADAPTER_STATUS_CHANGED,
                            String.format("{discovering:%b,available:%b}", isSearchBTDevice, state == BluetoothAdapter.STATE_ON));

                    if (state == BluetoothAdapter.STATE_OFF) {
                        Object[] keys = mWorkerHashMap.keySet().toArray();
                        for (Object key : keys) {
                            //断开连接，清掉缓存
                            BLEConnectionWorker bleConnectionWorker = mWorkerHashMap.get(key);
                            if (bleConnectionWorker != null) {
                                bleConnectionWorker.dispose(null);
                            }
                        }
                        mWorkerHashMap.clear();
                    }
                }

            } else if (action != null&&action.equalsIgnoreCase(STATUS_ACTION)) {

                BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
                execJsCallback(CALLBACK_ADAPTER_STATUS_CHANGED, String.format("{discovering:%b,available:%b}", isSearchBTDevice, adapter.isEnabled()));
            }
        }
    };
}
