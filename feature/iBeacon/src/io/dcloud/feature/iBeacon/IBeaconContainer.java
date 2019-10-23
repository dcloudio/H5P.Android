package io.dcloud.feature.iBeacon;

import android.bluetooth.BluetoothDevice;

import org.json.JSONArray;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class IBeaconContainer {

    private static JSONArray WxBeaconUUID;

    private Map<String, IBeaconRecord> iBeaconRecordMap = new HashMap<String, IBeaconRecord>();
    private long mExpireProcessTimeStampMillis = 0;
    private long mExpireTimeMillis = 8000;
    private int mRssiMinFilter = -75;

    synchronized public void update(BluetoothDevice device, int rssi, byte[] scanData) {
        if (rssi >= mRssiMinFilter && rssi < -1) {
            IBeaconRecord iBeaconRecord = IBeaconRecord.fromScanData(device, scanData, rssi);
            if (iBeaconRecord != null) {
                if (null != WxBeaconUUID&&WxBeaconUUID.length()>0) {
                    for (int i = 0;i<WxBeaconUUID.length();i++){
                        if (WxBeaconUUID.optString(i).equalsIgnoreCase(iBeaconRecord.getUuid())){
                            iBeaconRecordMap.put(iBeaconRecord.address, iBeaconRecord);
                        }
                    }
                } else {
                    iBeaconRecordMap.put(iBeaconRecord.address, iBeaconRecord);
                }
            }
        }
        processExpireBeacons();
    }
    public static void setWxBeaconUUID(JSONArray wxBeaconUUID) {
        WxBeaconUUID = wxBeaconUUID;
    }

    synchronized public Map<String, IBeaconRecord> getBeacons() {
        processExpireBeacons();
        return new HashMap<>(iBeaconRecordMap);
    }

    private void processExpireBeacons() {
        long currentTimeStamp = Calendar.getInstance().getTimeInMillis();
        if (currentTimeStamp - mExpireProcessTimeStampMillis >= 1000) {
            Map<String, IBeaconRecord> beaconMap = new HashMap<>();
            for (Map.Entry<String, IBeaconRecord> entry: iBeaconRecordMap.entrySet()) {
                if (currentTimeStamp - entry.getValue().timeStampMillis <= mExpireTimeMillis) {
                    beaconMap.put(entry.getKey(), entry.getValue());
                }
            }
            iBeaconRecordMap.clear();
            iBeaconRecordMap.putAll(beaconMap);
            mExpireProcessTimeStampMillis = currentTimeStamp;
        }
    }


}
