package io.dcloud.feature.bluetooth.connect;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.os.Handler;
import android.util.Log;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.UUID;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.StringUtil;
import io.dcloud.feature.bluetooth.BluetoothBaseAdapter;
import io.dcloud.feature.bluetooth.BluetoothFeature;

import static android.bluetooth.BluetoothDevice.TRANSPORT_LE;
import static io.dcloud.feature.bluetooth.BluetoothBaseAdapter.DESCRIPTOR_UUID;
import static io.dcloud.feature.bluetooth.BluetoothBaseAdapter._JS_FUNCTION;
import static io.dcloud.feature.bluetooth.BluetoothBaseAdapter.hexToByte;

/**
 * Created by Sinyi.liu on 8/7/20.
 * 用于管理跟单个设备的连接
 */
public class BLEConnectionWorker extends BluetoothGattCallback {
    private String mDeviceId;
    private IWebview mIWebview;
    private boolean isConnected = false;
    private boolean isDispose = false;
    private BluetoothGatt mBluetoothGatt;
    private Handler mHandler;
    private Queue<BluetoothGattMessage> mGattEventQueue;
    private ICommonBleCallback nextICommonBleCallback;
    private String createCallbackid;
    private boolean isServicesDiscovered = false;
    private BluetoothGattMessage mCurrentGattMessage;

    public BLEConnectionWorker(IWebview pwebview, String deviceId) {
        mDeviceId = deviceId;
        mIWebview = pwebview;
        mHandler = new Handler();
        mGattEventQueue = new LinkedList<>();
    }


    public String getDeviceId() {
        return mDeviceId;
    }

    public boolean isConnected() {
        return isConnected;
    }

    public boolean createBLEConnection(String callbackid, String deviceid, String timeout, ICommonBleCallback iCommonBleCallback) {
        this.nextICommonBleCallback = iCommonBleCallback;
        createCallbackid = callbackid;
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        int timeoutInt = 10 * 1000;
        try {
            timeoutInt = Integer.parseInt(timeout);
        } catch (Exception e) {
        }
        timeoutInt = Math.min(10 * 1000, timeoutInt);
        timeoutInt = Math.max(3 * 1000, timeoutInt);
        try {
            BluetoothDevice device = adapter.getRemoteDevice(deviceid);
            BluetoothGatt gatt = null;//设备出现的时候不自动连接
            if (android.os.Build.VERSION.SDK_INT >= 23) {
                gatt = device.connectGatt(mIWebview.getContext(), false, this, TRANSPORT_LE);
            } else {
                gatt = device.connectGatt(mIWebview.getContext(), false, this);
            }
            if (gatt == null) {
                JSUtil.execCallback(mIWebview, callbackid, StringUtil.format(_JS_FUNCTION, 10002, "not device"), JSUtil.ERROR, true, false);
                return false;
            }
            mBluetoothGatt = gatt;
            startCreateConnectTimeout(timeoutInt);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            createCallbackid = null;
            JSUtil.execCallback(mIWebview, callbackid, StringUtil.format(_JS_FUNCTION, 10002, "not device"), JSUtil.ERROR, true, false);
            dispose(null);
            return false;
        }
    }

