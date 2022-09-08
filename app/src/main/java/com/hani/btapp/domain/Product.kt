package com.hani.btapp.domain

import kotlinx.serialization.Serializable

/**
 * Created by hanif on 2022-08-31.
 */
@Serializable
data class Product(
    val model: String,
    val picture: String,
    val firmwares: List<Firmware>,
)