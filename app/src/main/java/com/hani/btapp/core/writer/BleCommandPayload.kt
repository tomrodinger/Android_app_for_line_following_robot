package com.hani.btapp.core.writer

import com.hani.btapp.utils.toBytes

/**
 * Created by hanif on 2022-08-10.
 */

const val BFLB_EFLASH_LOADER_CMD_RESET = 0x21
const val BFLB_EFLASH_LOADER_CMD_FLASH_ERASE = 0x30
const val BFLB_EFLASH_LOADER_CMD_FLASH_WRITE = 0x31

const val FLASH_START_ADDRESS = 0x3F000

interface BleCommandPayload {

    fun get(): ByteArray

    fun createPayload(command: Int, data: ByteArray): ByteArray {
        var checksum = 0
        data.forEach { item ->
            checksum += item
        }
        checksum += data.size and 0xFF
        checksum += (data.size shr 8) and 0xFF
        checksum = checksum and 0XFF
        return byteArrayOf(command.toByte()) +
                checksum.toBytes(1) +
                data.size.toBytes(2) +
                data
    }
}

class EraseFlashCommandPayload(private val size: Int): BleCommandPayload {
    override fun get(): ByteArray {
        var newSize = size + FLASH_START_ADDRESS
        val data = FLASH_START_ADDRESS.toBytes(4) + newSize.toBytes(4)
        return createPayload(BFLB_EFLASH_LOADER_CMD_FLASH_ERASE, data)
    }
}

class ProgramOnePageCommandPayload(private val byteArray: ByteArray) : BleCommandPayload {
    override fun get(): ByteArray {
        val data = 0.toBytes(4) + byteArray
        return createPayload(BFLB_EFLASH_LOADER_CMD_FLASH_WRITE, data)
    }
}

class SystemResetCommandPayload : BleCommandPayload {
    override fun get(): ByteArray {
        val data = 0.toBytes(1)
        return createPayload(BFLB_EFLASH_LOADER_CMD_RESET, data)
    }
}