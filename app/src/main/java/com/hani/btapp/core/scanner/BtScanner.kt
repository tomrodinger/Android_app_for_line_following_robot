package com.hani.btapp.core.scanner

/**
 * Created by hanif on 2022-08-03.
 */
interface BtScanner {
    fun startScan()
    fun stopScan(immediate: Boolean = true)
}