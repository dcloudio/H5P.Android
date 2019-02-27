package io.dcloud.feature.bluetooth;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanResult;
import android.os.ParcelUuid;
import android.util.SparseArray;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import io.dcloud.common.util.PdrUtil;

public class DCBluetoothDevice {

    /**
     * deviceId : EB:5D:B2:B6:78:77
     * name : Mi Band 3
     * RSSI : 0
     * advertisData :
     * advertisServiceUUIDs : []
     * serviceData : {}
     */

    private String deviceId;
    private String name;
    private int RSSI;
    private String advertisData;
    private JSONObject serviceData;
    private JSONArray advertisServiceUUIDs;
    private String localName;

    private static final int DATA_TYPE_FLAGS = 0x01;
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL = 0x02;
    private static final int DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE = 0x03;
    private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL = 0x04;
    private static final int DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE = 0x05;
    private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL = 0x06;
    private static final int DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE = 0x07;
    private static final int DATA_TYPE_LOCAL_NAME_SHORT = 0x08;
    private static final int DATA_TYPE_LOCAL_NAME_COMPLETE = 0x09;
    private static final int DATA_TYPE_TX_POWER_LEVEL = 0x0A;
    private static final int DATA_TYPE_SERVICE_DATA_16_BIT = 0x16;
    private static final int DATA_TYPE_SERVICE_DATA_32_BIT = 0x20;
    private static final int DATA_TYPE_SERVICE_DATA_128_BIT = 0x21;
    private static final int DATA_TYPE_MANUFACTURER_SPECIFIC_DATA = 0xFF;

    public static final ParcelUuid BASE_UUID =
            ParcelUuid.fromString("00000000-0000-1000-8000-00805F9B34FB");

    /**
     * Length of bytes for 16 bit UUID
     */
    public static final int UUID_BYTES_16_BIT = 2;
    /**
     * Length of bytes for 32 bit UUID
     */
    public static final int UUID_BYTES_32_BIT = 4;
    /**
     * Length of bytes for 128 bit UUID
     */
    public static final int UUID_BYTES_128_BIT = 16;