    private Runnable mConnectTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            if (createCallbackid == null) {
                return;
            }
            JSUtil.execCallback(mIWebview, createCallbackid, StringUtil.format(_JS_FUNCTION, 10012, "operate time out"), JSUtil.ERROR, true, false);
            createCallbackid = null;
            dispose(null);
        }
    };

    private void startCreateConnectTimeout(int timeout) {
        mHandler.postDelayed(mConnectTimeoutRunnable, timeout);
    }

    private void removeConnectTimeout() {
        mHandler.removeCallbacks(mConnectTimeoutRunnable);
    }


    public void closeBLEConnection() {
        dispose(null);
    }

    public void getBLEDeviceServices(final IWebview pwebview, final String callbackid) {
        if (isConnected && mBluetoothGatt != null) {
            if (isServicesDiscovered) {
                postBLEDeviceServices(pwebview, callbackid);
            } else {
                //先简单点，直接加延迟
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        postBLEDeviceServices(pwebview, callbackid);
                    }
                }, 500);
            }
        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10006, "no connection"), JSUtil.ERROR, true, false);
        }
    }

    public void postBLEDeviceServices(IWebview pwebview, String callbackid) {
        List<BluetoothGattService> services = mBluetoothGatt.getServices();
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

    }


    public void getBLEDeviceCharacteristics(IWebview pwebview, String callbackid, String serviceId) {
        if (isConnected && mBluetoothGatt != null) {
            BluetoothGattService currentService = mBluetoothGatt.getService(UUID.fromString(serviceId));
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
                    properties.put("write",isSupportWritePropertis(characteristic));
                    properties.put("read", (property & BluetoothGattCharacteristic.PROPERTY_READ) > 0);
                    properties.put("write", (property & BluetoothGattCharacteristic.PROPERTY_WRITE) > 0||(property & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)>0);
                    properties.put("notify", (property & BluetoothGattCharacteristic.PROPERTY_NOTIFY) > 0);
                    properties.put("indicate", (property & BluetoothGattCharacteristic.PROPERTY_INDICATE) > 0);
                    characteristicItem.put("properties", properties);
                } catch (JSONException e) {
                }
                characteristicArray.put(characteristicItem);
            }
            JSUtil.execCallback(pwebview, callbackid, String.format("{'characteristics':%s}", characteristicArray.toString()), JSUtil.OK, true, false);

        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10006, "no connection"), JSUtil.ERROR, true, false);
        }

    }

    public void setBLEMTU() {

    }

    public void writeBLECharacteristicValue(String callbackId, JSONObject param, IWebview iWebview) {
        BluetoothGattMessage bluetoothGattMessage = new BluetoothGattMessage(BluetoothGattMessage.WRITE, callbackId, param, iWebview);
        mGattEventQueue.add(bluetoothGattMessage);
        handleBluetoothGattEvent();

    }

    public void readBLECharacteristicValue(String callbackId, JSONObject param, IWebview iWebview) {
        BluetoothGattMessage bluetoothGattMessage = new BluetoothGattMessage(BluetoothGattMessage.READ, callbackId, param, iWebview);
        mGattEventQueue.add(bluetoothGattMessage);
        handleBluetoothGattEvent();
    }

    public void notifyBLECharacteristicValueChange(String callbackId, JSONObject param, IWebview iWebview) {
        BluetoothGattMessage bluetoothGattMessage = new BluetoothGattMessage(BluetoothGattMessage.NOTIFY, callbackId, param, iWebview);
        mGattEventQueue.add(bluetoothGattMessage);
        handleBluetoothGattEvent();
    }

    private void Log(int level, Object... arg) {
        BluetoothFeature.Log(level, arg);
    }

    private void handleBluetoothGattEvent() {
        if (mCurrentGattMessage != null) {
            //正在处理msg
            Log(Log.INFO,"当前正在处理 拦截>" ,mCurrentGattMessage.toString());
            return;
        }
        if (mGattEventQueue == null || mGattEventQueue.size() == 0) {
            return;
        }
        BluetoothGattMessage nextGattAction = mGattEventQueue.peek();
        if (nextGattAction == null) {
            return;
        }
        removeBluetoothGattEventTimeout();
        mCurrentGattMessage = nextGattAction;
        Log(Log.DEBUG,"处理Action >" , mCurrentGattMessage.toString());
        if (mCurrentGattMessage.getType() == BluetoothGattMessage.NOTIFY) {
            postNotifyBLECharacteristicValueChange(mCurrentGattMessage);
        } else if (mCurrentGattMessage.getType() == BluetoothGattMessage.WRITE) {
            postWriteBLECharacteristicValue(mCurrentGattMessage);
        } else if (mCurrentGattMessage.getType() == BluetoothGattMessage.READ) {
            postReadBLECharacteristicValue(mCurrentGattMessage);
        }


    }

    private void removeBluetoothGattEventTimeout() {
        mHandler.removeCallbacks(gattEventTimeoutRunnable);
    }

    private void callbackMessageFail(int code, String msg) {
        if (mCurrentGattMessage != null) {
            Log(Log.ERROR,"cb fail " ,code , msg , mCurrentGattMessage);
            removeBluetoothGattEventTimeout();
            if (!mCurrentGattMessage.isCallback) {
                mCurrentGattMessage.setCallback(true);
                mGattEventQueue.remove(mCurrentGattMessage);
                JSUtil.execCallback(mCurrentGattMessage.getPwebview(), mCurrentGattMessage.getCallbackId(),
                        StringUtil.format(_JS_FUNCTION, code, msg), JSUtil.ERROR, true, false);

            }
        }
        mCurrentGattMessage = null;
        handleBluetoothGattEvent();
    }

    private void callbackMessageSucceed() {
        if (mCurrentGattMessage != null) {
//            Log("cb succeed "  ,mCurrentGattMessage);

            removeBluetoothGattEventTimeout();
            mCurrentGattMessage.setCallback(true);
            mGattEventQueue.remove(mCurrentGattMessage);
            JSUtil.execCallback(mCurrentGattMessage.getPwebview(), mCurrentGattMessage.getCallbackId(), StringUtil.format(_JS_FUNCTION, 0, "ok"), JSUtil.OK, true, false);
        }
        mCurrentGattMessage = null;
        handleBluetoothGattEvent();
    }


    public boolean isConnectGatt() {
        return isConnected && mBluetoothGatt != null;
    }

    private void postWriteBLECharacteristicValue(BluetoothGattMessage bluetoothGattMessage) {
        if (!isConnectGatt()) {
            callbackMessageFail(10006, "no connection");
            return;
        }
        JSONObject param = bluetoothGattMessage.getParam();
        String serviceId = param.optString("serviceId");
        String deviceid = param.optString("deviceId");
        String characteristicId = param.optString("characteristicId");
        String valueOpt = param.optString("value");
        byte[] value = hexToByte(valueOpt);
        BluetoothGatt gatt = mBluetoothGatt;
        if (gatt != null) {
            BluetoothGattService service = gatt.getService(UUID.fromString(serviceId));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicId));
                if (characteristic == null) {
                    callbackMessageFail(10005, "no characteristic");
                    return;
                }
                if (isSupportWritePropertis(characteristic)) {
                    characteristic.setValue(value);
                    boolean isSuccess = gatt.writeCharacteristic(characteristic);
                    if (!isSuccess) {
                        callbackMessageFail(10008, "system error");
                        return;
                    }
                    initGattEventTimeout();
                } else {
                    callbackMessageFail(10007, "property not support");
                }
            } else {
                callbackMessageFail(10004, "no service");
            }
        }

    }

    private void postReadBLECharacteristicValue(BluetoothGattMessage bluetoothGattMessage) {

        if (isConnectGatt()) {
            JSONObject param = bluetoothGattMessage.getParam();
            String serviceId = param.optString("serviceId");
            String deviceid = param.optString("deviceId");
            String characteristicId = param.optString("characteristicId");
            BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(serviceId));
            if (service != null) {
                BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicId));
                if (characteristic == null) {
                    callbackMessageFail(10005, "no characteristic");
                    return;
                }
                boolean isSucceed = mBluetoothGatt.readCharacteristic(characteristic);
                if (!isSucceed) {
                    callbackMessageFail(10007, "property not support");
                } else {
                    initGattEventTimeout();
                }
            }
        } else {
            callbackMessageFail(10006, "no connection");
        }


    }

    private void initGattEventTimeout() {
        removeBluetoothGattEventTimeout();
        mHandler.postDelayed(gattEventTimeoutRunnable, 5 * 1000);
    }

    private void postNotifyBLECharacteristicValueChange(BluetoothGattMessage bluetoothGattMessage) {
        if (!isConnectGatt()) {
            callbackMessageFail(10006, "no connection");
            return;
        }
        JSONObject param = bluetoothGattMessage.getParam();
        String serviceId = param.optString("serviceId");
        String deviceid = param.optString("deviceId");
        String characteristicId = param.optString("characteristicId");
        boolean state = param.optBoolean("state", true);
        BluetoothGattService service = mBluetoothGatt.getService(UUID.fromString(serviceId));
        if (service != null) {
            // 设置Characteristic通知
            BluetoothGattCharacteristic characteristic = service.getCharacteristic(UUID.fromString(characteristicId));
            if (characteristic == null) {
                callbackMessageFail(10005, "no characteristic");
                return;
            }
            boolean isSucceed = mBluetoothGatt.setCharacteristicNotification(characteristic, state);
            if (!isSucceed) {
                callbackMessageFail(10007, "property not support");
                return;
            }
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(DESCRIPTOR_UUID);
            if (state) {
                // 向Characteristic的Descriptor属性写入通知开关，使蓝牙设备主动向手机发送数据
                if ((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) == BluetoothGattCharacteristic.PROPERTY_INDICATE) {
                    //以indicate的方式打开特征通道
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    isSucceed = mBluetoothGatt.writeDescriptor(descriptor);
                    Log(Log.INFO, "writeDescriptor INDICATION characteristic" + characteristic.getUuid().toString());
                } else {
                    //以notify的方式打开特征通道
                    descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    isSucceed = mBluetoothGatt.writeDescriptor(descriptor);
                    Log( Log.INFO,"writeDescriptor NOTIFICATION characteristic" + characteristic.getUuid().toString());
                }
            } else {
                descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
                isSucceed = mBluetoothGatt.writeDescriptor(descriptor);
            }

            if (!isSucceed) {
                callbackMessageFail(10008, "system error");
                return;
            }
            initGattEventTimeout();
        } else {
            callbackMessageFail(10004, "no service");
        }
    }


    public void dispose(String pAppid) {
        isDispose = true;
        isConnected = false;
        mIWebview = null;
        if (mBluetoothGatt != null) {
            closeGatt(mBluetoothGatt);
            mBluetoothGatt = null;
        }
        if (nextICommonBleCallback != null) {
            nextICommonBleCallback.onWorkerDispose(mDeviceId);
        }

    }

    private Runnable gattEventTimeoutRunnable = new Runnable() {
        @Override
        public void run() {
            callbackMessageFail(10012, "operate time out");
        }
    };

    @Override
    public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
        if (gatt == null || gatt.getDevice() == null || gatt.getDevice().getAddress() == null) {
            return;
        }
        if (!gatt.getDevice().getAddress().equals(mDeviceId)) {
            return;
        }
        mBluetoothGatt = gatt;
        removeConnectTimeout();
        Log(Log.ERROR, mDeviceId + " status :" + status + "  newState:" + newState);
        if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
            if (isDispose) {
                //关闭了
                closeGatt(gatt);
                dispose(null);
                return;
            }
            isConnected = true;
            gatt.discoverServices(); //启动服务发现
            if (createCallbackid != null) {
                JSUtil.execCallback(mIWebview, createCallbackid, StringUtil.format(_JS_FUNCTION, 0, "ok"), JSUtil.OK, true, true);
            }
            createCallbackid = null;
        } else {
            isConnected = false;
            if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                closeGatt(gatt);
                if (createCallbackid != null) {
                    JSUtil.execCallback(mIWebview, createCallbackid, StringUtil.format(_JS_FUNCTION, 10003, "connection fail"), JSUtil.ERROR, true, false);
                }
                createCallbackid = null;
                dispose(null);
            }
        }
        if (BLEConnectionWorker.this.nextICommonBleCallback != null) {
            this.nextICommonBleCallback.onConnectionStateChange(gatt, status, newState);
        }
    }

    private void closeGatt(BluetoothGatt bluetoothGatt) {
        if (bluetoothGatt != null) {
            bluetoothGatt.disconnect();
            bluetoothGatt.close();
        }
    }

    @Override
    public void onServicesDiscovered(BluetoothGatt gatt, int status) {
        super.onServicesDiscovered(gatt, status);
        isServicesDiscovered = true;

    }

    /**
     * 调用readCharacteristic后，设备数据上报
     *
     * @param gatt
     * @param characteristic
     * @param status
     */
    @Override
    public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicRead(gatt, characteristic, status);
        Log(Log.INFO,"onCharacteristicRead" , characteristic.getUuid());
        if (mCurrentGattMessage != null
                && mCurrentGattMessage.getType() == BluetoothGattMessage.READ) {
            callbackMessageSucceed();
        } else {
            handleBluetoothGattEvent();
        }
        if (nextICommonBleCallback != null) {
            nextICommonBleCallback.onCharacteristicChanged(gatt, characteristic, true);
        }
    }

    /**
     * 设备数据主动上报
     *
     * @param gatt
     * @param characteristic
     */
    @Override
    public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        super.onCharacteristicChanged(gatt, characteristic);
        if (nextICommonBleCallback != null) {
            nextICommonBleCallback.onCharacteristicChanged(gatt, characteristic, false);
        }
    }

    /**
     * notifyBLECharacteristicValueChange 接口实际成功回调
     *
     * @param gatt
     * @param descriptor
     * @param status
     */
    @Override
    public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
        super.onDescriptorWrite(gatt, descriptor, status);
        if (mCurrentGattMessage != null && mCurrentGattMessage.getType() == BluetoothGattMessage.NOTIFY) {
            callbackMessageSucceed();
        } else {
            handleBluetoothGattEvent();
        }
    }


    /**
     * writeBLECharacteristicValue 实际成功回调
     *
     * @param gatt
     * @param characteristic
     * @param status
     */
    @Override
    public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
        super.onCharacteristicWrite(gatt, characteristic, status);
        if (mCurrentGattMessage != null && mCurrentGattMessage.getType() == BluetoothGattMessage.WRITE) {
            callbackMessageSucceed();
        } else {
            handleBluetoothGattEvent();
        }
    }

    //蓝牙强度回调
    @Override
    public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        super.onReadRemoteRssi(gatt, rssi, status);
    }


    static class BluetoothGattMessage {
        public static final int WRITE = 1;
        public static final int READ = 2;
        public static final int NOTIFY = 3;

        private int type;
        private String callbackId;
        private JSONObject param;
        private IWebview pwebview;
        private boolean isCallback;

        public BluetoothGattMessage setCallback(boolean callback) {
            isCallback = callback;
            return this;
        }

        public BluetoothGattMessage(int type, String callbackId, JSONObject param, IWebview pwebview) {
            this.type = type;
            this.callbackId = callbackId;
            this.param = param;
            this.pwebview = pwebview;
        }

        public int getType() {
            return type;
        }

        public String getCallbackId() {
            return callbackId;
        }

        public JSONObject getParam() {
            return param;
        }

        public IWebview getPwebview() {
            return pwebview;
        }

        @Override
        public String toString() {
            return "{" +
                    "type=" + type +
                    ", param=" + param +
                    '}';
        }
    }


    /**
     * 判断特征是否支持写属性
     *
     * @param characteristic
     * @return
     */
    public static boolean isSupportWritePropertis(BluetoothGattCharacteristic characteristic) {
        if (characteristic == null) {
            return false;
        }
        int properties = characteristic.getProperties();
        if (BluetoothGattCharacteristic.PROPERTY_WRITE == (properties & BluetoothGattCharacteristic.PROPERTY_WRITE)) {
            return true;
        }
        if (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE == (properties & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) {
            return true;
        }
        if (BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE == (properties & BluetoothGattCharacteristic.PROPERTY_SIGNED_WRITE)) {
            return true;
        } else {
            return false;
        }
    }
}
