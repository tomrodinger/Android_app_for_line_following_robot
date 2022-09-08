package com.hani.btapp.core.scanner

/**
 * Created by hanif on 2022-07-29.
 */
data class BtScannerState(
    val isScanning: Boolean = false,
    val error: Boolean = false,
    val scannedDevices: List<BtScanResult> = listOf(),
)