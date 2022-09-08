package com.hani.btapp.core.service

/**
 * Created by hanif on 2022-08-22.
 */
interface FirmwareStepsHandler {
    fun setGattInteractor(gattInteractor: GattInteractor)
    fun onStartUpdatingFirmware()
}