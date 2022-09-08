package com.hani.btapp.data.firmware

import com.hani.btapp.domain.Product

/**
 * Created by hanif on 2022-08-09.
 */
interface FirmwareFetcher {
    suspend fun fetchFirmware(name: String): Result<ByteArray>
    suspend fun getFirmware(name: String): Result<ByteArray>
    suspend fun fetchAvailableFirmwares(): Result<Product>
}