package com.hani.btapp.core.service

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE
import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.lifecycle.LifecycleService
import com.hani.btapp.Logger
import com.hani.btapp.core.*
import com.hani.btapp.core.GattAttributes.GATT_CHARACTERISTIC_DEVICE_INFO_MANUFACTURER_NAME
import com.hani.btapp.core.GattAttributes.GATT_CHARACTERISTIC_DEVICE_INFO_MODEL_NUMBER_STRING
import com.hani.btapp.core.GattAttributes.GATT_CHARACTERISTIC_GENERIC_ACCESS_APPEARANCE
import com.hani.btapp.core.GattAttributes.GATT_CHARACTERISTIC_GENERIC_ACCESS_DEVICE_NAME
import com.hani.btapp.core.GattAttributes.GATT_CHARACTERISTIC_ROBOT_READ
import com.hani.btapp.core.GattAttributes.GATT_CHARACTERISTIC_ROBOT_WRITE
import com.hani.btapp.core.GattAttributes.GATT_SERVICE_DEVICE_INFO
import com.hani.btapp.core.GattAttributes.GATT_SERVICE_GENERIC_ACCESS
import com.hani.btapp.core.com.CommunicationScope
import com.hani.btapp.core.com.ServiceCommunicationChannel
import com.hani.btapp.data.firmware.FirmwareFetcher
import com.hani.btapp.extensions.canNotify
import com.hani.btapp.extensions.isReadable
import com.hani.btapp.extensions.isWritable
import com.hani.btapp.extensions.isWritableWithoutResponse
import com.hani.btapp.utils.toHex
import com.hani.btapp.utils.toInt
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

/**
 * Created by hanif on 2022-07-24.
 */

const val GATT_SUCCESS = BluetoothGatt.GATT_SUCCESS

@AndroidEntryPoint
class BluetoothLeService : LifecycleService(), GattInteractor {

    private val binder = LocalBinder()

    @Inject
    lateinit var serviceCommunicationChannel: ServiceCommunicationChannel

    @Inject
    lateinit var robotStateListener: RobotStateListener

    @Inject
    lateinit var firmwareStepsHandler: FirmwareStepsHandler

    private lateinit var btAdapter: BluetoothAdapter

    private val _robotBleType = MutableStateFlow<RobotBLEModel?>(null)
    val robotBLEType: StateFlow<RobotBLEModel?> = _robotBleType

    private val callbackScope = object : CoroutineScope {
        override val coroutineContext: CoroutineContext = Dispatchers.Main + SupervisorJob()
    }

    // Commands queue is used to read and write to gatt characteristics
    private val commandsQueue = GattCommandQueue(queueName = "COMMANDS")

    private var bluetoothGatt: BluetoothGatt? = null
    private var connectedAddress: String? = null
    private var readHandle: BluetoothGattCharacteristic? = null
    private var writeHandle: BluetoothGattCharacteristic? = null
    private var servicesReady = false
    private var notificationReady = false

    override fun onBind(intent: Intent): IBinder {
        super.onBind(intent)
        return binder
    }

    override fun onUnbind(intent: Intent?): Boolean {
        closeGattConnection()
        return super.onUnbind(intent)
    }

    override fun writeData(data: ByteArray) {
        writeHandle?.let { it -> writeCharacteristic(it, data) }
    }

    override suspend fun reconnect(delayAfterDisconnectMs: Long) {
        Logger.log("Disconnect and reconnect after $delayAfterDisconnectMs ms")
        broadcastState(GattConnectionState.Disconnected.FirmwareUpdate)
        closeGattConnection()
        delay(delayAfterDisconnectMs)
        reconnect()
    }

    override suspend fun connectIfNeeded() {
        if (!servicesReady || !notificationReady || readHandle == null || writeHandle == null) {
            reconnect()
        }
    }

    fun initialize(btAdapter: BluetoothAdapter) {
        this.btAdapter = btAdapter
        firmwareStepsHandler.setGattInteractor(this)
    }

