package io.dcloud.feature.bluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.StringUtil;

public class BluetoothUnder21 extends BluetoothBaseAdapter {

    private BTScanCallback m21ScanCallback;

    @Override
    public void closeBluetoothAdapter(IWebview pwebview, JSONArray args) {
        super.closeBluetoothAdapter(pwebview, args);
        if (isSearchBTDevice) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (null != adapter && null != m21ScanCallback) {
                adapter.stopLeScan(m21ScanCallback);
                isSearchBTDevice = false;
            }
        }
    }

    @Override
    public void startBluetoothDevicesDiscovery(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
        JSONObject param = args.optJSONObject(1);
        JSONArray serviceIds = param.optJSONArray("services");
        allowDuplicatesDevice = param.optBoolean("allowDuplicatesKey", false);
        String interval = param.optString("interval");
        if (isInit) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                m21ScanCallback = new BTScanCallback();
                if (serviceIds != null) {
                    UUID[] uuids = new UUID[serviceIds.length()];
                }
                if (adapter.isEnabled() && adapter.startLeScan(m21ScanCallback)) {
                    Intent intent = new Intent();
                    intent.setAction(STATUS_ACTION);
                    intent.putExtra(BluetoothAdapter.EXTRA_STATE, 12);
                    pwebview.getContext().sendBroadcast(intent);
                    isSearchBTDevice = true;
                    JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 0, "ok"), JSUtil.OK, true, false);
                } else {
                    JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10000, "not available\t"), JSUtil.ERROR, true, false);
                }
            } else {
                JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10001, "not init"), JSUtil.ERROR, true, false);
            }
        }
    }

    @Override
    public void stopBluetoothDevicesDiscovery(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
        if (isInit) {
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (null != m21ScanCallback && null != adapter) {
                adapter.stopLeScan(m21ScanCallback);
                isSearchBTDevice = false;
                Intent intent = new Intent();
                intent.setAction(STATUS_ACTION);
                intent.putExtra(BluetoothAdapter.EXTRA_STATE, 12);
                pwebview.getContext().sendBroadcast(intent);
                JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 0, "ok"), JSUtil.OK, true, false);
            }else{
                JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10008, "system error"), JSUtil.ERROR, true, false);

            }
        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10000, "not init"), JSUtil.ERROR, true, false);
        }
    }

    @Override
    public void getBluetoothDevices(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
        if (isInit) {
            StringBuilder builder = new StringBuilder();
            if (null != m21ScanCallback) {
                Map<String, DCBluetoothDevice> scanresult = m21ScanCallback.getScanList();
                for (String deviceid : scanresult.keySet()) {
                    builder.append(scanresult.get(deviceid).toString()).append(",");
                }
            }
            if (builder.lastIndexOf(",") > 5) {
                builder.deleteCharAt(builder.lastIndexOf(","));
            }
            JSUtil.execCallback(pwebview, callbackid, String.format("{devices:[%s]}", builder.toString()), JSUtil.OK, true, false);
        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10000, "not init"), JSUtil.ERROR, true, false);
        }
    }

    @Override
    public void dispose(String pAppid) {
        super.dispose(pAppid);
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter !=null) {
            adapter.stopLeScan(m21ScanCallback);
            isSearchBTDevice = false;
        }
    }

    class BTScanCallback implements BluetoothAdapter.LeScanCallback {
        private Map<String, DCBluetoothDevice> scanList;
        private String __JS__FUNCTION = "{devices:[%s]}";

        public BTScanCallback() {
            scanList = new HashMap<String, DCBluetoothDevice>();
        }

        @Override
        public void onLeScan(BluetoothDevice device1, int rssi, byte[] scanRecord) {

            DCBluetoothDevice dcDevice = new DCBluetoothDevice(device1, scanRecord);
            String name = dcDevice.getName();
            String leName = dcDevice.getLocalName();
            if (TextUtils.isEmpty(name) && TextUtils.isEmpty(leName)) {
                return;//过滤掉没有名称的设备
            }


            dcDevice.setRSSI(rssi);
            if (allowDuplicatesDevice) { // 允许重复设备上报
                execJsCallback(CALLBACK_DEVICE_FOUND,String.format(__JS__FUNCTION,dcDevice.toString()));
                scanList.put(device1.getAddress(), dcDevice);
            } else {
                String deviceId = device1.getAddress();
                if (!scanList.containsKey(deviceId)) {
                    scanList.put(deviceId, dcDevice);
                    execJsCallback(CALLBACK_DEVICE_FOUND,String.format(__JS__FUNCTION,dcDevice.toString()));
                }
            }
        }

        public Map<String, DCBluetoothDevice> getScanList() {
            return scanList;
        }
    }
}
