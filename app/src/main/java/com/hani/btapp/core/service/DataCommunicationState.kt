package com.hani.btapp.core.service

/**
 * Created by hanif on 2022-08-07.
 */
sealed interface DataCommunicationState {
    object Idle : DataCommunicationState
    object Ready : DataCommunicationState
    object Received : DataCommunicationState
    object Error : DataCommunicationState
    object ErrorGettingWriteHandle : DataCommunicationState
    object ErrorGettingReadHandle : DataCommunicationState
    data class Transmitting(val nrBytes: Int) : DataCommunicationState
    object FirmwareResponseError : DataCommunicationState
    object ErasingFlash : DataCommunicationState
    object SystemReset : DataCommunicationState
    object SendingFirmware : DataCommunicationState
    object ProgramOnePage : DataCommunicationState
    object WaitingResponse : DataCommunicationState
}