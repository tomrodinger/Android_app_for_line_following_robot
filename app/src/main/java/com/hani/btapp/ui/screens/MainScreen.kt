package com.hani.btapp.ui.screens

import android.bluetooth.BluetoothDevice
import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hani.btapp.AndroidUtils
import com.hani.btapp.BuildConfig
import com.hani.btapp.R
import com.hani.btapp.core.scanner.BtScanResult
import com.hani.btapp.core.scanner.BtScannerState
import com.hani.btapp.core.service.GattConnectionState
import com.hani.btapp.ui.UserEvent
import com.hani.btapp.ui.theme.components.AppTopBar
import com.hani.btapp.ui.theme.components.HorizontalSpacer
import com.hani.btapp.ui.theme.components.MenuItem
import com.hani.btapp.ui.theme.components.VerticalSpacer
import kotlinx.coroutines.launch

/**
 * Created by hanif on 2022-07-27.
 */

data class MainScreenData(
    val bluetoothOn: Boolean = false,
    val permissionsOk: Boolean = false,
    val scannerState: BtScannerState = BtScannerState(),
    val gattConnectionState: GattConnectionState = GattConnectionState.Idle,
    val connectedDeviceAddress: String? = null,
)

@Composable
fun MainScreen(
    screenData: MainScreenData,
    onUserEvent: (UserEvent) -> Unit,
) {
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val readyToUse = screenData.bluetoothOn && screenData.permissionsOk
    val isScanning = screenData.scannerState.isScanning
    val isConnected = screenData.gattConnectionState is
            GattConnectionState.Connected
    val isDisconnected = screenData.gattConnectionState is
            GattConnectionState.Disconnected
    val isConnecting = screenData.gattConnectionState is
            GattConnectionState.Connecting
    val isDiscoveringServices = (screenData.gattConnectionState as?
            GattConnectionState.Connected)?.discoveringServices ?: false

    Column {
        AppTopBar(
            title = "OTA Firmware Update",
            scaffoldState = scaffoldState,
            scope = coroutineScope
        )
        Scaffold(
            scaffoldState = scaffoldState,
            drawerContent = {
                MenuItem("Start scan", enabled = readyToUse && !isScanning) {
                    onUserEvent(UserEvent.StartScan)
                    coroutineScope.launch { scaffoldState.drawerState.close() }
                }
                VerticalSpacer(space = 4.dp)
                MenuItem("Stop scan", enabled = readyToUse && isScanning) {
                    onUserEvent(UserEvent.StopScan)
                    coroutineScope.launch { scaffoldState.drawerState.close() }
                }
                VerticalSpacer(space = 8.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text(text = "App version: ${BuildConfig.VERSION_NAME}", fontSize = 12.sp)
                    VerticalSpacer(space = 8.dp)
                    Text(
                        text = "${AndroidUtils.getDeviceName()}, SDK: ${Build.VERSION.SDK_INT}",
                        fontSize = 12.sp
                    )
                }
            }
        ) {
            var showScanButton = readyToUse && !isScanning
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                ) {
                    Box(Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = "Status:  ", fontSize = 20.sp)
                            val stateText = when {
                                isScanning -> "Scanning"
                                isDiscoveringServices -> "Discovering Services"
                                screenData.scannerState.error -> "Scan error"
                                isConnected -> "Connected"
                                isConnecting -> "connecting"
                                isDisconnected -> "disconnected"
                                !screenData.bluetoothOn -> "Bluetooth turned off"
                                !screenData.permissionsOk -> "Permission needed"
                                else -> "Idle"
                            }
                            Text(
                                text = stateText.uppercase(),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold
                            )

                            HorizontalSpacer(space = 8.dp)
                            if (isScanning || isConnecting) {
                                CircularProgressIndicator(
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        when {
                            isConnected -> R.drawable.ic_bluetooth_connected
                            isScanning -> R.drawable.ic_bluetooth_scan
                            isDisconnected -> R.drawable.ic_bluetooth_disconnected
                            else -> null
                        }?.let { imageRes ->
                            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                                Image(
                                    painter = painterResource(id = imageRes),
                                    contentDescription = "status",
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(color = Color.Blue),
                                )
                            }
                        }
                    }

                    VerticalSpacer(space = 8.dp)

                    ScannedDevices(
                        devices = screenData.scannerState.scannedDevices,
                        connectedDeviceAddress = screenData.connectedDeviceAddress,
                        isConnected = isConnected,
                        onDeviceClick = { onUserEvent(UserEvent.Connect(it)) }
                    )
                }

                Column(Modifier.align(Alignment.BottomCenter)) {
                    BigScanButton(
                        visible = showScanButton,
                        onScanClick = { onUserEvent.invoke(UserEvent.StartScan) },
                    )
                }
            }
        }
    }
}

