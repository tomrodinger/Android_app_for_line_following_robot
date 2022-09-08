package com.hani.btapp.core.service

/**
 * Created by hanif on 2022-08-22.
 */
interface RobotStateListener {
    fun onDisconnected(userInitiated: Boolean)
    fun onDataWritten()
    fun onResponse(data: ByteArray)
}