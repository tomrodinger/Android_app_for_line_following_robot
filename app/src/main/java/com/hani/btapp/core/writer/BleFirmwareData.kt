package com.hani.btapp.core.writer

import com.hani.btapp.Logger

/**
 * Created by hanif on 2022-08-08.
 */

private const val FLASH_PAGE_SIZE = 4096

class BleFirmwareData(private val firmwareData: ByteArray) {

    val totalBytes = firmwareData.size

    var bytesRead = 0
       private set

    private var nextPageIndex = 0

    var isReady = false
       private set

    fun reset() {
        nextPageIndex = 0
        bytesRead = 0
        isReady = false
    }

    fun getNextChunk(): ByteArray? {
        if (nextPageIndex >= totalBytes) {
            return null
        }
        var startIndex = nextPageIndex
        var endIndex = nextPageIndex + FLASH_PAGE_SIZE
        if (endIndex > totalBytes) {
            endIndex = totalBytes
        }
        return try {
            val nextChunk = firmwareData.copyOfRange(startIndex, endIndex)
            bytesRead += nextChunk.size
            Logger.log("getNextChunk: [$startIndex, $endIndex], " +
                    "totalSize: $totalBytes, chunkSize: ${nextChunk.size}")
            nextPageIndex += FLASH_PAGE_SIZE
            nextChunk
        } catch (e: Exception) {
            null
        }
    }

}