@Composable
private fun BigScanButton(
    visible: Boolean,
    onScanClick: () -> Unit,
) {
    val density = LocalDensity.current
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically {
            with(density) { 160.dp.roundToPx() }
        },
        exit = slideOutVertically {
            with(density) { 160.dp.roundToPx() }
        }
    ) {
        Box(contentAlignment = Alignment.Center) {
            OutlinedButton(
                onClick = onScanClick,
                shape = CircleShape,
                modifier = Modifier
                    .padding(horizontal = 2.dp)
                    .size(136.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colors.onPrimary,
                    backgroundColor = MaterialTheme.colors.primary,
                )
            ) {
                // Leave empty
            }
            Text(
                text = "SCAN",
                color = MaterialTheme.colors.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            )
        }
    }
}

@Composable
private fun ScannedDevices(
    devices: List<BtScanResult>,
    onDeviceClick: (String) -> Unit,
    isConnected: Boolean,
    connectedDeviceAddress: String?,
) {
    val detectedDevices = devices.size
    if (detectedDevices > 0) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = "Detected devices:  ", fontSize = 20.sp)
            Text(
                text = "$detectedDevices".uppercase(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
        }
        VerticalSpacer(space = 16.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(state = rememberScrollState(), enabled = true)
        ) {
            devices.forEach {
                DeviceEntry(
                    it,
                    isConnected = isConnected && it.deviceAddress == connectedDeviceAddress,
                    onClick = { adrs -> onDeviceClick(adrs) }
                )
            }
            VerticalSpacer(space = 136.dp)
        }
    }
}

@Composable
private fun RoundedText(
    text: String
) {
    val contentColor = Color.Black
    val backgroundColor = Color.White
    Box(contentAlignment = Alignment.Center) {
        OutlinedButton(
            onClick = { },
            shape = CircleShape,
            modifier = Modifier
                .padding(horizontal = 2.dp)
                .size(42.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = contentColor,
                backgroundColor = backgroundColor,
            )
        ) {
            // Leave empty
        }
        Text(text = text, color = contentColor)
    }
}

@Composable
private fun DeviceEntry(
    device: BtScanResult,
    isConnected: Boolean,
    onClick: (String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        elevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .clickable {
                onClick(device.deviceAddress)
            },
        backgroundColor = if (isConnected) MaterialTheme.colors.primary else
            MaterialTheme.colors.onPrimary
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(device.nameUi.uppercase(), fontSize = 18.sp)
                }
                VerticalSpacer(space = 4.dp)
                Text(
                    device.deviceAddress.uppercase(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Light
                )
                VerticalSpacer(space = 4.dp)
                val deviceType = when (device.deviceType) {
                    BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
                    BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
                    BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
                    BluetoothDevice.DEVICE_TYPE_UNKNOWN -> "Unknown"
                    else -> "Unknown"
                }
                Text(
                    buildAnnotatedString {
                        withStyle(
                            style = SpanStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Light
                            )
                        ) {
                            append("Device type: ")
                        }
                        withStyle(
                            style = SpanStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        ) {
                            append(deviceType)
                        }
                    }
                )
            }
            Box(modifier = Modifier.align(Alignment.CenterEnd)) {
                Row {
                    RoundedText(device.rssi.toString())
                }
            }
        }
    }
}