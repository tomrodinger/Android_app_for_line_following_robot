package com.hani.btapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hani.btapp.domain.Firmware
import com.hani.btapp.domain.Product
import com.hani.btapp.ui.FirmwareFileUiState
import com.hani.btapp.ui.FirmwareFilesListUiState
import com.hani.btapp.ui.theme.components.UiDivider
import com.hani.btapp.ui.theme.components.VerticalSpacer

/**
 * Created by hanif on 2022-08-22.
 */
@Composable
fun FirmwareChooserPopupDialog(
    firmwareFilesListUiState: FirmwareFilesListUiState,
    firmwareFileUiState: FirmwareFileUiState,
    onCancelClick: () -> Unit,
    onChooseClick: (Firmware) -> Unit,
    onRetryClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(color = Color.LightGray.copy(alpha = 0.8F)),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Card(
            shape = RoundedCornerShape(8.dp),
            elevation = 8.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Choose Firmware",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    fontSize = 18.sp,
                )
                UiDivider()
                MainUi(
                    firmwareFilesListUiState,
                    firmwareFileUiState,
                    onChooseClick,
                    onRetryClick
                )
                UiDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    Text(
                        text = "Cancel",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onCancelClick() }
                            .weight(1f)
                            .padding(12.dp)
                            .wrapContentWidth(Alignment.CenterHorizontally),
                    )
                }
            }
        }
    }
}

@Composable
private fun MainUi(
    firmwareFilesUiState: FirmwareFilesListUiState,
    firmwareFileUiState: FirmwareFileUiState,
    onChooseClick: (Firmware) -> Unit,
    onRetryClick: () -> Unit,
) {
    when {
        firmwareFilesUiState.loading || firmwareFileUiState.loading -> LoadingUi()
        firmwareFilesUiState.errorMessage != null -> ErrorUi(
            errorMessage = firmwareFilesUiState.errorMessage,
            onRetryClick = onRetryClick
        )
        firmwareFileUiState.error -> ErrorUi(onRetryClick = onRetryClick)
        else -> RobotUi(robot = firmwareFilesUiState.data!!, onChooseClick = onChooseClick)
    }
}

@Composable
private fun RobotUi(
    robot: Product,
    onChooseClick: (Firmware) -> Unit,
) {
    val model = robot.model
    val firmwares = robot.firmwares.sortedBy { it.date }
    Text(
        text = "Robot model: $model",
        fontSize = 18.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )
    UiDivider()

    LazyColumn(Modifier.fillMaxWidth()) {
        items(firmwares) { firmware ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clickable {
                        onChooseClick(firmware)
                    }
            ) {
                FirmwareUi(firmware)
                UiDivider()
            }
        }
    }
}

@Composable
private fun FirmwareUi(
    firmware: Firmware,
) {
    val name = firmware.url.removeSuffix(".bin")
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
        Text(text = name, fontWeight = FontWeight.SemiBold)
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text("${firmware.description}", maxLines = 2, fontSize = 12.sp)
            Text("Version: ${firmware.version}", fontSize = 12.sp)
            Text("Date: ${firmware.date}", fontSize = 12.sp)
        }
    }
}

@Composable
private fun LoadingUi() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        CircularProgressIndicator(
            strokeWidth = 3.dp,
            modifier = Modifier.size(32.dp)
        )
        VerticalSpacer(space = 12.dp)
        Text(text = "Loading")
    }
}

@Composable
private fun ErrorUi(
    errorMessage: String? = null,
    onRetryClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = errorMessage ?: "Error loading firmwares",
            fontWeight = FontWeight.Normal,
            color = Color.Red
        )
        VerticalSpacer(space = 8.dp)
        Button(onClick = onRetryClick) {
            Text(
                text = "Retry",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}