    @TargetApi(21)
    public DCBluetoothDevice(ScanResult result) {
        setDeviceId(result.getDevice().getAddress());
        setName(PdrUtil.isEmpty(result.getDevice().getName()) ? "" : result.getDevice().getName());
        setRSSI(result.getRssi());
        SparseArray<byte[]> mManufacturerSpecificData = result.getScanRecord().getManufacturerSpecificData();
        if (null != mManufacturerSpecificData && mManufacturerSpecificData.size() > 0) {
            setAdvertisData(bytesToHexString(mManufacturerSpecificData.valueAt(0)));
        }
        setLocalName(PdrUtil.isEmpty(result.getScanRecord().getDeviceName()) ? "" : result.getScanRecord().getDeviceName());
        try {
            setServiceData((map2Str(result.getScanRecord().getServiceData())));
            setAdvertisServiceUUIDs((list2Str(result.getScanRecord().getServiceUuids())));
        } catch (JSONException e) {
            setAdvertisServiceUUIDs(new JSONArray());
        }

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

    public DCBluetoothDevice(BluetoothDevice device, byte[] result) {
        setDeviceId(device.getAddress());
        setName(PdrUtil.isEmpty(device.getName()) ? "" : device.getName());
        parseFromBytes(result);
    }

    /**
     * copy from android source ScanRecord.java
     *
     * @param scanRecord
     * @return
     */
    private void parseFromBytes(byte[] scanRecord) {
        if (scanRecord == null) {
            return;
        }

        int currentPos = 0;
        int advertiseFlag = -1;
        List<ParcelUuid> serviceUuids = new ArrayList<ParcelUuid>();
        String localName = "";
        int txPowerLevel = Integer.MIN_VALUE;

        SparseArray<byte[]> manufacturerData = new SparseArray<byte[]>();
        Map<ParcelUuid, byte[]> serviceData = new HashMap<ParcelUuid, byte[]>();

        try {
            while (currentPos < scanRecord.length) {
                // length is unsigned int.
                int length = scanRecord[currentPos++] & 0xFF;
                if (length == 0) {
                    break;
                }
                // Note the length includes the length of the field type itself.
                int dataLength = length - 1;
                // fieldType is unsigned int.
                int fieldType = scanRecord[currentPos++] & 0xFF;
                switch (fieldType) {
                    case DATA_TYPE_FLAGS:
                        advertiseFlag = scanRecord[currentPos] & 0xFF;
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_16_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_16_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos,
                                dataLength, UUID_BYTES_16_BIT, serviceUuids);
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_32_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_32_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos, dataLength,
                                UUID_BYTES_32_BIT, serviceUuids);
                        break;
                    case DATA_TYPE_SERVICE_UUIDS_128_BIT_PARTIAL:
                    case DATA_TYPE_SERVICE_UUIDS_128_BIT_COMPLETE:
                        parseServiceUuid(scanRecord, currentPos, dataLength,
                                UUID_BYTES_128_BIT, serviceUuids);
                        break;
                    case DATA_TYPE_LOCAL_NAME_SHORT:
                    case DATA_TYPE_LOCAL_NAME_COMPLETE:
                        localName = new String(
                                extractBytes(scanRecord, currentPos, dataLength));
                        break;
                    case DATA_TYPE_TX_POWER_LEVEL:
                        txPowerLevel = scanRecord[currentPos];
                        break;
                    case DATA_TYPE_SERVICE_DATA_16_BIT:
                    case DATA_TYPE_SERVICE_DATA_32_BIT:
                    case DATA_TYPE_SERVICE_DATA_128_BIT:
                        int serviceUuidLength = UUID_BYTES_16_BIT;
                        if (fieldType == DATA_TYPE_SERVICE_DATA_32_BIT) {
                            serviceUuidLength = UUID_BYTES_32_BIT;
                        } else if (fieldType == DATA_TYPE_SERVICE_DATA_128_BIT) {
                            serviceUuidLength = UUID_BYTES_128_BIT;
                        }

                        byte[] serviceDataUuidBytes = extractBytes(scanRecord, currentPos,
                                serviceUuidLength);
                        ParcelUuid serviceDataUuid = parseUuidFrom(
                                serviceDataUuidBytes);
                        byte[] serviceDataArray = extractBytes(scanRecord,
                                currentPos + serviceUuidLength, dataLength - serviceUuidLength);
                        serviceData.put(serviceDataUuid, serviceDataArray);
                        break;
                    case DATA_TYPE_MANUFACTURER_SPECIFIC_DATA:
                        // The first two bytes of the manufacturer specific data are
                        // manufacturer ids in little endian.
                        int manufacturerId = ((scanRecord[currentPos + 1] & 0xFF) << 8) +
                                (scanRecord[currentPos] & 0xFF);
                        byte[] manufacturerDataBytes = extractBytes(scanRecord, currentPos + 2,
                                dataLength - 2);
                        manufacturerData.put(manufacturerId, manufacturerDataBytes);
                        break;
                    default:
                        // Just ignore, we don't handle such data type.
                        break;
                }
                currentPos += dataLength;
            }

            if (serviceUuids.isEmpty()) {
                serviceUuids = null;
            }
        } catch (Exception e) {
        }

