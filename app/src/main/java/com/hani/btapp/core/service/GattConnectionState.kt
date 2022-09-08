package com.hani.btapp.core.service

/**
 * Created by hanif on 2022-07-25.
 */

sealed interface GattConnectionState {
    object Idle : GattConnectionState
    object Connecting : GattConnectionState
    object DiscoverServicesFailed : GattConnectionState

    data class Connected(
        val address: String?,
        val readyToCommunicate: Boolean,
        val discoveringServices: Boolean = false,
        val deviceName: String = "-"
    ) : GattConnectionState

    sealed interface Disconnected : GattConnectionState {
        object UserInitiated : Disconnected
        object FirmwareUpdate : Disconnected
        data class Error(val statusCode: Int) : Disconnected
    }
}