    fun getConnectedDeviceAddress(): String? {
        return connectedAddress
    }

    @SuppressLint("MissingPermission")
    fun connectToDevice(address: String, userInitiated: Boolean = false): Boolean {
        Logger.log("connectToDevice: $address")
        if (userInitiated) {
            firmwareStepsHandler.setGattInteractor(this)
        }
        if (connectedAddress == address && bluetoothGatt != null) {
            Logger.log("Already connected to: $address")
            return false
        }
        return try {
            broadcastState(GattConnectionState.Connecting)
            val device = btAdapter.getRemoteDevice(address)
            bluetoothGatt = device.connectGatt(this, false, gattCallback)
            true
        } catch (e: Exception) {
            Logger.log("Could not connect. Error: $e")
            false
        }
    }

    fun disconnect() {
        robotStateListener.onDisconnected(userInitiated = true)
        closeGattConnection()
        broadcastState(GattConnectionState.Disconnected.UserInitiated)
    }

    @SuppressLint("MissingPermission")
    fun startUpdatingFirmware() {
        Logger.log("updateFirmware")
        if (!servicesReady) {
            Logger.log("service not ready")
            return
        }

        firmwareStepsHandler.onStartUpdatingFirmware()
    }

    private fun broadcastState(state: GattConnectionState) {
        serviceCommunicationChannel.publishConnectionState(state)
    }

    private fun broadcastCommunicationState(state: DataCommunicationState) {
        serviceCommunicationChannel.publishDataCommunicationState(state)
    }

    @SuppressLint("MissingPermission")
    private fun closeGattConnection() {
        Logger.log("Closing GATT connection")
        robotStateListener.onDisconnected(userInitiated = false)
        commandsQueue.clear()
        servicesReady = false
        notificationReady = false
        writeHandle = null
        readHandle = null
        bluetoothGatt?.let { gatt ->
            gatt.close()
            bluetoothGatt = null
        }
    }

    @SuppressLint("MissingPermission")
    private fun discoverServices() {
        bluetoothGatt?.discoverServices()
    }

    private fun onDeviceNameRead(deviceName: String) {
        broadcastState(
            GattConnectionState.Connected(
                address = connectedAddress,
                readyToCommunicate = servicesReady,
                discoveringServices = false,
                deviceName = deviceName,
            )
        )

        _robotBleType.value = RobotBLEModel.parse(deviceName)
    }

