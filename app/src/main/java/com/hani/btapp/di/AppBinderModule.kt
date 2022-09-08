package com.hani.btapp.di

import com.hani.btapp.core.com.CommunicationScope
import com.hani.btapp.core.com.CommunicationScopeImpl
import com.hani.btapp.core.BluetoothProvider
import com.hani.btapp.core.BluetoothProviderImpl
import com.hani.btapp.core.service.*
import com.hani.btapp.data.firmware.FirmwareFetcher
import com.hani.btapp.data.firmware.FirmwareFetcherImpl
import com.hani.btapp.data.firmware.remote.RemoteDataSource
import com.hani.btapp.data.firmware.remote.RemoteDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Created by hanif on 2022-07-30.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppBinderModule {

    @Binds
    abstract fun bindCommunicationScope(
        communicationScopeImpl: CommunicationScopeImpl
    ): CommunicationScope

    @Binds
    abstract fun bindBluetoothProvider(
        impl: BluetoothProviderImpl
    ): BluetoothProvider

    @Binds
    abstract fun bindRemoteDateSource(
        impl: RemoteDataSourceImpl
    ): RemoteDataSource

    @Binds
    abstract fun bindFirmwareFetcher(
        impl: FirmwareFetcherImpl
    ): FirmwareFetcher

    @Binds
    abstract fun bindGattStateListener(
        impl: FirmwareUpdateHandler
    ): RobotStateListener

    @Binds
    abstract fun bindFirmwareStepsHandler(
        impl: FirmwareUpdateHandler
    ) : FirmwareStepsHandler

}