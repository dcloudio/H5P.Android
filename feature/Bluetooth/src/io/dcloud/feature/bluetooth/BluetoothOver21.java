package io.dcloud.feature.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Intent;
import android.os.Build;
import android.os.ParcelUuid;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.util.JSUtil;
import io.dcloud.common.util.PdrUtil;
import io.dcloud.common.util.StringUtil;
@Deprecated
public class BluetoothOver21 extends BluetoothBaseAdapter {

    private BTScanCallback mScanCallback;

    @Override
    public void closeBluetoothAdapter(IWebview pwebview, JSONArray args) {
        super.closeBluetoothAdapter(pwebview, args);
        if (isSearchBTDevice && Build.VERSION.SDK_INT >= 21) {
            if (null != scanner && null != mScanCallback) {
                scanner.stopScan(mScanCallback);
                isSearchBTDevice = false;
            }
        }
    }

    @Override
    public void getBluetoothDevices(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
        if (isInit) {
            StringBuilder builder = new StringBuilder();
            if (null != mScanCallback) {
                Map<String, DCBluetoothDevice> scanresult = mScanCallback.getScanList();
                for (String deviceid : scanresult.keySet()) {
                    builder.append(scanresult.get(deviceid).toString() + ",");
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



    private BluetoothLeScanner scanner;


    @Override
    public void startBluetoothDevicesDiscovery(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
        JSONObject param = args.optJSONObject(1);
        JSONArray serviceIds = param.optJSONArray("services");
        allowDuplicatesDevice = param.optBoolean("allowDuplicatesKey", false);
        String interval = param.optString("interval");
        if (isInit) {
            mScanCallback = new BTScanCallback();
            BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
            if (adapter != null) {
                if (android.os.Build.VERSION.SDK_INT >= 21) {
                    scanner = adapter.getBluetoothLeScanner();
                    List<ScanFilter> scanFilters = null;
                    ScanSettings.Builder settings = new ScanSettings.Builder();
                    if (null != serviceIds && serviceIds.length() > 0) {
                        scanFilters = new ArrayList<ScanFilter>();
                        for (int i = 0; i < serviceIds.length(); i++) {
                            ScanFilter.Builder filter = new ScanFilter.Builder();
                            filter.setServiceUuid(new ParcelUuid(UUID.fromString(serviceIds.optString(i))));
                            scanFilters.add(filter.build());
                        }
                    }
                    if (!PdrUtil.isEmpty(interval)) {
                        try {
                            settings.setReportDelay(Long.parseLong(interval));
                        } catch (Exception e) {
                        }
                    }
                    scanner.startScan(scanFilters, settings.build(), mScanCallback);
                    Intent intent = new Intent();
                    intent.setAction(STATUS_ACTION);
                    intent.putExtra(BluetoothAdapter.EXTRA_STATE, 12);
                    pwebview.getContext().sendBroadcast(intent);
                    isSearchBTDevice = true;
                    JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 0, "ok"), JSUtil.OK, true, false);
                }
            }
        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10000, "not init"), JSUtil.ERROR, true, false);
        }
    }

    @Override
    public void stopBluetoothDevicesDiscovery(IWebview pwebview, JSONArray args) {
        String callbackid = args.optString(0);
        if (isInit) {
            if (Build.VERSION.SDK_INT >= 21) {
                if (null != scanner && null != mScanCallback) {
                    scanner.stopScan(mScanCallback);
                    isSearchBTDevice = false;
                    Intent intent = new Intent();
                    intent.setAction(STATUS_ACTION);
                    intent.putExtra(BluetoothAdapter.EXTRA_STATE, 12);
                    pwebview.getContext().sendBroadcast(intent);
                    JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 0, "ok"), JSUtil.OK, true, false);
                }
            }
        } else {
            JSUtil.execCallback(pwebview, callbackid, StringUtil.format(_JS_FUNCTION, 10000, "not init"), JSUtil.ERROR, true, false);
        }
    }



    @Override
    public void dispose(String pAppid) {
        super.dispose(pAppid);
        if (isSearchBTDevice) {
            if (Build.VERSION.SDK_INT >= 21) {
                scanner.stopScan(mScanCallback);
                isSearchBTDevice = false;
            }
        }
    }

    @TargetApi(21)
    private class BTScanCallback extends ScanCallback {

        //        private List<DCBluetoothDevice> scanList;
        private Map<String, DCBluetoothDevice> scanList;
        private IWebview pwebview;
        private String callbackId;
        private String __JS__FUNCTION = "{devices:[%s]}";

        public BTScanCallback() {
            this(null, null);
        }

        public BTScanCallback(IWebview pwebview, String callbackId) {
            this.pwebview = pwebview;
            this.callbackId = callbackId;
            scanList = new HashMap<String, DCBluetoothDevice>();
        }

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            String name = result.getDevice().getName();
            String leName = null;
            if (result.getScanRecord() != null) {
                leName = result.getScanRecord().getDeviceName();
            }
            if (TextUtils.isEmpty(name) && TextUtils.isEmpty(leName)) {
                return;//过滤掉没有名称的设备
            }
            DCBluetoothDevice device = new DCBluetoothDevice(result);

            if (allowDuplicatesDevice) { // 允许重复设备上报
                execJsCallback(CALLBACK_DEVICE_FOUND,String.format(__JS__FUNCTION,device.toString()));
            } else {
                String deviceId = result.getDevice().getAddress();
                if (!scanList.containsKey(deviceId)) {
                    scanList.put(deviceId, device);
                    execJsCallback(CALLBACK_DEVICE_FOUND,String.format(__JS__FUNCTION,device.toString()));
                }
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
        }

        public Map<String, DCBluetoothDevice> getScanList() {
            return scanList;
        }
    }


}
