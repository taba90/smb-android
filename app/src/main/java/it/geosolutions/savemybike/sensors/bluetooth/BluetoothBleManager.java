package it.geosolutions.savemybike.sensors.bluetooth;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.ContextWrapper;
import android.support.v4.app.Fragment;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import it.geosolutions.savemybike.data.service.BluetoothService;
import it.geosolutions.savemybike.ui.activity.SaveMyBikeActivity;
import it.geosolutions.savemybike.ui.fragment.BleFragment;

/**
 * @author Marco Volpini, Geosolutions S.a.s
 * This class manages the access to  the bluetooth scanner as well as connection to ble device.
 * It is a singleton in order to control the access to the resource avoiding that the scanner is
 * used at the same time from different classes.
 */
public class BluetoothBleManager {

    private ScanSettings settings;

    private static final String TAG = BluetoothBleManager.class.getSimpleName();
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;

    private boolean active;

    private String mBluetoothDeviceAddress;
    private BluetoothGatt mBluetoothGatt;

    private ContextWrapper currentContext;

    public final static UUID UUID_SAVEMYBIKE_SERVICE = UUID.fromString("00001583-1212-efde-1523-785feabcd123");

    public final static UUID UUID_SAVEMYBIKE_CHARACTERISTIC = UUID.fromString("00001584-1212-efde-1523-785feabcd123");


    private static final List<ScanFilter> scanFilters =Collections.unmodifiableList(
            new ArrayList<ScanFilter>(){{new ScanFilter.Builder().setDeviceName("AirBlue").build();}});
                    //.setServiceUuid(new ParcelUuid(UUID_SAVEMYBIKE_CHARACTERISTIC)).build();}});
    private static final int STATE_DISCONNECTED = 0;
    private static final int STATE_CONNECTING = 1;
    private static final int STATE_CONNECTED = 2;

    private int mConnectionState = STATE_DISCONNECTED;

    private boolean scanning = false;

    private static BluetoothBleManager manager;

    private Consumer<ContextWrapper> deactivateCurrent;

    private ScanCallback scanCallback;

    private BluetoothBleManager(int scanMode) {
        this.settings = (new ScanSettings.Builder().setScanMode(scanMode)).build();
        this.scanCallback = new SMBScanCallBack();
    }

    public static BluetoothBleManager get (int scanMode) {
        if (manager==null){
            manager=new BluetoothBleManager(scanMode);
        }
        return manager;
    }

    public static BluetoothBleManager get () {
        if (manager==null){
            manager=new BluetoothBleManager(ScanSettings.SCAN_MODE_LOW_POWER);
        }
        return manager;
    }


