package io.dcloud.feature.bluetooth;

import android.os.Build;

import org.json.JSONArray;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.StandardFeature;

public class BluetoothFeature extends StandardFeature {


    private BluetoothBaseAdapter bluetoothF;

    @Override
    public void init(AbsMgr pFeatureMgr, String pFeatureName) {
        super.init(pFeatureMgr, pFeatureName);
        if (Build.VERSION.SDK_INT > 21) {
            bluetoothF = new BluetoothOver21();
        } else if (Build.VERSION.SDK_INT > 18 && Build.VERSION.SDK_INT < 21) {
            bluetoothF = new BluetoothUnder21();
        } else {
            bluetoothF = new BluetoothBaseAdapter();
        }
    }

    public void openBluetoothAdapter(IWebview pwebview, JSONArray args) {
        bluetoothF.openBluetoothAdapter(pwebview, args);
    }

    public void closeBluetoothAdapter(IWebview pwebview, JSONArray args) {
        bluetoothF.closeBluetoothAdapter(pwebview, args);
    }

    public void getBluetoothAdapterState(IWebview pwebview, JSONArray args) {
        bluetoothF.getBluetoothAdapterState(pwebview, args);
    }

    public void onBluetoothAdapterStateChange(IWebview pwebview, JSONArray args) {
        bluetoothF.onBluetoothAdapterStateChange(pwebview, args);
    }

    public void onBluetoothDeviceFound(IWebview pwebview, JSONArray args) {
        bluetoothF.onBluetoothDeviceFound(pwebview, args);
    }

    public void getBluetoothDevices(IWebview pwebview, JSONArray args) {
        bluetoothF.getBluetoothDevices(pwebview, args);
    }

    public void getConnectedBluetoothDevices(IWebview pwebview, JSONArray args) {
        bluetoothF.getConnectedBluetoothDevices(pwebview, args);
    }

    public void startBluetoothDevicesDiscovery(IWebview pwebview, JSONArray args) {
        bluetoothF.startBluetoothDevicesDiscovery(pwebview, args);
    }

    public void stopBluetoothDevicesDiscovery(IWebview pwebview, JSONArray args) {
        bluetoothF.stopBluetoothDevicesDiscovery(pwebview, args);
    }

    public void createBLEConnection(IWebview pwebview, JSONArray args) {
        bluetoothF.createBLEConnection(pwebview, args);
    }

    public void closeBLEConnection(IWebview pwebview, JSONArray args) {
        bluetoothF.closeBLEConnection(pwebview, args);
    }

    public void getBLEDeviceCharacteristics(IWebview pwebview, JSONArray args) {
        bluetoothF.getBLEDeviceCharacteristics(pwebview, args);
    }

    public void getBLEDeviceServices(IWebview pwebview, JSONArray args) {
        bluetoothF.getBLEDeviceServices(pwebview, args);
    }

    public void notifyBLECharacteristicValueChange(IWebview pwebview, JSONArray args) {
        bluetoothF.notifyBLECharacteristicValueChange(pwebview, args);
    }

    public void onBLECharacteristicValueChange(IWebview pwebview, JSONArray args) {
        bluetoothF.onBLECharacteristicValueChange(pwebview, args);
    }

    public void onBLEConnectionStateChange(IWebview pwebview, JSONArray args) {
        bluetoothF.onBLEConnectionStateChange(pwebview, args);
    }

    public void readBLECharacteristicValue(IWebview pwebview, JSONArray args) {
        bluetoothF.readBLECharacteristicValue(pwebview, args);
    }

    public void writeBLECharacteristicValue(IWebview pwebview, JSONArray args) {
        bluetoothF.writeBLECharacteristicValue(pwebview, args);
    }

    @Override
    public void dispose(String pAppid) {
        bluetoothF.dispose(pAppid);
    }
}
