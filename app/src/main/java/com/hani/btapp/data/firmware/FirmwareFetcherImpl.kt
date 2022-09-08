package com.hani.btapp.data.firmware

import com.hani.btapp.data.firmware.remote.RemoteDataSource
import com.hani.btapp.domain.Product
import javax.inject.Inject

/**
 * Created by hanif on 2022-08-09.
 */
class FirmwareFetcherImpl @Inject constructor(
    private val remote: RemoteDataSource,
) : FirmwareFetcher {

    private var availableFirmwaresCache: Product? = null
    private var firmwareDataCache = HashMap<String, ByteArray>()

    override suspend fun fetchFirmware(name: String): Result<ByteArray> {
        return remote.fetchFirmwareData(name)
    }

    override suspend fun getFirmware(name: String): Result<ByteArray> {
        val cache = firmwareDataCache[name]
        if (cache == null) {
            val res = fetchFirmware(name)
            return when {
                res.isSuccess -> {
                    res.getOrNull()?.let {
                        firmwareDataCache[name] = it
                        res
                    } ?: kotlin.run {
                        res
                    }
                }
                else -> res
            }
        } else {
            return Result.success(cache)
        }
    }

    override suspend fun fetchAvailableFirmwares(): Result<Product> {
        availableFirmwaresCache?.let { availableFirmwaresCache ->
            return Result.success(availableFirmwaresCache)
        }
        val res = remote.fetchAvailableFirmwares()
        return when {
            res.isSuccess -> {
                res.getOrNull()?.let {
                    availableFirmwaresCache = it
                    Result.success(it)
                } ?: kotlin.run {
                    Result.failure(Exception("Firmwares list empty"))
                }
            }
            else -> {
                Result.failure(Exception("Firmwares list empty"))
            }
        }
    }

}