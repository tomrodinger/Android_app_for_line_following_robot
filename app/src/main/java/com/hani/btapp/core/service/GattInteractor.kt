package com.hani.btapp.core.service

/**
 * Created by hanif on 2022-08-22.
 */
interface GattInteractor {
    fun writeData(data: ByteArray)
    suspend fun connectIfNeeded()
    suspend fun reconnect(delayAfterDisconnectMs: Long)
}