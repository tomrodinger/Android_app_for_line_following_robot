package com.hani.btapp.core.service

/**
 * Created by hanif on 2022-08-20.
 */
data class FirmwareUpdateUiState(
    val isUpdating: Boolean = false,
    val updateSteps: List<FirmwareUpdateUiStep> = listOf(),
    val totalBytes: Int = 0,
    val sentBytes: Int = 0,
) {
    fun addStep(step: FirmwareUpdateUiStep): FirmwareUpdateUiState {
        val steps = ArrayList(updateSteps)
        steps.add(step)
        return this.copy(updateSteps = steps)
    }
}