    public boolean initialize(ContextWrapper wrapper) {
        if(isActive() && ! (currentContext instanceof Service))
            return false;
        if(currentContext!=null && !currentContext.equals(wrapper)){
            endCurrentContext();
        }
        currentContext=wrapper;
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) wrapper.getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }
        active = true;
        return active;
    }

    public void startScan() {
        if (!isScanning()) {
            if (!mBluetoothAdapter.isEnabled())
                 mBluetoothAdapter.enable();
            scanning = true;
            BluetoothLeScanner scanner = mBluetoothAdapter.getBluetoothLeScanner();
            scanner.startScan(null, settings, scanCallback);
        }
    }

    public void stopScan() {
        mBluetoothAdapter.getBluetoothLeScanner().flushPendingScanResults(scanCallback);
        if(isScanning()) {
            scanning = false;
            mBluetoothAdapter.getBluetoothLeScanner().stopScan(scanCallback);
        }
    }





    public boolean connect(final String address) {
        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG, "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }

        // Previously connected device.  Try to reconnect.
        if (mBluetoothDeviceAddress != null && address.equals(mBluetoothDeviceAddress)
                && mBluetoothGatt != null) {
            Log.d(TAG, "Trying to use an existing mBluetoothGatt for connection.");
            if (mBluetoothGatt.connect()) {
                mConnectionState = STATE_CONNECTING;
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
        if (device == null) {
            Log.w(TAG, "Device not found.  Unable to connect.");
            return false;
        }
        // We want to directly connect to the device, so we are setting the autoConnect
        // parameter to false.
        mBluetoothGatt = device.connectGatt(currentContext, false, mGattCallback);
        Log.d(TAG, "Trying to create a new connection.");
        mBluetoothDeviceAddress = address;
        mConnectionState = STATE_CONNECTING;
        return true;
    }


    public void disconnect() {
        if (mBluetoothAdapter == null || mBluetoothGatt == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        if (mConnectionState==STATE_CONNECTED)
             mBluetoothGatt.disconnect();
    }




    public String readCharacteristic(final BluetoothGattCharacteristic characteristic) {
        String result = null;
        final byte[] data = characteristic.getValue();
        if (data != null && data.length > 0) {
            final StringBuilder stringBuilder = new StringBuilder(data.length);
            for (byte byteChar : data)
                stringBuilder.append(String.format("%02X ", byteChar));
            result = stringBuilder.toString();
        }

        return result;
    }

    public void writeCharacteristic(BluetoothGattCharacteristic characteristic)
    {
        if (mBluetoothAdapter == null || mBluetoothGatt == null)
        {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGatt.writeCharacteristic(characteristic);
    }

    public void setAlarmMode(int mode)
    {
        if(mBluetoothGatt != null && mBluetoothGatt.getService(UUID_SAVEMYBIKE_SERVICE) != null)
        {
            BluetoothGattCharacteristic characteristic = mBluetoothGatt.getService(UUID_SAVEMYBIKE_SERVICE).getCharacteristic(UUID_SAVEMYBIKE_CHARACTERISTIC);
            if(characteristic != null)
            {
                byte[] strBytes = new byte[2];
                strBytes[0] = (byte)(0xA3);
                switch (mode)
                {
                    case 0:
                        strBytes[1] = (byte)(0x00); //NONE
                        break;
                    case 1:
                        strBytes[1] = (byte)(0x01); //BUZZ
                        break;
                    case 2:
                        strBytes[1] = (byte)(0x02); //LED
                        break;
                    case 3:
                        strBytes[1] = (byte)(0x03); //BUZLED
                        break;
                }
                characteristic.setValue(strBytes);
                writeCharacteristic(characteristic);
            }
        }
    }


    private List<BluetoothGattService> getSupportedGattServices() {
        if (mBluetoothGatt == null) return null;

        return mBluetoothGatt.getServices();
    }


    public boolean isScanning() {
        return scanning;
    }


    private void handleScanResult(Context context, List<String> addresses) {
        if (context instanceof BluetoothService) {
            addresses.forEach(a -> ((BluetoothService) context).issueRequest(a));
        } else if (context instanceof SaveMyBikeActivity) {
            SaveMyBikeActivity activity = (SaveMyBikeActivity) context;
            Fragment fragment = activity.getCurrentFragment();
            if (fragment instanceof BleFragment){
                addresses.forEach(a-> ((BleFragment)fragment).issueRequest(a));
                stopScan();
            }
        }
    }

    public boolean isBiggerThan21(){
        return android.os.Build.VERSION.SDK_INT >=21;
    }

    private void endCurrentContext(){
        stopScan();
        mBluetoothManager=null;
        mBluetoothAdapter=null;
        if (currentContext instanceof Service && BluetoothService.isRunning){
            ((Service) currentContext).stopForeground(true);
        }
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }


    private final class SMBScanCallBack extends ScanCallback {

        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            handleScanResult(currentContext, Arrays.asList(result.getDevice().getAddress()));
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            super.onBatchScanResults(results);
            List<String> addresses = results.stream().map(s -> s.getDevice().getAddress()).collect(Collectors.toList());
            handleScanResult(currentContext, addresses);
        }

        @Override
        public void onScanFailed(int errorCode) {
            super.onScanFailed(errorCode);
        }
    }


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mConnectionState = STATE_CONNECTED;
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:" +
                        mBluetoothGatt.discoverServices());
                getSupportedGattServices();
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mConnectionState = STATE_DISCONNECTED;
                Log.i(TAG, "Disconnected from GATT server.");
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {

            Log.w(TAG, "onServicesDiscovered received: " + status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //String result = readCharacteristic(characteristic);
                disconnect();
            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }
    };
}
