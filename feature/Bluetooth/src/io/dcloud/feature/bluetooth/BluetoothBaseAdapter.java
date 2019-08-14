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

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.StringUtil;

public class BluetoothBaseAdapter {

    boolean allowDuplicatesDevice = false;

    private Map<String, BluetoothGatt> bleConnected;

    private IWebview createBLEConnectionWebview;
    private String createBLEConnectionCallbackId;

    boolean isInit = false;

    boolean isSearchBTDevice = false;

    final String _JS_FUNCTION = "{code:%d,message:'%s'}";

    String STATUS_ACTION = "io.dcloud.bluetooth.sendsearch";

    private Map<String, HashMap<String, IWebview>> callbacks;
    private static String CALLBACK_ADAPTER_STATUS_CHANGED = "callback_adapter_status_changed";
    static String CALLBACK_DEVICE_FOUND = "callback_device_found";
    private static String CALLBACK_BLECHARACTERISTIC_VALUE_CHANGE = "callback_blecharacteristicvaluechange";
    private static String CALLBACK_CONNECTION_STATUS_CHANGED = "callback_connection_status_change";

    private BTBluetoothGattCallback gattCallback;

    public void openBluetoothAdapter(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
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
        isInit = true;
        gattCallback = new BTBluetoothGattCallback();
        JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 0, "ok"), JSUtil.OK, true, false);
    }

    public void dispose(String pAppid) {
        HashMap<String, IWebview> adapters = getStringIWebviewHashMap(CALLBACK_ADAPTER_STATUS_CHANGED);
        for (String key : adapters.keySet()) {
            if (null != adapters.get(key))
                adapters.get(key).getContext().unregisterReceiver(bluetoothStatuReceiver);
        }
        callbacks.clear();
        isInit = false;
        if (bleConnected != null && bleConnected.size() > 0) {
            for (String key : bleConnected.keySet()) {
                BluetoothGatt gatt = bleConnected.get(key);
                gatt.disconnect();
                gatt.close();
            }
            bleConnected.clear();
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
        } else if (!adapter.isEnabled()) {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10001, "not available"), JSUtil.ERROR, true, false);
            return;
        }
        isInit = false;
        if (bleConnected != null && bleConnected.size() > 0) {
            for (String key : bleConnected.keySet()) {
                BluetoothGatt gatt = bleConnected.get(key);
                gatt.disconnect();
                gatt.close();
            }
            bleConnected.clear();
        }
        JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 0, "ok"), JSUtil.OK, true, false);
    }


    public void getBluetoothAdapterState(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
        if (isInit) {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format("{discovering:%b,available:true}", isSearchBTDevice), JSUtil.OK, true, false);
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
                if (null != bleConnected && bleConnected.size() > 0) {
                    try {
                        for (BluetoothDevice device : devices) {
                            JSONObject object = new JSONObject();
                            if (bleConnected.containsKey(device.getAddress())) {
                                object.put("name", device.getName());
                                object.put("deviceId", device.getAddress());
                                array.put(object);
                            }
                        }
                    } catch (Exception e) {
                    }
                }
                JSUtil.execCallback(pwebview, callbackid, String.format("{devices:%s}", array.toString()), JSUtil.OK, true, false);
            }
        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10000, "not init"), JSUtil.ERROR, true, false);
        }
    }

    public void createBLEConnection(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
        JSONObject param = args.optJSONObject(1);
        String deviceid = param.optString("deviceId");
        String timeout = param.optString("timeout");
        checkNull(pwebview, callbackid, deviceid);
        createBLEConnectionWebview = pwebview;
        createBLEConnectionCallbackId = callbackid;
        if (isInit) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (bleConnected == null) bleConnected = new HashMap<String, BluetoothGatt>();
            try {
                BluetoothDevice device = adapter.getRemoteDevice(deviceid);
                if (bleConnected.containsKey(deviceid) && bleConnected.get(deviceid) != null) {
                    JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, -1, "already connect"), JSUtil.ERROR, true, false);
                    return;
                }
                if (gattCallback.isSearching()) {
//                    JSUtil.execCallback(pwebview, callbackid, String.format(_JS_FUNCTION, 10014, "is create connection"), JSUtil.ERROR, true, false);
                    return;
                }
                BluetoothGatt gatt = null;//设备出现的时候不自动连接
                if (android.os.Build.VERSION.SDK_INT >= 23) {
                    gatt = device.connectGatt(pwebview.getContext(), false, gattCallback, 2);
                } else {
                    gatt = device.connectGatt(pwebview.getContext(), false, gattCallback);
                }
                if (gatt != null)
                    gattCallback.setSearching(true);
            } catch (Exception e) {
                JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10002, "not device"), JSUtil.ERROR, true, false);
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
            if (bleConnected == null) {
                return;
            }
            if (bleConnected.containsKey(deviceid) && bleConnected.get(deviceid) != null) {
                bleConnected.get(deviceid).disconnect();
                bleConnected.get(deviceid).close();
                bleConnected.remove(deviceid);
                JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 0, "ok"), JSUtil.OK, true, false);
                return;
            } else {
                if (!gattCallback.isSearching()) {
                    JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10006, "no connection"), JSUtil.OK, true, false);
                }
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
            if (bleConnected != null && bleConnected.containsKey(deviceid) && bleConnected.get(deviceid) != null) {
                BluetoothGattService currentService = bleConnected.get(deviceid).getService(UUID.fromString(serviceId));
                if (currentService == null) {
                    JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10004, "no service"), JSUtil.ERROR, true, false);
                    return;
                }
                List<BluetoothGattCharacteristic> characteristics = currentService.getCharacteristics();
                JSONArray characteristicArray = new JSONArray();
                for (int i = 0; i < characteristics.size(); i++) {
                    BluetoothGattCharacteristic characteristic = characteristics.get(i);
                    JSONObject characteristicItem = null;
                    try {
                        characteristicItem = new JSONObject();
                        characteristicItem.put("uuid", characteristic.getUuid().toString().toUpperCase());
                        JSONObject properties = new JSONObject();
                        int property = characteristic.getProperties();
                        properties.put("read", (property & BluetoothGattCharacteristic.PROPERTY_READ) > 0);
                        properties.put("write", (property & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0);
                        properties.put("notify", (property & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0);
                        properties.put("indicate", (property & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0);
                        characteristicItem.put("properties", properties);
                    } catch (JSONException e) {
                    }
                    characteristicArray.put(characteristicItem);
                }
                JSUtil.execCallback(pwebview, callbackid, String.format("{'characteristics':%s}", characteristicArray.toString()), JSUtil.OK, true, false);
            } else {
                JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10004, "no service"), JSUtil.ERROR, true, false);
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
            BluetoothGatt gatt = getBluetoothGatt(deviceid);
            if (null != gatt) {
                List<BluetoothGattService> services = gatt.getServices();
                JSONArray serviceArray = new JSONArray();
                for (BluetoothGattService service : services) {
                    JSONObject serviceObject = new JSONObject();
                    try {
                        serviceObject.put("uuid", service.getUuid().toString().toUpperCase());
                        serviceObject.put("isPrimary", service.getType() == 0);
                    } catch (JSONException e) {
                    }
                    serviceArray.put(serviceObject);
                }
                JSUtil.execCallback(pwebview, callbackid, String.format("{'services':%s}", serviceArray.toString()), JSUtil.OK, true, false);
            } else {
                JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10004, "no service"), JSUtil.ERROR, true, false);
            }
        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10000, "not init"), JSUtil.ERROR, true, false);
        }
    }

    public void notifyBLECharacteristicValueChange(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
        JSONObject param = args.optJSONObject(1);
        String serviceId = param.optString("serviceId");
        String deviceid = param.optString("deviceId");
        String characteristicId = param.optString("characteristicId");
        boolean state = param.optBoolean("state", true);
        checkNull(pwebview, callbackid, serviceId, deviceid, characteristicId);
        if (isInit) {
            if (state) {
                BluetoothGatt gatt = getBluetoothGatt(deviceid);
                if (gatt != null) {
                    BluetoothGattService service = gatt.getService(UUID.fromString(serviceId));
                    if (service != null) {
                        // 设置Characteristic通知
                        BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicId));
                        if (characteristic == null) {
                            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10005, "no characteristic"), JSUtil.ERROR, true, false);
                            return;
                        }
                        boolean notification = gatt.setCharacteristicNotification(characteristic, true);
                        // 向Characteristic的Descriptor属性写入通知开关，使蓝牙设备主动向手机发送数据
                        boolean write = false;
                        if (null != characteristic.getDescriptors())
                            for (BluetoothGattDescriptor descriptor : characteristic.getDescriptors()) {
                                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); // 和通知类似,但服务端不主动发数据,只指示客户端读取数据
                                write = gatt.writeDescriptor(descriptor);
                            }
                        if (/*write && */notification)
                            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 0, "ok"), JSUtil.OK, true, false);
                        else
                            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10011, "notify fail"), JSUtil.ERROR, true, false);
                    } else {
                        JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10004, "no service"), JSUtil.ERROR, true, false);
                    }
                }
            }
        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10000, "not init"), JSUtil.ERROR, true, false);
        }
    }

    private BluetoothGatt getBluetoothGatt(String deviceid) {
        if (bleConnected != null && bleConnected.containsKey(deviceid) && bleConnected.get(deviceid) != null) {
            BluetoothGatt gatt = bleConnected.get(deviceid);
            return gatt;
        }
        return null;
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
            BluetoothGatt gatt = getBluetoothGatt(deviceid);
            if (gatt != null) {
                BluetoothGattService service = gatt.getService(UUID.fromString(serviceId));
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicId));
                    if (characteristic == null) {
                        JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10005, "no characteristic"), JSUtil.ERROR, true, false);
                        return;
                    }
                    if (gatt.readCharacteristic(characteristic)) {
                        JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 0, "ok"), JSUtil.OK, true, false);
                    } else {
                        JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10007, "property not support"), JSUtil.ERROR, true, false);
                    }
                }
            }
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
        StringBuffer sb = new StringBuffer(bArray.length);
        String sTemp;
        for (int i = 0; i < bArray.length; i++) {
            sTemp = Integer.toHexString(0xFF & bArray[i]);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase());
        }
        return sb.toString();
    }

    public void writeBLECharacteristicValue(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
        JSONObject param = args.optJSONObject(1);
        String serviceId = param.optString("serviceId");
        String deviceid = param.optString("deviceId");
        String characteristicId = param.optString("characteristicId");
        String valueOpt = param.optString("value");
        checkNull(pwebview, callbackid, serviceId, deviceid, characteristicId, valueOpt);
        byte[] value = hexToByte(valueOpt);
        if (isInit) {
            BluetoothGatt gatt = getBluetoothGatt(deviceid);
            if (gatt != null) {
                BluetoothGattService service = gatt.getService(UUID.fromString(serviceId));
                if (service != null) {
                    BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicId));
                    if (characteristic == null) {
                        JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10005, "no characteristic"), JSUtil.ERROR, true, false);
                        return;
                    }
                    characteristic.setValue(value);
                    if (gatt.writeCharacteristic(characteristic)) {
                        JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 0, "ok"), JSUtil.OK, true, false);
                    } else {
                        JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10007, "property not support"), JSUtil.ERROR, true, false);
                    }
                }
            }
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
        if (jscallback != null) {
            for (String key : jscallback.keySet()) {
                JSUtil.execCallback(jscallback.get(key), key, msg, JSUtil.OK, true, true);
            }
        }
    }

    private class BTBluetoothGattCallback extends BluetoothGattCallback {
        NotifyConnectStatus connectStatus = null;

        public void onConnectStatus(NotifyConnectStatus connectStatus) {
            this.connectStatus = connectStatus;
        }

        public void removeNotifyConnect() {
            this.connectStatus = null;
        }

        private boolean isSearching = false;

        public void setSearching(boolean searching) {
            isSearching = searching;
        }

        public boolean isSearching() {
            return isSearching;
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            //js回调，返回当前连接状态
            String deviceid = gatt.getDevice().getAddress();
            if (status == 0 && newState == 2) {
                gatt.discoverServices(); //启动服务发现
                bleConnected.put(deviceid, gatt);
                JSUtil.execCallback(createBLEConnectionWebview, createBLEConnectionCallbackId, StringUtil.format(_JS_FUNCTION, 0, "ok"), JSUtil.OK, true, true);
                execJsCallback(CALLBACK_CONNECTION_STATUS_CHANGED, StringUtil.format("{deviceId:'%s',connected:%b}", deviceid, true));
            } else {
                if (null != bleConnected && bleConnected.containsKey(deviceid)) {
                    bleConnected.get(deviceid).disconnect();
                    bleConnected.get(deviceid).close();
                    bleConnected.remove(deviceid);
                }
                if (newState == 0) {
                    JSUtil.execCallback(createBLEConnectionWebview, createBLEConnectionCallbackId, StringUtil.format(_JS_FUNCTION, 10012, "operate time out"), JSUtil.OK, true, true);
                }
                execJsCallback(CALLBACK_CONNECTION_STATUS_CHANGED, StringUtil.format("{deviceId:'%s',connected:%b}", deviceid, false));
            }
            isSearching = false;
        }


        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            execJsCallback(CALLBACK_BLECHARACTERISTIC_VALUE_CHANGE, String.format("{deviceId:'%s',serviceId:'%s',characteristicId:'%s',value:'%s'}", gatt.getDevice().getAddress().toUpperCase(), characteristic.getService().getUuid().toString().toUpperCase(), characteristic.getUuid().toString().toUpperCase(), bytesToHexString(characteristic.getValue())));
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            execJsCallback(CALLBACK_BLECHARACTERISTIC_VALUE_CHANGE, String.format("{deviceId:'%s',serviceId:'%s',characteristicId:'%s',value:'%s'}", gatt.getDevice().getAddress().toUpperCase(), characteristic.getService().getUuid().toString().toUpperCase(), characteristic.getUuid().toString().toUpperCase(), bytesToHexString(characteristic.getValue())));
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
//            JSUtil.execCallback(bleCharacteristicValueChangeWebview, bleCharacteristicValueChangeCallbackId, String.format("{deviceId:'%s',serviceId:'%s',characteristicId:'%s',value:'%s'}", gatt.getDevice().getAddress(), characteristic.getService().getUuid().toString(), characteristic.getUuid().toString(), Base64.encode(characteristic.getValue())), JSUtil.OK, true, true);
        }
    }

    private BroadcastReceiver bluetoothStatuReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int blueState = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, 0);
                execJsCallback(CALLBACK_ADAPTER_STATUS_CHANGED, String.format("{discovering:%b,available:%b}", isSearchBTDevice, blueState == BluetoothAdapter.STATE_ON));
            } else if (action.equalsIgnoreCase(STATUS_ACTION)) {
                execJsCallback(CALLBACK_ADAPTER_STATUS_CHANGED, String.format("{discovering:%b,available:%b}", isSearchBTDevice, true));
            }
        }
    };
}
