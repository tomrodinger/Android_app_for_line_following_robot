package com.hani.btapp.core

import android.bluetooth.BluetoothManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by hanif on 2022-07-30.
 */
@Singleton
class BluetoothProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BluetoothProvider {

    override val bluetoothManager: BluetoothManager
        get() = context.getSystemService(BluetoothManager::class.java)

}