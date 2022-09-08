package com.hani.btapp.permission

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.hani.btapp.AndroidUtils
import com.hani.btapp.Logger
import com.hani.btapp.core.BluetoothProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Created by hanif on 2022-07-24.
 */

@AssistedFactory
interface PermissionHandlerFactory {
    fun create(activity: ComponentActivity): PermissionHandler
}

class PermissionHandler @AssistedInject constructor(
    @Assisted private val activity: ComponentActivity,
    private val bluetoothProvider: BluetoothProvider
) {

    private val _state = MutableStateFlow(PermissionState())
    val state: StateFlow<PermissionState> = _state

    private val permissionsLauncher: ActivityResultLauncher<Array<String>> =
        activity.registerForActivityResult(
            ActivityResultContracts
                .RequestMultiplePermissions()
        ) {
        }
    private val enableBluetoothLauncher: ActivityResultLauncher<Intent> =
        activity.registerForActivityResult(
            ActivityResultContracts
                .StartActivityForResult()
        ) {
        }

    private val btAdapter by lazy {
        bluetoothProvider.bluetoothManager.adapter
    }

    fun check() {
        Logger.log("check")
        val hasPermission = AndroidUtils.isBluetoothPermissionGranted(activity)
        val isEnabled = btAdapter.isEnabled
        _state.value = _state.value.copy(
            hasPermission = hasPermission,
            isEnabled = isEnabled
        )
        Logger.log("Has Permission: $hasPermission")
        Logger.log("Is Bluetooth enabled : $isEnabled")
        if (!hasPermission) {
            requestBluetoothPermission()
        } else if (!isEnabled) {
            turnOnBluetooth()
        }
    }

    private fun requestBluetoothPermission() {
        Logger.log("requestBluetoothPermission")
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.BLUETOOTH
                )
            )
        } else {
            permissionsLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.BLUETOOTH_CONNECT,
                    Manifest.permission.BLUETOOTH_SCAN
                )
            )
        }
    }

    private fun turnOnBluetooth() {
        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }

}