package io.dcloud.feature.iBeacon;

import android.bluetooth.BluetoothDevice;

import java.util.Calendar;

public class IBeaconRecord {
    String address;
    String uuid;
    int major;
    int minor;
    int txPower;
    int rssi;
    long timeStampMillis;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public int getMajor() {
        return major;
    }

    public void setMajor(int major) {
        this.major = major;
    }

    public int getMinor() {
        return minor;
    }

    public void setMinor(int minor) {
        this.minor = minor;
    }

    public int getTxPower() {
        return txPower;
    }

    public void setTxPower(int txPower) {
        this.txPower = txPower;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public long getTimeStampMillis() {
        return timeStampMillis;
    }

    public void setTimeStampMillis(long timeStampMillis) {
        this.timeStampMillis = timeStampMillis;
    }

    public double getAccuracy() {
        if (rssi >= -1 || txPower >= 0)
            return -1.0D;

        double ratio = Double.parseDouble(String.valueOf(rssi)) / txPower;
        double rssiCorrection = 0.96D + Math.pow(Math.abs(rssi), 3.0D) % 10.0D / 150.0D;
        if (ratio <= 1.0D) {
            return Math.pow(ratio, 9.98D) * rssiCorrection;
        }
        double distance = Math.max(0, (0.103D + 0.89978D * Math.pow(ratio, 7.5D)) * rssiCorrection);

        if (Double.NaN == distance) {
            return -1.0D;
        }
        return distance;
    }

    public static IBeaconRecord fromScanData(BluetoothDevice device, byte[] scanData, int rssi) {

        int startByte = 2;
        boolean patternFound = false;
        while (startByte <= 5) {
            int start2 = ((int) scanData[startByte + 2] & 0xff);
            int start3 = ((int) scanData[startByte + 3] & 0xff);
            if ( start2== 0x02
                    &&  start3== 0x15) {
                // yes! This is an iBeacon
                patternFound = true;
                break;
            } else if (((int) scanData[startByte] & 0xff) == 0x2d
                    && ((int) scanData[startByte + 1] & 0xff) == 0x24
                    && ((int) scanData[startByte + 2] & 0xff) == 0xbf
                    && ((int) scanData[startByte + 3] & 0xff) == 0x16) {

                return null;
            } else if (((int) scanData[startByte] & 0xff) == 0xad
                    && ((int) scanData[startByte + 1] & 0xff) == 0x77
                    && ((int) scanData[startByte + 2] & 0xff) == 0x00
                    && ((int) scanData[startByte + 3] & 0xff) == 0xc6) {

                return null;
            }
            startByte++;
        }

        if (patternFound == false) {
            return null;
        }

        IBeaconRecord record = new IBeaconRecord();

        record.address = device.getAddress();
        // 获得Major属性
        record.major = (scanData[startByte + 20] & 0xff) * 0x100 + (scanData[startByte + 21] & 0xff);
        // 获得Minor属性
        record.minor = (scanData[startByte + 22] & 0xff) * 0x100 + (scanData[startByte + 23] & 0xff);
        record.txPower = (int)scanData[startByte+24];
        record.rssi = rssi;

        try {
            byte[] proximityUuidBytes = new byte[16];
            System.arraycopy(scanData, startByte + 4, proximityUuidBytes, 0, 16);
            String hexString = bytesToHex(proximityUuidBytes);
            StringBuilder sb = new StringBuilder();
            sb.append(hexString.substring(0, 8));
            sb.append("-");
            sb.append(hexString.substring(8, 12));
            sb.append("-");
            sb.append(hexString.substring(12, 16));
            sb.append("-");
            sb.append(hexString.substring(16, 20));
            sb.append("-");
            sb.append(hexString.substring(20, 32));
            record.uuid = sb.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        record.timeStampMillis = Calendar.getInstance().getTimeInMillis();

        return record;
    }

    private final static char[] hexArray = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
            '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    private static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
