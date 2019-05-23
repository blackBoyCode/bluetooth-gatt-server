package com.example.bluetoothgattserver;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.ParcelUuid;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class GattServerActivity extends AppCompatActivity {
    private static final String TAG = GattServerActivity.class.getSimpleName();


    //our text from the layout
    private TextView normalText;
    private String byteReceive;

    //Bluetooth API
    private BluetoothManager mBluetoothManager;
    private BluetoothGattServer mBluetoothGattServer;
    private BluetoothLeAdvertiser mBluetoothLeAdvertiser;


    //for getting current device
    private BluetoothDevice mBluetoothDevice;
    //for getting characteristic reference..
    private BluetoothGattCharacteristic mBluetoothGattCharacteristic;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        normalText = findViewById(R.id.normal_text);

        //always keep display on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //get bluetooth adapter
        mBluetoothManager = (BluetoothManager) getSystemService(BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();

        //do we have bluetooth support?
        if (!checkBluetoothSupport(bluetoothAdapter)) {
            //finish activity
            finish();
        }

        // Register for system Bluetooth events
        IntentFilter filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(mBluetoothReceiver, filter);
        if (!bluetoothAdapter.isEnabled()) {
            Log.d(TAG, "Bluetooth is currently disabled...enabling");
            bluetoothAdapter.enable();
        } else {
            Log.d(TAG, "Bluetooth enabled...starting services");
            startAdvertising();
            startServer();
        }

        //TODO reference to characteristic

        mBluetoothGattCharacteristic = mBluetoothGattServer.getService(CustomProfile.CUSTOM_SERVICE)
                .getCharacteristic(CustomProfile.CUSTOM_CHARACTERISTIC);

    }


    /**
     * verify if we have bluetooth support
     * @param bluetoothAdapter {@link BluetoothAdapter}.
     * @return boolean value (true or false) to see if we have bluetooth support
     */
    private boolean checkBluetoothSupport(BluetoothAdapter bluetoothAdapter) {

        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported",Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Bluetooth Low Energy is not supported",Toast.LENGTH_SHORT).show();

            return false;
        }

        return true;
    }

    /**
     * Listens for Bluetooth adapter events to enable/disable
     * advertising and server functionality.
     */
    private BroadcastReceiver mBluetoothReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.STATE_OFF);

            switch (state) {
                case BluetoothAdapter.STATE_ON:
                    startAdvertising();
                    startServer();
                    break;
                case BluetoothAdapter.STATE_OFF:
                    stopServer();
                    stopAdvertising();
                    break;
                default:
                    // Do nothing
            }

        }
    };



    /**
     * Begin advertising over Bluetooth that this device is connectable
     * and supports the Custom Service.
     */
    private void startAdvertising() {
        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (mBluetoothLeAdvertiser == null) {
            Log.w(TAG, "Failed to create advertiser");
            return;
        }

        AdvertiseSettings settings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .build();

        AdvertiseData data = new AdvertiseData.Builder()
                .setIncludeDeviceName(true)
                .setIncludeTxPowerLevel(false)
                .addServiceUuid(new ParcelUuid(CustomProfile.CUSTOM_SERVICE))
                .build();

        mBluetoothLeAdvertiser
                .startAdvertising(settings, data, mAdvertiseCallback);
    }


    /**
     * Stop Bluetooth advertisements.
     */
    private void stopAdvertising() {
        if (mBluetoothLeAdvertiser == null) return;

        mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
    }


    /**
     * Callback to receive information about the advertisement process.
     */
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            Log.i(TAG, "LE Advertise Started.");
        }

        @Override
        public void onStartFailure(int errorCode) {
            Log.w(TAG, "LE Advertise Failed: " + errorCode);
        }

    };


    private BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
            super.onConnectionStateChange(device, status, newState);


            //add bluetooth device to variable if state connected
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                //postDeviceChange(device, true);
                mBluetoothDevice = device;
                Log.d("BLUETOOTH SUCCESS", "connected");

            }else{
                Log.d("BLUETOOTH ERROR", "not connected");
            }

        }

        //to receive data (reading)??
        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {

            if (CustomProfile.CUSTOM_CHARACTERISTIC.equals(characteristic.getUuid())) {
                Toast.makeText(GattServerActivity.this,"readingRequest",Toast.LENGTH_SHORT).show();

                Log.i(TAG, "Read Custom Characteristic");
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0, "onCharacteristicReadActivate".getBytes());
            } else {
                // Invalid characteristic
                Log.w(TAG, "Invalid Characteristic Read: " + characteristic.getUuid());
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_FAILURE,
                        0,
                        null);
            }

        }

        // when your receive your bytes from the client
        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {

            Log.d("BLUETOOTH","PASS_1");

            if (CustomProfile.CUSTOM_CHARACTERISTIC.equals(characteristic.getUuid())) {

                //Toast.makeText(GattServerActivity.this,"writingRequest",Toast.LENGTH_SHORT).show();

                byteReceive = new String(value, StandardCharsets.UTF_8);
                //TODO testing for now...not final
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        normalText.setText(byteReceive);
                    }
                });//cannot run with normalText.setText alone UI THREAD must be separated
                   //the logic thing to do here is to use Handler


               // Toast.makeText(GattServerActivity.this, byteReceive, Toast.LENGTH_SHORT).show();

                Log.d("BYTE RECEIVED", byteReceive);
               // normalText.setText(byteReceive);


                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_SUCCESS,
                            0,
                            null);
                }
            } else {
                Log.w(TAG, "Unknown characteristic write request");
                if (responseNeeded) {
                    mBluetoothGattServer.sendResponse(device,
                            requestId,
                            BluetoothGatt.GATT_FAILURE,
                            0,
                            null);
                }
            }



        }


        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId, BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

            //set the value to enable notifications from client
            descriptor.setValue(value);

            //Important lesson: always have responseNeeded in anycase for the client to pick up
            if (responseNeeded) {
                mBluetoothGattServer.sendResponse(device,
                        requestId,
                        BluetoothGatt.GATT_SUCCESS,
                        0,
                        null);
            }


        }





    };






    /**
     * Initialize the GATT server instance with the services/characteristics
     * from the CustomProfile.
     */
    private void startServer() {
        mBluetoothGattServer = mBluetoothManager.openGattServer(this, mGattServerCallback);
        if (mBluetoothGattServer == null) {
            Log.w(TAG, "Unable to create GATT server");
            return;
        }

        mBluetoothGattServer.addService(CustomProfile.createCustomService());

    }


    /**
     * Shut down the GATT server.
     */
    private void stopServer() {
        if (mBluetoothGattServer == null) return;

        mBluetoothGattServer.close();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        BluetoothAdapter bluetoothAdapter = mBluetoothManager.getAdapter();
        if (bluetoothAdapter.isEnabled()) {
            stopServer();
            stopAdvertising();
        }

        unregisterReceiver(mBluetoothReceiver);
    }





    //TODO Button presses
    public void onButtonPressOne(View view){

        Toast.makeText(this,"ButtonPressONE",Toast.LENGTH_SHORT).show();
        byte[] bytesToSend = "ANDROID: sending ONE".getBytes();

        mBluetoothGattCharacteristic.setValue(bytesToSend);
        mBluetoothGattServer.notifyCharacteristicChanged(mBluetoothDevice, mBluetoothGattCharacteristic, false);

    }

    public void onButtonPressTwo(View view){

        Toast.makeText(this,"ButtonPressTWO",Toast.LENGTH_SHORT).show();
        byte[] bytesToSend = "ANDROID: sending TWO".getBytes();

        mBluetoothGattCharacteristic.setValue(bytesToSend);
        mBluetoothGattServer.notifyCharacteristicChanged(mBluetoothDevice, mBluetoothGattCharacteristic, false);

    }




}
