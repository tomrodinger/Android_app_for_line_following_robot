package com.hani.btapp.ui.screens

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hani.btapp.AndroidUtils
import com.hani.btapp.BuildConfig
import com.hani.btapp.R
import com.hani.btapp.core.service.DataCommunicationState
import com.hani.btapp.core.service.FirmwareUpdateUiState
import com.hani.btapp.core.service.FirmwareUpdateUiStep
import com.hani.btapp.core.service.GattConnectionState
import com.hani.btapp.ui.UserEvent
import com.hani.btapp.ui.theme.components.AppTopBar
import com.hani.btapp.ui.theme.components.HorizontalSpacer
import com.hani.btapp.ui.theme.components.MenuItem
import com.hani.btapp.ui.theme.components.VerticalSpacer
import kotlinx.coroutines.launch

data class ConnectedDeviceScreenData(
    val gattConnectionState: GattConnectionState,
    val dataCommunicationState: DataCommunicationState,
    val firmwareUpdateUiState: FirmwareUpdateUiState,
    val firmwareName: String,
)

/**
 * Created by hanif on 2022-08-07.
 */
@Composable
fun ConnectedDeviceScreen(
    screenData: ConnectedDeviceScreenData,
    onUserEvent: (UserEvent) -> Unit,
) {

    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()
    val isConnected = screenData.gattConnectionState is
            GattConnectionState.Connected
    val isDisconnected = screenData.gattConnectionState is
            GattConnectionState.Disconnected
    val isConnecting = screenData.gattConnectionState is
            GattConnectionState.Connecting
    val isDiscoveringServices = (screenData.gattConnectionState as?
            GattConnectionState.Connected)?.discoveringServices ?: false

    val deviceAddress = (screenData.gattConnectionState as?
            GattConnectionState.Connected)?.address ?: "-"
    val deviceName = (screenData.gattConnectionState as?
            GattConnectionState.Connected)?.deviceName ?: "-"

    val readyToCommunicate = (screenData.gattConnectionState
            is GattConnectionState.Connected) &&
            screenData.gattConnectionState.readyToCommunicate

    Column {
        AppTopBar(
            title = "OTA Firmware Update",
            scaffoldState = scaffoldState,
            scope = coroutineScope
        )
        Scaffold(
            scaffoldState = scaffoldState,
            drawerContent = {
                VerticalSpacer(space = 4.dp)
                MenuItem("Scanner", enabled = true) {
                    onUserEvent(UserEvent.BackToScanner)
                    coroutineScope.launch { scaffoldState.drawerState.close() }
                }
                VerticalSpacer(space = 4.dp)
                MenuItem("Disconnect", enabled = isConnected) {
                    onUserEvent(UserEvent.Disconnect)
                    coroutineScope.launch { scaffoldState.drawerState.close() }
                }
                VerticalSpacer(space = 4.dp)
                MenuItem(
                    "Choose Firmware",
                    enabled = !screenData.firmwareUpdateUiState.isUpdating
                ) {
                    onUserEvent(UserEvent.ChooseFirmware)
                    coroutineScope.launch { scaffoldState.drawerState.close() }
                }
                VerticalSpacer(space = 4.dp)
                MenuItem(
                    "Update Firmware", enabled = readyToCommunicate &&
                            !screenData.firmwareUpdateUiState.isUpdating
                ) {
                    onUserEvent(UserEvent.UpdateFirmware)
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = "Status:  ", fontSize = 20.sp)
                        val stateText = when {
                            isDiscoveringServices -> "Discovering Services"
                            isConnected -> "Connected"
                            isConnecting -> "connecting"
                            isDisconnected -> "disconnected"
                            else -> "Idle"
                        }
                        Text(
                            text = stateText.uppercase(),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        HorizontalSpacer(space = 8.dp)
                        if (isConnecting) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    when {
                        isConnected -> R.drawable.ic_bluetooth_connected
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

                TextWithName(name = "MAC", text = deviceAddress, Modifier.padding(bottom = 8.dp))
                TextWithName(name = "Device", text = deviceName, Modifier.padding(bottom = 8.dp))

                RxTxMessageUi(screenData.dataCommunicationState)

                val firmwareName = screenData.firmwareName
                val firmwareUpdateState = screenData.firmwareUpdateUiState

                if (firmwareName.isNotEmpty()) {
                    TextWithName(name = "Firmware file", text = firmwareName, Modifier.padding(top = 8.dp))
                }
                Button(
                    onClick = {
                        onUserEvent.invoke(UserEvent.ChooseFirmware)
                    },
                    enabled = readyToCommunicate && !firmwareUpdateState.isUpdating,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(text = "Choose Firmware File", fontWeight = FontWeight.Bold)
                }
                if (firmwareName.isNotEmpty()) {
                    Button(
                        onClick = {
                            onUserEvent.invoke(UserEvent.UpdateFirmware)
                        },
                        enabled = readyToCommunicate && !firmwareUpdateState.isUpdating,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(text = "Start Firmware Update", fontWeight = FontWeight.Bold)
                    }

                    VerticalSpacer(space = 8.dp)
                    FirmwareUpdateStepsUi(firmwareUpdateState)
                }
            }
        }
    }
}

@Composable
private fun TextWithName(
    name: String,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier,

    ) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(text = "$name: ")
        }
        Text(text = text)
    }
}