    private val gattCallback = object : BluetoothGattCallback() {

        override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            logConnectionState(status, newState)

            callbackScope.launch {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    when (newState) {
                        BluetoothProfile.STATE_CONNECTED -> {
                            broadcastState(
                                GattConnectionState.Connected(
                                    gatt?.device?.address,
                                    readyToCommunicate = servicesReady,
                                    discoveringServices = true
                                )
                            )
                            connectedAddress = gatt?.device?.address
                            bluetoothGatt = gatt
                            discoverServices()
                        }
                    }
                } else {
                    broadcastState(GattConnectionState.Disconnected.Error(statusCode = status))
                    closeGattConnection()
                }
            }
        }

        override fun onCharacteristicChanged(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?
        ) {
            super.onCharacteristicChanged(gatt, characteristic)

            val copy = characteristic?.value?.copyOf()
            if (characteristic?.uuid == readHandle?.uuid) {
                callbackScope.launch {
                    copy?.let { copy ->
                        robotStateListener.onResponse(copy)
                    }
                }
            }
        }

        override fun onCharacteristicWrite(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicWrite(gatt, characteristic, status)

            callbackScope.launch {
                Logger.log("onCharacteristicWrite. status: $status: " +
                        "${if (status == GATT_SUCCESS) "Success" else "Error"}")
                if (status == GATT_SUCCESS) {
                    robotStateListener.onDataWritten()
                }
            }
        }

        override fun onCharacteristicRead(
            gatt: BluetoothGatt?,
            characteristic: BluetoothGattCharacteristic?,
            status: Int
        ) {
            super.onCharacteristicRead(gatt, characteristic, status)
            Logger.log("onCharacteristicRead. UUID:${characteristic?.instanceId}, status:$status")

            if (status == GATT_SUCCESS) {
                characteristic?.let {
                    when (it.uuid) {
                        GATT_CHARACTERISTIC_GENERIC_ACCESS_DEVICE_NAME -> {
                            val deviceName = it.getStringValue(0)
                            onDeviceNameRead(deviceName)
                            "Device name: $deviceName"
                        }
                        GATT_CHARACTERISTIC_GENERIC_ACCESS_APPEARANCE -> {
                            val appearance = it.value?.let { byteArray ->
                                val intValue = byteArray.toInt()
                                val hexValue = byteArray.toHex()
                                when (BleAppearance.fromByteArray(byteArray)) {
                                    BleAppearance.PHONE -> "PHONE"
                                    BleAppearance.COMPUTER -> "COMPUTER"
                                    BleAppearance.CLOCK -> "CLOCK"
                                    BleAppearance.WATCH -> "WATCH"
                                    BleAppearance.DISPLAY -> "DISPLAY"
                                    BleAppearance.UNKNOWN -> "unknown by app: $intValue, $hexValue"
                                }
                            } ?: " - "
                            "Appearance: $appearance"
                        }
                        GATT_CHARACTERISTIC_DEVICE_INFO_MANUFACTURER_NAME ->
                            "Manufacturer: ${it.getStringValue(0)}"
                        GATT_CHARACTERISTIC_DEVICE_INFO_MODEL_NUMBER_STRING ->
                            "Model number: ${it.getStringValue(0)}"

                        else -> null
                    }
                }?.let {
                    Logger.log(it)
                }
            } else {
                Logger.log("Failed to read characteristic, UUID: ${characteristic?.uuid}")
            }

            commandsQueue.completeCommand()
        }

        override fun onServiceChanged(gatt: BluetoothGatt) {
            super.onServiceChanged(gatt)
            Logger.log("onServiceChanged")
            discoverServices()
        }

        override fun onDescriptorWrite(
            gatt: BluetoothGatt?,
            descriptor: BluetoothGattDescriptor?,
            status: Int
        ) {
            super.onDescriptorWrite(gatt, descriptor, status)

            callbackScope.launch {
                if (status == GATT_SUCCESS) {
                    Logger.log("Robot connected and ready!")
                    notificationReady = true
                }
                commandsQueue.completeCommand()
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
            super.onServicesDiscovered(gatt, status)

            callbackScope.launch {
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    servicesReady = true
                    broadcastState(
                        GattConnectionState.Connected(
                            address = gatt?.device?.address,
                            readyToCommunicate = true
                        )
                    )

                    gatt?.services?.forEach { service ->
                        grabReadAndWriteCharacteristics(service)

                        val serviceUUID = service.uuid
                        logService(service)
                        when (serviceUUID) {
                            GATT_SERVICE_GENERIC_ACCESS -> readGenericAccessService(service)
                            GATT_SERVICE_DEVICE_INFO -> readDeviceInfoService(service)
                        }
                    }

                    if (writeHandle == null || readHandle == null) {
                        Logger.log("Could not get read or write handle: " +
                                "writeHandle:$writeHandle, readHandle:$readHandle")
                        disconnect()
                        broadcastCommunicationState(
                            if (writeHandle == null)
                                DataCommunicationState.ErrorGettingWriteHandle else
                                DataCommunicationState.ErrorGettingReadHandle
                        )
                    }
                } else {
                    broadcastState(GattConnectionState.DiscoverServicesFailed)
                    broadcastState(
                        GattConnectionState.Connected(
                            gatt?.device?.address,
                            readyToCommunicate = false,
                            discoveringServices = false,
                        )
                    )
                }
            }
        }
    }

    private fun grabReadAndWriteCharacteristics(service: BluetoothGattService) {
        service.getCharacteristic(GATT_CHARACTERISTIC_ROBOT_READ)?.let {
            Logger.log("Found read characteristic")
            when {
                !it.isReadable() -> Logger.log("Read characteristic not readable!")
                !it.canNotify() -> Logger.log("Read characteristic not notifiable!")
                else -> {
                    Logger.log("Read characteristic ready!")
                    readHandle = it
                    subscribeNotifyForReadHandle()
                }
            }
        }
        service.getCharacteristic(GATT_CHARACTERISTIC_ROBOT_WRITE)?.let {
            Logger.log("Found write characteristic")
            when {
                it.isWritableWithoutResponse() || it.isWritable() -> {
                    Logger.log("Write characteristic ready!")
                    broadcastCommunicationState(DataCommunicationState.Ready)
                    writeHandle = it
                }
                else -> Logger.log("Write characteristic not writable!")
            }
        }
    }

    private fun readGenericAccessService(service: BluetoothGattService) {
        service.getCharacteristic(GATT_CHARACTERISTIC_GENERIC_ACCESS_DEVICE_NAME)?.let {
            commandsQueue.add(object : PieceOfWork {
                override val name = "readCharacteristic"
                override fun execute() {
                    readCharacteristic(it)
                }
            })
        }
        service.getCharacteristic(GATT_CHARACTERISTIC_GENERIC_ACCESS_APPEARANCE)?.let {
            commandsQueue.add(object : PieceOfWork {
                override val name = "readCharacteristic"
                override fun execute() {
                    readCharacteristic(it)
                }
            })
        }
    }

    private fun readDeviceInfoService(service: BluetoothGattService) {
        service.getCharacteristic(GATT_CHARACTERISTIC_DEVICE_INFO_MANUFACTURER_NAME)?.let {
            commandsQueue.add(object : PieceOfWork {
                override val name = "readCharacteristic"
                override fun execute() {
                    readCharacteristic(it)
                }
            })
        }
        service.getCharacteristic(GATT_CHARACTERISTIC_DEVICE_INFO_MODEL_NUMBER_STRING)?.let {
            commandsQueue.add(object : PieceOfWork {
                override val name = "readCharacteristic"
                override fun execute() {
                    readCharacteristic(it)
                }
            })
        }
    }

    private suspend fun reconnect() {
        Logger.log("Reconnecting...")
        connectedAddress?.let { adrs ->
            connectedAddress = null
            connectToDevice(adrs)
            while (!servicesReady || !notificationReady || readHandle == null || writeHandle == null) {
                delay(50)
            }
            Logger.log("Connected and ready")
        }
    }

    @SuppressLint("MissingPermission")
    private fun subscribeNotifyForReadHandle() {
        Logger.log("subscribeNotify")
        if (!servicesReady) {
            Logger.log("Services not ready")
            return
        }

        readHandle?.let {
            // Turn notification ON

            // Get the CCC Descriptor for the characteristic
            val descriptor = it.getDescriptor(GattAttributes.CCC_DESCRIPTOR_UUID)
            if (descriptor == null) {
                Logger.log("Cannot get CCC descriptor for characteristic ${it.uuid}")
                return
            }

            if (!it.canNotify()) {
                Logger.log("Cannot notify")
                return
            }

            commandsQueue.add(object : PieceOfWork {
                override val name = "setup notify"
                override fun execute() {
                    if (bluetoothGatt?.setCharacteristicNotification(
                            descriptor.characteristic,
                            true
                        ) == false
                    ) {
                        Logger.log("Cannot notify characteristic: ${it.uuid}")
                        return
                    }

                    if (!descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
                        Logger.log("Cannot notify characteristic, descriptor error: ${it.uuid}")
                        return
                    }

                    if (bluetoothGatt?.writeDescriptor(descriptor) == false) {
                        Logger.log(
                            "Cannot notify characteristic writeDescriptor error: ${it.uuid}"
                        )
                        return
                    }
                    Logger.log("Notify subscribed for characteristic: ${it.uuid}")
                }
            })
        }
    }

    @SuppressLint("MissingPermission")
    private fun readCharacteristic(characteristic: BluetoothGattCharacteristic) {
        Logger.log("Reading characteristic: ${characteristic.instanceId}")

        bluetoothGatt?.readCharacteristic(characteristic)
    }

    @SuppressLint("MissingPermission")
    private fun writeCharacteristic(
        characteristic: BluetoothGattCharacteristic,
        data: ByteArray
    ) {
        Logger.log("Write characteristic: ${characteristic.uuid}: ${data.size} bytes")
        characteristic.value = data
        characteristic.writeType = WRITE_TYPE_NO_RESPONSE
        bluetoothGatt?.writeCharacteristic(characteristic)
    }

    private fun logService(service: BluetoothGattService) {
        val serviceType = service.type
        val serviceId = service.instanceId
        val serviceUUID = service.uuid
        Logger.log("serviceUUID:$serviceUUID, serviceType:$serviceType, id:$serviceId")
        service.characteristics.forEach { characteristic ->
            val characteristicUUID = characteristic.uuid
            val canWrite = characteristic.isWritable()
            val canRead = characteristic.isReadable()
            val canNotify = characteristic.canNotify()
            val id = characteristic.instanceId
            Logger.log(
                "--- characteristicUUID:$characteristicUUID, id:$id, " +
                        "canWrite:$canWrite, " +
                        "canRead:$canRead, " +
                        "canNotify:$canNotify"
            )
        }
    }

    private fun logConnectionState(status: Int, newState: Int) {
        Logger.log("onConnectionStateChange, status:$status, newState:$newState")
        Logger.log("status:")
        when (status) {
            BluetoothGatt.GATT_SUCCESS -> Logger.log("GATT_SUCCESS")
            BluetoothGatt.GATT_FAILURE -> Logger.log("GATT_FAILURE")
            BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION -> Logger.log("GATT_INSUFFICIENT_AUTHENTICATION")
            BluetoothGatt.GATT_CONNECTION_CONGESTED -> Logger.log("GATT_CONNECTION_CONGESTED")
            BluetoothGatt.GATT_INSUFFICIENT_ENCRYPTION -> Logger.log("GATT_INSUFFICIENT_ENCRYPTION")
            BluetoothGatt.GATT_SERVER -> Logger.log("GATT_SERVER")
            BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED -> Logger.log("GATT_REQUEST_NOT_SUPPORTED")
            BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH -> Logger.log("GATT_INVALID_ATTRIBUTE_LENGTH")
            BluetoothGatt.GATT_INVALID_OFFSET -> Logger.log("GATT_INVALID_OFFSET")
            BluetoothGatt.GATT_READ_NOT_PERMITTED -> Logger.log("GATT_READ_NOT_PERMITTED")
            BluetoothGatt.GATT_WRITE_NOT_PERMITTED -> Logger.log("GATT_WRITE_NOT_PERMITTED")
            8 -> Logger.log("GATT_CONN_TIMEOUT")
            19 -> Logger.log("GATT_CONN_TERMINATE_PEER_USER")
            22 -> Logger.log("GATT_CONN_TERMINATE_LOCAL_HOST")
            62 -> Logger.log("GATT_CONN_FAIL_ESTABLISH")
            34 -> Logger.log("GATT_CONN_LMP_TIMEOUT")
            256 -> Logger.log("GATT_CONN_CANCEL")

            else -> Logger.log("Unknown GATT status: $status")
        }

        Logger.log("newState:")
        when (newState) {
            BluetoothProfile.STATE_CONNECTED -> Logger.log("STATE_CONNECTED")
            BluetoothProfile.STATE_DISCONNECTED -> Logger.log("STATE_DISCONNECTED")
            BluetoothProfile.STATE_CONNECTING -> Logger.log("STATE_CONNECTING")
            BluetoothProfile.STATE_DISCONNECTING -> Logger.log("STATE_DISCONNECTING")

            else -> Logger.log("Unknown GATT new state: $newState")
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothLeService {
            return this@BluetoothLeService
        }
    }


}