package com.hani.btapp.core.scanner

/**
 * Created by hanif on 2022-07-24.
 */
data class BtScanResult(
    val deviceAddress: String,
    val rssi: Int, // [-127, 126],
    val nameUi: String = "-",
    val deviceType: Int,
    val isConnectable: Boolean? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (other !is BtScanResult) return false
        return deviceAddress == other.deviceAddress
    }

    override fun hashCode(): Int {
        return deviceAddress.hashCode()
    }
}