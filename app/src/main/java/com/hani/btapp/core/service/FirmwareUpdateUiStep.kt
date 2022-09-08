package com.hani.btapp.core.service

/**
 * Created by hanif on 2022-08-21.
 */
enum class FirmwareUpdateUiStep(val uiString: String) {
    ENTERING_BOOTLOADER("Entering Bootloader"),
    ERASING_FLASH("Erasing flash"),
    RECONNECTING("Reconnecting. Please wait."),
    SENDING_FIRMWARE("Sending firmware"),
    FIRMWARE_SENT("Firmware sent"),
    RESTARTING_SYSTEM("Restarting system"),
    COMPLETE("Firmware update done")
}