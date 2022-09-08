package com.hani.btapp.utils

/**
 * Created by hanif on 2022-08-05.
 */

fun ByteArray.toHex(): String =
    joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }

fun ByteArray.toInt(): Int = sumOf { byte -> byte.toUByte().toInt() }

fun Byte.toHex(): String = "%02x".format(this)

fun Int.toBytes(numberOfBytes: Int): ByteArray {
    val buffer = ByteArray(numberOfBytes)
    for (i in 0 until numberOfBytes) buffer[i] = (this shr (i * 8)).toByte()
    return buffer
}

fun Long.toBytes(numberOfBytes: Int): ByteArray {
    val buffer = ByteArray(numberOfBytes)
    for (i in 0 until numberOfBytes) buffer[i] = (this shr (i * 8)).toByte()
    return buffer
}

fun ByteArray.toHexString() = this.joinToString(" ") { it.toHex() }