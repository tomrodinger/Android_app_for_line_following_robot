package com.hani.btapp.extensions

import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattService
import com.hani.btapp.core.GattAttributes

/**
 * Created by hanif on 2022-07-25.
 */

fun BluetoothGattCharacteristic.isReadable() =
    hasProperty(BluetoothGattCharacteristic.PROPERTY_READ)

fun BluetoothGattCharacteristic.isWritable() =
    hasProperty(BluetoothGattCharacteristic.PROPERTY_WRITE)

fun BluetoothGattCharacteristic.isWritableWithoutResponse() =
    hasProperty(BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)

fun BluetoothGattCharacteristic.canNotify() =
    hasProperty(BluetoothGattCharacteristic.PROPERTY_NOTIFY)

fun BluetoothGattCharacteristic.hasProperty(property: Int) =
    this.properties and property != 0

fun BluetoothGattService.isBattery() =
    this.uuid == GattAttributes.GATT_SERVICE_BATTERY

fun BluetoothGattCharacteristic.isBatteryLevel() =
    this.uuid == GattAttributes.GATT_CHARACTERISTIC_BATTERY_LEVEL