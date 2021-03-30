package io.dcloud.feature.bluetooth.connect;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 *
 * 这里处理一些全局回调
 * Created by Sinyi.liu on 8/7/20.
 */
public interface ICommonBleCallback {
    /**
     * 设备数据
     *
     * @param gatt
     * @param characteristic
     */
    void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic,boolean isRead);

    /**
     * 设备连接状态发生改变
     * @param gatt
     * @param
     */
    void onConnectionStateChange(BluetoothGatt gatt,  int status, int newState);

    /**
     * 连接断开，worker已经失效状态
     * @param deviceId
     */
    void onWorkerDispose(String deviceId);
}
