package com.hani.btapp.ui

import com.hani.btapp.domain.Firmware

/**
 * Created by hanif on 2022-07-29.
 */
sealed interface UserEvent {
    object StartScan : UserEvent
    object StopScan : UserEvent

    data class Connect(val address: String) : UserEvent
    object Disconnect : UserEvent
    object BackToScanner : UserEvent
    object UpdateFirmware : UserEvent
    object ChooseFirmware : UserEvent

    data class FirmwareChosen(val firmware: Firmware): UserEvent

}