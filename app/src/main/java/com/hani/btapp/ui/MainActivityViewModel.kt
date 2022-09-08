package com.hani.btapp.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hani.btapp.Logger
import com.hani.btapp.core.FinalFirmwareCreator
import com.hani.btapp.core.scanner.BleScanner
import com.hani.btapp.core.service.RobotBLEModel
import com.hani.btapp.data.firmware.FirmwareFetcher
import com.hani.btapp.domain.Firmware
import com.hani.btapp.domain.Product
import com.hani.btapp.domain.RobotModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Created by hanif on 2022-07-23.
 */

data class FirmwareFilesListUiState(
    val loading: Boolean = false,
    val errorMessage: String? = null,
    val data: Product? = null,
)

data class FirmwareFileUiState(
    val loading: Boolean = false,
    val error: Boolean = false,
    val name: String? = null,
)

@HiltViewModel
class MainActivityViewModel @Inject constructor(
    private val btScanner: BleScanner,
    private val firmwareFetcher: FirmwareFetcher,
    private val finalFirmwareCreator: FinalFirmwareCreator,
) : ViewModel() {

    val scannerState by lazy {
        btScanner.state
    }

    private val _userEvents = MutableSharedFlow<UserEvent>()
    val userEvents: SharedFlow<UserEvent> = _userEvents

    private val _firmwareFilesListUiState = MutableStateFlow(FirmwareFilesListUiState())
    val firmwareFilesListUiState: StateFlow<FirmwareFilesListUiState> = _firmwareFilesListUiState

    private val _firmwareFileUiState = MutableStateFlow(FirmwareFileUiState())
    val firmwareFileUiState: StateFlow<FirmwareFileUiState> = _firmwareFileUiState

    private val _closeFirmwareChooser = MutableStateFlow(false)
    val closeFirmwareChooser: StateFlow<Boolean> = _closeFirmwareChooser

    private var robotBLEModel: RobotBLEModel? = null

    fun startScan() {
        viewModelScope.launch {
            btScanner.startScan()
        }
    }

    fun setRobotBLEType(robotBLEType: RobotBLEModel) {
        this.robotBLEModel = robotBLEType
    }

    fun onUserEvent(event: UserEvent) {
        when (event) {
            UserEvent.StartScan -> startScan()
            UserEvent.StopScan -> stopScan()
            is UserEvent.Connect -> {
                viewModelScope.launch {
                    btScanner.stopScan(immediate = true)
                    _userEvents.emit(event)
                }
            }
            UserEvent.ChooseFirmware -> viewModelScope.launch { fetchFirmwareFiles() }
            is UserEvent.FirmwareChosen -> viewModelScope.launch {
                loadFirmware(event.firmware)
            }

            else -> viewModelScope.launch { _userEvents.emit(event) }
        }
    }

    fun consumeFirmwareChooserDialog() {
        _closeFirmwareChooser.value = false
    }

    private fun stopScan() {
        viewModelScope.launch { btScanner.stopScan(immediate = true) }
    }

    private suspend fun loadFirmware(firmware: Firmware) {
        Logger.log("Loading firmware file ${firmware.url}...")
        _firmwareFileUiState.value = _firmwareFileUiState.value.copy(
            loading = true, error = false)

        var error = true
        val res = firmwareFetcher.getFirmware(firmware.url)
        when {
            res.isSuccess -> {
                val data = res.getOrNull()
                if (data != null) {
                    Logger.log("Firmware file ${firmware.url}, read: ${data.size} bytes")

                    if (finalFirmwareCreator.createFirmwareWithBootHeader(data)) {
                        error = false
                        _firmwareFileUiState.value = _firmwareFileUiState.value.copy(
                            loading = false, error = false, name = firmware.url)
                    }
                }
            }
        }

        if (error) {
            Logger.log("Error loading firmware file ${firmware.url}")
            _firmwareFileUiState.value = _firmwareFileUiState.value.copy(
                loading = false, error = true)
        }

        _closeFirmwareChooser.value = true
    }

    private suspend fun fetchFirmwareFiles() {
        _firmwareFilesListUiState.value = _firmwareFilesListUiState.value.copy(
            loading = true, errorMessage = null
        )

        delay(1_000)
        val res = firmwareFetcher.fetchAvailableFirmwares()
        when {
            res.isSuccess -> {
                res.getOrNull()?.let { product ->
                    val robotModel = getRobotModelForBle(product.model)
                    if (robotModel != robotBLEModel) {
                        _firmwareFilesListUiState.value = _firmwareFilesListUiState.value.copy(
                            loading = false,
                            errorMessage = "Could not fetch product info. " +
                                    "Mismatch in Robot model: ${product.model},$robotBLEModel",
                            data = null
                        )
                    } else {
                        _firmwareFilesListUiState.value = _firmwareFilesListUiState.value.copy(
                            loading = false,
                            errorMessage = null,
                            data = product
                        )
                    }
                } ?: kotlin.run {
                    _firmwareFilesListUiState.value = _firmwareFilesListUiState.value.copy(
                        loading = false,
                        errorMessage = "Could not fetch product info",
                        data = null,
                    )
                }
            }
            else -> _firmwareFilesListUiState.value = _firmwareFilesListUiState.value.copy(
                loading = false,
                errorMessage = "Could not fetch product info",
                data = null,
            )
        }
    }

    private fun getRobotModelForBle(model: String): RobotBLEModel? {
        return when (RobotModel.parse(model)) {
            RobotModel.R1 -> RobotBLEModel.BL702

            else -> null
        }
    }


}