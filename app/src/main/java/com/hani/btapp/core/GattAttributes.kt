package com.hani.btapp.core

import java.util.*

/**
 * Created by hanif on 2022-07-26.
 */
object GattAttributes {

    // 16-bit UUID Numbers
    // https://btprodspecificationrefs.blob.core.windows.net/assigned-values/16-bit%20UUID%20Numbers%20Document.pdf

    // On some device, allowed to read only when paired
    val GATT_SERVICE_BATTERY = UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")
    val GATT_CHARACTERISTIC_BATTERY_LEVEL = UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb")

    // Generic access
    val GATT_SERVICE_GENERIC_ACCESS = UUID.fromString("00001800-0000-1000-8000-00805f9b34fb")
    val GATT_CHARACTERISTIC_GENERIC_ACCESS_DEVICE_NAME = UUID.fromString("00002a00-0000-1000-8000-00805f9b34fb")
    val GATT_CHARACTERISTIC_GENERIC_ACCESS_APPEARANCE = UUID.fromString("00002a01-0000-1000-8000-00805f9b34fb")

    // Device information
    val GATT_SERVICE_DEVICE_INFO = UUID.fromString("0000180a-0000-1000-8000-00805f9b34fb")
    val GATT_CHARACTERISTIC_DEVICE_INFO_MANUFACTURER_NAME = UUID.fromString("00002a29-0000-1000-8000-00805f9b34fb")
    val GATT_CHARACTERISTIC_DEVICE_INFO_MODEL_NUMBER_STRING = UUID.fromString("00002a24-0000-1000-8000-00805f9b34fb")

    // Client Characteristic Configuration. Used to set characteristic change notification.
    // See: https://medium.com/@martijn.van.welie/making-android-ble-work-part-3-117d3a8aee23
    val CCC_DESCRIPTOR_UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")


    // Service UUID 0000fff0-0000-1000-8000-00805f9b34fb
    val GATT_CHARACTERISTIC_ROBOT_READ = UUID.fromString("00070001-0745-4650-8d93-df59be2fc10a")
    val GATT_CHARACTERISTIC_ROBOT_WRITE = UUID.fromString("00070002-0745-4650-8d93-df59be2fc10a")


}