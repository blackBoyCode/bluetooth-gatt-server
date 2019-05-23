package com.example.bluetoothgattserver;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;

import java.util.UUID;
//SEND to git ...
public class CustomProfile {


    //Custom Service UUID
    public static UUID CUSTOM_SERVICE = UUID.fromString("df264931-0c69-481f-95e6-456ac7bb886f");//custom UUID provided by https://www.uuidgenerator.net/

    //Custom information characteristic
    public static UUID CUSTOM_CHARACTERISTIC = UUID.fromString("cf178e50-594e-4599-905e-8b53ab510cb7");

    //this will enable notification on a client device to receive characteristic changes
    //TODO important test defined CCCD "CAN I HAVE A CUSTOM ONE.."
    public static UUID CLIENT_CHARACTERISTIC_CONFIGURATION_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");


    /**
     * Return a configured {@link BluetoothGattService} instance for the
     * Custom Service.
     */
    public static BluetoothGattService createCustomService() {
        BluetoothGattService service = new BluetoothGattService(CUSTOM_SERVICE,
                BluetoothGattService.SERVICE_TYPE_PRIMARY);

        // Custom characteristic
        BluetoothGattCharacteristic customCharacteristic = new BluetoothGattCharacteristic(CUSTOM_CHARACTERISTIC,
                //Read and write characteristic
                BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_WRITE | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_WRITE | BluetoothGattCharacteristic.PERMISSION_READ);//TODO added property notify

        //TODO again testing...
        BluetoothGattDescriptor cccDescriptor = new BluetoothGattDescriptor(CLIENT_CHARACTERISTIC_CONFIGURATION_UUID, BluetoothGattDescriptor.PERMISSION_WRITE | BluetoothGattDescriptor.PERMISSION_READ );
        //cccDescriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); //doesn't work... :( can not set it this way..
        customCharacteristic.addDescriptor(cccDescriptor);


       // BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
       //         BluetoothGattCharacteristic.PERMISSION_READ);


        service.addCharacteristic(customCharacteristic);


        return service;
    }




}
