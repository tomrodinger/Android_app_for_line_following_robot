package com.hani.btapp.core.scanner

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice.DEVICE_TYPE_DUAL
import android.bluetooth.BluetoothDevice.DEVICE_TYPE_LE
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.os.Build
import com.hani.btapp.Logger
import com.hani.btapp.core.BluetoothProvider
import com.hani.btapp.core.com.CommunicationScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by hanif on 2022-07-24.
 */
class BleScanner @Inject constructor(
    private val bluetoothProvider: BluetoothProvider,
    private val scope: CommunicationScope,
) : BtScanner {

    private val _state = MutableStateFlow(BtScannerState())
    val state: StateFlow<BtScannerState> = _state

    // Stops scanning after 10 seconds.
    private val scanPeriod = 10_000L

    private val bleScanner by lazy {
        bluetoothProvider.bluetoothManager.adapter.bluetoothLeScanner
    }

    private var scanJob: Job? = null

    @SuppressLint("MissingPermission")
    override fun startScan() {
        Logger.log("startBLEScan")
        _state.value = _state.value.copy(
            isScanning = true,
            scannedDevices = listOf()
        )
        val scanSettings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build()
        bleScanner.startScan(null, scanSettings, callback)
        stopScan(immediate = false)
    }

    override fun stopScan(immediate: Boolean) {
        scanJob?.cancel()
        if (!immediate) {
            scanJob = scope.launch {
                delay(scanPeriod)
                stopScan()
            }
        } else {
            stopScan()
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScan() {
        if (!_state.value.isScanning) {
            return
        }
        Logger.log("stopScan")
        bleScanner.stopScan(callback)
        _state.value = _state.value.copy(isScanning = false)
        logScannedDevices()
    }

    private fun logScannedDevices() {
        Logger.log("------- SCANNED DEVICES SUMMARY ---------")
        val devices = _state.value.scannedDevices
        Logger.log("${devices.size} devices")
        devices.forEach {
            Logger.log(
                "DEVICE: " +
                        "name: ${it.nameUi}, " +
                        "type: ${it.deviceType}, " +
                        "address: ${it.deviceAddress}, " +
                        "rssi: ${it.rssi}, " +
                        "isConnectable: ${it.isConnectable}"
            )
        }
        Logger.log("-----------------------------------------")
    }

    private val callback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult?) {
            super.onScanResult(callbackType, result)
            result?.let { scanResult ->
                val device = scanResult.device
                if (device.type != DEVICE_TYPE_LE && device.type != DEVICE_TYPE_DUAL) {
                    return
                }

                val deviceName = when {
                    !device.name.isNullOrEmpty() -> device.name
                    !scanResult.scanRecord?.deviceName.isNullOrEmpty() -> scanResult.scanRecord?.deviceName
                        ?: "-"
                    else -> "-"
                }

                val isConnectable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                    scanResult.isConnectable else null

                Logger.log(
                    "New scan result: " +
                            "name: $deviceName " +
                            "type: ${device.type}, " +
                            "MAC:${device.address}, " +
                            "isConnectable:$isConnectable"
                )
                val res = BtScanResult(
                    deviceAddress = device.address,
                    rssi = scanResult.rssi,
                    nameUi = deviceName,
                    deviceType = device.type,
                    isConnectable = isConnectable,
                )
                val currentDevices = HashSet(_state.value.scannedDevices)
                if (currentDevices.contains(res)) {
                    currentDevices.remove(res)
                    currentDevices.add(res)
                } else {
                    currentDevices.add(res)
                }
                _state.value = _state.value.copy(
                    scannedDevices = currentDevices.sortedByDescending { device -> device.rssi }
                )
            }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)

            _state.value = _state.value.copy(error = true)
            scope.launch { stopScan(immediate = true) }
        }
    }

}