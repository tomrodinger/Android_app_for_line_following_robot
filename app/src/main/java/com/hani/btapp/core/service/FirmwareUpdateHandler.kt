package com.hani.btapp.core.service

import android.content.Context
import com.hani.btapp.Logger
import com.hani.btapp.core.com.CommunicationScope
import com.hani.btapp.core.com.ServiceCommunicationChannel
import com.hani.btapp.core.writer.*
import com.hani.btapp.utils.toHexString
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by hanif on 2022-08-21.
 */

private const val RESPONSE_OK = 0
private const val RESPONSE_NOK = -1
private const val MAX_NR_UPDATE_ATTEMPTS = 10

private const val MAGIC_CODE = "BL702BOOT"

@Singleton
class FirmwareUpdateHandler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scope: CommunicationScope,
    private val serviceCommunicationChannel: ServiceCommunicationChannel,
) : RobotStateListener, FirmwareStepsHandler {

    private lateinit var gattInteractor: GattInteractor
    private lateinit var firmwareData: BleFirmwareData

    // Used to check if response if sent from the robot.
    // This will be filled when onCharacteristicChanged is called.
    private val rxQueue = LinkedList<ByteArray>()

    private var firmwareStepsUiState = FirmwareUpdateUiState()
    private var isWriteDone = false
    private var responseJob: Job? = null
    private var firmwareUpdateJob: Job? = null

    override fun onDisconnected(userInitiated: Boolean) {
        Logger.log("Robot disconnected: User initiated: $userInitiated")

        if (userInitiated) {
            firmwareUpdateJob?.cancel()
            broadcastFirmwareStepUiState { firmwareStepsUiState = FirmwareUpdateUiState() }
        }

        responseJob?.cancel()
        clearRxQueue()
        if (this::firmwareData.isInitialized) {
            firmwareData.reset()
        }
    }

    override fun onDataWritten() {
        isWriteDone = true
    }

    override fun onResponse(data: ByteArray) {
        val totalBytes = firmwareData.totalBytes
        val bytesRead = firmwareData.bytesRead
        val bytesReadPercentage = (bytesRead.toDouble() / totalBytes.toDouble()) * 100
        Logger.log("$bytesRead/$totalBytes : $bytesReadPercentage %")
        broadcastFirmwareStepUiState {
            firmwareStepsUiState = firmwareStepsUiState.copy(
                totalBytes = totalBytes,
                sentBytes = bytesRead
            )
        }
        rxQueue.add(data)
    }

    override fun setGattInteractor(gattInteractor: GattInteractor) {
        this.gattInteractor = gattInteractor
        firmwareUpdateJob?.cancel()
        responseJob?.cancel()
        clearRxQueue()
        if (this::firmwareData.isInitialized) {
            firmwareData.reset()
        }
        clearStateForUi()
    }

    override fun onStartUpdatingFirmware() {
        var isSuccess = false
        firmwareUpdateJob?.cancel()
        firmwareUpdateJob = scope.launch {
            // Read in firmware data
            val data = getFirmwareData()
            data?.let { data ->
                firmwareData = BleFirmwareData(data)
                val size = data.size
                Logger.log("Firmware file read: $size bytes")

                for (attempts in 0..MAX_NR_UPDATE_ATTEMPTS) {
                    if (firmwareUpdateJob?.isActive == false) {
                        Logger.log("firmwareUpdateJob canceled")
                        break
                    }
                    Logger.log("Attempt number: ${attempts + 1}")

                    broadcastFirmwareStepUiState {
                        firmwareStepsUiState = FirmwareUpdateUiState(isUpdating = true)
                    }

                    gattInteractor.connectIfNeeded()

                    enterBootLoaderMode()
                    eraseFlash(size)

                    updateFirmwareUiStep(FirmwareUpdateUiStep.RECONNECTING)
                    gattInteractor.reconnect(15_000)

                    updateFirmwareUiStep(FirmwareUpdateUiStep.SENDING_FIRMWARE)
                    firmwareData.reset()

                    var firmwareChunk = firmwareData.getNextChunk()
                    var response = RESPONSE_NOK
                    while (firmwareChunk != null) {
                        response = sendOnePage(firmwareChunk)
                        if (response == RESPONSE_NOK) {
                            break
                        }
                        firmwareChunk = firmwareData.getNextChunk()
                    }

                    when (response) {
                        RESPONSE_OK -> {
                            Logger.log("Firmware sent")
                            updateFirmwareUiStep(FirmwareUpdateUiStep.FIRMWARE_SENT)
                            systemReset()
                            isSuccess = true
                        }
                        else -> Logger.log("TIME OUT or cancelled")
                    }

                    if (isSuccess) {
                        updateFirmwareUiStep(FirmwareUpdateUiStep.COMPLETE)
                        break
                    }
                }

                broadcastFirmwareStepUiState {
                    firmwareStepsUiState = firmwareStepsUiState.copy(isUpdating = false)
                }
            }
        }
    }

    private fun broadcastFirmwareStepUiState(update: () -> Unit) {
        update.invoke()
        serviceCommunicationChannel.publishFirmwareStepState(firmwareStepsUiState)
    }

    private fun getFirmwareData(): ByteArray? {
        val file = File(context.filesDir, "firmwarewbootheader.bin")
        if (!file.exists()) {
            Logger.log("Internal firmware file could not be found")
            return null
        }
        return file.readBytes()
    }

    private fun updateFirmwareUiStep(step: FirmwareUpdateUiStep) {
        broadcastFirmwareStepUiState {
            firmwareStepsUiState = firmwareStepsUiState.addStep(step)
        }
    }

    private fun clearStateForUi() {
        broadcastFirmwareStepUiState {
            firmwareStepsUiState = FirmwareUpdateUiState()
        }
    }

    private fun clearRxQueue() {
        rxQueue.clear()
    }

    private suspend fun enterBootLoaderMode() {
        Logger.log("Entering boot loader")
        updateFirmwareUiStep(FirmwareUpdateUiStep.ENTERING_BOOTLOADER)
        writeRawData(MAGIC_CODE.encodeToByteArray())
        while (!isWriteDone) {
            delay(10)
        }
        Logger.log("Robot in Boot loader mode")
    }

    private suspend fun eraseFlash(firmwareSize: Int) {
        Logger.log("Erasing flash")
        updateFirmwareUiStep(FirmwareUpdateUiStep.ERASING_FLASH)
        clearRxQueue()
        val eraseFlashPayload = EraseFlashCommandPayload(firmwareSize)
        writeRobotData(eraseFlashPayload.get())
        Logger.log("Flash erased")
    }

    private suspend fun sendOnePage(pageData: ByteArray): Int {
        clearRxQueue()
        val onePagePayload = ProgramOnePageCommandPayload(pageData).get()
        Logger.log("Transferring firmware: ${onePagePayload.size} bytes")
        writeRobotData(onePagePayload)
        return awaitResponse()
    }

    private suspend fun systemReset() {
        Logger.log("Perform system reset")
        updateFirmwareUiStep(FirmwareUpdateUiStep.RESTARTING_SYSTEM)
        val systemResetPayload = SystemResetCommandPayload()
        writeRobotData(systemResetPayload.get())
        Logger.log("System reset done")
        delay(100)
    }

    private suspend fun awaitResponse(): Int {
        var response = RESPONSE_NOK
        try {
            responseJob?.cancel()
            responseJob = scope.launch { response = getResponse() }
            responseJob?.join()
        } catch (c: CancellationException) {
            return response
        }
        return response
    }

    private suspend fun getResponse(): Int {
        var timeout = 300
        while (rxQueue.isEmpty() && timeout > 0) {
            Logger.log("Waiting for response...")
            timeout -= 1
            delay(10)
        }
        if (timeout == 0) {
            Logger.log("Time out. Did not receive response from BLE device")
            return RESPONSE_NOK
        }
        val response = rxQueue.pop()
        val responseHex = response.toHexString()
        Logger.log("Received a response: $responseHex, ${String(response)}")

        if (response.size != 2) {
            Logger.log("Error: didn't receive enough bytes in the response: ${response.size}")
            return RESPONSE_NOK
        }

        val header = response[0].toUByte().toInt()
        if (response[0].toInt() != 0x4f) {
            Logger.log("Error: response NACK or unknown response: $header")
            return RESPONSE_NOK
        }

        Logger.log("Got valid response: $responseHex: ${String(response)}")
        return RESPONSE_OK
    }


    private suspend fun writeRobotData(data: ByteArray) {
        val robotData = RobotData(data)
        var chunk = robotData.getNextChunk()
        while (chunk != null) {
            writeRawData(chunk)
            while (!isWriteDone)
                delay(10)
            chunk = robotData.getNextChunk()
        }
    }

    private fun writeRawData(data: ByteArray) {
        isWriteDone = false
        gattInteractor.writeData(data)
    }


}