        setLocalName(PdrUtil.isEmpty(localName) ? "" : localName);
        try {
            setServiceData((map2Str(serviceData)));
            setAdvertisServiceUUIDs((list2Str(serviceUuids)));
        } catch (JSONException e) {
            setAdvertisServiceUUIDs(new JSONArray());
        }
        if (manufacturerData.size() > 0) {
            setAdvertisData(bytesToHexString(manufacturerData.valueAt(0)));
        }

    }

    private int parseServiceUuid(byte[] scanRecord, int currentPos, int dataLength,
                                 int uuidLength, List<ParcelUuid> serviceUuids) {
        while (dataLength > 0) {
            byte[] uuidBytes = extractBytes(scanRecord, currentPos,
                    uuidLength);
            serviceUuids.add(parseUuidFrom(uuidBytes));
            dataLength -= uuidLength;
            currentPos += uuidLength;
        }
        return currentPos;
    }

    public ParcelUuid parseUuidFrom(byte[] uuidBytes) {
        if (uuidBytes == null) {
            throw new IllegalArgumentException("uuidBytes cannot be null");
        }
        int length = uuidBytes.length;
        if (length != UUID_BYTES_16_BIT && length != UUID_BYTES_32_BIT
                && length != UUID_BYTES_128_BIT) {
            throw new IllegalArgumentException("uuidBytes length invalid - " + length);
        }
        // Construct a 128 bit UUID.
        if (length == UUID_BYTES_128_BIT) {
            ByteBuffer buf = ByteBuffer.wrap(uuidBytes).order(ByteOrder.LITTLE_ENDIAN);
            long msb = buf.getLong(8);
            long lsb = buf.getLong(0);
            return new ParcelUuid(new UUID(msb, lsb));
        }
        // For 16 bit and 32 bit UUID we need to convert them to 128 bit value.
        // 128_bit_value = uuid * 2^96 + BASE_UUID
        long shortUuid;
        if (length == UUID_BYTES_16_BIT) {
            shortUuid = uuidBytes[0] & 0xFF;
            shortUuid += (uuidBytes[1] & 0xFF) << 8;
        } else {
            shortUuid = uuidBytes[0] & 0xFF;
            shortUuid += (uuidBytes[1] & 0xFF) << 8;
            shortUuid += (uuidBytes[2] & 0xFF) << 16;
            shortUuid += (uuidBytes[3] & 0xFF) << 24;
        }
        long msb = BASE_UUID.getUuid().getMostSignificantBits() + (shortUuid << 32);
        long lsb = BASE_UUID.getUuid().getLeastSignificantBits();
        return new ParcelUuid(new UUID(msb, lsb));
    }

    private byte[] extractBytes(byte[] scanRecord, int start, int length) {
        byte[] bytes = new byte[length];
        System.arraycopy(scanRecord, start, bytes, 0, length);
        return bytes;
    }

    private JSONObject map2Str(Map<ParcelUuid, byte[]> map) throws JSONException {
        JSONObject object = new JSONObject();
        if (null != map)
            for (ParcelUuid key : map.keySet()) {
                object.put(key.toString(), bytesToHexString(map.get(key)));
            }
        if (object.length() <= 0) {
            return null;
        }
        return object;
    }

    private JSONArray list2Str(List<ParcelUuid> list) {
        JSONArray array = new JSONArray();
        if (list != null) {
            for (Object object : list) {
                array.put(object.toString());
            }
        }
        return array;
    }

    public String getLocalName() {
        return localName;
    }

    public void setLocalName(String localName) {
        this.localName = localName;
    }

    public DCBluetoothDevice(String deviceId, String name, int RSSI, String advertisData, JSONObject serviceData, JSONArray advertisServiceUUIDs) {
        this.deviceId = deviceId;
        this.name = name;
        this.RSSI = RSSI;
        this.advertisData = advertisData;
        this.serviceData = serviceData;
        this.advertisServiceUUIDs = advertisServiceUUIDs;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getRSSI() {
        return RSSI;
    }

    public void setRSSI(int RSSI) {
        this.RSSI = RSSI;
    }

    public String getAdvertisData() {
        return advertisData;
    }

    public void setAdvertisData(String advertisData) {
        this.advertisData = advertisData;
    }

    public JSONObject getServiceData() {
        return serviceData;
    }

    public void setServiceData(JSONObject serviceData) {
        this.serviceData = serviceData;
    }

    public JSONArray getAdvertisServiceUUIDs() {
        return advertisServiceUUIDs;
    }

    public void setAdvertisServiceUUIDs(JSONArray advertisServiceUUIDs) {
        this.advertisServiceUUIDs = advertisServiceUUIDs;
    }

    @Override
    public String toString() {
        JSONObject object = new JSONObject();
        try {
            object.put("deviceId", deviceId);
            object.put("name", name);
            object.put("RSSI", RSSI);
            object.put("localName", localName);
            object.put("advertisServiceUUIDs", advertisServiceUUIDs);
            if (null != advertisData)
                object.put("advertisData", advertisData);
            if (null != serviceData)
                object.put("serviceData", serviceData);
        } catch (JSONException e) {
        }
        return object.toString();
    }
}
