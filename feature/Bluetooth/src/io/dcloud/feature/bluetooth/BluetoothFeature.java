package io.dcloud.feature.bluetooth;

import android.os.Build;
import android.util.Log;

import org.json.JSONArray;

import io.dcloud.common.DHInterface.AbsMgr;
import io.dcloud.common.DHInterface.IWebview;
import io.dcloud.common.DHInterface.StandardFeature;
import io.dcloud.common.adapter.util.PermissionUtil;

public class BluetoothFeature extends StandardFeature {
   public static boolean isPrintLog=false;

    private BluetoothBaseAdapter bluetoothF;

    @Override
    public void init(AbsMgr pFeatureMgr, String pFeatureName) {
        super.init(pFeatureMgr, pFeatureName);
        /*if (Build.VERSION.SDK_INT > 21) {
            bluetoothF = new BluetoothOver21();
        } else*/ if (Build.VERSION.SDK_INT > 18) {
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

    public void startBluetoothDevicesDiscovery(final IWebview pwebview, final JSONArray args) {
        PermissionUtil.useSystemPermissions(pwebview.getActivity(), new String[]{"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION"}, new PermissionUtil.Request() {
            int times = 0;
            @Override
            public void onGranted(String streamPerName) {
                times++;
                if (times == 2) {
                    bluetoothF.startBluetoothDevicesDiscovery(pwebview, args);
                }
            }

            @Override
            public void onDenied(String streamPerName) {
                times++;
                if (times == 2) {
                    bluetoothF.startBluetoothDevicesDiscovery(pwebview, args);
                }
            }
        });
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
    public void setBLEMTU(IWebview pwebview, JSONArray args){
        Log(Log.INFO, "setBLEMTU:"+args);
        bluetoothF.setBLEMTU(pwebview,args);

    }

    public void openBleDebugMode(IWebview pwebview, JSONArray args) {
        isPrintLog = true;
        Log(Log.INFO, "打开蓝牙调试模式");
    }

    public static void Log(int level, Object... arg) {
        if (level == Log.ERROR || BluetoothFeature.isPrintLog) {
            if (arg != null) {
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < arg.length; i++) {
                    stringBuilder.append(arg[i]).append(" ");
                }
                Log.println(level, "console", "[APP]" + stringBuilder);
            }
        }
    }
    @Override
    public void dispose(String pAppid) {
        bluetoothF.dispose(pAppid);
    }
}
