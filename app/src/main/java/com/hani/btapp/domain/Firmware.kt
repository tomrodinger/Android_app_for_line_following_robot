package com.hani.btapp.domain

import kotlinx.serialization.Serializable

/**
 * Created by hanif on 2022-08-31.
 */
@Serializable
data class Firmware(
    val version: String,
    val compatibility: Int,
    val date: String,
    val description: String,
    val url: String,
    val sha256: String,
)