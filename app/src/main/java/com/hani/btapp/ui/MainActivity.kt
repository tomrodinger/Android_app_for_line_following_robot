package com.hani.btapp.ui

import android.annotation.SuppressLint
import android.bluetooth.BluetoothManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.hani.btapp.Logger
import com.hani.btapp.extensions.collectWith
import com.hani.btapp.core.com.ServiceCommunicationChannel
import com.hani.btapp.core.service.BluetoothLeService
import com.hani.btapp.core.service.DataCommunicationState
import com.hani.btapp.core.service.FirmwareUpdateUiState
import com.hani.btapp.core.service.GattConnectionState
import com.hani.btapp.permission.PermissionHandler
import com.hani.btapp.permission.PermissionHandlerFactory
import com.hani.btapp.permission.PermissionState
import com.hani.btapp.toast
import com.hani.btapp.ui.screens.*
import com.hani.btapp.ui.theme.MyBtAppTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var permissionHandlerFactory: PermissionHandlerFactory

    @Inject
    lateinit var communicationChannel: ServiceCommunicationChannel

    private lateinit var viewModel: MainActivityViewModel
    private lateinit var permissionHandler: PermissionHandler

    private var bluetoothService: BluetoothLeService? = null

    private var isServiceBound = false

    private val btManager by lazy {
        getSystemService(BluetoothManager::class.java)
    }

    @OptIn(ExperimentalComposeUiApi::class)
    @SuppressLint("MissingPermission")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        packageManager.takeIf { !it.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE) }?.also {
            toast("Bluetooth low energy(BLE) not supported by device")
            finish()
        }

        permissionHandler = permissionHandlerFactory.create(this)
        viewModel = ViewModelProvider(this)[MainActivityViewModel::class.java]

        viewModel.userEvents.collectWith(this) { onUserEvent(it) }
        permissionHandler.state.collectWith(this) { onNewPermissionState(it) }

        setContent {
            MyBtAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    MainContent()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        permissionHandler.check()
    }

    override fun onDestroy() {
        super.onDestroy()

        if (isServiceBound) {
            unbindService(serviceConnection)
            isServiceBound = false
        }
    }

    private fun onUserEvent(event: UserEvent) {
        when (event) {
            is UserEvent.Connect -> bluetoothService?.connectToDevice(
                address = event.address, userInitiated = true)
            UserEvent.Disconnect -> bluetoothService?.disconnect()
            UserEvent.BackToScanner -> bluetoothService?.disconnect()
            UserEvent.UpdateFirmware -> bluetoothService?.startUpdatingFirmware()
        }
    }

    private fun onNewPermissionState(state: PermissionState) {
        if (state.hasPermission && state.isEnabled && bluetoothService == null && !isServiceBound) {
            bindService(
                Intent(this@MainActivity, BluetoothLeService::class.java),
                serviceConnection, Context.BIND_AUTO_CREATE
            )
        }
    }

    // Code to manage Service lifecycle.
    private val serviceConnection: ServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(
            componentName: ComponentName,
            service: IBinder
        ) {
            Logger.log("Service Connected")
            isServiceBound = true
            bluetoothService = (service as BluetoothLeService.LocalBinder).getService()
            bluetoothService?.let { bluetoothService ->
                bluetoothService.initialize(btManager.adapter)
                viewModel.startScan()

                bluetoothService.robotBLEType.collectWith(this@MainActivity) {
                    if (it != null) {
                        viewModel.setRobotBLEType(it)
                    }
                }
            }
        }

        override fun onServiceDisconnected(componentName: ComponentName) {
            Logger.log("Service Disconnected")
            isServiceBound = false
            bluetoothService = null
        }
    }

    @Composable
    private fun MainContent() {

        val navController = rememberNavController()

        val permissionState by permissionHandler.state.collectAsState()
        val scannerState by viewModel.scannerState.collectAsState()
        val connectionState by communicationChannel.gattConnectionState.collectAsState(
            initial = GattConnectionState.Idle
        )
        val dataCommunicationState by communicationChannel.dataCommunicationState.collectAsState(
            initial = DataCommunicationState.Idle
        )
        var selectedDeviceAddress by remember {
            mutableStateOf(bluetoothService?.getConnectedDeviceAddress())
        }
        val firmwareStepsState by communicationChannel.firmwareUpdateSteps.collectAsState(
            initial = FirmwareUpdateUiState()
        )
        val firmwareFilesListUiState by viewModel.firmwareFilesListUiState.collectAsState()
        val firmwareFileUiState by viewModel.firmwareFileUiState.collectAsState()

        val closeFirmwareChooser by viewModel.closeFirmwareChooser.collectAsState()
        if (closeFirmwareChooser) {
            navController.popBackStack()
            viewModel.consumeFirmwareChooserDialog()
        }

        NavHost(
            navController = navController,
            startDestination = "scanner_screen"
        ) {
            composable(route = "scanner_screen") {
                MainScreen(
                    screenData = MainScreenData(
                        bluetoothOn = permissionState.isEnabled,
                        permissionsOk = permissionState.hasPermission,
                        scannerState = scannerState,
                        gattConnectionState = connectionState,
                        connectedDeviceAddress = selectedDeviceAddress,
                    ),
                    onUserEvent = {
                        when (it) {
                            is UserEvent.Connect -> navController.navigate("connected_screen")
                        }
                        viewModel.onUserEvent(it)
                    },
                )
            }
            composable(route = "connected_screen") {
                ConnectedDeviceScreen(
                    screenData = ConnectedDeviceScreenData(
                        gattConnectionState = connectionState,
                        dataCommunicationState = dataCommunicationState,
                        firmwareUpdateUiState = firmwareStepsState,
                        firmwareName = firmwareFileUiState.name ?: "",
                    ),
                    onUserEvent = {
                        when (it) {
                            is UserEvent.Disconnect -> navController.popBackStack()
                            is UserEvent.BackToScanner -> navController.popBackStack()
                            is UserEvent.ChooseFirmware ->
                                navController.navigate("firmware_chooser")
                        }
                        viewModel.onUserEvent(it)
                    },
                )
            }
            composable(route = "firmware_chooser") {
                FirmwareChooserPopupDialog(
                    firmwareFilesListUiState = firmwareFilesListUiState,
                    firmwareFileUiState = firmwareFileUiState,
                    onCancelClick = {
                        navController.popBackStack()
                    },
                    onChooseClick = {
                        viewModel.onUserEvent(UserEvent.FirmwareChosen(it))
                    },
                    onRetryClick = {
                        viewModel.onUserEvent(UserEvent.ChooseFirmware)
                    },
                )
            }
        }
    }

}