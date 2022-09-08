package com.hani.btapp.core.writer

import com.hani.btapp.utils.toBytes

/**
 * Created by hanif on 2022-08-07.
 */
class RobotData(private val data: ByteArray) {

    private val size = data.size
    private var bytesProcessed = 0

    private var defaultPacketSizeBytes = 240
    var remainingBytes = size
       private set

    fun getNextChunk(): ByteArray? {
        var bytesToProcess = defaultPacketSizeBytes
        remainingBytes = size - bytesProcessed
        if (remainingBytes <= 0) {
            return null
        }
        if (remainingBytes < defaultPacketSizeBytes) {
            bytesToProcess = remainingBytes
        }
        val startByte = if (remainingBytes <= 240) {
            0.toBytes(1)
        } else {
            1.toBytes(1)
        }
        val nextPacket = startByte + data.copyOfRange(
            fromIndex = bytesProcessed,
            toIndex = bytesProcessed + bytesToProcess
        )
        bytesProcessed += bytesToProcess
        return nextPacket
    }

    fun reset() {
        remainingBytes = size
    }

}