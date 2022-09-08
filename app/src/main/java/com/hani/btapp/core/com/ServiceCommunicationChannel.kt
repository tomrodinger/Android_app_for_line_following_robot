package com.hani.btapp.core.com

import com.hani.btapp.core.service.DataCommunicationState
import com.hani.btapp.core.service.FirmwareUpdateUiState
import com.hani.btapp.core.service.GattConnectionState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by hanif on 2022-07-29.
 */
@Singleton
class ServiceCommunicationChannel @Inject constructor(
    private val scope: CommunicationScope
) {

    private val _gattConnectionState = MutableSharedFlow<GattConnectionState>()
    val gattConnectionState: SharedFlow<GattConnectionState> = _gattConnectionState

    private val _dataCommunicationState = MutableSharedFlow<DataCommunicationState>()
    val dataCommunicationState: SharedFlow<DataCommunicationState> = _dataCommunicationState

    private val _firmwareUpdateSteps = MutableStateFlow(FirmwareUpdateUiState())
    val firmwareUpdateSteps: StateFlow<FirmwareUpdateUiState> = _firmwareUpdateSteps

    fun publishConnectionState(state: GattConnectionState) {
        scope.launch {
            _gattConnectionState.emit(state)
        }
    }

    fun publishDataCommunicationState(state: DataCommunicationState) {
        scope.launch {
            _dataCommunicationState.emit(state)
        }
    }

    fun publishFirmwareStepState(state: FirmwareUpdateUiState) {
        scope.launch {
            _firmwareUpdateSteps.emit(state)
        }
    }

}