@Composable
private fun RxTxMessageUi(commState: DataCommunicationState) {
    Row(modifier = Modifier.fillMaxWidth()) {
        val communicationStateString =
            when (commState) {
                DataCommunicationState.Idle -> "Idle"
                DataCommunicationState.Received -> "Received"
                DataCommunicationState.Error -> "Error"
                is DataCommunicationState.Transmitting ->
                    "Transmitting ${commState.nrBytes} bytes"
                DataCommunicationState.Ready -> "Ready"
                DataCommunicationState.ErasingFlash -> "Erasing flash"
                DataCommunicationState.ProgramOnePage -> "Programming page"
                DataCommunicationState.SendingFirmware -> "Sending Firmware"
                DataCommunicationState.WaitingResponse -> "Waiting for response"
                DataCommunicationState.SystemReset -> "Performing system reset"
                DataCommunicationState.ErrorGettingReadHandle -> "Error: Read handle"
                DataCommunicationState.ErrorGettingWriteHandle -> "Error: Write handle"
                DataCommunicationState.FirmwareResponseError -> "Robot response error"
            }
        TextWithName(name = "Rx/Tx", text = communicationStateString)
    }
}

@Composable
private fun FirmwareUpdateStepsUi(firmwareUpdateState: FirmwareUpdateUiState) {
    val scrollState = rememberScrollState()
    LaunchedEffect(key1 = firmwareUpdateState) {
        scrollState.scrollTo(scrollState.maxValue)
    }
    val steps = firmwareUpdateState.updateSteps
    LazyColumn(
        Modifier
            .fillMaxWidth()
    ) {
        itemsIndexed(steps) { index, step ->
            val isLatest = index == steps.lastIndex
            val fontWeight = if (isLatest) FontWeight.SemiBold else FontWeight.Normal
            val isStepDone = !isLatest || step == FirmwareUpdateUiStep.COMPLETE
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isStepDone) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_check),
                        contentDescription = "check",
                        modifier = Modifier.size(16.dp),
                    )
                } else {
                    CircularProgressIndicator(
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(16.dp)
                    )
                }
                HorizontalSpacer(space = 8.dp)
                Text(
                    text = step.uiString,
                    fontWeight = fontWeight,
                )
            }
            if (step == FirmwareUpdateUiStep.SENDING_FIRMWARE &&
                firmwareUpdateState.totalBytes > 0 && firmwareUpdateState.sentBytes > 0
            ) {
                val total = (firmwareUpdateState.totalBytes) / 1024
                val sent = (firmwareUpdateState.sentBytes) / 1024
                val percentage = ((sent.toDouble() / total.toDouble()) * 100).toInt()
                val progressString = "$sent / $total KBytes,  $percentage %"
                Text(
                    text = progressString,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
    }
}
