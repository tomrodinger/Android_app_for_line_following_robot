package com.hani.btapp.data.firmware.remote

import android.content.Context
import com.hani.btapp.data.NetworkClient
import com.hani.btapp.domain.Product
import com.hani.btapp.domain.ProductResponse
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by hanif on 2022-08-09.
 */
@Singleton
class RemoteDataSourceImpl @Inject constructor(
    // Add a network client e.g. ktor

    @ApplicationContext private val context: Context
) : RemoteDataSource {

    private val client = NetworkClient.client

    override suspend fun fetchFirmwareData(firmwareFileName: String): Result<ByteArray> {
        val response = client.get {
            url {
                protocol = URLProtocol.HTTP
                host = "9o.at"
                path(firmwareFileName)
            }
        }
        return when (val status = response.status) {
            HttpStatusCode.OK -> {
                Result.success(response.readBytes())
            }
            else -> {
                Result.failure(Exception("Could not fetch firmware file: $firmwareFileName." +
                        " status: $status"))
            }
        }
    }

    override suspend fun fetchAvailableFirmwares(): Result<Product> {
        val response = client.get {
            contentType(ContentType.Application.Json)
            url {
                protocol = URLProtocol.HTTP
                host = "9o.at"
                path("firmware_list.json")
            }
        }
        return when (val status = response.status) {
            HttpStatusCode.OK -> {
                val res: ProductResponse = response.body()
                Result.success(res.product)
            }
            else -> {
                Result.failure(Exception("Could not fetch available firmwares. status: $status"))
            }
        }
    }


}